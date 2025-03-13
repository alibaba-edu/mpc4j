package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.JoinInputUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.PkPkSemiJoinTest;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.mrr20.Mrr20RandomEncodingConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.mrr20.Mrr20RandomEncodingPtoDesc;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.work.scape.s3pc.db.JoinResVerifyUtils.bitVectorTransToColumnIntArray;

/**
 * test cases for randomized encoding protocol
 *
 * @author Feng Han
 * @date 2025/2/25
 */
@RunWith(Parameterized.class)
public class RandomEncodingTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PkPkSemiJoinTest.class);
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
    private static final int[] KEY_DIM = new int[]{20, 64};

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            Mrr20RandomEncodingPtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new Mrr20RandomEncodingConfig.Builder(false).build()
        });
        configurations.add(new Object[]{
            Mrr20RandomEncodingPtoDesc.getInstance().getPtoName() + "(malicious)",
            new Mrr20RandomEncodingConfig.Builder(true).build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final RandomEncodingConfig config;

    public RandomEncodingTest(String name, RandomEncodingConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testSmallSize() {
        testOpi(false, SMALL_SIZE, SMALL_SIZE, KEY_DIM[0], false);
    }

    @Test
    public void testUbSize1() {
        testOpi(false, SMALL_SIZE, MIDDLE_SIZE, KEY_DIM[0], false);
    }

    @Test
    public void testUbSize2() {
        testOpi(true, MIDDLE_SIZE, SMALL_SIZE, KEY_DIM[0], false);
    }

    @Test
    public void testLargeSize() {
        testOpi(true, LARGE_SIZE, LARGE_SIZE, KEY_DIM[1], false);
    }

    @Test
    public void testSmallSizeDummy() {
        testOpi(true, SMALL_SIZE, MIDDLE_SIZE, KEY_DIM[1], true);
    }

    @Test
    public void testUbSizeDummy() {
        testOpi(true, SMALL_SIZE, MIDDLE_SIZE, KEY_DIM[0], true);
    }

    @Test
    public void testLargeSizeDummy() {
        testOpi(true, LARGE_SIZE, LARGE_SIZE, KEY_DIM[1], true);
    }

    private RandomEncodingParty[] getParties(boolean parallel) {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        boolean isMalicious = config.getSecurityModel().equals(SecurityModel.MALICIOUS);
        Abb3RpConfig abb3RpConfig = (isMalicious && USE_MT_TEST_MODE)
            ? new Abb3RpConfig.Builder(isMalicious, false)
            .setTripletProviderConfig(new TripletProviderConfig.Builder(true)
                .setRpZ2MtpConfig(RpMtProviderFactory.createZ2MtpConfigTestMode())
                .setRpZl64MtpConfig(RpMtProviderFactory.createZl64MtpConfigTestMode())
                .build()).build()
            : new Abb3RpConfig.Builder(isMalicious, false).build();
        Abb3Party[] abb3Parties = IntStream.range(0, 3).mapToObj(i ->
            new Abb3RpParty(rpcAll[i], abb3RpConfig)).toArray(Abb3RpParty[]::new);

        RandomEncodingParty[] parties = Arrays.stream(abb3Parties).map(each ->
            RandomEncodingFactory.createParty(each, config)).toArray(RandomEncodingParty[]::new);

        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(parties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return parties;
    }

    private void testOpi(boolean parallel, int leftTableSize, int rightTableSize, int keyDim, boolean withDummy) {
        RandomEncodingParty[] parties = getParties(parallel);
        TIntObjectHashMap<List<Integer>> leftHash = new TIntObjectHashMap<>();
        TIntObjectHashMap<List<Integer>> rightHash = new TIntObjectHashMap<>();
        BitVector[][] inputDb = new BitVector[][]{
            JoinInputUtils.getBinaryInput4PkJoin(leftTableSize, keyDim, 0, withDummy, leftHash),
            JoinInputUtils.getBinaryInput4PkJoin(rightTableSize, keyDim, 0, withDummy, rightHash)
        };
        try {
            LOGGER.info("-----test {}, (leftTableSize = {}, rightTableSize = {}, keyDim = {}) start-----",
                parties[0].getPtoDesc().getPtoName(), leftTableSize, rightTableSize, keyDim);
            RandomEncodingPartyThread[] threads = Arrays.stream(parties).map(p ->
                new RandomEncodingPartyThread(p, inputDb, withDummy)).toArray(RandomEncodingPartyThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (RandomEncodingPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            BitVector[][] res = threads[0].getResult();
            verifyRes(leftHash, rightHash, inputDb, res, keyDim);

            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (leftTableSize = {}, rightTableSize = {}) end, time:{}-----",
                parties[0].getPtoDesc().getPtoName(), leftTableSize, rightTableSize, time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void verifyRes(TIntObjectHashMap<List<Integer>> leftHash, TIntObjectHashMap<List<Integer>> rightHash,
                           BitVector[][] inputDb, BitVector[][] res, int keyDim) {
        Assert.assertEquals(inputDb[0][0].bitNum(), res[0][0].bitNum());
        Assert.assertEquals(inputDb[1][0].bitNum(), res[1][0].bitNum());
        Assert.assertEquals(res[0].length, res[1].length);

        int[] leftKey = bitVectorTransToColumnIntArray(Arrays.copyOf(inputDb[0], keyDim));
        int[] rightKey = bitVectorTransToColumnIntArray(Arrays.copyOf(inputDb[1], keyDim));
        BigInteger[] leftEnc = ZlDatabase.create(EnvType.STANDARD, true, res[0]).getBigIntegerData();
        BigInteger[] rightEnc = ZlDatabase.create(EnvType.STANDARD, true, res[1]).getBigIntegerData();
        HashSet<BigInteger> leftEncSet = new HashSet<>(Arrays.asList(leftEnc));
        HashSet<BigInteger> rightEncSet = new HashSet<>(Arrays.asList(rightEnc));
        // same key -> same encoding; different key -> different encoding
        for (int i = 0; i < leftKey.length; i++) {
            if (rightHash.containsKey(leftKey[i]) && inputDb[0][keyDim].get(i)) {
                for (int j : rightHash.get(leftKey[i])) {
                    Assert.assertEquals(0, leftEnc[i].compareTo(rightEnc[j]));
                }
            } else {
                Assert.assertFalse(rightEncSet.contains(leftEnc[i]));
            }
        }
        for (int i = 0; i < rightKey.length; i++) {
            if (leftHash.containsKey(rightKey[i]) && inputDb[1][keyDim].get(i)) {
                for (int j : leftHash.get(rightKey[i])) {
                    Assert.assertEquals(0, rightEnc[i].compareTo(leftEnc[j]));
                }
            } else {
                Assert.assertFalse(leftEncSet.contains(rightEnc[i]));
            }
        }
    }
}
