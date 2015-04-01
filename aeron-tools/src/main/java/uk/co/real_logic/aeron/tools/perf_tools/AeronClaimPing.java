package uk.co.real_logic.aeron.tools.perf_tools;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import uk.co.real_logic.aeron.common.concurrent.logbuffer.BufferClaim;
import uk.co.real_logic.agrona.MutableDirectBuffer;

/**
 * Created by philip on 3/27/15.
 */
public class AeronClaimPing extends AeronPing
{
    private BufferClaim bufferClaim;

    public AeronClaimPing()
    {

    }

    public void prepare()
    {
        super.prepare();
        bufferClaim = new BufferClaim();
    }

    public long sendPingAndReceivePong(ByteBuffer buff)
    {
        pongedMessageLatch = new CountDownLatch(1);

        if (pingPub.tryClaim(buff.capacity(), bufferClaim))
        {
            try
            {
                MutableDirectBuffer buffer = bufferClaim.buffer();
                int offset = bufferClaim.offset();
                buffer.wrap(buff, offset, buff.capacity());
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return -1;
            }
            finally
            {
                bufferClaim.commit();

                while (pongSub.poll(fragmentCountLimit) <= 0)
                {
                    idle.idle(0);
                }

                try
                {
                    if (pongedMessageLatch.await(10, TimeUnit.SECONDS))
                    {
                        return rtt >> 1;
                    }
                    else
                    {
                        return -1;
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    return -1;
                }
            }
        }
        else
        {
            return sendPingAndReceivePong(buff);
        }
    }
}
