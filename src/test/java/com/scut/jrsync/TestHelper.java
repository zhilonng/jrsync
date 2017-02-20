/*
 */
package com.scut.jrsync;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.scut.jrsync.Checksum32;
import com.scut.jrsync.FilePath;
import com.scut.jrsync.MD5;

public class TestHelper {

    public static String toHexString(byte[] byteArray) {
        final StringBuilder ret = new StringBuilder();
        for (final byte b : byteArray) {
            ret.append(toHexString(b));
        }
        return ret.toString();
    }

    private static String toHexString(byte b) {
        return Character.toString(toHexChar((b & 0xFF) >> 4))
            + Character.toString(toHexChar(b & 0x0F));
    }

    private static char toHexChar(int nibble) {
        if (nibble < 10) {
            return (char) ('0' + nibble);
        } else {
            return (char) ('A' + nibble - 10);
        }
    }

    public static byte[] toIso(String s) {
        try {
            return s.getBytes("ISO-8859-1");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String fromIso(byte[] content) {
        return new String(content, Charset.forName("ISO-8859-1"));
    }

    public static byte[] md4(String data) throws Exception {
        final MD5 md4 = new MD5();
        final byte[] b = toIso(data);
        md4.engineUpdate(b, 0, b.length);
        return md4.engineDigest();
    }

    public static List<String> getChildrenNames(FilePath dir) throws Exception {
        final List<String> actualNames = new ArrayList<String>();
        for (final FilePath p : dir.getChildrenSorted()) {
            actualNames.add(p.getName());
        }
        return actualNames;
    }

    public static String multiplyString(String s, int count) {
        final StringBuilder ret = new StringBuilder();
        for (int i = 0; i < count; i++) {
            ret.append(s);
        }
        return ret.toString();
    }

    public static int rollingChecksum(String block) {
        return Checksum32.determineFor(toIso(block));
    }

    public static byte[] shortMD4(String block1, int i) {
        return MD5.determineFor(toIso(block1), i);
    }

}
