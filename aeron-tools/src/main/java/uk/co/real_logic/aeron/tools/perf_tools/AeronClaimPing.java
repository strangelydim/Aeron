package uk.co.real_logic.aeron.tools.perf_tools;

import uk.co.real_logic.aeron.common.concurrent.logbuffer.BufferClaim;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

/**
 * Created by philip on 3/27/15.
 */
public class AeronClaimPing extends AeronPing
{
    private BufferClaim bufferClaim;

    public AeronClaimPing(PingRunner runner)
    {
        super(runner);
    }

    public void prepare()
    {
        super.prepare();
        bufferClaim = new BufferClaim();
    }

    public void sendPingAndReceivePong(UnsafeBuffer buff)
    {
        if (pingPub.tryClaim(buff.capacity(), bufferClaim))
        {
            try
            {
                MutableDirectBuffer buffer = bufferClaim.buffer();
                int offset = bufferClaim.offset();
                buffer.wrap(buff, 0, buff.capacity());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                bufferClaim.commit();

                while (pongSub.poll(fragmentCountLimit) <= 0)
                {
                    idle.idle(0);
                }
            }
        }
        else
        {
            sendPingAndReceivePong(buff);
        }
    }
}
