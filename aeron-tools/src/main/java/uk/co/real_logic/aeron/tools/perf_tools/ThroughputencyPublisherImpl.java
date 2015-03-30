package uk.co.real_logic.aeron.tools.perf_tools;

/**
 * Created by philip on 3/30/15.
 */
public interface ThroughputencyPublisherImpl
{
    void prepare();
    void connect();
    void sendMsg(int msgLen, long msgCount, byte marker);
    void shutdown();
}
