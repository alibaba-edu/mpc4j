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
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * stop watch
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * large log(n) for rows / columns
     */
    private static final int LOG_LARGE_N = 18;
    /**
     * small log(n) for rows / columns
     */
    private static final int LOG_SMALL_N = 10;
    /**
     * 测试类型
     */
    private static final TransBitMatrixType[] TYPES = new TransBitMatrixType[] {
        TransBitMatrixType.JDK,
        TransBitMatrixType.EKLUNDH,
        TransBitMatrixType.NATIVE,
        TransBitMatrixType.NATIVE_SPLIT_ROW,
        TransBitMatrixType.JDK_SPLIT_ROW,
        TransBitMatrixType.NATIVE_SPLIT_COL,
        TransBitMatrixType.JDK_SPLIT_COL,
    };

    @Test
    public void testLargeRowEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                name", "  log(row)", "  log(col)", " trans(ms)");
        int rows = 1 << LOG_LARGE_N;
        int columns = 1 << LOG_SMALL_N;
        for (TransBitMatrixFactory.TransBitMatrixType type : TYPES) {
            TransBitMatrix a = TransBitMatrixFactory.createInstance(type, rows, columns);
            int rowBytes = CommonUtils.getByteLength(rows);
            IntStream.range(0, columns).forEach(columnIndex -> {
                byte[] column = new byte[rowBytes];
                SECURE_RANDOM.nextBytes(column);
                BytesUtils.reduceByteArray(column, rows);
                a.setColumn(columnIndex, column);
            });
            STOP_WATCH.start();
            a.transpose();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            LOGGER.info(
                "{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(String.valueOf(LongUtils.ceilLog2(rows)), 10),
                StringUtils.leftPad(String.valueOf(LongUtils.ceilLog2(columns)), 10),
                StringUtils.leftPad(String.valueOf(time), 10)
            );
            STOP_WATCH.reset();
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }

    @Test
    public void testLargeColumnEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                name", "  log(row)", "  log(col)", " trans(ms)");
        int rows = 1 << LOG_SMALL_N;
        int columns = 1 << LOG_LARGE_N;
        for (TransBitMatrixFactory.TransBitMatrixType type : TYPES) {
            TransBitMatrix a = TransBitMatrixFactory.createInstance(type, rows, columns);
            int rowBytes = CommonUtils.getByteLength(rows);
            IntStream.range(0, columns).forEach(columnIndex -> {
                byte[] column = new byte[rowBytes];
                SECURE_RANDOM.nextBytes(column);
                BytesUtils.reduceByteArray(column, rows);
                a.setColumn(columnIndex, column);
            });
            STOP_WATCH.start();
            a.transpose();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            LOGGER.info(
                "{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(String.valueOf(LongUtils.ceilLog2(rows)), 10),
                StringUtils.leftPad(String.valueOf(LongUtils.ceilLog2(columns)), 10),
                StringUtils.leftPad(String.valueOf(time), 10)
            );
            STOP_WATCH.reset();
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
