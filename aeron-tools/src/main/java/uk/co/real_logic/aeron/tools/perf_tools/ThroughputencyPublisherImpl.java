package uk.co.real_logic.aeron.tools.perf_tools;

import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

/**
 * Created by philip on 3/30/15.
 */
public interface ThroughputencyPublisherImpl
{
    void prepare();
    void connect();
    void sendMsg(UnsafeBuffer buffer);
    void shutdown();
}
