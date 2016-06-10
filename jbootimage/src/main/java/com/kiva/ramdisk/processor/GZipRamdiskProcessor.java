package com.kiva.ramdisk.processor;

import com.kiva.Constant;
import com.kiva.IOUtils;
import com.kiva.ramdisk.RamdiskProcessor;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author kiva
 */

public class GZipRamdiskProcessor extends RamdiskProcessor {
    private byte[] decompressGzip(byte[] ramdisk) {
        GZIPInputStream is = null;

        try {
            is = new GZIPInputStream(new ByteArrayInputStream(ramdisk));
            return IOUtils.readFully(is);

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            IOUtils.safeClose(is);
        }

        return null;
    }

    @Override
    public void decompress(byte[] buffer, String outputDir) throws IOException {
        CpioArchiveInputStream is = null;
        File outputDirFile = new File(outputDir);

        try {
            if (!outputDirFile.exists()) {
                if (!outputDirFile.mkdirs()) {
                    throw new IOException("Failed to create output directory");
                }
            }

            byte[] cpioBuffer = decompressGzip(buffer);
            if (cpioBuffer == null) {
                throw new IOException("Failed to decompress gzip");
            }

            is = new CpioArchiveInputStream(new ByteArrayInputStream(cpioBuffer));
            CpioArchiveEntry entry;
            while ((entry = is.getNextCPIOEntry()) != null) {
                File file = new File(outputDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!file.exists() && !file.mkdirs()) {
                        throw new IOException("Failed to create directory");
                    }
                } else {
                    file.createNewFile();
                    IOUtils.write(file, IOUtils.read(is, (int) entry.getSize()));
                }
            }

        } catch (Exception e) {
            IOUtils.delete(outputDirFile);
            throw new IOException(e);
        } finally {
            IOUtils.safeClose(is);
        }
    }

    @Override
    public void compress(String fromDir, String outputName, File headerFile, File footerFile)
            throws IOException {
        File fromDirFile = new File(fromDir);
        if (!fromDirFile.exists()) {
            throw new IOException("fromDir does not exist.");
        }

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        CpioArchiveOutputStream cpioOut = new CpioArchiveOutputStream(byteOut);
        addAllFilesToCpio(cpioOut, fromDirFile);
        cpioOut.close();

        byte[] cpioBuffer = byteOut.toByteArray();
        byteOut = new ByteArrayOutputStream();
        GZIPOutputStream gzipOut = new GZIPOutputStream(byteOut);
        gzipOut.write(cpioBuffer);
        gzipOut.close();

        FileOutputStream os = new FileOutputStream(outputName);
        addFileIfFound(os, headerFile);
        os.write(byteOut.toByteArray());
        addFileIfFound(os, footerFile);
        os.close();
        byteOut.close();
    }

    private void addFileIfFound(FileOutputStream os, File file) throws IOException {
        if (file.exists()) {
            os.write(IOUtils.readFully(file));
        }
    }

    private void addAllFilesToCpio(CpioArchiveOutputStream cpioOut, File fromDirFile) throws IOException {
        for (String path : fromDirFile.list()) {
            File file = new File(fromDirFile, path);

            String cpioName = file.getAbsolutePath().substring(fromDirFile.getAbsolutePath().length());
            ArchiveEntry cpio = cpioOut.createArchiveEntry(file, cpioName);
            cpioOut.putArchiveEntry(cpio);

            if (file.isDirectory()) {
                cpioOut.closeArchiveEntry();
                addAllFilesToCpio(cpioOut, file);
            } else {
                byte[] content = IOUtils.readFully(file);
                cpioOut.write(content);
                cpioOut.closeArchiveEntry();
            }
        }
    }
}
