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
package org.kaazing.research.reactivestreams.example;

import org.kaazing.research.reactivestreams.client.Message;
import org.kaazing.research.reactivestreams.client.MessagePublisher;
import org.kaazing.research.reactivestreams.client.MessageSubscriber;
import org.kaazing.research.reactivestreams.destination.Pipe;

public class PubSubPipeExample {
    public static void main(String[] args) {
        Pipe pipe = new Pipe("udp://localhost:44300");

        //Use the default callback or override to implement a new onNext callback
        MessageSubscriber subscriber = new MessageSubscriber();
        subscriber.join(pipe);

        //Or provide a Java 8 callback using the Consumer interface and then the lambda can be used
        MessageSubscriber subscriber2 = new MessageSubscriber(
                message -> System.out.println("Received message with payload: " + message.getBuffer().getInt())
        );
        subscriber2.join(pipe);

        MessagePublisher publisher = new MessagePublisher();
        publisher.join(pipe);

        Message message = new Message();
        message.getBuffer().putInt(567);
        message.getBuffer().rewind();

        publisher.submit(message);
    }
}
