package uk.co.real_logic.aeron.tools.perf_tools;

import uk.co.real_logic.aeron.tools.MessagesAtMessagesPerSecondInterval;
import uk.co.real_logic.aeron.tools.RateController;
import uk.co.real_logic.aeron.tools.RateControllerInterval;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by philip on 3/30/15.
 */
public class ThroughputencyPublisherRunner implements RateController.Callback
{
    private ThroughputencyPublisherImpl impl = null;
    private int warmupMsgs = 10000;
    private int msgLen = 64;
    private RateController rateCtlr = null;
    private int msgIdx = 0;
    private long rtts[] = new long[11111100];
    private UnsafeBuffer buffer = null;
    private int msgCount = 0;

    public ThroughputencyPublisherRunner(String[] args)
    {
        try
        {
            Class<?> cl = Class.forName(args[0]);
            Constructor<?> cons = cl.getConstructor(ThroughputencyPublisherRunner.class);
            impl = (ThroughputencyPublisherImpl)cons.newInstance(this);
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (InstantiationException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        catch (NoSuchMethodException e)
        {
            e.printStackTrace();
        }
        catch (InvocationTargetException e)
        {
            e.printStackTrace();
        }

        List<RateControllerInterval> intervals = new ArrayList<RateControllerInterval>();
        intervals.add(new MessagesAtMessagesPerSecondInterval(100, 10));
        intervals.add(new MessagesAtMessagesPerSecondInterval(1000, 100));
        intervals.add(new MessagesAtMessagesPerSecondInterval(10000, 1000));
        intervals.add(new MessagesAtMessagesPerSecondInterval(100000, 10000));
        intervals.add(new MessagesAtMessagesPerSecondInterval(1000000, 100000));
        intervals.add(new MessagesAtMessagesPerSecondInterval(10000000, 1000000));

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
        impl.prepare();
        impl.connect();
        run();
        impl.shutdown();
    }

    private void run()
    {
        for (int i = 0; i < warmupMsgs; i++)
        {
            buffer.putLong(0, msgCount++);
            buffer.putLong(8, System.nanoTime());
            buffer.putByte(16, (byte)'x');
            impl.sendMsg(buffer);
        }

        long start = System.currentTimeMillis();
        while (rateCtlr.next())
        {
            //System.out.println("AAAAAAAAAAAAAA");
        }
        long end = System.currentTimeMillis();

        System.out.println("Test Run took: " + (end - start) / 1000 + " seconds");

        double sum = 0.0;
        for (int i = 0; i < 100; i++)
        {
            sum += rtts[i] / 1000.0;
        }
        System.out.println("Mean latency for 10msgs/sec: " + sum / 100.0);

        sum = 0.0;
        for (int i = 100; i < 1100; i++)
        {
            sum += rtts[i] / 1000.0;
        }
        System.out.println("Mean latency for 100msgs/sec: " + sum / 1000.0);

        sum = 0.0;
        for (int i = 1100; i < 11100; i++)
        {
            sum += rtts[i] / 1000.0;
        }
        System.out.println("Mean latency for 1Kmsgs/sec: " + sum / 10000.0);

        sum = 0.0;
        for (int i = 11100; i < 111100; i++)
        {
            sum += rtts[i] / 1000.0;
        }
        System.out.println("Mean latency for 10Kmsgs/sec: " + sum / 100000.0);

        sum = 0.0;
        for (int i = 111100; i < 1111100; i++)
        {
            sum += rtts[i] / 1000.0;
        }
        System.out.println("Mean latency for 100Kmsgs/sec: " + sum / 1000000.0);

        sum = 0.0;
        for (int i = 1111100; i < 11111100; i++)
        {
            sum += rtts[i] / 1000.0;
        }
        System.out.println("Mean latency for 1Mmsgs/sec: " + sum / 10000000.0);
//        for (int i = 0; i < rtts.length; i++)
  //      {
    //        System.out.println("IDX: " + i + " RTT: " + rtts[i] + "us");
      //  }
    }

    public int onNext()
    {
        buffer.putLong(0, msgCount++);
        buffer.putLong(8, System.nanoTime());
        buffer.putByte(16, (byte)'x');
        impl.sendMsg(buffer);

        return msgLen;
    }

    public void gotMessage(long idx, long rtt)
    {
        rtts[(int)idx] = rtt >> 1;
        //System.out.println("IDX: " + idx + ", RTT: " + rtt);
    }

    public static void main(String[] args)
    {
        new ThroughputencyPublisherRunner(args);
    }

}
