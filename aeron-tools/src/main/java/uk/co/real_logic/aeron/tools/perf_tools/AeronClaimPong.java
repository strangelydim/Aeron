package uk.co.real_logic.aeron.tools.perf_tools;

import uk.co.real_logic.aeron.common.concurrent.logbuffer.BufferClaim;
import uk.co.real_logic.aeron.common.concurrent.logbuffer.Header;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;


/**
 * Created by philip on 3/27/15.
 */
public class AeronClaimPong extends AeronPong
{
    private BufferClaim bufferClaim = null;

    public AeronClaimPong()
    {
        super();
    }

    public void prepare()
    {
        super.prepare();
        bufferClaim = new BufferClaim();
    }


    public void pingHandler(DirectBuffer buffer, int offset, int length, Header header)
    {
        if (buffer.getByte(offset) == (byte)'q')
        {
            running.set(false);
            return;
        }
        if (pongPub.tryClaim(length, bufferClaim))
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
            pingHandler(buffer, offset, length, header);
        }
    }
}
