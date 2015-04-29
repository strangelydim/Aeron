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
import org.kaazing.messaging.destination.Pipe;
import org.kaazing.messaging.discovery.DiscoveryService;
import org.kaazing.messaging.transport.aeron.AeronSendingTransport;
import org.kaazing.messaging.transport.aeron.AeronReceivingTransport;
import org.kaazing.messaging.transport.aeron.AeronTransportContext;
import org.kaazing.messaging.transport.amqp.AmqpProtonReceivingTransport;
import org.kaazing.messaging.transport.amqp.AmqpProtonSendingTransport;
import org.kaazing.messaging.transport.amqp.AmqpProtonTransportContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TransportFactory
{
    public static SendingTransport createSendingTransport(BaseTransportContext context, MessageFlow messageFlow)
    {
        if(context instanceof AeronTransportContext)
        {
            if (messageFlow instanceof Pipe)
            {
                Pipe pipe = (Pipe) messageFlow;
                return new AeronSendingTransport((AeronTransportContext) context, pipe.getLogicalName(), pipe.getStreamId());
            }
            else
            {
                throw new UnsupportedOperationException("Not yet supporting this type of message flow");
            }
        }
        else if(context instanceof AmqpProtonTransportContext)
        {
            if (messageFlow instanceof Pipe)
            {
                Pipe pipe = (Pipe) messageFlow;
                return new AmqpProtonSendingTransport((AmqpProtonTransportContext) context, pipe.getLogicalName());
            }
            else
            {
                throw new UnsupportedOperationException("Not yet supporting this type of message flow");
            }
        }
        else
        {
            throw new UnsupportedOperationException("Not yet supporting this type of transport context");
        }
    }

    public static ReceivingTransport createReceivingTransport(BaseTransportContext context, MessageFlow messageFlow, Consumer<Message> messageHandler)
    {
        ReceivingTransport receivingTransport = null;
        if(context instanceof AeronTransportContext)
        {
            if (messageFlow instanceof Pipe)
            {
                Pipe pipe = (Pipe) messageFlow;
                receivingTransport = new AeronReceivingTransport((AeronTransportContext) context, pipe.getLogicalName(), pipe.getStreamId(), messageHandler);
            }
            else if(messageFlow.isDiscoveryRequired())
            {
                receivingTransport = new AeronReceivingTransport((AeronTransportContext) context, AeronTransportContext.DEFAULT_AERON_SUBSCRIPTION_CHANNEL, AeronTransportContext.globalStreamIdCtr.getAndIncrement(), messageHandler);
            }
            else
            {
                throw new UnsupportedOperationException("Not yet supporting this type of message flow");
            }
        }
        else if(context instanceof AmqpProtonTransportContext)
        {
            if (messageFlow instanceof Pipe)
            {
                Pipe pipe = (Pipe) messageFlow;
                receivingTransport = new AmqpProtonReceivingTransport((AmqpProtonTransportContext) context, pipe.getLogicalName(), messageHandler);
            }
            else if(messageFlow.isDiscoveryRequired())
            {
                receivingTransport = new AmqpProtonReceivingTransport((AmqpProtonTransportContext) context, AmqpProtonTransportContext.DEFAULT_AMQP_SUBSCRIPTION_ADDRESS, messageHandler);
            }
            else
            {
                throw new UnsupportedOperationException("Not yet supporting this type of message flow");
            }
        }
        else
        {
            throw new UnsupportedOperationException("Not yet supporting this type of transport context");
        }

        return receivingTransport;
    }

    public static List<SendingTransport> discoverSendingTransports(BaseTransportContext context, MessageFlow messageFlow, DiscoveryService discoveryService)
    {
        List<SendingTransport> sendingTransports = new ArrayList<SendingTransport>();
        if(context instanceof AeronTransportContext)
        {
            List<TransportHandle> transportHandles = discoveryService.getInterestList(messageFlow);
            for(TransportHandle transportHandle : transportHandles)
            {
                if(transportHandle.getType() == TransportHandle.Type.Aeron)
                {
                    String channel = transportHandle.getPhysicalAddress();
                    int streamId = 0;
                    String streamIdStr = transportHandle.getExtras().get("streamId");
                    if(streamIdStr != null)
                    {
                        streamId = Integer.parseInt(streamIdStr);
                    }
                    sendingTransports.add(new AeronSendingTransport((AeronTransportContext) context, channel, streamId));
                }
                else
                {
                    //Cannot create a sending transport using this transport context
                }
            }
        }
        else if(context instanceof AmqpProtonTransportContext)
        {
            List<TransportHandle> transportHandles = discoveryService.getInterestList(messageFlow);
            for(TransportHandle transportHandle : transportHandles)
            {
                if(transportHandle.getType() == TransportHandle.Type.AMQP)
                {
                    String address = transportHandle.getPhysicalAddress();
                    sendingTransports.add(new AmqpProtonSendingTransport((AmqpProtonTransportContext) context, address));
                }
                else
                {
                    //Cannot create a sending transport using this transport context
                }
            }
        }
        else
        {
            throw new UnsupportedOperationException("Not yet supporting this type of transport context");
        }
        return sendingTransports;
    }

    public static SendingTransport createSendingTransportFromHandle(BaseTransportContext context, TransportHandle transportHandle)
    {
        SendingTransport sendingTransport = null;
        if(context instanceof AeronTransportContext)
        {
            if(transportHandle.getType() == TransportHandle.Type.Aeron)
            {
                String channel = transportHandle.getPhysicalAddress();
                int streamId = 0;
                String streamIdStr = transportHandle.getExtras().get("streamId");
                if(streamIdStr != null)
                {
                    streamId = Integer.parseInt(streamIdStr);
                }
                sendingTransport = new AeronSendingTransport((AeronTransportContext) context, channel, streamId);
                sendingTransport.setRemoteId(transportHandle.getId());
            }
            else
            {
                //Cannot create a sending transport using this transport context
            }

        }
        else if(context instanceof AmqpProtonTransportContext)
        {
            if(transportHandle.getType() == TransportHandle.Type.AMQP)
            {
                String address = transportHandle.getPhysicalAddress();
                sendingTransport = new AmqpProtonSendingTransport((AmqpProtonTransportContext) context, address);
                sendingTransport.setRemoteId(transportHandle.getId());
            }
            else
            {
                //Cannot create a sending transport using this transport context
            }
        }
        else
        {
            throw new UnsupportedOperationException("Not yet supporting this type of transport context");
        }
        return sendingTransport;
    }
}
