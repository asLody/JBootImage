package com.kiva;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author kiva
 */

public class ByteArray {
    private int length;
    private byte[] bytes;
    private int pos;

    public ByteArray(byte[] bytes) {
        this.length = bytes.length;
        this.bytes = bytes;
        seek(0);
    }

    public byte[] getBytes() {
        return bytes;
    }

    public byte[] readBytes(int count) {
        byte[] buf = new byte[count];
        int limit = count;
        if (count + pos >= length) {
            limit = length - pos - 1;
        }
        System.arraycopy(bytes, pos, buf, 0, limit);
        pos += count;
        return buf;
    }

    public int readU4() {
        return ByteBuffer.wrap(readBytes(4))
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt();
    }

    public void seek(int newPos) {
        this.pos = newPos;
    }

    public static ByteArray wrap(byte[] bytes) {
        return new ByteArray(bytes);
    }
}
