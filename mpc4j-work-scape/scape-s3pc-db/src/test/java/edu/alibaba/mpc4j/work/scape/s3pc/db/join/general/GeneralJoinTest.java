package edu.alibaba.mpc4j.work.scape.s3pc.db.join.general;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.JoinInputUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.general.hzf22.Hzf22GeneralJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.general.hzf22.Hzf22GeneralJoinPtoDesc;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * general join test cases
 *
 * @author Feng Han
 * @date 2025/2/20
 */
@RunWith(Parameterized.class)
public class GeneralJoinTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneralJoinTest.class);
    /**
     * use simulate mtp or not
     */
    private static final boolean USE_MT_TEST_MODE = true;
    /**
     * small input size
     */
    private static final int SMALL_SIZE = 8;
    /**
     * middle input size
     */
    private static final int MIDDLE_SIZE = 1 << 9;
    /**
     * large input size
     */
    private static final int LARGE_SIZE = 1 << 12;
    /**
     * large frequency bound of the left join_key,
     * large frequency bound of the right join_key,
     * large payload dimension of left table, where the payload is stored in long form
     * large payload dimension of right table, where the payload is stored in long form
     */
    private static final int[] LARGE_PARAM = new int[]{4, 4, 2, 3};
    /**
     * small frequency bound of the left join_key,
     * small frequency bound of the right join_key,
     * small payload dimension of left table, where the payload is stored in long form
     * small payload dimension of right table, where the payload is stored in long form
     */
    private static final int[] SMALL_PARAM = new int[]{3, 2, 1, 1};

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            Hzf22GeneralJoinPtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new Hzf22GeneralJoinConfig.Builder(false).build(), false
        });
        configurations.add(new Object[]{
            Hzf22GeneralJoinPtoDesc.getInstance().getPtoName() + "(malicious + ac use aby3)",
            new Hzf22GeneralJoinConfig.Builder(true).build(), false
        });
        configurations.add(new Object[]{
            Hzf22GeneralJoinPtoDesc.getInstance().getPtoName() + "(malicious + ac use mac)",
            new Hzf22GeneralJoinConfig.Builder(true).build(), true
        });

        return configurations;
    }

    /**
     * config
     */
    private final GeneralJoinConfig config;
    /**
     * verify use mac
     */
    private final boolean baseUseMac;

    public GeneralJoinTest(String name, GeneralJoinConfig config, boolean baseUseMac) {
        super(name);
        this.config = config;
        this.baseUseMac = baseUseMac;
    }

    @Test
    public void testSmallSize() {
        testOpi(false, SMALL_SIZE, SMALL_SIZE, LARGE_PARAM, false);
    }

    @Test
    public void testUbSize1() {
        testOpi(false, SMALL_SIZE, MIDDLE_SIZE, LARGE_PARAM, false);
    }

    @Test
    public void testUbSize2() {
        testOpi(false, MIDDLE_SIZE, SMALL_SIZE, LARGE_PARAM, false);
    }

    @Test
    public void testMiddleSize() {
        testOpi(false, MIDDLE_SIZE, MIDDLE_SIZE, LARGE_PARAM, true);
    }

    @Test
    public void testLargeSize() {
        testOpi(true, LARGE_SIZE, LARGE_SIZE, SMALL_PARAM, false);
    }

    @Test
    public void testUbSizeSorted() {
        testOpi(false, MIDDLE_SIZE, SMALL_SIZE, LARGE_PARAM, true);
    }

    @Test
    public void testLargeSizeSorted() {
        testOpi(true, LARGE_SIZE, LARGE_SIZE, SMALL_PARAM, true);
    }

    private GeneralJoinParty[] getParties(boolean parallel) {
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

        GeneralJoinParty[] parties = Arrays.stream(abb3Parties).map(each ->
            GeneralJoinFactory.createParty(each, config)).toArray(GeneralJoinParty[]::new);

        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(parties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return parties;
    }

    private void testOpi(boolean parallel, int leftNum, int rightNum, int[] params, boolean inputIsSorted) {
        int resultUpperBound = params[0] * params[1] * Math.max(leftNum, rightNum);
        HashMap<Long, List<Integer>> leftHash = new HashMap<>(leftNum), rightHash = new HashMap<>(rightNum);
        long[][] leftPlain = JoinInputUtils.getInput4GeneralJoin(leftNum, params[2], params[0], leftHash);
        long[][] rightPlain = JoinInputUtils.getInput4GeneralJoin(rightNum, params[3], params[1], rightHash);

        LongVector[] leftPlainVec = Arrays.stream(leftPlain).map(LongVector::create).toArray(LongVector[]::new);
        LongVector[] rightPlainVec = Arrays.stream(rightPlain).map(LongVector::create).toArray(LongVector[]::new);
        int[] keyIndex = new int[]{0};

        GeneralJoinParty[] parties = getParties(parallel);
        try {
            LOGGER.info("-----test {}, (leftNum = {}, rightNum = {}, params:{}) start-----",
                parties[0].getPtoDesc().getPtoName(), leftNum, rightNum, Arrays.toString(params));
            GeneralJoinPartyThread[] threads = Arrays.stream(parties).map(p ->
                new GeneralJoinPartyThread(p, leftPlainVec, rightPlainVec, keyIndex, keyIndex, resultUpperBound, inputIsSorted)).toArray(GeneralJoinPartyThread[]::new);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (GeneralJoinPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            LongVector[] result = threads[0].getPlainRes();
            long[][] resLongPlain = Arrays.stream(result).map(LongVector::getElements).toArray(long[][]::new);
            int trueLen = JoinInputUtils.getRealInner4GeneralJoin(leftHash, rightHash);
            LOGGER.info("expect output length is {}", trueLen);
            JoinInputUtils.checkInner4General(leftPlain, rightPlain, params[2], params[3], resLongPlain, leftHash, rightHash, trueLen);

            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (leftNum = {}, rightNum = {}) end, time:{}-----", parties[0].getPtoDesc().getPtoName(), leftNum, rightNum, time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}

