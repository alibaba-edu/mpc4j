package edu.alibaba.mpc4j.common.tool.bitvector;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory.BitVectorType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * BitVector efficiency test.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
@Ignore
public class BitVectorEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitVectorEfficiencyTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * log(n)
     */
    private static final int LOG_N = 8;
    /**
     * time format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.0000");
    /**
     * number of merge operations
     */
    private static final int MERGE_NUM = 100;
    /**
     * the stop watch
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * types
     */
    private static final BitVectorType[] TYPES = new BitVectorType[] {
        BitVectorType.COMBINED_BIT_VECTOR,
        BitVectorType.BYTES_BIT_VECTOR,
        BitVectorType.BIGINTEGER_BIT_VECTOR,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
            "                     name", "   bit_num",
            "create(us)", "   xor(us)", "   xori(us)", "   and(us)", "   andi(us)",
            "   set(us)", "   get(us)", " merge(us)"
        );
        testEfficiency(1);
        testEfficiency(1 << 4);
        testEfficiency(1 << 8);
        testEfficiency(1 << 12);
        testEfficiency(1 << 16);
        testEfficiency(1 << 18);
        testEfficiency(1 << 19);
        testEfficiency(1 << 20);
    }

    private void testEfficiency(int bitNum) {
        for (BitVectorType type : TYPES) {
            int n = 1 << LOG_N;
            // create
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> BitVectorFactory.createRandom(type, bitNum, SECURE_RANDOM));
            STOP_WATCH.stop();
            double createRandomTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();

            BitVector bitVector1 = BitVectorFactory.createRandom(type, bitNum, SECURE_RANDOM);
            BitVector bitVector2 = BitVectorFactory.createRandom(type, bitNum, SECURE_RANDOM);
            BitVector setBitVector = BitVectorFactory.createRandom(type, bitNum, SECURE_RANDOM);
            // xor
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> bitVector1.xor(bitVector2));
            STOP_WATCH.stop();
            double xorTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();
            // xori
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> bitVector1.xori(bitVector2));
            STOP_WATCH.stop();
            double xoriTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();
            // and
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> bitVector1.and(bitVector2));
            STOP_WATCH.stop();
            double andTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();
            // andi
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> bitVector1.andi(bitVector2));
            STOP_WATCH.stop();
            double andiTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();
            // generate random positions and random values
            boolean[] randomBinaries = new boolean[n];
            int[] randomPositions = new int[n];
            IntStream.range(0, n).forEach(index -> {
                randomPositions[index] = SECURE_RANDOM.nextInt(bitNum);
                randomBinaries[index] = SECURE_RANDOM.nextBoolean();
            });
            // set
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> setBitVector.set(randomPositions[index], randomBinaries[index]));
            STOP_WATCH.stop();
            double setTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();
            // get
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> setBitVector.get(randomPositions[index]));
            STOP_WATCH.stop();
            double getTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();
            // merge
            STOP_WATCH.start();
            IntStream.range(0, n / 8).forEach(index -> {
                BitVector mergeBitVector = BitVectorFactory.createEmpty(type);
                IntStream.range(0, MERGE_NUM).forEach(mergeIndex -> mergeBitVector.merge(bitVector1));
            });
            STOP_WATCH.stop();
            double mergeTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();
            LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 25),
                StringUtils.leftPad(String.valueOf(bitNum), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(createRandomTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(xorTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(xoriTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(andTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(andiTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(setTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(getTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(mergeTime), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
