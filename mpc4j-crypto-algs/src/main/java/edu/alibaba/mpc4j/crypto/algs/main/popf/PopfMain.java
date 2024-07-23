package edu.alibaba.mpc4j.crypto.algs.main.popf;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.crypto.algs.popf.Zlp24LongPopfEngine;
import edu.alibaba.mpc4j.crypto.algs.restriction.LongEmptyRestriction;
import edu.alibaba.mpc4j.crypto.algs.utils.distribution.HgdFactory;
import edu.alibaba.mpc4j.crypto.algs.utils.range.LongRange;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import org.apache.commons.lang3.time.StopWatch;
import org.bouncycastle.crypto.CryptoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * POPF main.
 *
 * @author Liqiang Peng
 * @date 2024/5/15
 */
public class PopfMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(PopfMain.class);
    /**
     * task name
     */
    public static final String ALGS_TYPE_NAME = "POPF";
    /**
     * warmup element set size
     */
    private static final int WARMUP_ELEMENT_SET_SIZE = 1 << 10;
    /**
     * properties
     */
    private final Properties properties;

    public PopfMain(Properties properties) {
        this.properties = properties;
    }

    private void warmup() throws CryptoException {
        SecureRandom secureRandom = new SecureRandom();
        Zlp24LongPopfEngine popfEngine = new Zlp24LongPopfEngine();
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(key);
        LongRange rangeD = new LongRange(0, WARMUP_ELEMENT_SET_SIZE);
        LongRange rangeR = new LongRange(0, WARMUP_ELEMENT_SET_SIZE);
        popfEngine.init(key, new LongEmptyRestriction(rangeD, rangeR));
        long minInput = rangeD.getStart();
        long maxInput = rangeD.getEnd();
        TLongList outputs = new TLongArrayList();
        int count = 0;
        for (long input = minInput; input <= maxInput; input++) {
            long output = popfEngine.popf(input);
            outputs.add(output);
            // verify partial order-preserving
            if (count > 0) {
                Preconditions.checkArgument(outputs.get(count) >= outputs.get(count - 1));
            }
            count++;
        }
    }

    public void run() throws CryptoException {
        warmup();
        LOGGER.info("read common settings");
        HgdFactory.HgdType hgdType = MainPtoConfigUtils.readEnum(HgdFactory.HgdType.class, properties, "hgd_type");
        int repeatNum = PropertiesUtils.readInt(properties, "repeat_num");
        boolean parallel = PropertiesUtils.readBoolean(properties, "parallel");
        int[] logRangeSize = PropertiesUtils.readLogIntArray(properties, "log_range_size");
        long inputRangeSize = 1L << logRangeSize[0];
        long outputRangeSize = 1L << logRangeSize[1];
        StopWatch stopWatch = new StopWatch();
        LOGGER.info(
            "inputRangeSize = {}, outputRangeSize = {}, repeatNum = {}, parallel = {}",
            inputRangeSize, outputRangeSize, repeatNum, parallel
        );
        LOGGER.info("execute");
        LongRange rangeD = new LongRange(0, inputRangeSize);
        LongRange rangeR = new LongRange(0, outputRangeSize);
        long minInput = rangeD.getStart();
        long maxInput = rangeD.getEnd();
        stopWatch.start();
        TLongList[] outputs = new TLongArrayList[repeatNum];
        IntStream intStream = IntStream.range(0, repeatNum);
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(i -> {
            outputs[i] = new TLongArrayList();
            SecureRandom secureRandom = new SecureRandom();
            Zlp24LongPopfEngine popfEngine = new Zlp24LongPopfEngine(hgdType);
            byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            secureRandom.nextBytes(key);
            popfEngine.init(key, new LongEmptyRestriction(rangeD, rangeR));
            for (long input = minInput; input <= maxInput; input++) {
                try {
                    outputs[i].add((int) popfEngine.popf(input));
                } catch (CryptoException e) {
                    e.printStackTrace();
                }
            }
        });
        stopWatch.stop();
        long runningTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        LOGGER.info("finish, running time {}ms", runningTime);
    }
}