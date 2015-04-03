package uk.co.real_logic.aeron.tools.perf_tools;

import uk.co.real_logic.aeron.Aeron;
import uk.co.real_logic.aeron.FragmentAssemblyAdapter;
import uk.co.real_logic.aeron.Publication;
import uk.co.real_logic.aeron.Subscription;
import uk.co.real_logic.aeron.common.concurrent.logbuffer.Header;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.BusySpinIdleStrategy;

/**
 * Created by philipjohnson1 on 4/2/15.
 */
public class AeronThroughputencySubscriber
{
    private Aeron.Context ctx = null;
    private FragmentAssemblyAdapter dataHandler = null;
    private Aeron aeron = null;
    private Publication pub = null;
    private Subscription sub = null;
    private int pubStreamId = 11;
    private int subStreamId = 10;
    private String subChannel = "udp://localhost:44444";
    private String pubChannel = "udp://localhost:55555";
    private int fragmentCountLimit;
    private BusySpinIdleStrategy idle = new BusySpinIdleStrategy();
    private boolean running = true;

    public AeronThroughputencySubscriber()
    {
        ctx = new Aeron.Context();
        dataHandler = new FragmentAssemblyAdapter(this::msgHandler);
        aeron = Aeron.connect(ctx);
        pub = aeron.addPublication(pubChannel, pubStreamId);
        sub = aeron.addSubscription(subChannel, subStreamId, dataHandler);
        fragmentCountLimit = 1;

        while (running)
        {
            int fragmentsRead = sub.poll(fragmentCountLimit);
            idle.idle(fragmentsRead);
        }
        sub.close();
        pub.close();
        ctx.close();
        aeron.close();
    }

    public void msgHandler(DirectBuffer buffer, int offset, int length, Header header)
    {
        if (buffer.getByte(offset) == (byte)'q')
        {
            running = false;
            return;
        }

        while (!pub.offer(buffer, offset, length))
        {

        }
    }

    public static void main(String[] args)
    {
        new AeronThroughputencySubscriber();
    }
}
