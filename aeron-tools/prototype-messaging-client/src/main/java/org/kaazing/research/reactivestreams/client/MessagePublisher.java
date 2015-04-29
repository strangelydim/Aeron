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
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CopyOnWriteArrayList;

public class MessagePublisher implements Publisher<Message> {
    private final CopyOnWriteArrayList<Subscriber<? super Message>> subscribers;
    private final CopyOnWriteArrayList<MessageFlow> messageFlows;

    public MessagePublisher() {
        subscribers = new CopyOnWriteArrayList<Subscriber<? super Message>>();
        messageFlows = new CopyOnWriteArrayList<MessageFlow>();
    }

    @Override
    public void subscribe(Subscriber<? super Message> subscriber) {
        subscribers.add(subscriber);

        //TODO: Notify the subscriber with a new subscription "handle" that will need to be stored
        Subscription newSubscription = new MessageSubscription();
        subscriber.onSubscribe(newSubscription);
    }

    public void join(MessageFlow messageFlow) {
        messageFlows.add(messageFlow);

        //TODO: Probably need the transport to bubble up the subscribe for each subscriber that joins to this publisher
        // but for demonstration purposes, we could use the interest list and just call subscriber for each one

        //After joining the message flow, get the interested subscribers and subscribe them to this publisher
        for(Subscriber<? super Message> subscriber : messageFlow.getInterest().getInterestList()) {
            subscribe(subscriber);
        }
    }

    //Non-blocking async send that can fail
    public boolean offer(Message message) {
        for(Subscriber<? super Message> subscriber : subscribers) {
            subscriber.onNext(message);
        }
        return true;
    }

    //Blocking send that synchronously block
    public void submit(Message message) {
        for(Subscriber<? super Message> subscriber : subscribers) {
            subscriber.onNext(message);
        }
    }


    public void close() {
        for (Subscriber<? super Message> subscriber : subscribers) {
            subscriber.onComplete();
        }
    }
}