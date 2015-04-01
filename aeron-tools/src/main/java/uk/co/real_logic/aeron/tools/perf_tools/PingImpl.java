package uk.co.real_logic.aeron.tools.perf_tools;

import java.nio.ByteBuffer;

public interface PingImpl
{
  void prepare();
  void connect();
  long sendPingAndReceivePong(ByteBuffer buf);
  void sendExitMsg(ByteBuffer buf);
  void shutdown();
}
