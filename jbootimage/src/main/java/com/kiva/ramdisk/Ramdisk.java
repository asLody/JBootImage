package com.kiva.ramdisk;

import java.io.IOException;

/**
 * @author kiva
 */

public abstract class Ramdisk {
    private RamdiskType ramdiskType;
    private RamdiskProcessor ramdiskProcessor;

    public RamdiskProcessor getRamdiskProcessor() {
        return ramdiskProcessor;
    }

    void setRamdiskProcessor(RamdiskProcessor processor) {
        this.ramdiskProcessor = processor;
    }

    public void setRamdiskType(RamdiskType ramdiskType) {
        this.ramdiskType = ramdiskType;
    }

    public RamdiskType getRamdiskType() {
        return ramdiskType;
    }

    public void writeExtraFile(String baseDir) throws IOException {}

    public abstract byte[] getRamdiskBuffer();
}
