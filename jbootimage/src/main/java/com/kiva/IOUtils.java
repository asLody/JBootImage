package com.kiva;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author kiva
 */

public class IOUtils {

    public static void safeClose(Closeable is) {
        if (is != null) {
            try {
                is.close();
            } catch (Throwable e) {
                //Ignore
            }
        }
    }

    public static void write(File file, byte[] bytes) throws IOException {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(bytes);
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    public static byte[] read(InputStream is, int count) throws IOException {
        byte[] buf = new byte[count];
        is.read(buf);
        return buf;
    }

    public static byte[] readFully(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[128];
        int i;
        for (;;) {
            if ((i = is.read(buffer)) < 0) {
                break;
            }
            os.write(buffer, 0, i);
        }
        return os.toByteArray();
    }

    public static boolean delete(File dir) {
        if (!dir.exists()) {
            return false;
        }

        if (dir.isDirectory()) {
            String[] children = dir.list();
            //递归删除目录中的子目录下
            for (String child : children) {
                boolean success = delete(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }

    public static byte[] readFully(File file) throws IOException {
        FileInputStream is = new FileInputStream(file);
        byte[] bytes = readFully(is);
        is.close();
        return bytes;
    }
}
