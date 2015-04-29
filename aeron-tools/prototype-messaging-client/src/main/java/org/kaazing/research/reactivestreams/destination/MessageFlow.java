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
package org.kaazing.research.reactivestreams.destination;

import org.kaazing.research.reactivestreams.client.Message;
import org.kaazing.research.reactivestreams.interest.BasicStatefulInterest;
import org.kaazing.research.reactivestreams.interest.StatefulInterest;

public class MessageFlow {
    private String identifier;
    public MessageFlow(String identifier) {
        this.identifier = identifier;
    }

    //Should discovery tie in at this point?
    private StatefulInterest<Message> interest = new BasicStatefulInterest<Message>();
    public StatefulInterest<Message> getInterest() {
        return interest;
    }
}

//Top level object
//Message Flow
//Stream
//Channel

//Topic

//Queue

//Directed known address
//Pipe
//Link