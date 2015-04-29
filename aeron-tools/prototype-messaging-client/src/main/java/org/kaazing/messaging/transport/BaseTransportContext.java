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
package org.kaazing.messaging.transport;

import org.kaazing.messaging.client.Message;
import org.kaazing.messaging.destination.MessageFlow;
import org.kaazing.messaging.discovery.DiscoveryService;
import org.kaazing.messaging.discovery.InterestListChangedEvent;
import uk.co.real_logic.agrona.concurrent.AtomicArray;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class BaseTransportContext implements TransportContext
{
    private DiscoveryService discoveryService;
    protected final AtomicArray<ReceivingTransport> receivingTransports = new AtomicArray<ReceivingTransport>();

    @Override
    public List<SendingTransport> createSendingTransports(MessageFlow messageFlow)
    {
        List<SendingTransport> sendingTransports = null;
        if(messageFlow.isDiscoveryRequired())
        {
            if (discoveryService != null)
            {
                sendingTransports = TransportFactory.discoverSendingTransports(this, messageFlow, discoveryService);
            }
            else
            {
                throw new UnsupportedOperationException("Cannot resolve sending transports since discovery service is null");
            }
        }
        else
        {
            sendingTransports = new ArrayList<SendingTransport>();
            //Create a single sending transport if possible when there is no discovery service
            SendingTransport sendingTransport = TransportFactory.createSendingTransport(this, messageFlow);
            sendingTransports.add(sendingTransport);
        }
        return sendingTransports;
    }

    @Override
    public SendingTransport createSendingTransportFromHandle(TransportHandle transportHandle)
    {
        return TransportFactory.createSendingTransportFromHandle(this, transportHandle);

    }

    @Override
    public List<ReceivingTransport> createReceivingTransports(MessageFlow messageFlow, Consumer<Message> messageHandler)
    {
        List<ReceivingTransport> receivingTransports = new ArrayList<ReceivingTransport>();
        ReceivingTransport receivingTransport = TransportFactory.createReceivingTransport(this, messageFlow, messageHandler);
        receivingTransports.add(receivingTransport);
        if(discoveryService != null)
        {
            discoveryService.register(messageFlow, receivingTransport.getHandle());
        }
        if (receivingTransport.isPollable())
        {
            addPollableReceivingTransport(receivingTransport);
        }
        return receivingTransports;
    }

    @Override
    public void unregisterReceivingTransport(MessageFlow messageFlow, ReceivingTransport receivingTransport)
    {
        if(discoveryService != null)
        {
            discoveryService.unregister(messageFlow, receivingTransport.getHandle());
        }
    }

    @Override
    public void setDiscoveryService(DiscoveryService discoveryService)
    {
        this.discoveryService = discoveryService;
    }

    @Override
    public void addInterestListChangedListener(MessageFlow messageFlow, Consumer<InterestListChangedEvent> listener)
    {
        if(discoveryService != null)
        {
            discoveryService.addInterestListChangedListener(messageFlow, listener);
        }
    }

    public void addPollableReceivingTransport(ReceivingTransport receivingTransport)
    {
        synchronized (receivingTransports)
        {
            receivingTransports.add(receivingTransport);
        }
    }

    public void removePollableReceivingTransport(ReceivingTransport receivingTransport)
    {
        synchronized (receivingTransports)
        {
            receivingTransports.remove(receivingTransport);
        }
    }

    @Override
    public abstract void close();


}
