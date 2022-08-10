package edu.alibaba.mpc4j.common.tool.benes;

import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkFactory.BenesNetworkType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 贝奈斯网络性能测试。
 *
 * @author Weiran Liu
 * @date 2022/8/5
 */
@Ignore
public class BenesNetworkEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BenesNetworkEfficiencyTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0000.00");
    /**
     * 秒表
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * 性能测试置换表大小
     */
    private static final int[] PERMUTATION_NUM_ARRAY = new int[] {1 << 12, 1 << 14, 1 << 16, 1 << 18};
    /**
     * 测试类型
     */
    private static final BenesNetworkType[] TYPES = new BenesNetworkType[] {
        BenesNetworkType.NATIVE_BENES_NETWORK,
        BenesNetworkType.JDK_BENES_NETWORK,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                name", " perm. num", "create(us)", " perm.(us)");
        for (int num : PERMUTATION_NUM_ARRAY) {
            testEfficiency(num);
        }
    }

    private void testEfficiency(int num) {
        for (BenesNetworkType type : TYPES) {
            List<Integer> shufflePermutationMap = IntStream.range(0, num)
                .boxed()
                .collect(Collectors.toList());
            Collections.shuffle(shufflePermutationMap, SECURE_RANDOM);
            int[] permutationMap = shufflePermutationMap.stream()
                .mapToInt(permutation -> permutation)
                .toArray();

            STOP_WATCH.start();
            BenesNetwork<Integer> benesNetwork = BenesNetworkFactory.createInstance(type, permutationMap);
            STOP_WATCH.stop();
            double createTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();

            Vector<Integer> inputVector = IntStream.range(0, num)
                .boxed().
                collect(Collectors.toCollection(Vector::new));
            STOP_WATCH.start();
            benesNetwork.permutation(inputVector);
            STOP_WATCH.stop();
            double permuteTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();

            LOGGER.info("{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(String.valueOf(num), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(createTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(permuteTime), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
