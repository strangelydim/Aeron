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
package org.kaazing.messaging.discovery;

import org.kaazing.messaging.destination.MessageFlow;
import org.kaazing.messaging.transport.TransportHandle;

import java.util.List;
import java.util.function.Consumer;

public interface DiscoveryService
{
    public List<TransportHandle> getInterestList(MessageFlow messageFlow);

    public void addInterestListChangedListener(MessageFlow messageFlow, Consumer<InterestListChangedEvent> listener);

    public void register(MessageFlow messageFlow, TransportHandle handle);

    public void unregister(MessageFlow messageFlow, TransportHandle handle);

    public DiscoveryService start();

    public DiscoveryService stop();
}
