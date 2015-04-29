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
package org.kaazing.research.reactivestreams.client;

import org.kaazing.research.reactivestreams.destination.MessageFlow;
import org.reactivestreams.*;
import org.reactivestreams.Subscription;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class MessageSubscriber implements Subscriber<Message> {
    private final CopyOnWriteArrayList<MessageFlow> messageFlows;
    private Consumer<Message> onNext;
    public MessageSubscriber() {
        this(null);
    }

    public MessageSubscriber(Consumer<Message> onNext) {
        this.onNext = onNext;
        messageFlows = new CopyOnWriteArrayList<MessageFlow>();
    }

    @Override
    public void onSubscribe(Subscription s) {
        System.out.println("Subscribed to subscription: " + s);
        s.request(1);
    }

    @Override
    public void onNext(Message message) {
        if(onNext != null) {
            onNext.accept(message);
        } else {
            System.out.println("Received message in subscriber with no callback: " + message);
        }
    }

    @Override
    public void onError(Throwable t) {
        System.err.println("Received error: " + t);
        t.printStackTrace();
    }

    @Override
    public void onComplete() {
        System.out.println("Subscription complete");
    }

    public void join(MessageFlow messageFlow) {
        messageFlows.add(messageFlow);
        messageFlow.getInterest().getInterestList().add(this);
    }
}
