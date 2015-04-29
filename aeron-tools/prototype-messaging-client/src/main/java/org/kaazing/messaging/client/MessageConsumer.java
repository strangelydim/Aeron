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
import org.kaazing.messaging.transport.ReceivingTransport;
import org.kaazing.messaging.transport.TransportContext;
import org.kaazing.messaging.transport.TransportList;

import java.util.function.Consumer;

public class MessageConsumer
{
    private final TransportContext context;
    private final MessageFlow messageFlow;
    private final Consumer<Message> messageHandler;
    private TransportList<ReceivingTransport, Message> transports = new TransportList<ReceivingTransport, Message>();


    public MessageConsumer(TransportContext context, MessageFlow messageFlow, Consumer<Message> messageHandler)
    {
        this.context = context;
        this.messageFlow = messageFlow;
        this.messageHandler = messageHandler;
        this.transports.lockedAddAll(context.createReceivingTransports(messageFlow, messageHandler));
    }

    public MessageFlow getMessageFlow()
    {
        return messageFlow;
    }

    /**
     * Removes all receiving transports from the message consumer, unregisters them, and closes them
     */
    public void close()
    {
        final Consumer<ReceivingTransport> closeAction = new Consumer<ReceivingTransport>()
        {
            @Override
            public void accept(ReceivingTransport receivingTransport)
            {
                receivingTransport.close();
            }
        };

        transports.lockedRemoveAllAndDoAction(closeAction);
    }
}
