package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.JoinResVerifyUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.JoinInputUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.hzf22.Hzf22PkPkJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.hzf22.Hzf22PkPkJoinPtoDesc;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.mrr20.Mrr20PkPkJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.mrr20.Mrr20PkPkJoinPtoDesc;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * test cases for PkPk join protocol
 *
 * @author Feng Han
 * @date 2025/2/20
 */
@RunWith(Parameterized.class)
public class PkPkJoinTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PkPkJoinTest.class);
    /**
     * use simulate mtp or not
     */
    private static final boolean USE_MT_TEST_MODE = true;
    /**
     * small input size
     */
    private static final int SMALL_SIZE = 7;
    /**
     * middle input size
     */
    private static final int MIDDLE_SIZE = (1 << 7) + 3;
    /**
     * large input size
     */
    private static final int LARGE_SIZE = 1 << 12;
    /**
     * input key dimensions, where the key is stored in long form
     */
    private static final int[] KEY_DIM = new int[]{28, 64};
    /**
     * the small payload dimension of two table
     */
    private static final int SMALL_PAYLOAD_DIM = 0;
    /**
     * the large payload dimension of two table
     */
    private static final int LARGE_PAYLOAD_DIM = 80;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            Mrr20PkPkJoinPtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new Mrr20PkPkJoinConfig.Builder(false).build(), false
        });
        configurations.add(new Object[]{
            Mrr20PkPkJoinPtoDesc.getInstance().getPtoName() + "(malicious + ac use mac)",
            new Mrr20PkPkJoinConfig.Builder(true).build(), true
        });

        configurations.add(new Object[]{
            Hzf22PkPkJoinPtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new Hzf22PkPkJoinConfig.Builder(false).build(), false
        });
        configurations.add(new Object[]{
            Hzf22PkPkJoinPtoDesc.getInstance().getPtoName() + "(malicious + ac use mac)",
            new Hzf22PkPkJoinConfig.Builder(true).build(), true
        });

        return configurations;
    }
    /**
     * config
     */
    private final PkPkJoinConfig config;
    /**
     * verify with mac
     */
    private final boolean baseUseMac;

    public PkPkJoinTest(String name, PkPkJoinConfig config, boolean baseUseMac) {
        super(name);
        this.config = config;
        this.baseUseMac = baseUseMac;
    }

    @Test
    public void testSmallSize() {
        testOpi(false, SMALL_SIZE, SMALL_SIZE, KEY_DIM[0], SMALL_PAYLOAD_DIM, SMALL_PAYLOAD_DIM, false);
    }

    @Test
    public void testUbSize() {
        testOpi(false, SMALL_SIZE, MIDDLE_SIZE, KEY_DIM[0], SMALL_PAYLOAD_DIM, LARGE_PAYLOAD_DIM, false);
    }

    @Test
    public void testLargeKeySize() {
        testOpi(false, SMALL_SIZE, MIDDLE_SIZE, KEY_DIM[1], SMALL_PAYLOAD_DIM, LARGE_PAYLOAD_DIM, false);
    }

    @Test
    public void testLargeSize() {
        testOpi(true, LARGE_SIZE, LARGE_SIZE, KEY_DIM[0], LARGE_PAYLOAD_DIM, SMALL_PAYLOAD_DIM, false);
    }

    @Test
    public void testMiddleSizeSorted() {
        testOpi(true, SMALL_SIZE, MIDDLE_SIZE, KEY_DIM[1], SMALL_PAYLOAD_DIM, LARGE_PAYLOAD_DIM, true);
    }

    @Test
    public void testUbSizeSorted() {
        testOpi(true, SMALL_SIZE, MIDDLE_SIZE, KEY_DIM[0], SMALL_PAYLOAD_DIM, LARGE_PAYLOAD_DIM, true);
    }

    @Test
    public void testLargeSizeSorted() {
        testOpi(true, LARGE_SIZE, LARGE_SIZE, KEY_DIM[1], LARGE_PAYLOAD_DIM, SMALL_PAYLOAD_DIM, true);
    }

    private PkPkJoinParty[] getParties(boolean parallel) {
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

        PkPkJoinParty[] parties = Arrays.stream(abb3Parties).map(each ->
            PkPkJoinFactory.createParty(each, config)).toArray(PkPkJoinParty[]::new);

        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(parties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return parties;
    }

    private void testOpi(boolean parallel, int leftTableSize, int rightTableSize,
                         int keyDim, int leftPayloadDim, int rightPayloadDim, boolean isInputSorted) {
        PkPkJoinParty[] parties = getParties(parallel);
        TIntObjectHashMap<List<Integer>> leftHash = new TIntObjectHashMap<>();
        TIntObjectHashMap<List<Integer>> rightHash = new TIntObjectHashMap<>();
        BitVector[][] inputDb = new BitVector[][]{
            JoinInputUtils.getBinaryInput4PkJoin(leftTableSize, keyDim, leftPayloadDim, leftHash),
            JoinInputUtils.getBinaryInput4PkJoin(rightTableSize, keyDim, rightPayloadDim, rightHash)
        };
        int[] keyIndex = IntStream.range(0, keyDim).toArray();
        try {
            LOGGER.info("-----test {}, (leftTableSize = {}, rightTableSize = {}, keyDim = {}, leftPayloadDim = {}, rightPayloadDim = {}) start-----",
                parties[0].getPtoDesc().getPtoName(), leftTableSize, rightTableSize, keyDim, leftPayloadDim, rightPayloadDim);
            PkPkJoinPartyThread[] threads = Arrays.stream(parties).map(p ->
                new PkPkJoinPartyThread(p, inputDb, keyIndex, keyIndex, isInputSorted)).toArray(PkPkJoinPartyThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (PkPkJoinPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            BitVector[] res = threads[0].getResult();
            int trueLen = JoinInputUtils.getRealInner4GeneralJoin(leftHash, rightHash);
            LOGGER.info("expect output length: {}", trueLen);
            JoinResVerifyUtils.checkInner4General(inputDb[0], inputDb[1], keyDim, res, leftHash, rightHash, trueLen);

            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (leftTableSize = {}, rightTableSize = {}) end, time:{}-----",
                parties[0].getPtoDesc().getPtoName(), leftTableSize, rightTableSize, time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
