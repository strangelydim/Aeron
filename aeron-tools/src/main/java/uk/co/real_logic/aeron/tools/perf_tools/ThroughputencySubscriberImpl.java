package uk.co.real_logic.aeron.tools.perf_tools;

/**
 * Created by philip on 3/30/15.
 */
public interface ThroughputencySubscriberImpl
{
    void prepare();
    void run();
    void shutdown();
}
