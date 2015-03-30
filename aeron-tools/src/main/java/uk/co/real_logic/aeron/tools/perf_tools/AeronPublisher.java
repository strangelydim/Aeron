package uk.co.real_logic.aeron.tools.perf_tools;

import uk.co.real_logic.aeron.Aeron;
import uk.co.real_logic.aeron.FragmentAssemblyAdapter;
import uk.co.real_logic.aeron.Publication;
import uk.co.real_logic.aeron.Subscription;
import uk.co.real_logic.aeron.common.concurrent.logbuffer.Header;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.BusySpinIdleStrategy;
import uk.co.real_logic.agrona.concurrent.IdleStrategy;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

/**
 * Created by philip on 3/30/15.
 */
public class AeronPublisher implements ThroughputencyPublisherImpl
{
    private Aeron.Context ctx = null;
    private FragmentAssemblyAdapter dataHandler = null;
    private Aeron aeron = null;
    protected Publication pub = null;
    private Subscription sub = null;
    private CountDownLatch connectionLatch = null;
    private int pubStreamId = 10;
    private int subStreamId = 11;
    private String pubChannel = "udp://localhost:44444";
    private String subChannel = "udp://localhost:55555";
    private int fragmentCountLimit;
    private ThroughputencyPublisherRunner parent = null;
    private Thread subThread = null;
    private boolean running = true;
    private IdleStrategy idle = null;

    public AeronPublisher(ThroughputencyPublisherRunner parent)
    {
        this.parent = parent;
    }

    public void prepare()
    {
        ctx = new Aeron.Context()
                .newConnectionHandler(this::newConnectionHandler);
        dataHandler = new FragmentAssemblyAdapter(this::msgHandler);
        aeron = Aeron.connect(ctx);
        pub = aeron.addPublication(pubChannel, pubStreamId);
        sub = aeron.addSubscription(subChannel, subStreamId, dataHandler);
        connectionLatch = new CountDownLatch(1);
        fragmentCountLimit = 1;
        idle = new BusySpinIdleStrategy();

        Runnable task = new Runnable()
        {
            public void run()
            {
                while (running)
                {
                    while (sub.poll(fragmentCountLimit) <= 0)
                    {
                        idle.idle(0);
                    }
                }
            }
        };
        subThread = new Thread(task);
        subThread.start();
    }

    public void connect()
    {
        try
        {
            connectionLatch.await();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void sendMsg(int msgLen, long msgCount, byte marker)
    {
        UnsafeBuffer atomicBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(msgLen));

        do
        {
            atomicBuffer.putLong(0, msgCount);
            atomicBuffer.putLong(8, System.nanoTime());
            atomicBuffer.putByte(16, marker);
        }
        while (!pub.offer(atomicBuffer, 0, msgLen));
    }

    public void shutdown()
    {
        running = false;
        try
        {
            subThread.join();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        pub.close();
        aeron.close();
        sub.close();
        ctx.close();
    }

    private void newConnectionHandler(String channel, int streamId,
                                      int sessionId, String sourceInfo)
    {
        if (channel.equals(subChannel) && subStreamId == streamId)
        {
            connectionLatch.countDown();
        }
    }

    private void msgHandler(DirectBuffer buffer, int offset, int length,
                            Header header)
    {
        long msgCount = buffer.getLong(offset);
        long ts = buffer.getLong(offset + 8);
        byte marker = buffer.getByte(offset + 16);

        long rtt = System.nanoTime() - ts;
        if (marker != (byte)'w')
        {
            parent.gotMessage(msgCount, rtt);
        }
    }
}
