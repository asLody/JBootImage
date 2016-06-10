package com.kiva.ramdisk;

import com.kiva.BootType;

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author kiva
 */
public enum RamdiskType {
    GZIP(new GZipMimeMatcher()),
    LZMA(new LzmaMimeMatcher()),
    LZOP(new LzopMimeMatcher()),
    LZ4(new Lz4MimeMatcher()),
    XZ(new XzMimeMatcher());
    
    public static RamdiskType detect(BootType bootType, byte[] bytes) {
        for (RamdiskType type : RamdiskType.values()) {
            if (type.matcher.isMatch(bootType, bytes)) {
                return type;
            }
        }
        return null;
    }

    public final MimeMatcher matcher;

    RamdiskType(MimeMatcher matcher) {
        this.matcher = matcher;
    }

    public interface MimeMatcher {
        boolean isMatch(BootType bootType, byte[] bytes);
    }

    private static class GZipMimeMatcher implements MimeMatcher {

        private final static int GZIP_MAGIC = 0x8b1f;

        @Override
        public boolean isMatch(BootType bootType, byte[] bytes) {
            try {
                return readUShort(new ByteArrayInputStream(bytes)) == GZIP_MAGIC;
            } catch (IOException e) {
                // Will not going to happen.
            }
            // But we have to process it.
            return false;
        }

        private int readUShort(InputStream in) throws IOException {
            int b = readUByte(in);
            return (readUByte(in) << 8) | b;
        }

        private int readUByte(InputStream in) throws IOException {
            int b = in.read();
            if (b == -1) {
                throw new EOFException();
            }
            if (b < -1 || b > 255) {
                // Report on this.in, not argument in; see read{Header, Trailer}.
                throw new IOException("read() returned value out of range -1..255: " + b);
            }
            return b;
        }
    }

    private static class LzmaMimeMatcher implements MimeMatcher {
        @Override
        public boolean isMatch(BootType bootType, byte[] bytes) {
            if (bytes == null || bytes.length < 3) {
                return false;
            }

            if (bytes[0] != 0x5d) {
                return false;
            }

            if (bytes[1] != 0) {
                return false;
            }

            if (bytes[2] != 0) {
                return false;
            }
            return true;
        }
    }

    private static class LzopMimeMatcher implements MimeMatcher {
        @Override
        public boolean isMatch(BootType bootType, byte[] bytes) {
            return false;
        }
    }

    private static class Lz4MimeMatcher implements MimeMatcher {
        @Override
        public boolean isMatch(BootType bootType, byte[] bytes) {
            return false;
        }
    }

    private static class XzMimeMatcher implements MimeMatcher {
        @Override
        public boolean isMatch(BootType bootType, byte[] bytes) {
//            return XZCompressorInputStream.matches(bytes, bytes.length);
            return false;
        }
    }
}
