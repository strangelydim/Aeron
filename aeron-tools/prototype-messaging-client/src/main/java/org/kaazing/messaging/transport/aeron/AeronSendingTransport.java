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
import org.kaazing.messaging.transport.SendingTransport;
import uk.co.real_logic.aeron.Publication;

public class AeronSendingTransport implements SendingTransport
{
    private final String channel;
    private final int streamId;
    private String remoteId;
    private final Publication publication;
    private final AeronTransportContext aeronTransportContext;
    public AeronSendingTransport(AeronTransportContext aeronTransportContext, String channel, int streamId)
    {
        this.aeronTransportContext = aeronTransportContext;
        this.channel = channel;
        this.streamId = streamId;
        this.publication = aeronTransportContext.getAeron().addPublication(channel, streamId);
    }

    @Override
    public void submit(Message message)
    {
        throw new UnsupportedOperationException("blocking submit method is not supported with this transport");
    }

    @Override
    public long offer(Message message)
    {
        return publication.offer(message.getBuffer());
    }

    @Override
    public void close()
    {
        publication.close();
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
