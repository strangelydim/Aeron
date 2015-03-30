package uk.co.real_logic.aeron.tools.perf_tools;

import uk.co.real_logic.aeron.common.concurrent.logbuffer.BufferClaim;
import uk.co.real_logic.agrona.MutableDirectBuffer;


/**
 * Created by philip on 3/30/15.
 */
public class AeronClaimPublisher extends AeronPublisher
{
    private BufferClaim bufferClaim = null;

    public AeronClaimPublisher(ThroughputencyPublisherRunner parent)
    {
        super(parent);
        bufferClaim = new BufferClaim();
    }

    public void sendMsg(int msgLen, long msgCount, byte marker)
    {
        if (pub.tryClaim(msgLen, bufferClaim))
        {
            try
            {
                MutableDirectBuffer buffer = bufferClaim.buffer();
                int offset = bufferClaim.offset();
                buffer.putLong(offset, msgCount);
                buffer.putLong(offset + 8, System.nanoTime());
                buffer.putByte(offset + 16, marker);
            }
            catch (Exception e)
            {
                System.exit(1);
            }
            finally
            {
                bufferClaim.commit();
            }
        }
        else
        {
            sendMsg(msgLen, msgCount, marker);
        }
    }
}
