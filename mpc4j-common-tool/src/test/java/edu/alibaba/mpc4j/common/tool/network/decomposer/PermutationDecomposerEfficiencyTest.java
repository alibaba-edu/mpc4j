package edu.alibaba.mpc4j.common.tool.network.decomposer;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkEfficiencyTest;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.network.decomposer.PermutationDecomposerFactory.DecomposerType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * permutation decomposer efficiency test.
 *
 * @author Weiran Liu
 * @date 2024/3/28
 */
@Ignore
@RunWith(Parameterized.class)
public class PermutationDecomposerEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermutationNetworkEfficiencyTest.class);
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * stop watch
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * numbers of inputs
     */
    private static final int[] LOG_N_ARRAY = new int[]{12, 14, 16, 18, 20};

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // CGP20
        configurations.add(new Object[]{DecomposerType.CGP20.name(), DecomposerType.CGP20});
        // LLL24
        configurations.add(new Object[]{DecomposerType.LLL24.name(), DecomposerType.LLL24});

        return configurations;
    }

    /**
     * type
     */
    private final DecomposerType type;

    public PermutationDecomposerEfficiencyTest(String name, DecomposerType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}\t{}", "                name", "         N", "         T", "create(us)", " perm.(us)");
        for (int logN : LOG_N_ARRAY) {
            for (int logT = logN / 2; logT <= logN; logT += 2) {
                testEfficiency(logN, logT);
            }
            LOGGER.info(StringUtils.rightPad("", 60, '-'));
        }
    }

    private void testEfficiency(int logN, int logT) {
        int n = 1 << logN;
        int t = 1 << logT;
        int[] permutation = PermutationNetworkUtils.randomPermutation(n, SECURE_RANDOM);

        STOP_WATCH.start();
        PermutationDecomposer decomposer = PermutationDecomposerFactory.createComposer(type, n, t);
        STOP_WATCH.stop();
        long createTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
        STOP_WATCH.reset();

        byte[][] inputVector = IntStream.range(0, n)
            .mapToObj(i -> IntUtils.nonNegIntToFixedByteArray(i, CommonConstants.BLOCK_BYTE_LENGTH))
            .toArray(byte[][]::new);
        STOP_WATCH.start();
        byte[][] outputVector = BytesUtils.clone(inputVector);
        decomposer.setPermutation(permutation);
        for (int i = 0; i < decomposer.getD(); i++) {
            byte[][][] groups = decomposer.splitVector(outputVector, i);
            outputVector = decomposer.combineGroups(groups, i);
        }
        STOP_WATCH.stop();
        long permuteTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
        STOP_WATCH.reset();

        LOGGER.info("{}\t{}\t{}\t{}\t{}",
            StringUtils.leftPad(type.name(), 20),
            StringUtils.leftPad(String.valueOf(n), 10),
            StringUtils.leftPad(String.valueOf(t), 10),
            StringUtils.leftPad(String.valueOf(createTime), 10),
            StringUtils.leftPad(String.valueOf(permuteTime), 10)
        );
    }
}
