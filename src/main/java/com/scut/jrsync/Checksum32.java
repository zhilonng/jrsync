
package com.scut.jrsync;

/**
 * ������У����
 * (1)��a(k,l)= (xk+.....xl) mod M
 * (2)��b(b,l)=((l-k+1)xk+...(l-l+1)xl) mod M
 * (3)��s(k,l) = a(k,l) +2^16 b(k,l)
 * M = 65521
 * s(k+1,l+1)������a(k+l,l+l) �� b(k+1,l+1) ���ٵõ���
 * a(k+1,l+1) = (a(k,l)-xk+x(l+1)) mod M
 * b(k+1,l+1) = (b(k,l)-(l-k+1)xk+a(k+1,l+1)) mod M
 * 
 */
public class Checksum32 {

    /**
     * The first half of the checksum.
     */
    private int a;

    /**
     * The second half of the checksum.
     */
    private int b;

    /**
     * The place from whence the current checksum has been computed. ��ʼ��
     */
    private int k;   

    /**
     * The place to where the current checksum has been computed.  ������  �γ�data(k,l)
     */
    private int l;

    /**
     * The block from which the checksum is computed. bocksize��С
     */
    private byte[] block;

    public Checksum32() {
        this.a = 0;
        this.b = 0;
        this.k = 0;
    }

    /**
     * Return the value of the currently computed checksum.
     *
     * @return The currently computed checksum.
     */
    public int getValue() {
        return (this.a & 0xffff) | (this.b << 16);
    }

    /**
     * Reset the checksum.
     */
    public void reset() {
        this.k = 0;
        this.a = 0;
        this.b = 0;
        this.l = 0;
    }

    /**
     * ��������У�������
     * "Roll" the checksum. This method takes a single byte as byte
     */
    public byte roll(byte bt) {
        final byte rollingOut = this.block[this.k];
        this.a -= rollingOut;
        this.b -= this.l * rollingOut;
        this.a += bt;
        this.b += this.a;
        this.block[this.k] = bt;
        this.k++;
        if (this.k == this.l) {
            this.k = 0;
        }
        return rollingOut;
    }

    /**
     * Update the checksum by trimming off a byte only, not adding anything.
     */
    public void trim() {
        this.a -= this.block[this.k % this.block.length];
        this.b -= this.l * (this.block[this.k % this.block.length]);
        this.k++;
        this.l--;
    }

    public static int determineFor(byte[] block) {
        final Checksum32 c = new Checksum32();
        c.check(block, 0, block.length);
        return c.getValue();
    }

    /**
     */
    public void check(byte[] buf, int off, int len) {
        this.block = new byte[len];
        System.arraycopy(buf, off, this.block, 0, len);  //��buf���ݸ��Ƶ�block��
        this.reset();
        this.l = this.block.length;
        int i;
        /*��ʽ�������
         * (1)��a(k,l)= (xk+.....xl) mod M
         * (2)��b(b,l)=((l-k+1)xk+...(l-l+1)xl) mod M
         * (3)��s(k,l) = a(k,l) +2^16 b(k,l)
         */
        for (i = 0; i < this.block.length - 4; i += 4) {
            this.b += 4 * (this.a + this.block[i])
                    + 3 * this.block[i + 1]
                    + 2 * this.block[i + 2]
                    + this.block[i + 3];
            this.a += this.block[i] + this.block[i + 1] + this.block[i + 2] + this.block[i + 3];
        }
        for (; i < this.block.length; i++) {
            this.a += this.block[i];
            this.b += this.a;
        }
    }

    public void copyBlock(byte[] buffer) {
        assert buffer.length == this.l;
        System.arraycopy(this.block, this.k, buffer, 0, this.l - this.k);
        System.arraycopy(this.block, 0, buffer, this.l - this.k, this.k);
    }

}
