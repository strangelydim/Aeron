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
package org.kaazing.messaging.transport.amqp;

import org.kaazing.messaging.client.Message;
import org.kaazing.messaging.transport.ReceivingTransport;
import org.kaazing.messaging.transport.TransportHandle;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.Section;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class AmqpProtonReceivingTransport implements ReceivingTransport, Consumer<org.apache.qpid.proton.message.Message>
{
    private final int defaultBufferSize = 4096;
    private final String address;
    private final AmqpProtonTransportContext amqpProtonTransportContext;
    private final Consumer<Message> messageHandler;
    private final TransportHandle handle;
    private final ThreadLocal<Message> tlMessage = new ThreadLocal<Message>()
    {
        @Override
        protected Message initialValue()
        {
            return new Message(defaultBufferSize);
        }
    };

    public AmqpProtonReceivingTransport(AmqpProtonTransportContext amqpProtonTransportContext, String address, Consumer<Message> messageHandler)
    {
        this.amqpProtonTransportContext = amqpProtonTransportContext;
        this.address = address;

        amqpProtonTransportContext.getMessenger().subscribe(address);
        amqpProtonTransportContext.addSubscription(address, this);
        this.messageHandler = messageHandler;
        this.handle = new TransportHandle(address, TransportHandle.Type.AMQP);
    }


    @Override
    public TransportHandle getHandle()
    {
        return handle;
    }

    @Override
    public void close()
    {
        amqpProtonTransportContext.removeSubscription(address, this);
    }


    @Override
    public void accept(org.apache.qpid.proton.message.Message amqpMessage)
    {
        Section section = amqpMessage.getBody();
        if(section instanceof Data)
        {
            Binary binaryData = ((Data) section).getValue();
            byte[] bytes = binaryData.getArray();
            int offset = binaryData.getArrayOffset();
            int length = binaryData.getLength();
            Message message = tlMessage.get();
            message.getUnsafeBuffer().putBytes(0, bytes, offset, length);
            //TODO(JAF): Map header information into message metadata
            messageHandler.accept(message);
        }
        else if(section instanceof AmqpValue)
        {
            Object amqpValueObject = ((AmqpValue) section).getValue();
            String amqpValueString = amqpValueObject.toString();
            Message message = tlMessage.get();

            //TODO(JAF): Handle growing the internal buffer
            message.getUnsafeBuffer().putBytes(0, amqpValueString.getBytes(StandardCharsets.UTF_8));

            //TODO(JAF): Map header information into message metadata

            messageHandler.accept(message);
        }
        else
        {
            //TODO(JAF): Support other AMQP body types
            System.out.println("Unsupported AMQP body type");
        }
    }

    @Override
    public int poll(int limit)
    {
        throw new UnsupportedOperationException("This transport is not pollable");
    }

    @Override
    public boolean isPollable()
    {
        return false;
    }
}
