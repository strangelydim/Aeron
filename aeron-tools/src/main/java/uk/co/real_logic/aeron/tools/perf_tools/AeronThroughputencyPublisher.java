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
import uk.co.real_logic.agrona.concurrent.NoOpIdleStrategy;
import uk.co.real_logic.agrona.concurrent.IdleStrategy;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
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
    private int warmUpMsgs = 100000;
    private int msgLen = 20;
    private RateController rateCtlr = null;
    private UnsafeBuffer buffer = null;
    private long timestamps[][] = new long[2][41111100];
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
        fragmentCountLimit = 2;
        idle = new NoOpIdleStrategy();

        List<RateControllerInterval> intervals = new ArrayList<RateControllerInterval>();
        intervals.add(new MessagesAtMessagesPerSecondInterval(100, 10));
        intervals.add(new MessagesAtMessagesPerSecondInterval(1000, 100));
        intervals.add(new MessagesAtMessagesPerSecondInterval(10000, 1000));
        intervals.add(new MessagesAtMessagesPerSecondInterval(100000, 10000));
        intervals.add(new MessagesAtMessagesPerSecondInterval(1000000, 100000));
        intervals.add(new MessagesAtMessagesPerSecondInterval(10000000, 1000000));
        intervals.add(new MessagesAtMessagesPerSecondInterval(30000000, 3000000));
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
                        //idle.idle(0);
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

            while (!pub.offer(buffer, 0, buffer.capacity()))
            {
                idle.idle(0);
            }
        }

        int start = (int)System.currentTimeMillis();
        while (rateCtlr.next())
        {

        }
        int total = (int)(System.currentTimeMillis() - start) / 1000;
        buffer.putByte(0, (byte)'q');

        while (!pub.offer(buffer, 0, buffer.capacity()))
        {
            idle.idle(0);
        }

        System.out.println("Duration: " + total + " seconds");
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
        System.currentTimeMillis();
        timestamps[0][msgCount] = System.nanoTime();

        while (!pub.offer(buffer, 0, buffer.capacity()))
        {
            //idle.idle(0);
            timestamps[0][msgCount] = System.nanoTime();
        }
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
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        computeStats(0, 100, "10mps");
        computeStats(100, 1000, "100mps");
        computeStats(1000, 11000, "1Kmps");
        computeStats(11000, 111000, "10Kmps");
        computeStats(111000, 1111000, "100Kmps");
        computeStats(1111000, 11111000, "1Mmps");
        computeStats(11111000, 41111000, "3Mmps");
    }

    private void computeStats(int start, int end, String title)
    {
        double sum = 0.0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (int i = start; i < end; i++)
        {
            double ts = (timestamps[1][i] - timestamps[0][i]) / 1000.0;
            sum += ts;
            if (ts  < min)
            {
                min = ts;
            }
            if (ts > max)
            {
                max = ts;
            }
        }
        System.out.println("Mean latency for " + title + ": " + sum / (end - start));
        generateScatterPlot(title, min, max, start, end);
        try
        {
            PrintWriter out = new PrintWriter(new FileOutputStream("pub.ts"));
            for (int i = 1; i< timestamps[0].length; i++)
            {
                out.println(timestamps[0][i] - timestamps[0][i - 1]);
            }
            out.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void generateScatterPlot(String title, double min, double max, int start, int end)
    {
        BufferedImage image = new BufferedImage(500, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        FontMetrics fm = g2.getFontMetrics();
        String filename = title + ".png";
        File imageFile = new File(filename);
        int width = 390;
        int height = 370;
        int num = end - start;
        double stepY = (double) (height / (double) (max));
        double stepX = (double) width / num;

        g2.setColor(Color.white);
        g2.fillRect(0, 0, 500, 400);
        g2.setColor(Color.black);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.drawString("Latency ScatterPlot (microseconds)", 250 - fm.stringWidth("Latency ScatterPlot (microseconds)") / 2, 20);
        g2.drawString("" + max, 10, 20);
        g2.drawLine(100, 20, 100, 390);
        g2.drawLine(100, 390, 490, 390);

        g2.setColor(Color.red);
        for (int i = start; i < end; i++)
        {
            if ((timestamps[1][i] - timestamps[0][i]) / 1000.0 <= max)
            {
                int posX = 100 + (int) (stepX * (i - start));
                int posY = 390 - (int) (stepY * ((timestamps[1][i] - timestamps[0][i]) / 1000.0));
                g2.drawLine(posX, posY, posX, 390);
            }
        }

        try
        {
            ImageIO.write(image, "png", imageFile);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        new AeronThroughputencyPublisher();
    }
}
