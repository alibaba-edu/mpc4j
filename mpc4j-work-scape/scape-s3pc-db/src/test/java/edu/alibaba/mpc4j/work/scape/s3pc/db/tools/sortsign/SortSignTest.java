package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.hzf22.Hzf22SortSignConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.hzf22.Hzf22SortSignPtoDesc;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * test sortSign
 *
 * @author Feng Han
 * @date 2025/2/20
 */
@RunWith(Parameterized.class)
public class SortSignTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(SortSignTest.class);
    /**
     * use simulate mtp or not
     */
    private static final boolean USE_MT_TEST_MODE = true;
    /**
     * small input size
     */
    private static final int SMALL_SIZE = 11;
    /**
     * middle input size
     */
    private static final int MIDDLE_SIZE = 1 << 9;
    /**
     * large input size
     */
    private static final int LARGE_SIZE = 1 << 12;
    /**
     * large input size
     */
    private static final int LARGE_KEY_DIM = 3;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            Hzf22SortSignPtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new Hzf22SortSignConfig.Builder(false).build(), false
        });
        configurations.add(new Object[]{
            Hzf22SortSignPtoDesc.getInstance().getPtoName() + "(malicious + ac use aby3)",
            new Hzf22SortSignConfig.Builder(true).build(), false
        });
        configurations.add(new Object[]{
            Hzf22SortSignPtoDesc.getInstance().getPtoName() + "(malicious + ac use mac)",
            new Hzf22SortSignConfig.Builder(true).build(), true
        });

        return configurations;
    }

    /**
     * configure
     */
    private final Hzf22SortSignConfig config;
    /**
     * verify with mac
     */
    private final boolean baseUseMac;

    public SortSignTest(String name, Hzf22SortSignConfig config, boolean baseUseMac) {
        super(name);
        this.config = config;
        this.baseUseMac = baseUseMac;
    }

    @Test
    public void testAllSmallSize() {
        testOpi(false, SMALL_SIZE, 1, true);
        testOpi(false, SMALL_SIZE, 1, false);
    }

    @Test
    public void testAllMiddleSize() {
        testOpi(false, MIDDLE_SIZE, 1, true);
        testOpi(false, MIDDLE_SIZE, 1, false);
    }

    @Test
    public void testAllLargeSize() {
        testOpi(true, LARGE_SIZE, 1, true);
        testOpi(true, LARGE_SIZE, 1, false);
    }

    @Test
    public void testLargeKeyDim() {
        testOpi(true, MIDDLE_SIZE, LARGE_KEY_DIM, true);
        testOpi(true, MIDDLE_SIZE, LARGE_KEY_DIM, false);
    }


    private SortSignParty[] getParties(boolean parallel) {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        boolean isMalicious = config.getSecurityModel().equals(SecurityModel.MALICIOUS);
        Abb3RpConfig abb3RpConfig = (isMalicious && USE_MT_TEST_MODE)
            ? new Abb3RpConfig.Builder(isMalicious, baseUseMac)
            .setTripletProviderConfig(new TripletProviderConfig.Builder(true)
                .setRpZ2MtpConfig(RpMtProviderFactory.createZ2MtpConfigTestMode())
                .setRpZl64MtpConfig(RpMtProviderFactory.createZl64MtpConfigTestMode())
                .build()).build()
            : new Abb3RpConfig.Builder(isMalicious, baseUseMac).build();
        Abb3Party[] abb3Parties = IntStream.range(0, 3).mapToObj(i ->
            new Abb3RpParty(rpcAll[i], abb3RpConfig)).toArray(Abb3RpParty[]::new);

        SortSignParty[] parties = Arrays.stream(abb3Parties).map(each ->
            SortSignFactory.createParty(each, config)).toArray(SortSignParty[]::new);

        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(parties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return parties;
    }

    private void testOpi(boolean parallel, int allNum, int keyDim, boolean inputIsSorted) {
        SortSignParty[] parties = getParties(parallel);
        int leftNum = new SecureRandom().nextInt(1, allNum);
        int rightNum = allNum - leftNum;
        LongVector[][] inputTables = getExample(new int[]{leftNum, rightNum}, keyDim, inputIsSorted);
        try {
            LOGGER.info("-----test {}, (leftNum = {}, rightNum = {}, keyDim = {}) start-----",
                parties[0].getPtoDesc().getPtoName(), leftNum, rightNum, keyDim);
            SortSignPartyThread[] threads = Arrays.stream(parties).map(p ->
                new SortSignPartyThread(p, inputIsSorted, inputTables, keyDim)).toArray(SortSignPartyThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (SortSignPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            verifyRes(inputTables, threads[0].getOutput(), keyDim);

            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (leftNum = {}, rightNum = {}, keyDim = {}) end, time:{}-----", parties[0].getPtoDesc().getPtoName(),
                leftNum, rightNum, keyDim, time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static LongVector[][] getExample(int[] nums, int keyDim, boolean inputIsSorted) {
        int upperBoundKey = Math.max((nums[0] + nums[1]) >> 2, 1);
        LongVector[][] res = new LongVector[2][keyDim + 1];
        for (int tableInd = 0; tableInd < 2; tableInd++) {
            // 为了测试验证，我们要求随机key的取值域较小。因此如果有多维key，则设置前K-1维key的取值都为0
            for (int i = 0; i < keyDim - 1; i++) {
                res[tableInd][i] = LongVector.createZeros(nums[tableInd]);
            }
            long[] tmp = IntStream.range(0, nums[tableInd])
                .mapToLong(x -> SECURE_RANDOM.nextLong(upperBoundKey))
                .toArray();
            long[] tmpFlag = IntStream.range(0, nums[tableInd])
                .mapToLong(x -> SECURE_RANDOM.nextLong(2))
                .toArray();
            if(inputIsSorted){
                tmp = Arrays.stream(tmp).sorted().toArray();
                tmpFlag = Arrays.stream(tmpFlag).sorted().map(ea -> 1 - ea).toArray();
            }
            res[tableInd][keyDim - 1] = LongVector.create(tmp);
            res[tableInd][keyDim] = LongVector.create(tmpFlag);
        }
        return res;
    }

    private void verifyRes(LongVector[][] plainInputTable, LongVector[] output, int keyDim) {
        int leftNum = plainInputTable[0][0].getNum();
        int rightNum = plainInputTable[1][0].getNum();

        MathPreconditions.checkEqual("output.length", "5", output.length, 5);
        for (LongVector longVector : output) {
            MathPreconditions.checkEqual("output[i].getNum()", "leftNum + rightNum", longVector.getNum(), leftNum + rightNum);
        }
        int[] perm = Arrays.stream(output[output.length - 1].getElements()).mapToInt(x -> (int) x).toArray();
        PermutationNetworkUtils.validPermutation(perm);
        long[] sortRes = new long[leftNum + rightNum];
        long[] sortInvFlag = new long[leftNum + rightNum];
        long[] sortId = new long[leftNum + rightNum];
        for (int i = 0; i < leftNum; i++) {
            sortRes[perm[i]] = plainInputTable[0][keyDim - 1].getElement(i);
            sortInvFlag[perm[i]] = 1 - plainInputTable[0][keyDim].getElement(i);
            sortId[perm[i]] = 0;
        }
        for (int i = 0; i < rightNum; i++) {
            sortRes[perm[i + leftNum]] = plainInputTable[1][keyDim - 1].getElement(i);
            sortInvFlag[perm[i + leftNum]] = 1 - plainInputTable[1][keyDim].getElement(i);
            sortId[perm[i + leftNum]] = 1;
        }

        for (int i = 0; i < sortRes.length - 1; i++) {
            // verify shuffleId
            Assert.assertEquals(output[3].getElement(i), sortId[i]);
            // verify sort res
            Assert.assertTrue((sortInvFlag[i] << 32) + sortRes[i] * 2 + sortId[i]
                <= (sortInvFlag[i + 1] << 32) + sortRes[i + 1] * 2 + sortId[i + 1]);
            boolean equalKey = sortInvFlag[i] == 0 && sortInvFlag[i + 1] == 0 && sortRes[i] == sortRes[i + 1];
            // verify E_1
            boolean realE1 = (sortId[i] != sortId[i + 1]) && equalKey;
            Assert.assertEquals(output[0].getElement(i + 1), realE1 ? 1 : 0);
            // verify E_upper, E_down
            Assert.assertEquals(output[1].getElement(i + 1), equalKey ? 1 : 0);
            Assert.assertEquals(output[2].getElement(i), equalKey ? 1 : 0);
        }
        Assert.assertEquals(output[0].getElement(0), 0);
        Assert.assertEquals(output[1].getElement(0), 0);
        Assert.assertEquals(output[2].getElement(sortRes.length - 1), 0);
    }

}
