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
package org.kaazing.messaging.example;

import org.kaazing.messaging.client.Message;
import org.kaazing.messaging.client.MessageProducer;
import org.kaazing.messaging.destination.Pipe;
import org.kaazing.messaging.transport.BaseTransportContext;
import org.kaazing.messaging.transport.aeron.AeronTransportContext;

public class RxJavaPipeProducerExample
{
    public static void main(String[] args)
    {
        BaseTransportContext context = new AeronTransportContext();

        Pipe pipe = new Pipe("aeron:udp?remote=127.0.0.1:40124", 10);

        MessageProducer messageProducer = new MessageProducer(context, pipe);

        for(int i = 0; i < 10; i++)
        {
            Message message = new Message(1024);
            message.getUnsafeBuffer().putInt(0, 100 + i);
            message.setBufferOffset(0);
            message.setBufferLength(4);


            boolean result = messageProducer.offer(message);
            System.out.println("Sent message with result: " + result);

        }
        messageProducer.close();
        context.close();
    }
}
