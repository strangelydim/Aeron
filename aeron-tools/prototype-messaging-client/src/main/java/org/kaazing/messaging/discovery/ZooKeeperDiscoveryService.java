/*
    Copyright 2015 Kaazing Corporation

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
package org.kaazing.messaging.discovery;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.kaazing.messaging.destination.MessageFlow;
import org.kaazing.messaging.transport.TransportHandle;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.KeeperException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ZooKeeperDiscoveryService implements DiscoveryService
{
    private static final String PATH = "/kaazing/topics";
    private final String connectionString;
    private final CuratorFramework client;
    private TreeCache cache = null;
    private final Gson gson;
    private final Map<String, List<Consumer<InterestListChangedEvent>>> listeners = new ConcurrentHashMap<>();

    public ZooKeeperDiscoveryService(String connectionString)
    {
        this.connectionString = connectionString;
        client = CuratorFrameworkFactory.newClient(this.connectionString, new ExponentialBackoffRetry(1000, 3));
        GsonBuilder builder = new GsonBuilder();
        gson = builder.create();
    }

    @Override
    public ZooKeeperDiscoveryService start()
    {
        try
        {
            client.start();

            // in this example we will cache data. Notice that this is optional.
            cache = new TreeCache(client, PATH);

            //Use async POST_INITIALIZED_EVENT to do the buildling in the background
            //cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
            cache.start();

            final AtomicBoolean initialized = new AtomicBoolean(false);

            TreeCacheListener listener = new TreeCacheListener()
            {
                @Override
                public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception
                {
                    switch ( event.getType() )
                    {
                        case NODE_ADDED:
                        {
                            System.out.println("Node added: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
                            String logicalName = event.getData().getPath().split("__")[1];

                            List<Consumer<InterestListChangedEvent>> messageFlowListeners = listeners.get(logicalName);
                            if(messageFlowListeners != null)
                            {
                                ChildData childData = event.getData();
                                byte[] bytes = childData.getData();
                                String jsonStr = new String(bytes, StandardCharsets.UTF_8);
                                TransportHandle transportHandle = gson.fromJson(jsonStr, TransportHandle.class);
                                List<TransportHandle> added = new ArrayList<>();
                                added.add(transportHandle);
                                InterestListChangedEvent interestListChangedEvent = new InterestListChangedEvent(added, null, null);
                                for(Consumer<InterestListChangedEvent> eventListener : messageFlowListeners)
                                {
                                    eventListener.accept(interestListChangedEvent);
                                }
                            }
                            break;
                        }

                        case NODE_UPDATED:
                        {
                            System.out.println("Node changed: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
                            String logicalName = event.getData().getPath().split("__")[1];

                            List<Consumer<InterestListChangedEvent>> messageFlowListeners = listeners.get(logicalName);
                            if(messageFlowListeners != null)
                            {
                                ChildData childData = event.getData();
                                byte[] bytes = childData.getData();
                                String jsonStr = new String(bytes, StandardCharsets.UTF_8);
                                TransportHandle transportHandle = gson.fromJson(jsonStr, TransportHandle.class);
                                List<TransportHandle> updated = new ArrayList<>();
                                updated.add(transportHandle);
                                InterestListChangedEvent interestListChangedEvent = new InterestListChangedEvent(null, updated, null);
                                for(Consumer<InterestListChangedEvent> eventListener : messageFlowListeners)
                                {
                                    eventListener.accept(interestListChangedEvent);
                                }
                            }
                            break;
                        }

                        case NODE_REMOVED:
                        {
                            System.out.println("Node removed: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
                            String logicalName = event.getData().getPath().split("__")[1];

                            List<Consumer<InterestListChangedEvent>> messageFlowListeners = listeners.get(logicalName);
                            if(messageFlowListeners != null)
                            {
                                ChildData childData = event.getData();
                                byte[] bytes = childData.getData();
                                String jsonStr = new String(bytes, StandardCharsets.UTF_8);
                                TransportHandle transportHandle = gson.fromJson(jsonStr, TransportHandle.class);
                                List<TransportHandle> removed = new ArrayList<>();
                                removed.add(transportHandle);
                                InterestListChangedEvent interestListChangedEvent = new InterestListChangedEvent(null, null,removed);
                                for(Consumer<InterestListChangedEvent> eventListener : messageFlowListeners)
                                {
                                    eventListener.accept(interestListChangedEvent);
                                }
                            }
                            break;
                        }

                        case INITIALIZED:
                        {
                            System.out.println("Cache initialized");
                            initialized.set(true);
                            break;
                        }

                        //TODO(JAF): Handle disconnect
                    }
                }
            };
            cache.getListenable().addListener(listener);
            while(initialized.get() != true)
            {
                System.out.println("Waiting for cache to be initialized...");
                Thread.sleep(100);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public ZooKeeperDiscoveryService stop()
    {
        CloseableUtils.closeQuietly(cache);
        CloseableUtils.closeQuietly(client);
        return this;
    }

    @Override
    public List<TransportHandle> getInterestList(MessageFlow messageFlow)
    {
        //TODO(JAF): Add support for more than one node on a single topic
        List<TransportHandle> interestList = new ArrayList<TransportHandle>();

        Map<String, ChildData> childDataMap = cache.getCurrentChildren(PATH + ZKPaths.PATH_SEPARATOR + messageFlow.getLogicalName());
        if(childDataMap != null)
        {
            for(ChildData childData : childDataMap.values())
            {
                byte[] bytes = childData.getData();
                String jsonStr = new String(bytes, StandardCharsets.UTF_8);
                TransportHandle transportHandle = gson.fromJson(jsonStr, TransportHandle.class);
                interestList.add(transportHandle);
            }
        }

        return interestList;
    }

    @Override
    public void addInterestListChangedListener(MessageFlow messageFlow, Consumer<InterestListChangedEvent> listener)
    {
        List<Consumer<InterestListChangedEvent>> messageFlowListeners = listeners.get(messageFlow.getLogicalName());
        if(messageFlowListeners == null)
        {
            messageFlowListeners = new CopyOnWriteArrayList<Consumer<InterestListChangedEvent>>();
            if(listeners.putIfAbsent(messageFlow.getLogicalName(), messageFlowListeners) != null)
            {
                messageFlowListeners = listeners.get(messageFlow.getLogicalName());
            }
        }
        messageFlowListeners.add(listener);
    }

    @Override
    public void register(MessageFlow messageFlow, TransportHandle handle)
    {
        //TODO(JAF): Add support for more than one node on a single topic
        String path = ZKPaths.makePath(PATH + ZKPaths.PATH_SEPARATOR +  messageFlow.getLogicalName(), handle.getId() + "__" + messageFlow.getLogicalName());
        byte[] bytes = gson.toJson(handle).getBytes(StandardCharsets.UTF_8);
        try
        {
            client.setData().forPath(path, bytes);
        }
        catch (KeeperException.NoNodeException e)
        {
            try
            {
                //Try creating the parents if needed
                client.create().creatingParentsIfNeeded().forPath(path, bytes);
            }
            catch (Exception ex)
            {
                //TODO(JAF): Handle registration exception
                ex.printStackTrace();
            }
        }
        catch (Exception ex)
        {
            //TODO(JAF): Handle registration exception
            ex.printStackTrace();
        }

    }

    @Override
    public void unregister(MessageFlow messageFlow, TransportHandle handle)
    {
        //TODO(JAF): Add support for more than one node on a single topic
        String path = ZKPaths.makePath(PATH + ZKPaths.PATH_SEPARATOR +  messageFlow.getLogicalName(), handle.getId() + "__" + messageFlow.getLogicalName());
        try
        {
            client.delete().forPath(path);
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

}
