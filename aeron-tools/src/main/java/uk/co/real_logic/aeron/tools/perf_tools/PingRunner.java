package uk.co.real_logic.aeron.tools.perf_tools;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.Arrays;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.*;

public class PingRunner
{
    private int numMsgs = 10000000;
    private int numWarmupMsgs = 100000;
    private int msgLen = 20;
    private long[][] timestamps = null;
    private double[] sortedRTT = null;
    private double[] tmp = null;
    private int idx;
    private boolean warmedUp = false;
    private PingImpl impl = null;
    private String transport = "";
    private UnsafeBuffer buff = null;
    private int numSent = 0;

    public PingRunner(String[] args)
    {
        timestamps = new long[2][numMsgs];
        idx = 0;

        try
        {
            Class<?> cl = Class.forName(args[0]);
            Constructor<?> cons = cl.getConstructor(PingRunner.class);
            impl = (PingImpl)cons.newInstance(this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        buff = new UnsafeBuffer(ByteBuffer.allocateDirect(msgLen));
        transport = args[0];
        impl.prepare();
        impl.connect();
        run();
        impl.shutdown();
        printStats();
    }

    private void run()
    {
        // Send warm-up messages
        System.out.println("Sending Warm-up Messages");
        buff.putByte(0, (byte) 'w');
        for (int i = 0; i < numWarmupMsgs; i++)
        {
            buff.putInt(1, i);
            impl.sendPingAndReceivePong(buff);
        }

        System.out.println("Sending Real Messages");
        buff.putByte(0, (byte)'p');
        for (int i = 0; i < numMsgs; i++)
        {
            buff.putInt(1, i);
            timestamps[0][i] = System.nanoTime();
            impl.sendPingAndReceivePong(buff);
        }

        buff.putByte(0, (byte)'q');
        impl.sendExitMsg(buff);
    }

    public void msgCallback(DirectBuffer buffIn, int offset, int length)
    {
        timestamps[1][buffIn.getInt(offset + 1)] = System.nanoTime();
    }

    private void printStats()
    {
        double sum = 0.0;
        double max = 0;
        double min = Long.MAX_VALUE;
        int maxIdx = 0;
        int minIdx = 0;
        double mean = 0.0;
        double stdDev = 0.0;

        tmp = new double[timestamps[0].length];

        for (int i = 0; i < tmp.length; i++)
        {
            tmp[i] = (timestamps[1][i] - timestamps[0][i]) / 1000.0;
        }

        sortedRTT = new double[tmp.length];
        System.arraycopy(tmp, 0, sortedRTT, 0, tmp.length);
        Arrays.sort(sortedRTT);

        for (int i = 0; i < numMsgs; i++)
        {
            if (tmp[i] > max)
            {
                max = tmp[i];
                maxIdx = i;
            }
            if (tmp[i] < min)
            {
                min = tmp[i];
                minIdx = i;
            }
            sum += (double)(tmp[i]);
        }

        mean = sum / numMsgs;
        sum = 0;
        for (int i = 0; i < numMsgs; i++)
        {
            sum += Math.pow(mean - tmp[i], 2);
        }
        stdDev = Math.sqrt(sum / numMsgs);

        BufferedImage image = new BufferedImage(1800, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.white);
        g2.fillRect(0, 0, 1800, 1000);
        generateScatterPlot(image, g2, 0, 0, min, max, .9, mean, stdDev);
        generateScatterPlot(image, g2, 600, 0, min, max, .99, mean, stdDev);
        generateScatterPlot(image, g2, 1200, 0, min, max, .999, mean, stdDev);
        generateScatterPlot(image, g2, 0, 500, min, max, .9999, mean, stdDev);
        generateScatterPlot(image, g2, 600, 500, min, max, .99999, mean, stdDev);
        generateScatterPlot(image, g2, 1200, 500, min, max, .999999, mean, stdDev);

        g2.setColor(Color.black);
        g2.drawString(String.format("Mean: %.3fus Std. Dev %.3fus", mean, stdDev), 20, 940);
        g2.drawString("Min: " + min + " Index: " + minIdx, 20, 960);
        g2.drawString("Max: " + max + " Index: " + maxIdx, 20, 980);

        String filename = transport + "_scatterplot.png";
        File imageFile = new File(filename);
        try
        {
            ImageIO.write(image, "png", imageFile);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        System.out.format("Mean: %.3fus\n", mean);
        System.out.format("Std Dev: %.3fus\n", stdDev);
        System.out.format("Min: %.3fus Index %d\n", min, minIdx);
        System.out.format("Max: %.3fus. Index %d\n", max, maxIdx);
        System.exit(0);
    }

      private void generateScatterPlot(BufferedImage image, Graphics2D g,
                                       int x, int y,
                                       double min, double max, double percentile, double mean, double stdDev)
    {
        FontMetrics fm = g.getFontMetrics();
        int width = 390;
        int height = 370;
        int num = (int)((numMsgs - 1) * percentile);
        double newMax = sortedRTT[num];
        double stepY = (double)(height / (double)(newMax - min));
        double stepX = (double)width / numMsgs;

        g.setColor(Color.black);

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.drawString("Latency ScatterPlot (us) " + percentile + " percentile",
                x + 100 + width / 2 - fm.stringWidth("Latency ScatterPlot (microseconds)" + percentile + " percentile") / 2,
                y + 20);
        g.drawString("" + newMax, x + 10, y + 20);
        g.drawString("" + min, x + 10, y + 390);
        g.drawLine(x + 100, y + 20, x + 100, y + 390);
        g.drawLine(x + 100, y + 390, x + 490, y + 390);

        g.setColor(Color.red);
        for (int i = 0; i < numMsgs; i++)
        {
            if (tmp[i] < newMax)
            {
                int posX = x + 100 + (int)(stepX * i);
                int posY = y + 390 - (int)(stepY * (tmp[i] - min));
                g.fillRect(posX, posY, 1, 1);
            }
        }
    }

    public static void main(String[] args)
  {
    new PingRunner(args);
  }
}
