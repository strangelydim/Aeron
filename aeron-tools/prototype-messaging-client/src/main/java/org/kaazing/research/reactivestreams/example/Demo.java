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
import org.kaazing.research.reactivestreams.destination.Topic;

public class Demo {
    public static void main(String[] args) {
        Topic topic = new Topic("STOCKS.ABC");

        MessageSubscriber subscriber = new MessageSubscriber();
        subscriber.join(topic);

        MessageSubscriber subscriber2 = new MessageSubscriber(
                message -> System.out.println("Received message with payload: " + message.getBuffer().getInt())
        );
        subscriber2.join(topic);

        MessagePublisher publisher = new MessagePublisher();
        publisher.join(topic);

        Message message = new Message();
        message.getBuffer().putInt(567);

        publisher.submit(message);
    }
}
