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
import org.kaazing.messaging.transport.SendingTransport;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.message.impl.MessageImpl;

public class AmqpProtonSendingTransport implements SendingTransport
{
    private final String address;
    private String remoteId;
    private final AmqpProtonTransportContext amqpTransportContext;
    private final ThreadLocal<org.apache.qpid.proton.message.Message> tlAmqpMessage = new ThreadLocal<org.apache.qpid.proton.message.Message>()
    {
        @Override
        protected org.apache.qpid.proton.message.Message initialValue()
        {
            return new MessageImpl();
        }
    };

    public AmqpProtonSendingTransport(AmqpProtonTransportContext amqpTransportContext, String address)
    {
        this.amqpTransportContext = amqpTransportContext;
        this.address = address;
    }

    @Override
    public void submit(Message message)
    {
        org.apache.qpid.proton.message.Message amqpMessage = tlAmqpMessage.get();
        amqpMessage.setAddress(address);

        message.getBuffer();
        byte[] bytesToSend = new byte[message.getBufferLength()];
        message.getBuffer().getBytes(message.getBufferOffset(), bytesToSend);
        Binary binary = new Binary(bytesToSend);
        amqpMessage.setBody(new Data(binary));

        amqpTransportContext.getMessenger().put(amqpMessage);

        //TODO(JAF) See if this should be moved elsewhere to do batching
        amqpTransportContext.getMessenger().send();
    }

    @Override
    public long offer(Message message)
    {
        throw new UnsupportedOperationException("non-blocking submit method is not supported with this transport");
    }

    @Override
    public void close()
    {
        //Nothing specific to do
    }

    @Override
    public void setRemoteId(String remoteId)
    {
        this.remoteId = remoteId;
    }

    @Override
    public String getRemoteId()
    {
        return remoteId;
    }
}
