package com.kiva;

import com.kiva.ramdisk.RamdiskType;

import java.io.Serializable;

/**
 * @author kiva
 */

public class BootImageInfo implements Serializable {
    public static final int BOOT_MAGIC_SIZE = 8;
    public static final int BOOT_NAME_SIZE = 16;
    public static final int BOOT_ARGS_SIZE = 512;
    public static final int UNUSED_SIZE = 4;

    public transient byte[] magic;
    public int kernelSize;  /* size in bytes */
    public int kernelAddr;  /* physical load addr */

    public int ramdiskSize; /* size in bytes */
    public int ramdiskAddr; /* physical load addr */

    public int secondSize;  /* size in bytes */
    public int secondAddr;  /* physical load addr */

    public int tagsAddr;    /* physical addr for kernel tags */
    public int pageSize;    /* flash page size we assume */
    public int dtSize;      /* device tree size in bytes */
    public transient byte[] unused;       /* future expansion: should be 0 */
    public byte[] name; /* asciiz product name */

    public byte[] cmdline;

    public transient int[] id; /* timestamp / checksum / sha1 / etc */

    public BootType bootType;
    public RamdiskType ramdiskType;
}
