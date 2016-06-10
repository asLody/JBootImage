package com.kiva.ramdisk;

import java.io.File;
import java.io.IOException;

/**
 * @author kiva
 */

public abstract class RamdiskProcessor {
    public abstract void decompress(byte[] buffer, String outputDir)
            throws IOException;

    public abstract void compress(String fromDir, String outputName, File headerFile, File footerFile)
            throws IOException;
}
