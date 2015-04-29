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
package org.kaazing.messaging.client;

import org.kaazing.messaging.destination.MessageFlow;
import org.kaazing.messaging.discovery.InterestListChangedEvent;
import org.kaazing.messaging.transport.*;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToLongBiFunction;

public class MessageProducer
{
    private final MessageFlow messageFlow;
    private TransportList<SendingTransport, Message> transports = new TransportList<SendingTransport, Message>();
    private final TransportContext context;
    private final ToLongBiFunction<SendingTransport, Message> submitAction = new ToLongBiFunction<SendingTransport, Message>()
    {
        @Override
        public long applyAsLong(SendingTransport sendingTransport, Message message)
        {
            sendingTransport.submit(message);
            return 0;
        }
    };

    private final ToLongBiFunction<SendingTransport, Message> offerAction = new ToLongBiFunction<SendingTransport, Message>()
    {
        @Override
        public long applyAsLong(SendingTransport sendingTransport, Message message)
        {
            return sendingTransport.offer(message);
        }
    };

    private final Predicate<SendingTransport> sendingTransportMatch = new Predicate<SendingTransport>()
    {
        @Override
        public boolean test(SendingTransport sendingTransport)
        {
            return false;
        }
    };

    private final Consumer<InterestListChangedEvent> interestListConsumer = new Consumer<InterestListChangedEvent>()
    {
        @Override
        public void accept(InterestListChangedEvent interestListChangedEvent)
        {
            List<TransportHandle> added = interestListChangedEvent.added;
            List<TransportHandle> removed = interestListChangedEvent.removed;

            //Handle updated later if necessary
            List<TransportHandle> updated = interestListChangedEvent.updated;


            if(added != null)
            {
                for(int i = 0; i < added.size(); i++)
                {
                    TransportHandle transportHandle = added.get(i);
                    if(transportHandle != null)
                    {
                        final String remoteId = transportHandle.getId();
                        SendingTransport match = transports.findFirst(
                                (sendingTransport) -> remoteId.equals(sendingTransport.getRemoteId())
                        );

                        if(match == null)
                        {
                            transports.lockedAdd(context.createSendingTransportFromHandle(transportHandle));
                        }
                    }
                }
            }

            if(removed != null)
            {
                for(int i = 0; i < removed.size(); i++)
                {
                    TransportHandle transportHandle = removed.get(i);
                    if(transportHandle != null)
                    {
                        final String remoteId = transportHandle.getId();
                        if(remoteId != null)
                        {
                            transports.lockedRemove(
                                    (sendingTransport) -> remoteId.equals(sendingTransport.getRemoteId())
                            );
                        }
                    }
                }
            }
        }
    };

    public MessageProducer(BaseTransportContext context, MessageFlow messageFlow)
    {
        this.messageFlow = messageFlow;
        this.transports.lockedAddAll(context.createSendingTransports(messageFlow));
        this.context = context;

        //Hook up a listener to populate the transport list dynamically with the discovery service
        context.addInterestListChangedListener(messageFlow, interestListConsumer);
    }

    public MessageFlow getMessageFlow()
    {
        return messageFlow;
    }

    /**
     * Blocking call to submit a message
     * @param message
     */
    public void submit(Message message)
    {
        transports.doActionWithArg(0, submitAction, message);
    }

    /**
     * Non-blocking call to submit a message
     * @param message
     * @return true if successful or false otherwise
     */
    public boolean offer(Message message)
    {
        return transports.doActionWithArgToBoolean(0, offerAction, message);
    }


    /**
     * Removes all sending transports from the message producer and closes them
     */
    public void close()
    {
        final Consumer<SendingTransport> closeAction = new Consumer<SendingTransport>()
        {
            @Override
            public void accept(SendingTransport sendingTransport)
            {
                sendingTransport.close();
            }
        };

        transports.lockedRemoveAllAndDoAction(closeAction);
    }
}
