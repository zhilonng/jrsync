/*
   
 */
package com.scut.jrsync;

import java.io.IOException;
import java.io.OutputStream;
//Ð´ÎÄ¼þÁ÷
public class MD5OutputStream extends OutputStream {

    private final MD5 md4;
    private final OutputStream decorated;

    public MD5OutputStream(OutputStream decorated) {
        this.decorated = decorated;
        this.md4 = new MD5();
    }

    @Override
    public void write(int b) throws IOException {
        this.md4.engineUpdate((byte) b);
        this.decorated.write(b);
    }

    @Override
    public void write(byte[] buffer, int off, int len) throws IOException {
        this.md4.engineUpdate(buffer, off, len);
        this.decorated.write(buffer, off, len);
    }

    @Override
    public void close() throws IOException {
        this.decorated.close();
    }

    public byte[] getDigest() {
        return this.md4.engineDigest();
    }

}
