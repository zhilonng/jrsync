/*
 */
package com.scut.jrsync;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestHelperTest {

    @Test
    public void testToHexString() {
        assertEquals("", TestHelper.toHexString(new byte[0]));
        assertEquals("01", TestHelper.toHexString(new byte[] {1}));
        assertEquals("0A10", TestHelper.toHexString(new byte[] {10, 16}));
        assertEquals("FF", TestHelper.toHexString(new byte[] {(byte) 0xFF}));
    }

}
