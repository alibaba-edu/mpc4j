package edu.alibaba.mpc4j.work.scape.s3pc.db.group;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
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
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeParty;
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
 * test group flag computation
 *
 * @author Feng Han
 * @date 2025/2/24
 */
@RunWith(Parameterized.class)
public class GroupFlagTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupFlagTest.class);
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
    private static final int[] BINARY_KEY_DIM = new int[]{20, 64};
    /**
     * input key dimensions, where the key is stored in long form
     */
    private static final int[] ARITHMETIC_KEY_DIM = new int[]{1, 3};

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            Hzf22GroupExtremePtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new Hzf22GroupExtremeConfig.Builder(false).build(), false
        });
        configurations.add(new Object[]{
            Hzf22GroupExtremePtoDesc.getInstance().getPtoName() + "(malicious, tuple)",
            new Hzf22GroupExtremeConfig.Builder(true).build(), false
        });
        configurations.add(new Object[]{
            Hzf22GroupExtremePtoDesc.getInstance().getPtoName() + "(malicious, mac)",
            new Hzf22GroupExtremeConfig.Builder(true).build(), true
        });

        return configurations;
    }

    /**
     * config
     */
    private final GroupExtremeConfig config;
    /**
     * verify with mac
     */
    private final boolean baseUseMac;

    public GroupFlagTest(String name, GroupExtremeConfig config, boolean baseUseMac) {
        super(name);
        this.config = config;
        this.baseUseMac = baseUseMac;
    }

    @Test
    public void testSmallSize() {
        testOpi(false, SMALL_SIZE, BINARY_KEY_DIM[0], ARITHMETIC_KEY_DIM[0]);
    }

    @Test
    public void testMiddleSize() {
        testOpi(false, MIDDLE_SIZE, BINARY_KEY_DIM[1], ARITHMETIC_KEY_DIM[1]);
    }

    @Test
    public void testLargeSize() {
        testOpi(true, LARGE_SIZE, BINARY_KEY_DIM[1], ARITHMETIC_KEY_DIM[1]);
    }

    private GroupExtremeParty[] getParties(boolean parallel) {
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

        GroupExtremeParty[] parties = Arrays.stream(abb3Parties).map(each ->
            GroupExtremeFactory.createParty(each, config)).toArray(GroupExtremeParty[]::new);

        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(parties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return parties;
    }

    private void testOpi(boolean parallel, int inputSize, int bInputDim, int aInputDim) {
        GroupExtremeParty[] parties = getParties(parallel);

        int minDIm = Math.min(LongUtils.ceilLog2(inputSize), bInputDim);
        int maxValue = 1 << minDIm;
        int byteDim = CommonUtils.getByteLength(bInputDim);
        BigInteger[] plainBig = IntStream.range(0, inputSize)
            .mapToObj(i -> BigInteger.valueOf(SECURE_RANDOM.nextInt(maxValue)))
            .sorted().toArray(BigInteger[]::new);

//        to verify binary operation is correct, set bPlainTab = null, and BINARY_KEY_DIM = new int[]{0}
//        BitVector[] bPlainTab = null;
        byte[][] plainByte = Arrays.stream(plainBig).map(ea -> BigIntegerUtils.nonNegBigIntegerToByteArray(ea, byteDim)).toArray(byte[][]::new);
        ZlDatabase zlDatabase = ZlDatabase.create(bInputDim, plainByte);
        BitVector[] bPlainTab = new BitVector[bInputDim + 1];
        System.arraycopy(zlDatabase.bitPartition(EnvType.STANDARD, parallel), 0, bPlainTab, 0, bInputDim);
        bPlainTab[bInputDim] = BitVectorFactory.createOnes(inputSize);

//        to verify arithmetic operation is correct, set bPlainTab = null, and ARITHMETIC_KEY_DIM = new int[]{0}
//        LongVector[] aPlainTab = null;
        LongVector[] aPlainTab = new LongVector[aInputDim + 1];
        for (int i = 0; i < aInputDim - 1; i++) {
            aPlainTab[i] = LongVector.createZeros(inputSize);
        }
        aPlainTab[aInputDim - 1] = LongVector.create(Arrays.stream(plainBig).mapToLong(BigInteger::longValue).toArray());
        aPlainTab[aInputDim] = LongVector.createOnes(inputSize);

        try {
            LOGGER.info("-----test {}, (inputSize = {}, bInputDim = {}, aInputDim = {}) start-----",
                parties[0].getPtoDesc().getPtoName(), inputSize, bInputDim, aInputDim);
            GroupPartyThread[] threads = Arrays.stream(parties).map(p ->
                    new GroupPartyThread(p, bPlainTab, IntStream.range(0, bInputDim).toArray(),
                        aPlainTab, IntStream.range(0, aInputDim).toArray()))
                .toArray(GroupPartyThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (GroupPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            BitVector bFlag = threads[0].getBinaryResult();
            LongVector aFlag = threads[0].getArithmeticResult();
            verify(plainBig, bFlag, aFlag);

            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (inputSize = {}, inputDim = {}, aInputDim = {}) end, time:{}-----",
                parties[0].getPtoDesc().getPtoName(), inputSize, bInputDim, aInputDim, time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void verify(BigInteger[] plainBig, BitVector bFlag, LongVector aFlag) {
        if (bFlag != null) {
            MathPreconditions.checkEqual("bFlag.bitNum", "plainBig.length", bFlag.bitNum(), plainBig.length);
            Assert.assertFalse(bFlag.get(0));
            for (int i = 1; i < plainBig.length; i++) {
                if (plainBig[i].compareTo(plainBig[i - 1]) == 0) {
                    Assert.assertTrue(bFlag.get(i));
                } else {
                    Assert.assertFalse(bFlag.get(i));
                }
            }
        }
        if (aFlag != null) {
            MathPreconditions.checkEqual("aFlag.getNum", "plainBig.length", aFlag.getNum(), plainBig.length);
            Assert.assertEquals(0L, aFlag.getElement(0));
            for (int i = 1; i < plainBig.length; i++) {
                if (plainBig[i].compareTo(plainBig[i - 1]) == 0) {
                    Assert.assertEquals(1L, aFlag.getElement(i));
                } else {
                    Assert.assertEquals(0L, aFlag.getElement(i));
                }
            }
        }
    }
}
