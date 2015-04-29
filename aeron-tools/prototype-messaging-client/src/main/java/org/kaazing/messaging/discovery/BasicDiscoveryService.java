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

import org.kaazing.messaging.destination.MessageFlow;
import org.kaazing.messaging.transport.TransportHandle;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class BasicDiscoveryService implements DiscoveryService
{
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private Map<String, CopyOnWriteArrayList<TransportHandle>> interestMap = new HashMap<>();

    @Override
    public List<TransportHandle> getInterestList(MessageFlow messageFlow)
    {
        return interestMap.get(messageFlow.getLogicalName());
    }

    @Override
    public void addInterestListChangedListener(MessageFlow messageFlow, Consumer<InterestListChangedEvent> listener)
    {
        //TODO(JAF): Do nothing since there are no dynamic change events
    }

    @Override
    public void register(MessageFlow messageFlow, TransportHandle handle)
    {
        readWriteLock.writeLock().lock();
        try
        {
            CopyOnWriteArrayList<TransportHandle> interestList = interestMap.get(messageFlow.getLogicalName());
            if(interestList == null)
            {
                interestList = new CopyOnWriteArrayList<TransportHandle>();
                interestMap.put(messageFlow.getLogicalName(), interestList);
            }
            //TODO(JAF): Need to avoid dups in the list
            interestList.add(handle);
        }
        finally
        {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public void unregister(MessageFlow messageFlow, TransportHandle handle)
    {
        readWriteLock.writeLock().lock();
        try
        {
            CopyOnWriteArrayList<TransportHandle> interestList = interestMap.get(messageFlow.getLogicalName());
            if(interestList != null)
            {
                //TODO(JAF): Need to avoid dups in the list
                interestList.remove(handle);
            }
        }
        finally
        {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public BasicDiscoveryService start()
    {
        return this;
    }

    @Override
    public BasicDiscoveryService stop()
    {
        return this;
    }
}
