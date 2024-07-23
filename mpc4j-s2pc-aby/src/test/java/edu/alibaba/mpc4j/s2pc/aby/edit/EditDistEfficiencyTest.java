package edu.alibaba.mpc4j.s2pc.aby.edit;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.edit.s2pc.diag.S2pcDiagEditDistConfig;
import edu.alibaba.mpc4j.s2pc.aby.edit.s2pc.diag.S2pcDiagEditDistPtoDesc;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
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
 * Edit distance efficiency thread.
 *
 * @author Feng Han, Li Peng
 * @date 2024/4/12
 */
@RunWith(Parameterized.class)
public class EditDistEfficiencyTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditDistEfficiencyTest.class);
    /**
     * char pool.
     */
    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ";
    /**
     * use parallel.
     */
    private static final boolean PARALLEL = true;
    /**
     * number of test elements.
     */
    private static final int TEST_NUM = 1 << 10;
    /**
     * max string length in random test.
     */
    private static final int MAX_STR_LEN = 20;
    /**
     * string length in fixed test.
     */
    private static final int FIXED_STR_LEN = 30;
    /**
     * use silent ot.
     */
    private static final boolean SILENT = false;
    /**
     * need extend zl.
     */
    private static final boolean NEED_EXTEND = false;
    /**
     * the number of increment of zl length in a single extend step.
     */
    private static final int INCREMENT = 1;
    /**
     * need to prune unneeded cells.
     */
    private static final boolean NEED_PRUNE = false;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        configurations.add(new Object[]{
            S2pcDiagEditDistPtoDesc.getInstance().getPtoName(),
            new S2pcDiagEditDistConfig.Builder(SILENT)
                .setNeedExtend(NEED_EXTEND).setIncrement(INCREMENT).setNeedPrune(NEED_PRUNE).build()
        });
        return configurations;
    }

    private final DistCmpConfig config;

    public EditDistEfficiencyTest(String name, DistCmpConfig config) {
        super(name);
        this.config = config;
    }

    protected int plainEditDist(String word1, String word2) {
        if (word1 == null || word1.isEmpty()) {
            return word2.length();
        }
        if (word2 == null || word2.isEmpty()) {
            return word1.length();
        }

        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();

        int n = word1.length();
        int m = word2.length();

        int[][] dp = new int[n + 1][m + 1];

        for (int i = 0; i < n + 1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j < m + 1; j++) {
            dp[0][j] = j;
        }

        int maxNum = Math.max(m, n);

        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < m + 1; j++) {
                // pruning
                if (Math.abs(2 * (j - i) + n - m) > maxNum) {
                    dp[i][j] = maxNum + 1;
                    continue;
                }
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1])) + 1;
                }
            }
        }
        return dp[n][m];
    }

    @Test
    public void testEfficiencyFixed() {
        String[] left = new String[TEST_NUM];
        String[] right = new String[TEST_NUM];
        SecureRandom secureRandom = new SecureRandom();
        long count = 0;
        for (int i = 0; i < left.length; i++) {
            // avg length is fixed
            int leftLen = FIXED_STR_LEN;

            StringBuilder sb = new StringBuilder(leftLen);
            for (int t = 0; t < leftLen; t++) {
                sb.append(CHAR_POOL.charAt(secureRandom.nextInt(CHAR_POOL.length())));
            }
            left[i] = sb.toString();
            int rightLen = FIXED_STR_LEN;

            sb = new StringBuilder(rightLen);
            for (int t = 0; t < rightLen; t++) {
                sb.append(CHAR_POOL.charAt(secureRandom.nextInt(CHAR_POOL.length())));
            }
            right[i] = sb.toString();
            count += (long) left[i].length() * right[i].length();
        }
        LOGGER.info("finish generating random string");
        int testTime = 1;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (int i = 0; i < testTime; i++) {
            testEdit(left, right, PARALLEL);
        }
        stopWatch.stop();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        LOGGER.info("total compare count:{}", count);
        LOGGER.info("average compare time:{}", time / testTime);
    }

    @Test
    public void testEfficiencyRandom() {
        String[] left = new String[TEST_NUM];
        String[] right = new String[TEST_NUM];
        SecureRandom secureRandom = new SecureRandom();
        long count = 0;
        for (int i = 0; i < left.length; i++) {
            // avg length is MAX_STR_LEN/2
            int leftLen = secureRandom.nextInt(MAX_STR_LEN - 1) + 1;

            StringBuilder sb = new StringBuilder(leftLen);
            for (int t = 0; t < leftLen; t++) {
                sb.append(CHAR_POOL.charAt(secureRandom.nextInt(CHAR_POOL.length())));
            }
            left[i] = sb.toString();
            int rightLen = secureRandom.nextInt(MAX_STR_LEN - 1) + 1;

            sb = new StringBuilder(rightLen);
            for (int t = 0; t < rightLen; t++) {
                sb.append(CHAR_POOL.charAt(secureRandom.nextInt(CHAR_POOL.length())));
            }
            right[i] = sb.toString();
            count += (long) left[i].length() * right[i].length();
        }
        LOGGER.info("finish generating random string");
        int testTime = 1;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (int i = 0; i < testTime; i++) {
            testEdit(left, right, PARALLEL);
        }
        stopWatch.stop();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        LOGGER.info("total compare count:{}", count);
        LOGGER.info("average compare time:{}", time / testTime);
    }

    private void testEdit(String[] senderStr, String[] receiverStr, boolean parallel) {
        // init z2 circuit
        Z2cConfig z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, SILENT);
        Z2cParty z2cSender = Z2cFactory.createSender(firstRpc, secondRpc.ownParty(), z2cConfig);
        Z2cParty z2cReceiver = Z2cFactory.createReceiver(secondRpc, firstRpc.ownParty(), z2cConfig);
        // init edit
        DistCmpSender sender = EditDistFactory.createSender(z2cSender, secondRpc.ownParty(), config);
        DistCmpReceiver receiver = EditDistFactory.createReceiver(z2cReceiver, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        receiver.setTaskId(randomTaskId);
        sender.setTaskId(randomTaskId);
        receiver.setParallel(parallel);
        sender.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", receiver.getPtoDesc().getPtoName());
            int maxLength = 0;
            for (String each : receiverStr) {
                maxLength = Math.max(maxLength, each.length());
            }
            for (String each : senderStr) {
                maxLength = Math.max(maxLength, each.length());
            }
            EditDistReceiverThread receiverThread
                = new EditDistReceiverThread(receiver, receiverStr, maxLength);
            EditDistSenderThread senderThread
                = new EditDistSenderThread(sender, senderStr, maxLength);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            receiverThread.start();
            senderThread.start();
            // stop
            receiverThread.join();
            senderThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            printAndResetRpc(time);
            int[] compRes = senderThread.getRes();
            int[] realRes = IntStream.range(0, senderStr.length).map(i -> plainEditDist(senderStr[i], receiverStr[i])).toArray();
            Assert.assertArrayEquals(compRes, realRes);
            LOGGER.info("-----test {} end, time cost: {}ms-----", receiver.getPtoDesc().getPtoName(), time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
