/*

 */
package com.scut.jrsync;

import java.io.IOException;
import java.io.InputStream;
//¶ÁÎÄ¼þÁ÷
public class MD5InputStream extends InputStream {

    private final InputStream decorated;
    private final MD5 md4;

    public MD5InputStream(InputStream decorated) {
        this.decorated = decorated;
        this.md4 = new MD5();
    }

    @Override
    public int read() throws IOException {
        final int r = this.decorated.read();
        if (r >= 0) {
            this.md4.engineUpdate((byte) r);
        }
        return r;
    }

    @Override
    public int read(byte[] buffer, int off, int len) throws IOException {
        final int actual = this.decorated.read(buffer, off, len);
        if (actual > 0) {
            this.md4.engineUpdate(buffer, off, actual);
        }
        return actual;
    }

    @Override
    public void close() throws IOException {
        this.decorated.close();
    }

    public byte[] getDigest() {
        return this.md4.engineDigest();
    }

}
