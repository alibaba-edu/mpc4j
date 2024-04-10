package edu.alibaba.mpc4j.common.tool.utils;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
 * serialize utilities.
 *
 * @author Weiran Liu
 * @date 2024/1/3
 */
public class SerializeUtils {

    /**
     * private constructor.
     */
    private SerializeUtils() {
        // empty
    }

    /**
     * Compresses unequal-size data.
     *
     * @param origin original data.
     * @return compressed data.
     */
    public static byte[] compressUnequal(List<byte[]> origin) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            for (byte[] data : origin) {
                // write length
                dataOutputStream.writeInt(data.length);
                // write data
                dataOutputStream.write(data);
            }
            dataOutputStream.close();
            byteArrayOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Unexpected IOException");
        }
    }

    /**
     * Decompresses unequal-size data.
     *
     * @param compressed compressed data.
     * @return original data.
     */
    public static List<byte[]> decompressUnequal(byte[] compressed) {
        try {
            List<byte[]> origin = new LinkedList<>();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressed);
            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
            while (dataInputStream.available() > 0) {
                int expectLength = dataInputStream.readInt();
                byte[] data = new byte[expectLength];
                int actualLength = dataInputStream.read(data);
                MathPreconditions.checkEqual("expect", "actual", expectLength, actualLength);
                origin.add(data);
            }
            return origin;
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Unexpected IOException");
        }
    }

    /**
     * Compresses equal-size data.
     *
     * @param origin original data.
     * @param length length for each data.
     * @return compressed data.
     */
    public static byte[] compressEqual(List<byte[]> origin, int length) {
        // we allow equal-size with length = 0
        MathPreconditions.checkNonNegative("length", length);
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            for (byte[] data : origin) {
                MathPreconditions.checkEqual("expect", "actual", length, data.length);
                // write data
                dataOutputStream.write(data);
            }
            dataOutputStream.close();
            byteArrayOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Unexpected IOException");
        }
    }

    /**
     * Decompresses equal-size data.
     *
     * @param compressed compressed data.
     * @param length     length for each data.
     * @return original data.
     */
    public static List<byte[]> decompressEqual(byte[] compressed, int length) {
        MathPreconditions.checkNonNegative("length", length);
        try {
            List<byte[]> origin = new LinkedList<>();
            // read data
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressed);
            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
            while (dataInputStream.available() > 0) {
                byte[] data = new byte[length];
                int actualLength = dataInputStream.read(data);
                MathPreconditions.checkEqual("expect", "actual", length, actualLength);
                origin.add(data);
            }
            return origin;
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Unexpected IOException");
        }
    }
}
