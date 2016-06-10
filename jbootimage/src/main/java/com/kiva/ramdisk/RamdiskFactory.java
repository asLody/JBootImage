package com.kiva.ramdisk;


import com.kiva.BootType;
import com.kiva.ramdisk.processor.GZipRamdiskProcessor;

/**
 * @author kiva
 */

public class RamdiskFactory {
    public static Ramdisk makeRamdisk(BootType bootType, byte[] buffer)
            throws UnsupportedOperationException {
        Ramdisk ramdisk = null;
        switch (bootType) {
            case MTK:
                ramdisk = new MTKRamdisk(buffer);
                break;
            case QUALCOMM:
                ramdisk = new QualcommRamdisk(buffer);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported boot type: " + bootType);
        }

        RamdiskType ramdiskType = RamdiskType.detect(bootType, ramdisk.getRamdiskBuffer());
        ramdisk.setRamdiskProcessor(makeRamdiskProcessor(ramdiskType));
        ramdisk.setRamdiskType(ramdiskType);
        return ramdisk;
    }

    public static RamdiskProcessor makeRamdiskProcessor(RamdiskType ramdiskType)
        throws UnsupportedOperationException {
        if (ramdiskType != null) {
            switch (ramdiskType) {
                case GZIP:
                    return new GZipRamdiskProcessor();
                // TODO
            }
        }

        throw new UnsupportedOperationException("Unsupported ramdisk format: " + ramdiskType);
    }
}
