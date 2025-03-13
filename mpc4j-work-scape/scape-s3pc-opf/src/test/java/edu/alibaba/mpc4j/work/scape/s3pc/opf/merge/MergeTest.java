package edu.alibaba.mpc4j.work.scape.s3pc.opf.merge;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.bitonic.BitonicMergeConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.bitonic.BitonicMergePtoDesc;
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
 * test cases for merge
 *
 * @author Feng Han
 * @date 2025/2/21
 */
@RunWith(Parameterized.class)
public class MergeTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(MergeTest.class);
    /**
     * use simulate mtp or not
     */
    private static final boolean USE_MT_TEST_MODE = true;
    /**
     * balanced small input sizes
     */
    private static final int[] BALANCED_SMALL_SIZE = new int[]{8, 8};
    /**
     * balanced small input sizes
     */
    private static final int[] BALANCED_LARGE_SIZE = new int[]{1 << 12, 1 << 12};
    /**
     * small input sizes
     */
    private static final int[] SMALL_SIZE = new int[]{7, 5};
    /**
     * middle input sizes
     */
    private static final int[] MIDDLE_SIZE = new int[]{786, 901};
    /**
     * large input sizes
     */
    private static final int[] LARGE_SIZE = new int[]{(1 << 12) + 897, (1 << 12) + 901};
    /**
     * small input dimension
     */
    private static final int SMALL_KEY_DIM = 7;
    /**
     * large input size
     */
    private static final int LARGE_KEY_DIM = 81;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            BitonicMergePtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new BitonicMergeConfig.Builder(false).build()
        });
        configurations.add(new Object[]{
            BitonicMergePtoDesc.getInstance().getPtoName() + "(malicious + ac use aby3)",
            new BitonicMergeConfig.Builder(true).build()
        });

        return configurations;
    }

    /**
     * configure
     */
    private final BitonicMergeConfig config;

    public MergeTest(String name, BitonicMergeConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testBalancedSmallSize() {
        testOpi(false, BALANCED_SMALL_SIZE, SMALL_KEY_DIM);
    }

    @Test
    public void testBalancedLargeSize() {
        testOpi(false, BALANCED_LARGE_SIZE, LARGE_KEY_DIM);
    }

    @Test
    public void testSmallSize() {
        testOpi(false, SMALL_SIZE, SMALL_KEY_DIM);
    }

    @Test
    public void testMiddleSize() {
        testOpi(false, MIDDLE_SIZE, SMALL_KEY_DIM);
    }

    @Test
    public void testLargeSize() {
        testOpi(true, LARGE_SIZE, SMALL_KEY_DIM);
    }

    @Test
    public void testLargeKeyDim() {
        testOpi(true, MIDDLE_SIZE, LARGE_KEY_DIM);
    }

    private MergeParty[] getParties(boolean parallel) {
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

        MergeParty[] parties = Arrays.stream(abb3Parties).map(each ->
            MergeFactory.createParty(each, config)).toArray(MergeParty[]::new);

        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(parties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return parties;
    }

    public BigInteger[][] genPlainBig(int[] inputSizes, int keyDim) {
        return Arrays.stream(inputSizes).mapToObj(x -> {
            long upperBound = keyDim < 63 ? 1L << keyDim : x;
            return IntStream.range(0, x)
                .mapToObj(i -> BigInteger.valueOf(SECURE_RANDOM.nextLong(upperBound)))
                .sorted().toArray(BigInteger[]::new);
        }).toArray(BigInteger[][]::new);
    }

    public BitVector[][] genInput(int keyDim, BigInteger[][] inputBig) {
        int byteL = CommonUtils.getByteLength(keyDim);
        return Arrays.stream(inputBig).map(array -> {
            byte[][] bytes = BigIntegerUtils.nonNegBigIntegersToByteArrays(array, byteL);
            ZlDatabase tmp = ZlDatabase.create(keyDim, bytes);
            return tmp.bitPartition(EnvType.STANDARD_JDK, true);
        }).toArray(BitVector[][]::new);
    }

    private void testOpi(boolean parallel, int[] inputSizes, int keyDim) {
        MergeParty[] parties = getParties(parallel);
        BigInteger[][] inputBig = genPlainBig(inputSizes, keyDim);
        BitVector[][] inputBitVec = genInput(keyDim, inputBig);
        try {
            LOGGER.info("-----test {}, (leftNum = {}, rightNum = {}, keyDim = {}) start-----",
                parties[0].getPtoDesc().getPtoName(), inputSizes[0], inputSizes[1], keyDim);
            MergePartyThread[] threads = Arrays.stream(parties).map(p ->
                new MergePartyThread(p, inputBitVec[0], inputBitVec[1])).toArray(MergePartyThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (MergePartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            BitVector[] res = threads[0].getOutput();
            BigInteger[] output = ZlDatabase.create(EnvType.STANDARD, true, res).getBigIntegerData();
            BigInteger[] input = new BigInteger[inputSizes[0] + inputSizes[1]];
            System.arraycopy(inputBig[0], 0, input, 0, inputSizes[0]);
            System.arraycopy(inputBig[1], 0, input, inputSizes[0], inputSizes[1]);
            input = Arrays.stream(input).sorted().toArray(BigInteger[]::new);
            for (int i = 0; i < output.length; i++) {
                Assert.assertEquals(0, output[i].compareTo(input[i]));
            }

            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (leftNum = {}, rightNum = {}, keyDim = {}) end, time:{}-----", parties[0].getPtoDesc().getPtoName(),
                inputSizes[0], inputSizes[1], keyDim, time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
