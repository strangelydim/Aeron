package uk.co.real_logic.aeron.tools.perf_tools;

import uk.co.real_logic.aeron.common.concurrent.logbuffer.BufferClaim;
import uk.co.real_logic.aeron.common.concurrent.logbuffer.Header;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;

/**
 * Created by philip on 3/30/15.
 */
public class AeronClaimSubscriber extends AeronSubscriber implements ThroughputencySubscriberImpl
{
    private BufferClaim bufferClaim = null;

    public AeronClaimSubscriber()
    {
        super();
        bufferClaim = new BufferClaim();
    }

    public void msgHandler(DirectBuffer buffer, int offset, int length, Header header)
    {
        if (pub.tryClaim(length, bufferClaim))
        {
            try
            {
                MutableDirectBuffer newBuffer = bufferClaim.buffer();
                int newOffset = bufferClaim.offset();
                newBuffer.putBytes(newOffset, buffer, offset, length);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                bufferClaim.commit();
            }
        }
        else
        {
            msgHandler(buffer, offset, length, header);
        }
    }
}
