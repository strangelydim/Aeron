package uk.co.real_logic.aeron.tools.perf_tools;

import java.util.concurrent.CountDownLatch;

import uk.co.real_logic.aeron.Aeron;
import uk.co.real_logic.aeron.FragmentAssemblyAdapter;
import uk.co.real_logic.aeron.Publication;
import uk.co.real_logic.aeron.Subscription;
import uk.co.real_logic.aeron.common.concurrent.logbuffer.Header;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.*;

public class AeronPing implements PingImpl
{
    private Aeron.Context ctx = null;
    private FragmentAssemblyAdapter dataHandler = null;
    private Aeron aeron = null;
    protected Publication pingPub = null;
    protected Subscription pongSub = null;
    private CountDownLatch pongConnectionLatch = null;
    private int pingStreamId = 10;
    private int pongStreamId = 11;
    private String pingChannel = "udp://localhost:44444";
    private String pongChannel = "udp://localhost:55555";
    protected int fragmentCountLimit;
    protected IdleStrategy idle = null;
    private PingRunner runner = null;

    public AeronPing(PingRunner runner)
    {
        idle = new BusySpinIdleStrategy();
        this.runner = runner;
    }

    public void prepare()
    {
        ctx = new Aeron.Context()
                .newConnectionHandler(this::newPongConnectionHandler);
        dataHandler = new FragmentAssemblyAdapter(this::pongHandler);
        aeron = Aeron.connect(ctx);
        pingPub = aeron.addPublication(pingChannel, pingStreamId);
        pongSub = aeron.addSubscription(pongChannel, pongStreamId, dataHandler);
        pongConnectionLatch = new CountDownLatch(1);
        fragmentCountLimit = 1;
    }

    public void connect()
    {
        try
        {
            pongConnectionLatch.await();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void sendPingAndReceivePong(UnsafeBuffer buff)
    {
        do
        {
        }
        while (!pingPub.offer(buff, 0, buff.capacity()));

        while (pongSub.poll(fragmentCountLimit) <= 0)
        {
            idle.idle(0);
        }
    }

    public void sendExitMsg(UnsafeBuffer buff)
    {
        do
        {
        }
        while (!pingPub.offer(buff, 0, buff.capacity()));
    }

    public void shutdown()
    {
        pingPub.close();
        pongSub.close();
        ctx.close();
        aeron.close();
    }

    private void newPongConnectionHandler(String channel, int streamId,
        int sessionId, String sourceInfo)
    {
        if (channel.equals(pongChannel) && pongStreamId == streamId)
        {
            pongConnectionLatch.countDown();
        }
    }

    private void pongHandler(DirectBuffer buffer, int offset, int length,
        Header header)
    {
        runner.msgCallback(buffer, offset, length);
    }
}
