package edu.alibaba.mpc4j.work.scape.s3pc.opf.agg;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggFnParam.AggOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.hzf22.Hzf22AggConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.hzf22.Hzf22AggPtoDesc;
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
 * agg test.
 *
 * @author Feng Han
 * @date 2025/2/27
 */
@RunWith(Parameterized.class)
public class AggTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(AggTest.class);
    /**
     * operations to be tested
     */
    private static final AggOp[] types = new AggOp[]{
        AggOp.SUM,
        AggOp.MIN,
        AggOp.MAX
    };
    /**
     * use simulate mtp or not
     */
    private static final boolean USE_MT_TEST_MODE = true;
    /**
     * small input sizes
     */
    private static final int SMALL_SIZE = 7;
    /**
     * middle input sizes
     */
    private static final int MIDDLE_SIZE = 901;
    /**
     * large input sizes
     */
    private static final int LARGE_SIZE = (1 << 14) + 897;
    /**
     * small input dimension
     */
    private static final int SMALL_BINARY_DIM = 20;
    /**
     * large input size
     */
    private static final int LARGE_BINARY_DIM = 64;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            Hzf22AggPtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new Hzf22AggConfig.Builder(false).build(), false
        });
        configurations.add(new Object[]{
            Hzf22AggPtoDesc.getInstance().getPtoName() + "(malicious + ac use aby3)",
            new Hzf22AggConfig.Builder(true).build(), false
        });
        configurations.add(new Object[]{
            Hzf22AggPtoDesc.getInstance().getPtoName() + "(malicious + ac use mac)",
            new Hzf22AggConfig.Builder(true).build(), true
        });

        return configurations;
    }

    /**
     * configure
     */
    private final AggConfig config;
    /**
     * configure
     */
    private final boolean verifyWithMac;

    public AggTest(String name, AggConfig config, boolean verifyWithMac) {
        super(name);
        this.config = config;
        this.verifyWithMac = verifyWithMac;
    }

    @Test
    public void testSize1() {
        for (AggOp aggOp : types) {
            testOpi(false, 1, SMALL_BINARY_DIM, aggOp, true, false);
            testOpi(false, 1, SMALL_BINARY_DIM, aggOp, false, true);
        }
    }

    @Test
    public void testSize5() {
        for (AggOp aggOp : types) {
            testOpi(false, 5, SMALL_BINARY_DIM, aggOp, true, false);
            testOpi(false, 5, SMALL_BINARY_DIM, aggOp, false, true);
        }
    }

    @Test
    public void testSmallSize() {
        for (AggOp aggOp : types) {
            testOpi(false, SMALL_SIZE, SMALL_BINARY_DIM, aggOp, true, false);
            testOpi(false, SMALL_SIZE, SMALL_BINARY_DIM, aggOp, false, true);
        }
    }

    @Test
    public void testMiddleSize() {
        for (AggOp aggOp : types) {
            testOpi(false, MIDDLE_SIZE, SMALL_BINARY_DIM, aggOp, true, false);
            testOpi(false, MIDDLE_SIZE, SMALL_BINARY_DIM, aggOp, false, true);
        }
    }

    @Test
    public void testLargeSize() {
        for (AggOp aggOp : types) {
            testOpi(true, LARGE_SIZE, SMALL_BINARY_DIM, aggOp, true, false);
            testOpi(true, LARGE_SIZE, SMALL_BINARY_DIM, aggOp, false, true);
        }
    }

    @Test
    public void testLargeBinaryDim() {
        for (AggOp aggOp : types) {
            testOpi(true, LARGE_SIZE, LARGE_BINARY_DIM, aggOp, true, false);
        }
    }

    private AggParty[] getParties(boolean parallel) {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        boolean isMalicious = config.getSecurityModel().equals(SecurityModel.MALICIOUS);
        Abb3RpConfig abb3RpConfig = (isMalicious && USE_MT_TEST_MODE)
            ? new Abb3RpConfig.Builder(isMalicious, verifyWithMac)
            .setTripletProviderConfig(new TripletProviderConfig.Builder(true)
                .setRpZ2MtpConfig(RpMtProviderFactory.createZ2MtpConfigTestMode())
                .setRpZl64MtpConfig(RpMtProviderFactory.createZl64MtpConfigTestMode())
                .build()).build()
            : new Abb3RpConfig.Builder(isMalicious, verifyWithMac).build();
        Abb3Party[] abb3Parties = IntStream.range(0, 3).mapToObj(i ->
            new Abb3RpParty(rpcAll[i], abb3RpConfig)).toArray(Abb3RpParty[]::new);

        AggParty[] parties = Arrays.stream(abb3Parties).map(each ->
            AggFactory.createParty(each, config)).toArray(AggParty[]::new);

        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(parties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return parties;
    }

    public LongVector[] genLongData(int inputSize) {
        return new LongVector[]{
            LongVector.create(IntStream.range(0, inputSize).mapToLong(i -> SECURE_RANDOM.nextLong()).toArray()),
            LongVector.create(IntStream.range(0, inputSize).mapToLong(i -> SECURE_RANDOM.nextBoolean() ? 1 : 0).toArray())
        };
    }

    public BitVector[][] genBinaryData(int inputSize, int inputDim) {
        return new BitVector[][]{
            IntStream.range(0, inputDim).mapToObj(i -> BitVectorFactory.createRandom(inputSize, SECURE_RANDOM)).toArray(BitVector[]::new),
            new BitVector[]{BitVectorFactory.createRandom(inputSize, SECURE_RANDOM)}
        };
    }

    private void testOpi(boolean parallel, int inputSizes, int binaryDim, AggOp aggOp, boolean testBinary, boolean testArithmetic) {
        AggParty[] parties = getParties(parallel);
        LongVector[] inputLongVec = testArithmetic ? genLongData(inputSizes) : new LongVector[]{null, null};
        BitVector[][] inputBitVec = testBinary ? genBinaryData(inputSizes, binaryDim) : new BitVector[][]{null, new BitVector[]{null}};
        try {
            LOGGER.info("-----test {}, (inputSizes = {}, binaryDim = {}, aggType = {}) start-----",
                parties[0].getPtoDesc().getPtoName(), inputSizes, binaryDim, aggOp.name());
            AggPartyThread[] threads = Arrays.stream(parties)
                .map(p -> new AggPartyThread(p, inputBitVec[0], inputBitVec[1][0], inputLongVec[0], inputLongVec[1], aggOp))
                .toArray(AggPartyThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (AggPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            if (testArithmetic) {
                verifyArithmeticOutput(inputLongVec, threads[0].aOutput(), aggOp);
            }
            if (testBinary) {
                verifyBinaryOutput(inputBitVec, threads[0].bOutput(), aggOp);
            }

            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (inputSizes = {}, binaryDim = {}, aggType = {}) end, time:{}-----",
                parties[0].getPtoDesc().getPtoName(), inputSizes, binaryDim, aggOp, time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void verifyArithmeticOutput(LongVector[] inputLongVec, LongVector output, AggOp aggOp) {
        Assert.assertEquals(1, output.getNum());
        switch (aggOp) {
            case SUM: {
                long sum = IntStream.range(0, inputLongVec[0].getNum())
                    .mapToLong(i -> inputLongVec[0].getElement(i) * inputLongVec[1].getElement(i))
                    .sum();
                Assert.assertEquals(output.getElement(0), sum);
                break;
            }
            case MAX, MIN: {
                BigInteger[] allBig = Arrays.stream(inputLongVec[0].getElements())
                    .mapToObj(LongUtils::longToByteArray)
                    .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
                    .toArray(BigInteger[]::new);
                boolean[] flag = new boolean[allBig.length];
                IntStream.range(0, allBig.length).forEach(i -> flag[i] = inputLongVec[1].getElement(i) == 1);
                BigInteger res = BigIntegerUtils.byteArrayToNonNegBigInteger(LongUtils.longToByteArray(output.getElement(0)));
                verifyExtreme(allBig, flag, res, 64, aggOp);
                break;
            }
            default:
                throw new IllegalArgumentException("Invalid " + AggOp.class.getSimpleName() + ": " + aggOp.name());
        }
    }

    private void verifyBinaryOutput(BitVector[][] inputVec, BitVector[] output, AggOp aggOp) {
        Assert.assertEquals(1, output[0].bitNum());
        BigInteger[] inputBig = ZlDatabase.create(EnvType.STANDARD, true, inputVec[0]).getBigIntegerData();
        boolean[] flag = BinaryUtils.byteArrayToBinary(inputVec[1][0].getBytes(), inputVec[1][0].bitNum());
        BigInteger res = ZlDatabase.create(EnvType.STANDARD, true, output).getBigIntegerData()[0];
        switch (aggOp) {
            case SUM: {
                BigInteger sum = IntStream.range(0, inputBig.length)
                    .mapToObj(i -> flag[i] ? inputBig[i] : BigInteger.ZERO)
                    .reduce(BigInteger::add)
                    .orElse(BigInteger.ZERO);
                sum = sum.mod(BigInteger.ONE.shiftLeft(64));
                Assert.assertEquals(res.compareTo(sum), 0);
                break;
            }
            case MAX, MIN: {
                verifyExtreme(inputBig, flag, res, inputVec[0].length, aggOp);
                break;
            }
            default:
                throw new IllegalArgumentException("Invalid " + AggOp.class.getSimpleName() + ": " + aggOp.name());
        }
    }

    private void verifyExtreme(BigInteger[] input, boolean[] flag, BigInteger res, int bitLen, AggOp aggOp) {
        boolean valid = false;
        BigInteger last = aggOp.equals(AggOp.MAX) ? BigInteger.ZERO : BigInteger.ONE.shiftLeft(bitLen);
        for (int i = 0; i < input.length; i++) {
            valid = valid | flag[i];
            if (flag[i]) {
                if (aggOp.equals(AggOp.MAX)) {
                    last = last.max(input[i]);
                } else {
                    last = last.min(input[i]);
                }
            }
        }
        last = valid ? last : BigInteger.ZERO;
        Assert.assertEquals(res.compareTo(last), 0);
    }
}
