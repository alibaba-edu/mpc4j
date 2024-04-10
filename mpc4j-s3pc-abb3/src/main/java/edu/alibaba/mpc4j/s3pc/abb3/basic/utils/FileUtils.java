package edu.alibaba.mpc4j.s3pc.abb3.basic.utils;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;

/**
 * Utilities for file operation
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class FileUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    public static void writeFile(BitVector[] data, String filePath){
        writeByteFile(filePath, bitVectorToFileMsg(data));
    }

    public static BitVector[] readFileIntoBitVectors(String filePath, boolean deleteFlag){
        return fileMsgToBitVector(readByteFile(filePath, deleteFlag));
    }

    public static void writeFile(long[][] data, String filePath){
        writeByteFile(filePath, longMatrixToFileMsg(data));
    }

    public static long[][] readFileIntoLongMatrix(String filePath, boolean deleteFlag){
        return fileMsgToLongMatrix(readByteFile(filePath, deleteFlag));
    }


    /***
     * save a byte matrix into a new-created file
     * @param filePath file path + name
     * @param input byte matrix
     */
    protected static void writeByteFile(String filePath, byte[][] input) {
        File file = new File(filePath);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            for (byte[] x : input) {
                fos.write(x);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /***
     * read the file and return all data
     * @param filePath file path + name
     */
    protected static byte[] readByteFile(String filePath, boolean deleteFlag) {
        try {
            File file = new File(filePath);
            assert file.exists();
            int length = (int) file.length();
            byte[] data = new byte[length];
            FileInputStream tmp = new FileInputStream(file);
            int success = tmp.read(data);
            if (success < 0) {
                throw new Exception("fail to read the files");
            }
            tmp.close();
            if(deleteFlag){
                deleteFile(filePath);
            }
            return data;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    protected static byte[][] bitVectorToFileMsg(BitVector[] data) {
        // encode method:
        //      1. add a new byte array
        //          1.1 add number of vectors
        //          1.2 add bitNum of each vectors
        //      2. add byte arrays
        byte[][] res = new byte[data.length + 1][];
        int[] info = new int[data.length + 1];
        info[0] = data.length;
        for (int i = 0; i < data.length; i++) {
            info[i + 1] = data[i].bitNum();
            res[i + 1] = data[i].getBytes();
        }
        res[0] = IntUtils.intArrayToByteArray(info);
        return res;
    }

    protected static BitVector[] fileMsgToBitVector(byte[] msg) {
        // reverse the encoding
        int num = IntUtils.byteArrayToInt(Arrays.copyOf(msg, 4));
        int endPos = (num + 1) << 2;
        int[] bitNums = IntUtils.byteArrayToIntArray(Arrays.copyOfRange(msg, 4, endPos));
        BitVector[] res = new BitVector[num];
        for (int i = 0; i < num; i++) {
            int byteLen = CommonUtils.getByteLength(bitNums[i]);
            res[i] = BitVectorFactory.create(bitNums[i], Arrays.copyOfRange(msg, endPos, endPos + byteLen));
            endPos += byteLen;
        }
        MathPreconditions.checkEqual("endPos", "msg.length", endPos, msg.length);
        return res;
    }

    protected static byte[][] longMatrixToFileMsg(long[][] data) {
        // encode method:
        //      1. add a new byte array
        //          1.1 add number of vectors
        //          1.2 add length of each vectors
        //      2. add byte arrays
        byte[][] res = new byte[data.length + 1][];
        int[] info = new int[data.length + 1];
        info[0] = data.length;
        for (int i = 0; i < data.length; i++) {
            info[i + 1] = data[i].length;
            res[i + 1] = LongUtils.longArrayToByteArray(data[i]);
        }
        res[0] = IntUtils.intArrayToByteArray(info);
        return res;
    }

    protected static long[][] fileMsgToLongMatrix(byte[] msg) {
        // reverse the encoding
        int num = IntUtils.byteArrayToInt(Arrays.copyOf(msg, 4));
        int endPos = (num + 1) << 2;
        int[] lengths = IntUtils.byteArrayToIntArray(Arrays.copyOfRange(msg, 4, endPos));
        long[][] res = new long[num][];
        for (int i = 0; i < num; i++) {
            int byteLen = lengths[i] << 3;
            res[i] = LongUtils.byteArrayToLongArray(Arrays.copyOfRange(msg, endPos, endPos + byteLen));
            endPos += byteLen;
        }
        MathPreconditions.checkEqual("endPos", "msg.length", endPos, msg.length);
        return res;
    }

    /***
     * delete the given file
     * @param filePath file path + name
     */
    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        try {
            if (!file.exists()) {
                throw new Exception("This file does not exist: " + filePath);
            }
            boolean delRes = file.delete();
            if (!delRes) {
                throw new Exception("This file can not be deleted: " + filePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static boolean deleteSingleFile(String filePath) {
        File file = new File(filePath);
        try {
            if (!file.exists()) {
                LOGGER.error("不存在对应文件" + filePath);
                return false;
            }
            boolean delRes = file.delete();
            if (!delRes) {
                LOGGER.error("无法删除对应文件" + filePath);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean deleteAllFile(String dir) {
        File dirFile = new File(dir);
        // 如果dir对应的文件不存在，或者不是一个目录，则退出
        if ((!dirFile.exists()) || (!dirFile.isDirectory())) {
            LOGGER.error("删除文件夹失败：" + dir + "不存在！");
            return false;
        }
        // 删除文件夹中的所有文件包括子文件夹
        File[] files = dirFile.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                // 删除子文件
                if (file.isFile()) {
                    if (!deleteSingleFile(file.getAbsolutePath())) {
                        LOGGER.error("删除文件  {}  失败！", file.getAbsolutePath());
                        return false;
                    }
                }
                // 删除子文件夹
                else if (file.isDirectory()) {
                    if (!deleteAllFile(file.getAbsolutePath())) {
                        LOGGER.error("删除文件夹  {}  失败！", file.getAbsolutePath());
                        return false;
                    }
                }
            }
        }
        // 删除当前文件夹
        return dirFile.delete();
    }
}
