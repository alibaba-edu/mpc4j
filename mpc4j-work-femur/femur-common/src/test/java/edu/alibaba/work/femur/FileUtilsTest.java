package edu.alibaba.work.femur;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.set.TLongSet;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * File utilities test.
 *
 * @author Weiran Liu
 * @date 2024/12/3
 */
public class FileUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtilsTest.class);
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public FileUtilsTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testReadWrite1() throws IOException {
        testReadWrite(0);
    }

    @Test
    public void testReadWrite1M() throws IOException {
        testReadWrite(20);
    }

    private void testReadWrite(int maxLogNum) throws IOException {
        String filePath = "../data/random_log_" + maxLogNum + "_uint64.sosd";
        String fileDictionaryPath = "../data/";
        File fileDictionary = new File(fileDictionaryPath);
        if (!fileDictionary.exists()) {
            boolean success = fileDictionary.mkdir();
            assert success;
        }
        int entryBitLength = Long.SIZE;
        int maxNum = 1 << maxLogNum;
        // write random data
        TLongSet keySet = FileUtils.writeSosdData(filePath, maxNum, secureRandom);
        // read random data
        TLongObjectMap<byte[]> database = FileUtils.readSosdData(filePath, maxNum, entryBitLength);
        long[] writeKeyArray = keySet.toArray();
        Arrays.sort(writeKeyArray);
        long[] readKeyArray = database.keys();
        Arrays.sort(readKeyArray);
        Assert.assertArrayEquals(writeKeyArray, readKeyArray);
        // read less data, use Math.max(maxNum / 2, 1) to ensure num >= 1
        database = FileUtils.readSosdData(filePath, Math.max(maxNum / 2, 1), entryBitLength);
        Assert.assertEquals(Math.max(maxNum / 2, 1), database.size());
        // read more data, we do not have that much data, so read to the maximal num.
        database = FileUtils.readSosdData(filePath, maxNum * 2, entryBitLength);
        Assert.assertEquals(maxNum, database.size());
    }

    @Test
    public void testReadFb() {
        String path = "../data/fb_200M_uint64.sosd";
        int maxNum = 1 << 22;
        try {
            TLongObjectMap<byte[]> database = FileUtils.readSosdData(path, maxNum, 64);
            Assert.assertEquals(maxNum, database.size());
        } catch (IOException e) {
            LOGGER.warn("fb_200M_uint64 is not in the path {}, download or place it", new File(path).getAbsolutePath());
        }
    }
}
