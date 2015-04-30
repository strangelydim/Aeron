/*
 * Copyright 2015 Kaazing Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.aeron.tools;

import java.io.IOException;
import java.io.InputStream;

/**
 * This is an InputStream that returns random data using {@link uk.co.real_logic.aeron.tools.TLRandom}
 * Data is generated on the read() call and not stored, so marking the stream will not work.
 * There is no end of file for the input stream.
 * Created by bhorst on 3/17/15.
 */
public class RandomInputStream extends InputStream
{
    /**
     * There are always random numbers available.
     * @return Integer.MAX_VALUE
     */
    @Override
    public int available()
    {
        return Integer.MAX_VALUE;
    }

    /**
     * Can't go back in the random stream, sorry.
     * @return Always false
     */
    @Override
    public boolean markSupported()
    {
        return false;
    }

    @Override
    public int read() throws IOException
    {
        return TLRandom.current().nextInt() & 0x0000_00FF;
    }

    /**
     * Does nothing.
     * @param b
     * @return The value passed in
     */
    @Override
    public long skip(final long b)
    {
        return b;
    }

    /**
     * Returns between 0 and up to 400 bytes, or less if the buffer is not large enough.
     * @param b
     * @return
     */
    @Override
    public int read(final byte[] b) throws IOException
    {
        int bytesRead = TLRandom.current().nextInt(400);
        if (bytesRead > b.length)
        {
            bytesRead = b.length;
        }
        return read(b, 0, bytesRead);
    }

    /**
     * Put random data into the byte array. This will always read the given length.
     * @param b
     * @param off Where to start in the buffer
     * @param len Amount of bytes to read
     * @return Always the same as len
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException
    {
        int remaining = len;
        int offset = off;
        while (remaining >= 4)
        {
            final int data = TLRandom.current().nextInt();
            b[offset++] = (byte)(data >>> 24);
            b[offset++] = (byte)(data >>> 16);
            b[offset++] = (byte)(data >>> 8);
            b[offset++] = (byte)data;
            remaining -= 4;
        }
        final int data = TLRandom.current().nextInt();
        while (remaining > 0)
        {
            b[offset++] = (byte)(data >>> remaining * 8);
            remaining -= 1;
        }
        return len;
    }
}