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

public class PubSubExample {
    public static void main(String[] args) {
        MessageSubscriber subscriber = new MessageSubscriber();
        MessagePublisher publisher = new MessagePublisher();

        publisher.subscribe(subscriber);

        Message message = new Message();
        message.getBuffer().putInt(567);
        message.getBuffer().rewind();

        publisher.submit(message);
    }

}
