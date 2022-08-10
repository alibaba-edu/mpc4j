package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

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
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 布尔矩阵性能测试。
 *
 * @author Weiran Liu
 * @date 2022/7/26
 */
@Ignore
public class TransBitMatrixEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransBitMatrixEfficiencyTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 秒表
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * 较大行数/列数
     */
    private static final int LOG_LARGE_N = 18;
    /**
     * 较小行数/列数
     */
    private static final int LOG_SMALL_N = 10;
    /**
     * 次数输出格式
     */
    private static final DecimalFormat LOG_N_DECIMAL_FORMAT = new DecimalFormat("00");
    /**
     * 测试类型
     */
    private static final TransBitMatrixFactory.TransBitMatrixType[] TYPES = new TransBitMatrixFactory.TransBitMatrixType[] {
        TransBitMatrixFactory.TransBitMatrixType.NAIVE,
        TransBitMatrixFactory.TransBitMatrixType.EKLUNDH,
        TransBitMatrixFactory.TransBitMatrixType.NATIVE,
        TransBitMatrixFactory.TransBitMatrixType.NATIVE_SPLIT_ROW,
        TransBitMatrixFactory.TransBitMatrixType.JDK_SPLIT_ROW,
        TransBitMatrixFactory.TransBitMatrixType.NATIVE_SPLIT_COL,
        TransBitMatrixFactory.TransBitMatrixType.JDK_SPLIT_COL,
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
                StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(LongUtils.ceilLog2(rows)), 10),
                StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(LongUtils.ceilLog2(columns)), 10),
                StringUtils.leftPad(String.valueOf(time), 10)
            );
            STOP_WATCH.reset();
        }
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
                StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(LongUtils.ceilLog2(rows)), 10),
                StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(LongUtils.ceilLog2(columns)), 10),
                StringUtils.leftPad(String.valueOf(time), 10)
            );
            STOP_WATCH.reset();
        }
    }
}
