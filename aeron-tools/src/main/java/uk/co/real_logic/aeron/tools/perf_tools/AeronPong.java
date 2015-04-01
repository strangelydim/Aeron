package uk.co.real_logic.aeron.tools.perf_tools;

import uk.co.real_logic.aeron.*;
import uk.co.real_logic.aeron.common.concurrent.logbuffer.Header;


import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.*;

import java.util.concurrent.atomic.AtomicBoolean;

public class AeronPong implements PongImpl
{
  private Aeron.Context ctx = null;
  private FragmentAssemblyAdapter dataHandler = null;
  private Aeron aeron = null;
  protected Publication pongPub = null;
  private Subscription pingSub = null;
  private int pingStreamId = 10;
  private int pongStreamId = 11;
  private String pingChannel = "udp://localhost:44444";
  private String pongChannel = "udp://localhost:55555";
  private int fragmentCountLimit;
  private BusySpinIdleStrategy idle = new BusySpinIdleStrategy();
  protected AtomicBoolean running = new AtomicBoolean(true);

  public AeronPong()
  {

  }

  public void prepare()
  {
    ctx = new Aeron.Context();
    dataHandler = new FragmentAssemblyAdapter(this::pingHandler);
    aeron = Aeron.connect(ctx);
    pongPub = aeron.addPublication(pongChannel, pongStreamId);
    pingSub = aeron.addSubscription(pingChannel, pingStreamId, dataHandler);
    fragmentCountLimit = 1;
  }

  public void run()
  {
    while (running.get())
    {
      int fragmentsRead = pingSub.poll(fragmentCountLimit);
      idle.idle(fragmentsRead);
    }
    System.out.println("Done");
    shutdown();
  }

  public void shutdown()
  {
    pingSub.close();
    pongPub.close();
    aeron.close();
    ctx.close();
  }

  public void pingHandler(DirectBuffer buffer, int offset, int length, Header header)
  {
    if (buffer.getByte(offset) == (byte)'q')
    {
      running.set(false);
      return;
    }

    while (!pongPub.offer(buffer, offset, length))
    {
      idle.idle(0);
    }
  }
}
