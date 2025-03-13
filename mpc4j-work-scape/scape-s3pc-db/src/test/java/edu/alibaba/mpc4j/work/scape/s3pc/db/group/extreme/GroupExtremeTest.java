package edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeFactory.ExtremeType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.hzf22.Hzf22GroupExtremeConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.hzf22.Hzf22GroupExtremePtoDesc;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * test case for group extreme
 *
 * @author Feng Han
 * @date 2025/2/24
 */
@RunWith(Parameterized.class)
public class GroupExtremeTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupExtremeTest.class);
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
            Hzf22GroupExtremePtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new Hzf22GroupExtremeConfig.Builder(false).build()
        });
        configurations.add(new Object[]{
            Hzf22GroupExtremePtoDesc.getInstance().getPtoName() + "(malicious)",
            new Hzf22GroupExtremeConfig.Builder(true).build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final GroupExtremeConfig config;

    public GroupExtremeTest(String name, GroupExtremeConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testSmallSize() {
        testOpi(false, SMALL_SIZE, KEY_DIM[0], ExtremeType.MAX);
        testOpi(false, SMALL_SIZE, KEY_DIM[0], ExtremeType.MIN);
    }

    @Test
    public void testMiddleSize() {
        testOpi(false, MIDDLE_SIZE, KEY_DIM[1], ExtremeType.MAX);
        testOpi(false, MIDDLE_SIZE, KEY_DIM[1], ExtremeType.MIN);
    }

    @Test
    public void testLargeSize() {
        testOpi(true, LARGE_SIZE, KEY_DIM[1], ExtremeType.MAX);
        testOpi(true, LARGE_SIZE, KEY_DIM[1], ExtremeType.MIN);
    }

    private GroupExtremeParty[] getParties(boolean parallel) {
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

        GroupExtremeParty[] parties = Arrays.stream(abb3Parties).map(each ->
            GroupExtremeFactory.createParty(each, config)).toArray(GroupExtremeParty[]::new);

        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(parties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return parties;
    }

    private void testOpi(boolean parallel, int inputSize, int inputDim, ExtremeType extremeType) {
        GroupExtremeParty[] parties = getParties(parallel);

        int minDIm = Math.min(LongUtils.ceilLog2(inputSize), inputDim);
        int maxValue = 1 << minDIm;
        int byteDim = CommonUtils.getByteLength(inputDim);
        BigInteger[] plainBig = IntStream.range(0, inputSize)
            .mapToObj(i -> BigInteger.valueOf(SECURE_RANDOM.nextInt(maxValue)))
            .toArray(BigInteger[]::new);
        byte[][] plainByte = Arrays.stream(plainBig).map(ea -> BigIntegerUtils.nonNegBigIntegerToByteArray(ea, byteDim)).toArray(byte[][]::new);
        ZlDatabase zlDatabase = ZlDatabase.create(inputDim, plainByte);
        BitVector[] plainVec = zlDatabase.bitPartition(EnvType.STANDARD, parallel);
        BitVector plainFlag = BitVectorFactory.createRandom(inputSize, SECURE_RANDOM);
        plainFlag.set(0, false);

        try {
            LOGGER.info("-----test {}, (inputSize = {}, inputDim = {}, extremeType = {}) start-----",
                parties[0].getPtoDesc().getPtoName(), inputSize, inputDim, extremeType.name());
            GroupExtremePartyThread[] threads = Arrays.stream(parties).map(p ->
                new GroupExtremePartyThread(p, plainVec, plainFlag, extremeType)).toArray(GroupExtremePartyThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (GroupExtremePartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            BitVector[] res = threads[0].getResult();
            verify(plainBig, plainFlag, extremeType, res);

            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (inputSize = {}, inputDim = {}, extremeType = {}) end, time:{}-----",
                parties[0].getPtoDesc().getPtoName(), inputSize, inputDim, extremeType.name(), time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void verify(BigInteger[] plainBig, BitVector plainFlag, ExtremeType extremeType, BitVector[] res) {
        BigInteger[] resBig = ZlDatabase.create(EnvType.STANDARD, true, Arrays.copyOf(res, res.length - 1)).getBigIntegerData();
        BitVector resFlag = res[res.length - 1];
        BigInteger lastBig = plainBig[0];
        for (int i = 0; i < plainBig.length; i++) {
            if (plainFlag.get(i)) {
                if (extremeType.equals(ExtremeType.MAX)) {
                    lastBig = lastBig.compareTo(plainBig[i]) >= 0 ? lastBig : plainBig[i];
                } else {
                    lastBig = lastBig.compareTo(plainBig[i]) <= 0 ? lastBig : plainBig[i];
                }
            } else {
                lastBig = plainBig[i];
            }
            boolean isLast = (i == plainBig.length - 1 || (!plainFlag.get(i + 1)));
            if (isLast) {
                Assert.assertTrue(resFlag.get(i));
                Assert.assertEquals(lastBig, resBig[i]);
            } else {
                Assert.assertFalse(resFlag.get(i));
            }
        }
    }

}
