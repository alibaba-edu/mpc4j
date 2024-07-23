package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory.TransBitMatrixType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * efficiency tests for bit matrix transpose.
 *
 * @author Weiran Liu
 * @date 2022/7/26
 */
@Ignore
public class TransBitMatrixEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransBitMatrixEfficiencyTest.class);
    /**
     * large log(n) for rows / columns
     */
    private static final int LOG_LARGE_N = 18;
    /**
     * small log(n) for rows / columns
     */
    private static final int LOG_SMALL_N = 10;
    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * stop watch
     */
    private final StopWatch stopWatch;

    public TransBitMatrixEfficiencyTest() {
        secureRandom = new SecureRandom();
        stopWatch = new StopWatch();
    }

    @Test
    public void testLargeRowEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                name", "  log(row)", "  log(col)", " trans(ms)");
        int rows = 1 << LOG_LARGE_N;
        int columns = 1 << LOG_SMALL_N;
        for (TransBitMatrixType type : TransBitMatrixType.values()) {
            TransBitMatrix a = TransBitMatrixFactory.createInstance(type, rows, columns);
            int rowBytes = CommonUtils.getByteLength(rows);
            IntStream.range(0, columns).forEach(columnIndex -> {
                byte[] column = new byte[rowBytes];
                secureRandom.nextBytes(column);
                BytesUtils.reduceByteArray(column, rows);
                a.setColumn(columnIndex, column);
            });
            stopWatch.start();
            a.transpose();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            LOGGER.info(
                "{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(String.valueOf(LongUtils.ceilLog2(rows)), 10),
                StringUtils.leftPad(String.valueOf(LongUtils.ceilLog2(columns)), 10),
                StringUtils.leftPad(String.valueOf(time), 10)
            );
            stopWatch.reset();
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }

    @Test
    public void testLargeColumnEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                name", "  log(row)", "  log(col)", " trans(ms)");
        int rows = 1 << LOG_SMALL_N;
        int columns = 1 << LOG_LARGE_N;
        for (TransBitMatrixType type : TransBitMatrixType.values()) {
            TransBitMatrix a = TransBitMatrixFactory.createInstance(type, rows, columns);
            int rowBytes = CommonUtils.getByteLength(rows);
            IntStream.range(0, columns).forEach(columnIndex -> {
                byte[] column = new byte[rowBytes];
                secureRandom.nextBytes(column);
                BytesUtils.reduceByteArray(column, rows);
                a.setColumn(columnIndex, column);
            });
            stopWatch.start();
            a.transpose();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            LOGGER.info(
                "{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(String.valueOf(LongUtils.ceilLog2(rows)), 10),
                StringUtils.leftPad(String.valueOf(LongUtils.ceilLog2(columns)), 10),
                StringUtils.leftPad(String.valueOf(time), 10)
            );
            stopWatch.reset();
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
