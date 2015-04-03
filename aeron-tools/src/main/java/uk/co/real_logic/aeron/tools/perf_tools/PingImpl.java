package uk.co.real_logic.aeron.tools.perf_tools;

import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public interface PingImpl
{
  void prepare();
  void connect();
  void sendPingAndReceivePong(UnsafeBuffer buf);
  void sendExitMsg(UnsafeBuffer buf);
  void shutdown();
}
