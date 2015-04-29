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
package org.kaazing.messaging.transport.aeron;

import org.kaazing.messaging.client.Message;
import org.kaazing.messaging.transport.ReceivingTransport;
import org.kaazing.messaging.transport.TransportHandle;
import uk.co.real_logic.aeron.Subscription;
import uk.co.real_logic.aeron.common.concurrent.logbuffer.DataHandler;
import uk.co.real_logic.aeron.common.concurrent.logbuffer.Header;
import uk.co.real_logic.agrona.DirectBuffer;

import java.util.function.Consumer;

public class AeronReceivingTransport implements ReceivingTransport, DataHandler
{
    private final String channel;
    private final int streamId;
    private final Subscription subscription;
    private final AeronTransportContext aeronTransportContext;
    private final Consumer<Message> messageHandler;
    private final TransportHandle handle;
    private final ThreadLocal<Message> tlMessage = new ThreadLocal<Message>()
    {
        @Override
        protected Message initialValue()
        {
            return new Message();
        }
    };

    public AeronReceivingTransport(AeronTransportContext aeronTransportContext, String channel, int streamId, Consumer<Message> messageHandler)
    {
        this.aeronTransportContext = aeronTransportContext;
        this.channel = channel;
        this.streamId = streamId;
        this.subscription = aeronTransportContext.getAeron().addSubscription(channel, streamId, this);
        this.messageHandler = messageHandler;
        this.handle = new TransportHandle(channel, TransportHandle.Type.Aeron);
        this.handle.getExtras().put("streamId", String.valueOf(streamId));
    }

    @Override
    public TransportHandle getHandle()
    {
        return handle;
    }

    @Override
    public void close()
    {
        aeronTransportContext.removePollableReceivingTransport(this);
        subscription.close();
    }

    @Override
    public void onData(DirectBuffer buffer, int offset, int length, Header header)
    {
        Message message = tlMessage.get();
        message.setBuffer(buffer);
        message.setBufferOffset(offset);
        message.setBufferLength(length);
        //TODO(JAF): Map header information into message metadata

        messageHandler.accept(message);
    }

    @Override
    public int poll(final int limit)
    {
        return subscription.poll(limit);
    }

    @Override
    public boolean isPollable()
    {
        return true;
    }
}
