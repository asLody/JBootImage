package com.kiva.ramdisk;

import com.kiva.BootImage;
import com.kiva.Constant;
import com.kiva.IOUtils;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * @author kiva
 */

public class MTKRamdisk extends Ramdisk implements Constant {
    private byte[] header;
    private byte[] ramdisk;

    public MTKRamdisk(byte[] bytes) {
        ramdisk = new byte[bytes.length - 512];
        header = new byte[512];
        System.arraycopy(bytes, 0, header, 0, 512);
        System.arraycopy(bytes, 512, ramdisk, 0, ramdisk.length);
    }

    @Override
    public void writeExtraFile(String baseDir) throws IOException {
        File headerFile = new File(baseDir, HEADER_FILE_NAME);
        IOUtils.write(headerFile, header);
    }

    @Override
    public byte[] getRamdiskBuffer() {
        return ramdisk;
    }
}
