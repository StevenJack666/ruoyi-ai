package org.ruoyi.common.core.utils.openapi.sm3.child;

public abstract class GeneralDigest implements ExtendedDigest {
    private static final int BYTE_LENGTH = 64;
    private byte[] xBuf;
    private int xBufOff;
    private long byteCount;

    protected GeneralDigest() {
        this.xBuf = new byte[4];
        this.xBufOff = 0;
    }

    protected GeneralDigest(GeneralDigest t) {
        this.xBuf = new byte[t.xBuf.length];
        System.arraycopy(t.xBuf, 0, this.xBuf, 0, t.xBuf.length);

        this.xBufOff = t.xBufOff;
        this.byteCount = t.byteCount;
    }

    public void update(byte in) {
        this.xBuf[(this.xBufOff++)] = in;

        if (this.xBufOff == this.xBuf.length) {
            processWord(this.xBuf, 0);
            this.xBufOff = 0;
        }

        this.byteCount += 1L;
    }

    public void update(byte[] in, int inOff, int len) {
        do {
            update(in[inOff]);

            inOff++;
            len--;

            if (this.xBufOff == 0) {
                break;
            }
        } while (len > 0);

        while (len > this.xBuf.length) {
            processWord(in, inOff);

            inOff += this.xBuf.length;
            len -= this.xBuf.length;
            this.byteCount += this.xBuf.length;
        }

        while (len > 0) {
            update(in[inOff]);

            inOff++;
            len--;
        }
    }

    public void finish() {
        long bitLength = this.byteCount << 3;

        update((byte) -128);

        while (this.xBufOff != 0) {
            update((byte) 0);
        }

        processLength(bitLength);

        processBlock();
    }

    public void reset() {
        this.byteCount = 0L;

        this.xBufOff = 0;
        for (int i = 0; i < this.xBuf.length; i++) {
            this.xBuf[i] = 0;
        }
    }

    public int getByteLength() {
        return BYTE_LENGTH;
    }

    protected abstract void processWord(byte[] paramArrayOfByte, int paramInt);

    protected abstract void processLength(long paramLong);

    protected abstract void processBlock();
}
