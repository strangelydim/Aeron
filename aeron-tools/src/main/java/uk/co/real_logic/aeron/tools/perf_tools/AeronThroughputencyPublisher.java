package uk.co.real_logic.aeron.tools.perf_tools;

import uk.co.real_logic.aeron.Aeron;
import uk.co.real_logic.aeron.FragmentAssemblyAdapter;
import uk.co.real_logic.aeron.Publication;
import uk.co.real_logic.aeron.Subscription;
import uk.co.real_logic.aeron.common.concurrent.logbuffer.Header;
import uk.co.real_logic.aeron.tools.MessagesAtMessagesPerSecondInterval;
import uk.co.real_logic.aeron.tools.RateController;
import uk.co.real_logic.aeron.tools.RateControllerInterval;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.BusySpinIdleStrategy;
import uk.co.real_logic.agrona.concurrent.IdleStrategy;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by philipjohnson1 on 4/2/15.
 */
public class AeronThroughputencyPublisher implements RateController.Callback
{
    private Aeron.Context ctx = null;
    private FragmentAssemblyAdapter dataHandler = null;
    private Aeron aeron = null;
    private Publication pub = null;
    private Subscription sub = null;
    private CountDownLatch connectionLatch = null;
    private int pubStreamId = 10;
    private int subStreamId = 11;
    private String pubChannel = "udp://localhost:44444";
    private String subChannel = "udp://localhost:55555";
    private int fragmentCountLimit;
    private Thread subThread = null;
    private boolean running = true;
    private IdleStrategy idle = null;
    private int warmUpMsgs = 10000;
    private int msgLen = 64;
    private RateController rateCtlr = null;
    private UnsafeBuffer buffer = null;
    private long timestamps[][] = new long[2][111111100];
    private int msgCount = 0;

    public AeronThroughputencyPublisher()
    {
        ctx = new Aeron.Context()
                .newConnectionHandler(this::connectionHandler);
        dataHandler = new FragmentAssemblyAdapter(this::msgHandler);
        aeron = Aeron.connect(ctx);
        pub = aeron.addPublication(pubChannel, pubStreamId);
        sub = aeron.addSubscription(subChannel, subStreamId, dataHandler);
        connectionLatch = new CountDownLatch(1);
        fragmentCountLimit = 1;
        idle = new BusySpinIdleStrategy();

        List<RateControllerInterval> intervals = new ArrayList<RateControllerInterval>();
        intervals.add(new MessagesAtMessagesPerSecondInterval(100, 10));
        intervals.add(new MessagesAtMessagesPerSecondInterval(1000, 100));
        intervals.add(new MessagesAtMessagesPerSecondInterval(10000, 1000));
        intervals.add(new MessagesAtMessagesPerSecondInterval(100000, 10000));
        intervals.add(new MessagesAtMessagesPerSecondInterval(1000000, 100000));

        buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(msgLen));
        msgCount = 0;

        try
        {
            rateCtlr = new RateController(this, intervals);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        Runnable task = new Runnable()
        {
            public void run()
            {
                while (running)
                {
                    while (sub.poll(fragmentCountLimit) <= 0 && running)
                    {
                        idle.idle(0);
                    }
                }
                System.out.println("Done");
            }
        };
        subThread = new Thread(task);
        subThread.start();

        try
        {
            connectionLatch.await();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        for (int i = 0; i < warmUpMsgs; i++)
        {
            buffer.putByte(0, (byte)'w');

            do
            {

            }
            while (!pub.offer(buffer, 0, buffer.capacity()));
        }

        while (rateCtlr.next())
        {

        }

        buffer.putByte(0, (byte)'q');

        while (!pub.offer(buffer, 0, buffer.capacity()))
        {
            idle.idle(0);
        }

        try
        {
            Thread.sleep(1000);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

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
        sub.close();
        ctx.close();
        aeron.close();
        computeStats();
    }

    public int onNext()
    {
        buffer.putByte(0, (byte)'p');
        buffer.putInt(1, msgCount);
        timestamps[0][msgCount] = System.nanoTime();
        do
        {

        }
        while (!pub.offer(buffer, 0, buffer.capacity()));
        msgCount++;

        return msgLen;
    }

    private void connectionHandler(String channel, int streamId,
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
        if (buffer.getByte(offset) == (byte)'p')
        {
            timestamps[1][buffer.getInt(offset + 1)] = System.nanoTime();
        }
    }

    private void computeStats()
    {
        double sum = 0.0;
        for (int i = 0; i < 100; i++)
        {
            sum += (timestamps[1][i] - timestamps[0][i]) / 1000.0;
        }
        System.out.println("Mean latency for 10msgs/sec: " + sum / 100.0);

        sum = 0.0;
        for (int i = 100; i < 1100; i++)
        {
            sum += (timestamps[1][i] - timestamps[0][i]) / 1000.0;
        }
        System.out.println("Mean latency for 100msgs/sec: " + sum / 1000.0);

        sum = 0.0;
        for (int i = 1100; i < 11100; i++)
        {
            sum += (timestamps[1][i] - timestamps[0][i]) / 1000.0;
        }
        System.out.println("Mean latency for 1Kmsgs/sec: " + sum / 10000.0);

        sum = 0.0;
        for (int i = 11100; i < 111100; i++)
        {
            sum += (timestamps[1][i] - timestamps[0][i]) / 1000.0;
        }
        System.out.println("Mean latency for 10Kmsgs/sec: " + sum / 100000.0);
    }

    public static void main(String[] args)
    {
        new AeronThroughputencyPublisher();
    }
}
