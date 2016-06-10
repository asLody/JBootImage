package com.kiva;

import com.kiva.ramdisk.Ramdisk;
import com.kiva.ramdisk.RamdiskFactory;
import com.kiva.ramdisk.RamdiskProcessor;
import com.kiva.ramdisk.RamdiskType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

import static com.kiva.BootType.MTK;

/**
 * @author Lody
 *
 */
public class BootImage implements Constant {
    private static final byte MAGIC[] = {0x41, 0x4E, 0x44, 0x52, 0x4F, 0x49, 0x44, 0x21};

    private BootImageInfo bootImageInfo;
    private ByteArray image;
    private Ramdisk ramdisk;
    private boolean isUnpack;
    private String source;

    private byte[] kernelBuf;
    private byte[] ramdiskBuf;
    private byte[] secondBuf;

    private BootImage(String source, boolean isUnpack) {
        this.isUnpack = isUnpack;
        this.source = source;
    }

    public static BootImage fromFile(String file) throws IOException {
        File bootFile = new File(file);
        if (!bootFile.isFile()) {
            throw new IOException("Invalid file: " + file);
        }

        BootImage image = new BootImage(file, true);

        image.image = ByteArray.wrap(IOUtils.readFully(bootFile));
        image.bootImageInfo = new BootImageInfo();

        image.detectBootType();
        image.loadImage();
        return image;
    }

    public static BootImage fromDir(String dir) throws IOException {
        File bootDir = new File(dir);
        if (!bootDir.isDirectory()) {
            throw new IOException("Invalid directory: " + dir);
        }

        BootImage image = new BootImage(dir, false);
        image.loadConfig(new File(bootDir, CONFIG_FILE_NAME));
        return image;
    }

    private void loadConfig(File file) throws IOException {
        if (isUnpack) {
            return;
        }

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        try {
            bootImageInfo = (BootImageInfo) ois.readObject();
        } catch (ClassNotFoundException e) {
            // Will not going happen
        }
        ois.close();
    }

    private void detectBootType() {
        if (!isUnpack) {
            return;
        }

        // TODO
        bootImageInfo.bootType = MTK;
    }

    private void loadImage() throws IOException {
        if (!isUnpack) {
            return;
        }

        bootImageInfo.magic = image.readBytes(BootImageInfo.BOOT_MAGIC_SIZE);
        if (!checkMagic(bootImageInfo.magic)) {
            throw new IOException("Invalid boot.img");
        }

        bootImageInfo.kernelSize = image.readU4();
        bootImageInfo.kernelAddr = image.readU4();
        bootImageInfo.ramdiskSize = image.readU4();
        bootImageInfo.ramdiskAddr = image.readU4();
        bootImageInfo.secondSize = image.readU4();
        bootImageInfo.secondAddr = image.readU4();
        bootImageInfo.tagsAddr = image.readU4();
        bootImageInfo.pageSize = image.readU4();
        bootImageInfo.dtSize = image.readU4();
        bootImageInfo.unused = image.readBytes(BootImageInfo.UNUSED_SIZE);
        bootImageInfo.name = image.readBytes(BootImageInfo.BOOT_NAME_SIZE);
        bootImageInfo.cmdline = image.readBytes(BootImageInfo.BOOT_ARGS_SIZE);
        bootImageInfo.id = null;// Needn't

        // Peek kernel
        image.seek(bootImageInfo.pageSize);
        kernelBuf = image.readBytes(bootImageInfo.kernelSize);

        // Peek Ramdisk
        int ramdiskPos = (((bootImageInfo.kernelSize + bootImageInfo.pageSize)
                / (bootImageInfo.pageSize * 2)) + 1)
                * (bootImageInfo.pageSize * 2);
        image.seek(ramdiskPos);
        ramdiskBuf = image.readBytes(bootImageInfo.ramdiskSize);

        if (bootImageInfo.secondSize > 0) {
            // Peek Second stage
            int secondPos = (((ramdiskPos + bootImageInfo.ramdiskSize)
                    / (bootImageInfo.pageSize * 2)) + 1)
                    * (bootImageInfo.pageSize * 2);
            image.seek(secondPos);
            secondBuf = image.readBytes(bootImageInfo.secondSize);
        }

        ramdisk = RamdiskFactory.makeRamdisk(bootImageInfo.bootType, getRamdiskBuffer());
        if (ramdisk == null) {
            throw new IOException("Failed to create ramdisk instance");
        }

        bootImageInfo.ramdiskType = ramdisk.getRamdiskType();
    }

    private static boolean checkMagic(byte[] magic) {
        return Arrays.equals(magic, MAGIC);
    }

    public void unpack(String outputDir) throws IOException {
        if (!isUnpack) {
            return;
        }

        File outputDirFile = new File(outputDir);

        FileOutputStream kernelOutput = null;
        FileOutputStream secondOutput = null;
        ObjectOutputStream configOutput = null;
        try {
            if (!outputDirFile.exists()) {
                if (!outputDirFile.mkdirs()) {
                    throw new IOException("Failed to create output directory");
                }
            }

            kernelOutput = new FileOutputStream(
                    new File(outputDirFile, KERNEL_FILE_NAME));
            kernelOutput.write(getKernelBuffer());

            if (bootImageInfo.secondSize > 0) {
                secondOutput = new FileOutputStream(
                        new File(outputDirFile, SECOND_FILE_NAME));
                secondOutput.write(getSecondBuffer());
            }

            ramdisk.writeExtraFile(outputDir);
            ramdisk.getRamdiskProcessor()
                    .decompress(ramdisk.getRamdiskBuffer(),
                            (outputDirFile.getAbsolutePath()
                                    + File.separator + RAMDISK_DIR_NAME));

            configOutput = new ObjectOutputStream(new FileOutputStream(
                    new File(outputDirFile, CONFIG_FILE_NAME)));
            configOutput.writeObject(bootImageInfo);
        } catch (Exception e) {
            IOUtils.delete(outputDirFile);
            throw new IOException(e);
        } finally {
            IOUtils.safeClose(kernelOutput);
            IOUtils.safeClose(secondOutput);
            IOUtils.safeClose(configOutput);
        }
    }

    public void repack(String outputFile) throws IOException {
        if (isUnpack) {
            return;
        }

        File kernelFile = new File(source, KERNEL_FILE_NAME);
        if (!kernelFile.exists()) {
            throw new IOException("kernel does not exist");
        }

        File packedRamdiskFile = new File(source, RAMDISK_PACKED_FILE_NAME);
        File ramdiskDir = new File(source, RAMDISK_DIR_NAME);
        File secondFile = new File(source, SECOND_FILE_NAME);

        if (!ramdiskDir.exists() && !packedRamdiskFile.exists()) {
            throw new IOException("Neither ramdisk dir nor packed ramdisk exists");
        }

        if (getSecondSize() > 0 && !secondFile.exists()) {
            throw new IOException("Second stage file does not exist");
        }

        if (ramdiskDir.exists()) {
            RamdiskProcessor processor = RamdiskFactory.makeRamdiskProcessor(getRamdiskType());
            if (processor == null) {
                throw new IOException("Failed to create ramdisk processor");
            }

            File headerFile = new File(source, HEADER_FILE_NAME);
            File footerFile = new File(source, FOOTER_FILE_NAME);
            processor.compress(ramdiskDir.getAbsolutePath(),
                    packedRamdiskFile.getAbsolutePath(),
                    headerFile, footerFile);
        }

        int kernelSize = (int) kernelFile.length();
        int ramdiskSize = (int) packedRamdiskFile.length();

        RandomAccessFile raf = new RandomAccessFile(outputFile, "rw");
        raf.seek(0);
        raf.write(MAGIC);
        raf.writeInt(kernelSize);
        raf.writeInt(getKernelAddr());
        raf.writeInt(ramdiskSize);
        raf.writeInt(getRamdiskAddr());
        raf.writeInt(getSecondSize());
        raf.writeInt(getSecondAddr());
        raf.writeInt(getTagsSize());
        raf.writeInt(getPageSize());
        raf.writeInt(getDtSize());
        raf.write(new byte[BootImageInfo.UNUSED_SIZE]);
        raf.write(getName());
        raf.write(getCmdline());
        // id

        // kernel
        raf.seek(getPageSize());
        raf.write(IOUtils.readFully(kernelFile));

        // ramdisk
        int ramdiskPos = (((kernelSize + bootImageInfo.pageSize)
                / (bootImageInfo.pageSize * 2)) + 1)
                * (bootImageInfo.pageSize * 2);
        raf.seek(ramdiskPos);
        raf.write(IOUtils.readFully(packedRamdiskFile));

        // second
        if (getSecondSize() > 0) {
            int secondPos = (((ramdiskSize / (bootImageInfo.pageSize * 2)) + 1)
                    * (bootImageInfo.pageSize * 2)) + ramdiskPos;
            raf.seek(secondPos);
            raf.write(IOUtils.readFully(secondFile));
        }

        raf.close();
    }


    public byte[] getMagic() {
        return bootImageInfo.magic;
    }

    public int getKernelSize() {
        return bootImageInfo.kernelSize;
    }

    public int getKernelAddr() {
        return bootImageInfo.kernelAddr;
    }

    public int getRamdiskSize() {
        return bootImageInfo.ramdiskSize;
    }

    public int getRamdiskAddr() {
        return bootImageInfo.ramdiskAddr;
    }

    public int getSecondSize() {
        return bootImageInfo.secondSize;
    }

    public int getSecondAddr() {
        return bootImageInfo.secondAddr;
    }

    public int getTagsSize() {
        return bootImageInfo.tagsAddr;
    }

    public int getPageSize() {
        return bootImageInfo.pageSize;
    }

    public int getDtSize() {
        return bootImageInfo.dtSize;
    }

    public byte[] getUnused() {
        return bootImageInfo.unused;
    }

    public byte[] getName() {
        return bootImageInfo.name;
    }

    public byte[] getCmdline() {
        return bootImageInfo.cmdline;
    }

    public byte[] getKernelBuffer() {
        return kernelBuf;
    }

    public byte[] getRamdiskBuffer() {
        return ramdiskBuf;
    }

    public byte[] getSecondBuffer() {
        return secondBuf;
    }

    public Ramdisk getRamdisk() {
        return ramdisk;
    }

    public RamdiskType getRamdiskType() {
        return bootImageInfo.ramdiskType;
    }

    public BootType getBootType() {
        return bootImageInfo.bootType;
    }
}
