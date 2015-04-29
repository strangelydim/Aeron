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
package org.kaazing.messaging.transport.aeron;

import org.kaazing.messaging.transport.BaseTransportContext;
import org.kaazing.messaging.transport.ReceivingTransport;
import uk.co.real_logic.aeron.Aeron;
import uk.co.real_logic.agrona.concurrent.AtomicArray;
import uk.co.real_logic.agrona.concurrent.BackoffIdleStrategy;
import uk.co.real_logic.agrona.concurrent.IdleStrategy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AeronTransportContext extends BaseTransportContext
{
    //TODO(JAF): Change this to be an interface scan instead of localhost
    public static final String DEFAULT_AERON_SUBSCRIPTION_CHANNEL = "aeron:udp?remote=127.0.0.1:40123";
    public static final AtomicInteger globalStreamIdCtr = new AtomicInteger(10);

    private final static int FRAGMENT_COUNT_LIMIT = 10;
    private final Aeron.Context aeronContext;
    private final Aeron aeron;
    final ExecutorService executor = Executors.newFixedThreadPool(1);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final IdleStrategy idleStrategy = new BackoffIdleStrategy(
            100, 10, TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.MICROSECONDS.toNanos(100));



    public AeronTransportContext()
    {
        aeronContext = new Aeron.Context();
        aeron = Aeron.connect(aeronContext);
        executor.execute(() -> pollReceivingTransports());
    }

    protected Aeron.Context getAeronContext()
    {
        return aeronContext;
    }

    protected Aeron getAeron()
    {
        return aeron;
    }

    protected void pollReceivingTransports()
    {
        try
        {
            final AtomicArray.ToIntLimitedFunction<ReceivingTransport> action = new AtomicArray.ToIntLimitedFunction<ReceivingTransport>()
            {
                @Override
                public int apply(ReceivingTransport receivingTransport, int limit)
                {
                    return receivingTransport.poll(limit);
                }
            };

            int roundRobinIndex = 0;
            while (running.get())
            {
                if (receivingTransports.size() >= ++roundRobinIndex)
                {
                    roundRobinIndex = 0;
                }
                int fragmentsRead = receivingTransports.doLimitedAction(roundRobinIndex, FRAGMENT_COUNT_LIMIT, action);
                idleStrategy.idle(fragmentsRead);
            }
        }
        catch (final Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public void close()
    {
        running.set(false);
        executor.shutdown();
        try
        {
            executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        aeron.close();
        aeronContext.close();
    }
}
