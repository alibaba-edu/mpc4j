package edu.alibaba.mpc4j.common.structure.main;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.SparseGf2eDokvs;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * GF2K OKVS efficiency.
 *
 * @author Weiran Liu
 * @date 2024/1/23
 */
public class Gf2eDokvsEfficiency {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gf2eDokvsEfficiency.class);
    /**
     * task name
     */
    public static final String TASK_NAME = "GF2E_DOKVS";
    /**
     * time format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * types
     */
    private static final Gf2eDokvsType[] TYPES = Gf2eDokvsFactory.Gf2eDokvsType.values();
    /**
     * properties
     */
    private final Properties properties;
    /**
     * stop watch
     */
    private final StopWatch stopWatch;

    public Gf2eDokvsEfficiency(Properties properties) {
        this.properties = properties;
        stopWatch = new StopWatch();
    }

    public void testEfficiency() throws IOException {
        LOGGER.info("Read {} settings", Gf2eDokvs.class.getSimpleName());
        int[] logNs = PropertiesUtils.readLogIntArray(properties, "log_n");
        int keyByteLength = PropertiesUtils.readInt(properties, "key_byte_length");
        int valueByteLength = PropertiesUtils.readInt(properties, "value_byte_length");
        String filePath = TASK_NAME
            + "_" + keyByteLength
            + "_" + valueByteLength
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = String.format(
            "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
            "                          name", "      logN", "         m", "    sparse", "     dense", "  parallel",
            " encode(s)", " decode(s)", "dEncode(s)", "dDecode(s)"
        );
        LOGGER.info(tab);
        printWriter.println(tab);
        for (int logN : logNs) {
            testEfficiency(logN, keyByteLength, valueByteLength, false, printWriter);
            testEfficiency(logN, keyByteLength, valueByteLength, true, printWriter);
        }
        printWriter.close();
        fileWriter.close();
    }

    private void testEfficiency(int logN, int keyByteLength, int valueByteLength, boolean parallelEncode, PrintWriter printWriter) {
        int n = 1 << logN;
        int l = valueByteLength * Byte.SIZE;
        // create key-value map
        Map<ByteBuffer, byte[]> keyValueMap = randomKeyValueMap(n, keyByteLength, valueByteLength);
        MathPreconditions.checkEqual("kv.size", "1 << log(n)", n,1 << logN);
        for (Gf2eDokvsType type : TYPES) {
            if (type.equals(Gf2eDokvsType.DISTINCT_GBF)) {
                // skip GBF
                continue;
            }
            int hashNum = Gf2eDokvsFactory.getHashKeyNum(type);
            byte[][] keys = CommonUtils.generateRandomKeys(hashNum, SECURE_RANDOM);
            Gf2eDokvs<ByteBuffer> dokvs = Gf2eDokvsFactory.createInstance(EnvType.STANDARD, type, n, l, keys);
            dokvs.setParallelEncode(parallelEncode);

            stopWatch.start();
            byte[][] nonDoublyStorage = dokvs.encode(keyValueMap, false);
            stopWatch.stop();
            double nonDoublyEncodeTime = (double) stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000;
            stopWatch.reset();
            Stream<ByteBuffer> nonDoublyKeyStream = keyValueMap.keySet().stream();
            nonDoublyKeyStream = parallelEncode ? nonDoublyKeyStream.parallel() : nonDoublyKeyStream;
            stopWatch.start();
            nonDoublyKeyStream.forEach(key -> dokvs.decode(nonDoublyStorage, key));
            stopWatch.stop();
            double nonDoublyDecodeTime = (double) stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000;
            stopWatch.reset();
            stopWatch.start();
            byte[][] doublyStorage = dokvs.encode(keyValueMap, true);
            stopWatch.stop();
            double doublyEncodeTime = (double) stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000;
            stopWatch.reset();
            Stream<ByteBuffer> doublyKeyStream = keyValueMap.keySet().stream();
            doublyKeyStream = parallelEncode ? doublyKeyStream.parallel() : doublyKeyStream;
            stopWatch.start();
            doublyKeyStream.forEach(key -> dokvs.decode(doublyStorage, key));
            stopWatch.stop();
            double doublyDecodeTime = (double) stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000;
            stopWatch.reset();
            boolean sparse = (dokvs instanceof SparseGf2eDokvs);
            String sparsePositionRange = sparse ? String.valueOf(((SparseGf2eDokvs<ByteBuffer>) dokvs).sparsePositionRange()) : "-";
            String densePositionRange = sparse ? String.valueOf(((SparseGf2eDokvs<ByteBuffer>) dokvs).densePositionRange()) : "-";
            String info = String.format(
                "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
                StringUtils.leftPad(type.name(), 30),
                StringUtils.leftPad(String.valueOf(logN), 10),
                StringUtils.leftPad(String.valueOf(dokvs.getM()), 10),
                StringUtils.leftPad(sparsePositionRange, 10),
                StringUtils.leftPad(densePositionRange, 10),
                StringUtils.leftPad(String.valueOf(parallelEncode), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(nonDoublyEncodeTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(nonDoublyDecodeTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(doublyEncodeTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(doublyDecodeTime), 10)
            );
            printWriter.println(info);
            LOGGER.info(info);
            printWriter.println(info);
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
        printWriter.println(StringUtils.rightPad("", 60, '-'));
    }

    private Map<ByteBuffer, byte[]> randomKeyValueMap(int n, int keyByteLength, int valueByteLength) {
        Map<ByteBuffer, byte[]> keyValueMap = new HashMap<>(n);
        IntStream.range(0, n).forEach(index -> {
            byte[] keyBytes = new byte[keyByteLength];
            SECURE_RANDOM.nextBytes(keyBytes);
            byte[] valueBytes = BytesUtils.randomByteArray(valueByteLength, SECURE_RANDOM);
            keyValueMap.put(ByteBuffer.wrap(keyBytes), valueBytes);
        });
        return keyValueMap;
    }
}
