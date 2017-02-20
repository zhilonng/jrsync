/*
 */
package com.scut.jrsync;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;

import com.scut.jrsync.MD5InputStream;
import com.scut.jrsync.MD5OutputStream;
import com.scut.jrsync.StreamHelper;

public class MD5StreamTest {

    @Test
    public void testReadBytes() throws Exception {
        final byte[] input = TestHelper.toIso("dies ist ein test");
        final MD5InputStream md4 = new MD5InputStream(new ByteArrayInputStream(input));
        for (final byte element : input) {
            assertEquals(element, md4.read());
        }
        assertEquals(-1, md4.read());

        assertEquals(
                TestHelper.toHexString(TestHelper.md4("dies ist ein test")),
                TestHelper.toHexString(md4.getDigest()));
    }

    @Test
    public void testReadByteArray() throws Exception {
        final byte[] input = TestHelper.toIso("dies ist ein test");
        final MD5InputStream md4 = new MD5InputStream(new ByteArrayInputStream(input));
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        StreamHelper.copy(md4, output, input.length);

        assertEquals(
                TestHelper.toHexString(input),
                TestHelper.toHexString(output.toByteArray()));

        assertEquals(
                TestHelper.toHexString(TestHelper.md4("dies ist ein test")),
                TestHelper.toHexString(md4.getDigest()));
    }

    @Test
    public void testReadByteArrayMultipleTimes() throws Exception {
        final byte[] input = TestHelper.toIso("dies ist ein test");
        final MD5InputStream md4 = new MD5InputStream(new ByteArrayInputStream(input));
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        StreamHelper.copy(md4, output, 10);
        StreamHelper.copy(md4, output, 10);
        StreamHelper.copy(md4, output, 10);

        assertEquals(
                TestHelper.toHexString(input),
                TestHelper.toHexString(output.toByteArray()));

        assertEquals(
                TestHelper.toHexString(TestHelper.md4("dies ist ein test")),
                TestHelper.toHexString(md4.getDigest()));
    }

    @Test
    public void testReadWithBufferedStream() throws Exception {
        final byte[] input = TestHelper.toIso("dies ist ein test");
        final MD5InputStream md4 = new MD5InputStream(new ByteArrayInputStream(input));
        final BufferedInputStream buffered = new BufferedInputStream(md4);
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        StreamHelper.copy(buffered, output, input.length);

        assertEquals(
                TestHelper.toHexString(input),
                TestHelper.toHexString(output.toByteArray()));

        assertEquals(
                TestHelper.toHexString(TestHelper.md4("dies ist ein test")),
                TestHelper.toHexString(md4.getDigest()));
    }

    @Test
    public void testWriteBytes() throws Exception {
        final byte[] data = TestHelper.toIso("dies ist ein test");
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final MD5OutputStream md4 = new MD5OutputStream(buffer);
        md4.write(data);
        assertEquals("dies ist ein test", TestHelper.fromIso(buffer.toByteArray()));
        assertEquals(
                TestHelper.toHexString(TestHelper.md4("dies ist ein test")),
                TestHelper.toHexString(md4.getDigest()));
    }

}
