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
package org.kaazing.research.reactivestreams.interest;

import org.reactivestreams.Subscriber;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BasicStatefulInterest<T> implements StatefulInterest<T> {

    private List<Subscriber<T>> interestList = new CopyOnWriteArrayList<>();
    @Override
    public List<Subscriber<T>> getInterestList() {
        return interestList;
    }

    @Override
    public void addInterest(Subscriber<T> subscriber) {
        interestList.add(subscriber);
    }

    @Override
    public void removeInterest(Subscriber<T> subscriber) {
        interestList.remove(subscriber);
    }
}
