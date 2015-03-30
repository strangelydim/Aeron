package uk.co.real_logic.aeron.tools.perf_tools;

import uk.co.real_logic.aeron.Aeron;
import uk.co.real_logic.aeron.FragmentAssemblyAdapter;
import uk.co.real_logic.aeron.Publication;
import uk.co.real_logic.aeron.Subscription;
import uk.co.real_logic.aeron.common.concurrent.logbuffer.Header;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.BusySpinIdleStrategy;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by philip on 3/30/15.
 */
public class AeronSubscriber implements ThroughputencySubscriberImpl
{
    private Aeron.Context ctx = null;
    private FragmentAssemblyAdapter dataHandler = null;
    private Aeron aeron = null;
    protected Publication pub = null;
    private Subscription sub = null;
    private int pubStreamId = 11;
    private int subStreamId = 10;
    private String subChannel = "udp://localhost:44444";
    private String pubChannel = "udp://localhost:55555";
    private int fragmentCountLimit;
    private BusySpinIdleStrategy idle = new BusySpinIdleStrategy();
    private AtomicBoolean running = new AtomicBoolean(true);

    public AeronSubscriber()
    {

    }

    public void prepare()
    {
        ctx = new Aeron.Context();
        dataHandler = new FragmentAssemblyAdapter(this::msgHandler);
        aeron = Aeron.connect(ctx);
        pub = aeron.addPublication(pubChannel, pubStreamId);
        sub = aeron.addSubscription(subChannel, subStreamId, dataHandler);
        fragmentCountLimit = 1;
    }

    public void run()
    {
        while (running.get())
        {
            int fragmentsRead = sub.poll(fragmentCountLimit);
            idle.idle(fragmentsRead);
        }
    }

    public void shutdown()
    {
        running.set(false);
        sub.close();
        pub.close();
        ctx.close();
        aeron.close();
    }

    public void msgHandler(DirectBuffer buffer, int offset, int length, Header header)
    {
        while (!pub.offer(buffer, offset, length))
        {
            idle.idle(0);
        }
    }
}
