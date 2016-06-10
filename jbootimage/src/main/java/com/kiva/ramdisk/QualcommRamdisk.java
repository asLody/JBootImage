package com.kiva.ramdisk;

/**
 * @author kiva
 */
public class QualcommRamdisk extends Ramdisk {
    private byte[] ramdisk;

    public QualcommRamdisk(byte[] bytes) {
        this.ramdisk = bytes;
    }

    @Override
    public byte[] getRamdiskBuffer() {
        return this.ramdisk;
    }
}
