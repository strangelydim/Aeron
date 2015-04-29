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
package org.kaazing.messaging.rx;

import org.kaazing.messaging.client.Message;
import org.kaazing.messaging.client.MessageConsumer;
import org.kaazing.messaging.destination.MessageFlow;
import org.kaazing.messaging.transport.TransportContext;
import rx.Observable;
import rx.Subscriber;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class MessageConsumerRxAdapter
{
    public static Observable<Message> createSingleSubscriberObservable(TransportContext context, MessageFlow messageFlow)
    {
        Observable<Message> observableConsumer = Observable.create(new Observable.OnSubscribe<Message>()
        {
            MessageConsumer messageConsumer = null;
            @Override
            public void call(final rx.Subscriber<? super Message> observer)
            {
                if (!observer.isUnsubscribed() && messageConsumer == null)
                {
                    Consumer<Message> messageHandler = new Consumer<Message>()
                    {
                        @Override
                        public void accept(Message message)
                        {
                            if(observer.isUnsubscribed())
                            {
                                //TODO(JAF): Add correct threading support for stopping the message consumer when unsubscribed
                                if(messageConsumer != null)
                                {
                                    messageConsumer.close();
                                    messageConsumer = null;
                                }
                            }
                            else
                            {
                                observer.onNext(message);
                            }
                        }
                    };
                    messageConsumer = new MessageConsumer(context, messageFlow, messageHandler);
                }
                else
                {
                    observer.onError(new UnsupportedOperationException("Cannot create a second subscriber to this observable. " +
                            "Use createMultiObservable function"));
                }
            }
        });
        return observableConsumer;
    }

    public static Observable<Message> createObservable(TransportContext context, MessageFlow messageFlow)
    {
        final List<rx.Subscriber<? super Message>> observers = new CopyOnWriteArrayList<>();
        final Subscriber<? super Message> singleObserver;

        Consumer<Message> messageHandler = new Consumer<Message>()
        {
            @Override
            public void accept(Message message)
            {
                rx.Subscriber<? super Message> observerToRemove = null;
                for(rx.Subscriber<? super Message> observer : observers)
                {
                    if (observer != null)
                    {
                        if(observer.isUnsubscribed())
                        {
                            observerToRemove = observer;
                        }
                        else
                        {
                            observer.onNext(message);
                        }
                    }
                }
                if(observerToRemove != null)
                {
                    observers.remove(observerToRemove);
                }
            }
        };
        MessageConsumer messageConsumer = new MessageConsumer(context, messageFlow, messageHandler);

        Observable<Message> observableConsumer = Observable.create(new Observable.OnSubscribe<Message>()
        {
            @Override
            public void call(final rx.Subscriber<? super Message> observer)
            {
                if (!observer.isUnsubscribed())
                {
                    observers.add(observer);
                }
            }
        });
        return observableConsumer;
    }
}
