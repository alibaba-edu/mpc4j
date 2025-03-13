package edu.alibaba.work.femur;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

import java.io.*;
import java.security.SecureRandom;

/**
 * file utilities.
 *
 * @author Weiran Liu
 * @date 2024/12/3
 */
public class FileUtils {
    /**
     * private constructor.
     */
    private FileUtils() {
        // empty
    }

    /**
     * Reads data from SOSD (Search on Sorted Data) format. Search on Sorted Data (SOSD) is a new benchmark that
     * allows researchers to compare their new (learned) index structures on both synthetic and real-world datasets.
     * Details can be found at <a href="https://learnedsystems.github.io/SOSDLeaderboard/">SOSD website</a>.
     * <p>
     * Here we read data until we reach max_num, or all data in files are read.
     *
     * @param path           data file path.
     * @param entryBitLength entry bit length.
     * @return key-value database.
     * @throws IOException if file is not found.
     */
    public static TLongObjectMap<byte[]> readSosdData(String path, int entryBitLength) throws IOException {
        return readSosdData(path, entryBitLength, new SecureRandom());
    }

    /**
     * Reads data from SOSD (Search on Sorted Data) format. Search on Sorted Data (SOSD) is a new benchmark that
     * allows researchers to compare their new (learned) index structures on both synthetic and real-world datasets.
     * Details can be found at <a href="https://learnedsystems.github.io/SOSDLeaderboard/">SOSD website</a>.
     * <p>
     * Here we read data until we reach max_num, or all data in files are read.
     *
     * @param path           data file path.
     * @param entryBitLength entry bit length.
     * @param secureRandom   random state.
     * @return key-value database.
     * @throws IOException if file is not found.
     */
    public static TLongObjectMap<byte[]> readSosdData(String path, int entryBitLength, SecureRandom secureRandom) throws IOException {
        MathPreconditions.checkPositive("entryBitLength", entryBitLength);
        File fileName = new File(path);
        int entryByteLength = CommonUtils.getByteLength(entryBitLength);
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileName));
        byte[] bytes = new byte[Long.BYTES];
        // read total num
        Preconditions.checkArgument(in.read(bytes) != -1);
        BytesUtils.reverseByteArray(bytes);
        long num = LongUtils.byteArrayToLong(bytes);
        TLongObjectMap<byte[]> keyValueDatabase = new TLongObjectHashMap<>((int) num);
        for (int i = 0; i < num; i++) {
            Preconditions.checkArgument(in.read(bytes) != -1);
            BytesUtils.reverseByteArray(bytes);
            byte[] entry = BytesUtils.randomByteArray(entryByteLength, entryBitLength, secureRandom);
            keyValueDatabase.put(LongUtils.byteArrayToLong(bytes), entry);
        }
        in.close();
        return keyValueDatabase;
    }

    /**
     * Reads data from with SOSD (Search on Sorted Data) format. Search on Sorted Data (SOSD) is a new benchmark that
     * allows researchers to compare their new (learned) index structures on both synthetic and real-world datasets.
     * Details can be found at <a href="https://learnedsystems.github.io/SOSDLeaderboard/">SOSD website</a>.
     * <p>
     * Here we read data until we reach max_num, or all data in files are read.
     *
     * @param path           data file path.
     * @param maxNum         maximal number of distinct elements.
     * @param entryBitLength entry bit length.
     * @return key-value database.
     * @throws IOException if file is not found.
     */
    public static TLongObjectMap<byte[]> readSosdData(String path, int maxNum, int entryBitLength) throws IOException {
        return readSosdData(path, maxNum, entryBitLength, new SecureRandom());
    }

    /**
     * Reads data from with SOSD (Search on Sorted Data) format. Search on Sorted Data (SOSD) is a new benchmark that
     * allows researchers to compare their new (learned) index structures on both synthetic and real-world datasets.
     * Details can be found at <a href="https://learnedsystems.github.io/SOSDLeaderboard/">SOSD website</a>.
     * <p>
     * Here we read data until we reach max_num, or all data in files are read.
     *
     * @param path           data file path.
     * @param maxNum         maximal number of distinct elements.
     * @param entryBitLength entry bit length.
     * @param secureRandom   random state.
     * @return key-value database.
     * @throws IOException if file is not found.
     */
    public static TLongObjectMap<byte[]> readSosdData(String path, int maxNum, int entryBitLength, SecureRandom secureRandom) throws IOException {
        MathPreconditions.checkPositive("maxNum", maxNum);
        MathPreconditions.checkPositive("entryBitLength", entryBitLength);
        File fileName = new File(path);
        int entryByteLength = CommonUtils.getByteLength(entryBitLength);
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileName));
        byte[] bytes = new byte[Long.BYTES];
        // read total num
        Preconditions.checkArgument(in.read(bytes) != -1);
        BytesUtils.reverseByteArray(bytes);
        long num = LongUtils.byteArrayToLong(bytes);
        TLongObjectMap<byte[]> keyValueDatabase = new TLongObjectHashMap<>(maxNum);
        for (int i = 0; i < num; i++) {
            Preconditions.checkArgument(in.read(bytes) != -1);
            BytesUtils.reverseByteArray(bytes);
            byte[] entry = BytesUtils.randomByteArray(entryByteLength, entryBitLength, secureRandom);
            keyValueDatabase.put(LongUtils.byteArrayToLong(bytes), entry);
            if (keyValueDatabase.size() == maxNum) {
                break;
            }
        }
        in.close();
        return keyValueDatabase;
    }

    /**
     * Generates random data and write in with SOSD (Search on Sorted Data) format. All data are distinct.
     *
     * @param path         data file path.
     * @param maxNum       maximal number of distinct elements.
     * @param secureRandom random state.
     * @throws IOException for IO exception.
     */
    public static TLongSet writeSosdData(String path, int maxNum, SecureRandom secureRandom)
        throws IOException {
        MathPreconditions.checkPositive("maxNum", maxNum);
        // generate random data
        TLongSet keySet = new TLongHashSet(maxNum);
        while (keySet.size() < maxNum) {
            keySet.add(secureRandom.nextLong());
        }
        // write data
        File file = new File(path);
        boolean success;
        if (file.exists()) {
            success = file.delete();
            Preconditions.checkArgument(success);
        }
        success = file.createNewFile();
        Preconditions.checkArgument(success);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        // write total num
        byte[] bytes = LongUtils.longToByteArray(maxNum);
        BytesUtils.reverseByteArray(bytes);
        out.write(bytes);
        // write keys
        for (long key : keySet.toArray()) {
            byte[] keyBytes = LongUtils.longToByteArray(key);
            BytesUtils.reverseByteArray(keyBytes);
            out.write(keyBytes);
        }
        out.flush();
        out.close();

        return keySet;
    }
}
