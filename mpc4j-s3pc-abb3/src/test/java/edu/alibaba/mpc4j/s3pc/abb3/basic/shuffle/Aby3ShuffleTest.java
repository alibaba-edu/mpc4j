package edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate.Aby3Z2cConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate.Aby3Z2cFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.TripletRpLongConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.TripletRpLongCpFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.aby3.Aby3LongConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.mac.Cgh18RpLongConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.mac.Cgh18RpLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleOperations.AcShuffleRes;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleOperations.BcShuffleRes;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleOperations.ShuffleOp;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.replicate.Aby3ShuffleConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.replicate.Aby3ShuffleFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.replicate.Aby3ShuffleParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.replicate.Aby3ShufflePtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.ShuffleUtils;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
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

/**
 * shuffle party test.
 *
 * @author Feng Han
 * @date 2024/02/04
 */
@RunWith(Parameterized.class)
public class Aby3ShuffleTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(Aby3ShuffleTest.class);

    public static final ShuffleOp[] opAll = new ShuffleOp[]{
        ShuffleOp.B_SHUFFLE_ROW,
        ShuffleOp.B_SHUFFLE_COLUMN,
        ShuffleOp.B_PERMUTE_NETWORK,
        ShuffleOp.B_SWITCH_NETWORK,
        ShuffleOp.B_DUPLICATE_NETWORK,
        ShuffleOp.A_SHUFFLE,
        ShuffleOp.A_SHUFFLE_OPEN,
        ShuffleOp.A_INV_SHUFFLE,
        ShuffleOp.A_PERMUTE_NETWORK,
        ShuffleOp.A_SWITCH_NETWORK,
        ShuffleOp.A_DUPLICATE_NETWORK
    };

    private static final boolean USE_MT_TEST_MODE = false;
    private static final String TUPLE_DIR = "./";

    private static final int B_BATCH_NUM = 64;

    private static final int A_BATCH_NUM = 2;

    private static final int SMALL_SIZE = 1 << 6;

    private static final int MIDDLE_SIZE = 1 << 9;

    private static final int LARGE_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            Aby3ShufflePtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new Aby3ShuffleConfig.Builder(false).build(), false
        });
        configurations.add(new Object[]{
            Aby3ShufflePtoDesc.getInstance().getPtoName() + "(malicious + ac use aby3)",
            new Aby3ShuffleConfig.Builder(true).build(), false
        });
        configurations.add(new Object[]{
            Aby3ShufflePtoDesc.getInstance().getPtoName() + "(malicious + ac use mac)",
            new Aby3ShuffleConfig.Builder(true).build(), true
        });

        return configurations;
    }

    private final Aby3ShuffleConfig config;
    private final boolean baseUseMac;

    public Aby3ShuffleTest(String name, Aby3ShuffleConfig config, boolean baseUseMac) {
        super(name);
        this.config = config;
        this.baseUseMac = baseUseMac;
    }

    @Test
    public void testAllSmallSize() {
        testOpi(false, opAll, new int[]{SMALL_SIZE, B_BATCH_NUM}, new int[]{SMALL_SIZE, A_BATCH_NUM});
    }

    @Test
    public void testEachSmallSize() {
        for (ShuffleOp op : opAll) {
            ShuffleOp[] single = new ShuffleOp[]{op};
            testOpi(false, single, new int[]{SMALL_SIZE, B_BATCH_NUM}, new int[]{SMALL_SIZE, A_BATCH_NUM});
        }
    }

    @Test
    public void testAllMiddleSize() {
        testOpi(false, opAll, new int[]{MIDDLE_SIZE, B_BATCH_NUM}, new int[]{MIDDLE_SIZE, A_BATCH_NUM});
    }

    @Test
    public void testEachMiddleSize() {
        for (ShuffleOp op : opAll) {
            ShuffleOp[] single = new ShuffleOp[]{op};
            testOpi(false, single, new int[]{MIDDLE_SIZE, B_BATCH_NUM}, new int[]{MIDDLE_SIZE, A_BATCH_NUM});
        }
    }

    @Test
    public void testAllLargeSize() {
        testOpi(false, opAll, new int[]{LARGE_SIZE, B_BATCH_NUM}, new int[]{LARGE_SIZE, A_BATCH_NUM});
    }
    @Test
    public void testEachLargeSize() {
        for(ShuffleOp op : opAll){
            ShuffleOp[] single = new ShuffleOp[]{op};
            testOpi(false, single, new int[]{LARGE_SIZE, B_BATCH_NUM}, new int[]{LARGE_SIZE, A_BATCH_NUM});
        }
    }

    private Aby3ShuffleParty[] getParties(boolean parallel) {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        boolean isMalicious = config.getSecurityModel().equals(SecurityModel.MALICIOUS);
        TripletProviderConfig providerConfig;
        if (isMalicious && USE_MT_TEST_MODE) {
            providerConfig = new TripletProviderConfig.Builder(true)
                .setRpZ2MtpConfig(RpMtProviderFactory.createZ2MtpConfigTestMode(TUPLE_DIR))
                .setRpZl64MtpConfig(RpMtProviderFactory.createZl64MtpConfigTestMode(TUPLE_DIR))
                .build();
        } else {
            providerConfig = new TripletProviderConfig.Builder(isMalicious).build();
        }

        TripletProvider[] tripletProviders = IntStream.range(0, 3).mapToObj(i ->
            new TripletProvider(rpcAll[i], providerConfig)).toArray(TripletProvider[]::new);

        TripletZ2cParty[] bcParties = IntStream.range(0, 3).mapToObj(i ->
                Aby3Z2cFactory.createParty(rpcAll[i], new Aby3Z2cConfig.Builder(isMalicious).build(), tripletProviders[i]))
            .toArray(TripletZ2cParty[]::new);

        TripletRpLongConfig tripletRpZl64cConfig = baseUseMac
            ? new Cgh18RpLongConfig.Builder().build()
            : new Aby3LongConfig.Builder(isMalicious).build();
        Cgh18RpLongParty[] macParties = new Cgh18RpLongParty[3];
        TripletLongParty[] acParties = IntStream.range(0, 3).mapToObj(i ->
            TripletRpLongCpFactory.createParty(rpcAll[i], tripletRpZl64cConfig, tripletProviders[i])).toArray(TripletLongParty[]::new);
        if (baseUseMac) {
            IntStream.range(0, 3).forEach(i -> macParties[i] = (Cgh18RpLongParty) acParties[i]);
        } else {
            Cgh18RpLongConfig cgh18RpZl64cConfig = new Cgh18RpLongConfig.Builder().build();
            IntStream.range(0, 3).forEach(i -> macParties[i] = new Cgh18RpLongParty(rpcAll[i], cgh18RpZl64cConfig, tripletProviders[i]));
        }
        Aby3ShuffleParty[] shuffleParties = IntStream.range(0, 3).mapToObj(i ->
            Aby3ShuffleFactory.createParty(config, bcParties[i], acParties[i], macParties[i])).toArray(Aby3ShuffleParty[]::new);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(shuffleParties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return shuffleParties;
    }

    private void verifyAsRes(ShuffleOp op, AcShuffleRes[] data) throws MpcAbortException {
        LOGGER.info("verifying " + op.name());
        switch (op) {
            case A_SHUFFLE: {
                int length = data[0].input[0].getNum();
                HashMap<BigInteger, int[]> inputHashMap = new HashMap<>();
                IntStream.range(0, length).forEach(i -> {
                    long[] tmpLongArray = Arrays.stream(data[0].input).mapToLong(longVector -> longVector.getElement(i)).toArray();
                    BigInteger tmp = BigIntegerUtils.byteArrayToBigInteger(LongUtils.longArrayToByteArray(tmpLongArray));
                    if (inputHashMap.containsKey(tmp)) {
                        inputHashMap.get(tmp)[0]++;
                    } else {
                        inputHashMap.put(tmp, new int[]{1});
                    }
                });
                IntStream.range(0, length).forEach(i -> {
                    long[] tmpLongArray = Arrays.stream(data[0].output).mapToLong(longVector -> longVector.getElement(i)).toArray();
                    BigInteger tmp = BigIntegerUtils.byteArrayToBigInteger(LongUtils.longArrayToByteArray(tmpLongArray));
                    Assert.assertTrue(inputHashMap.containsKey(tmp));
                    inputHashMap.get(tmp)[0]--;
                    Assert.assertTrue(inputHashMap.get(tmp)[0] >= 0);
                });
                break;
            }
            case A_SHUFFLE_OPEN:{
                LongVector[] inputShould = ShuffleUtils.applyPermutationToRows(data[0].input, data[0].fun);
                Assert.assertArrayEquals(inputShould, data[0].output);
                break;
            }
            case A_INV_SHUFFLE: {
                LongVector[] inputShould = ShuffleUtils.applyPermutationToRows(data[0].output, data[0].fun);
                Assert.assertArrayEquals(inputShould, data[0].input);
                break;
            }
            case A_PERMUTE_NETWORK:
            case A_SWITCH_NETWORK: {
                for (AcShuffleRes oneRes : data) {
                    LOGGER.info("verifying output length:{}", oneRes.output[0].getNum());
                    int dim = oneRes.input.length;
                    for (int i = 0; i < oneRes.output[0].getNum(); i++) {
                        if (oneRes.fun[i] < oneRes.input[0].getNum()) {
                            for (int j = 0; j < dim; j++) {
                                Assert.assertEquals(oneRes.output[j].getElement(i), oneRes.input[j].getElement(oneRes.fun[i]));
                            }
                        } else {
                            for (int j = 0; j < dim; j++) {
                                Assert.assertEquals(oneRes.output[j].getElement(i), 0);
                            }
                        }
                    }
                }
                break;
            }
            case A_DUPLICATE_NETWORK: {
                Assert.assertEquals(data[0].input.length, data[0].output.length);
                for (int i = 0; i < data[0].output.length; i++) {
                    Assert.assertEquals(data[0].input[i].getNum(), data[0].output[i].getNum());
                    Assert.assertEquals(data[0].input[i].getElement(0), data[0].output[i].getElement(0));
                    for (int j = 1; j < data[0].input[i].getNum(); j++) {
                        Assert.assertEquals(data[0].output[i].getElement(j), data[0].flag[j] ? data[0].output[i].getElement(j - 1) : data[0].input[i].getElement(j));
                    }
                }
                break;
            }
            default:
                throw new IllegalArgumentException("wrong operation for arithmetic shuffle: " + op.name());
        }
    }

    private void verifyBsRes(ShuffleOp op, BcShuffleRes[] data) {
        LOGGER.info("verifying " + op.name());
        switch (op) {
            case B_SHUFFLE_ROW:
                testBinaryContain(data[0].input, data[0].output, true);
                break;
            case B_SHUFFLE_COLUMN:
                testBinaryContain(data[0].input, data[0].output, false);
                break;
            case B_PERMUTE_NETWORK:
            case B_SWITCH_NETWORK: {
                for (BcShuffleRes oneRes : data) {
                    BigInteger[] inputBig = ZlDatabase.create(EnvType.STANDARD, true, oneRes.input).getBigIntegerData();
                    BigInteger[] outputBig = ZlDatabase.create(EnvType.STANDARD, true, oneRes.output).getBigIntegerData();
                    Assert.assertEquals(outputBig.length, oneRes.fun.length);
                    for (int i = 0; i < outputBig.length; i++) {
                        if (oneRes.fun[i] < inputBig.length) {
                            Assert.assertEquals(outputBig[i], inputBig[oneRes.fun[i]]);
                        } else {
                            Assert.assertEquals(outputBig[i], BigInteger.ZERO);
                        }
                    }
                }
                break;
            }
            case B_DUPLICATE_NETWORK: {
                BigInteger[] inputBig = ZlDatabase.create(EnvType.STANDARD, true, data[0].input).getBigIntegerData();
                BigInteger[] outputBig = ZlDatabase.create(EnvType.STANDARD, true, data[0].output).getBigIntegerData();
                Assert.assertEquals(inputBig.length, outputBig.length);
                Assert.assertEquals(inputBig[0], outputBig[0]);
                for (int j = 1; j < outputBig.length; j++) {
                    Assert.assertEquals(outputBig[j], data[0].flag[j] ? outputBig[j - 1] : inputBig[j]);
                }
                break;
            }
            default:
                throw new IllegalArgumentException("wrong operation for binary shuffle: " + op.name());
        }
    }

    private void testBinaryContain(BitVector[] input, BitVector[] output, boolean inRow) {
        BigInteger[] inputBig, outputBig;
        if (inRow) {
            inputBig = Arrays.stream(input).map(x -> BigIntegerUtils.byteArrayToNonNegBigInteger(x.getBytes())).toArray(BigInteger[]::new);
            outputBig = Arrays.stream(output).map(x -> BigIntegerUtils.byteArrayToNonNegBigInteger(x.getBytes())).toArray(BigInteger[]::new);
        } else {
            inputBig = ZlDatabase.create(EnvType.STANDARD, true, input).getBigIntegerData();
            outputBig = ZlDatabase.create(EnvType.STANDARD, true, output).getBigIntegerData();
        }
        Assert.assertEquals(inputBig.length, outputBig.length);
        HashMap<BigInteger, int[]> inputHashMap = new HashMap<>();
        for (BigInteger eachInput : inputBig) {
            if (inputHashMap.containsKey(eachInput)) {
                inputHashMap.get(eachInput)[0]++;
            } else {
                inputHashMap.put(eachInput, new int[]{1});
            }
        }
        for (BigInteger eachOut : outputBig) {
            Assert.assertTrue(inputHashMap.containsKey(eachOut));
            inputHashMap.get(eachOut)[0]--;
            Assert.assertTrue(inputHashMap.get(eachOut)[0] >= 0);
        }
    }

    private void testOpi(boolean parallel, ShuffleOp[] ops, int[] bitParam, int[] longParam) {
        Aby3ShuffleParty[] parties = getParties(parallel);
        try {
            LOGGER.info("-----test {}, (bitParam = {}, longParam = {}) start-----",
                parties[0].getPtoDesc().getPtoName(), Arrays.toString(bitParam), Arrays.toString(longParam));
            Aby3ShufflePartyThread[] threads = Arrays.stream(parties).map(p ->
                new Aby3ShufflePartyThread(p, bitParam, longParam, ops)).toArray(Aby3ShufflePartyThread[]::new);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (Aby3ShufflePartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            for (ShuffleOp op : ops) {
                if (op.name().startsWith("A_")) {
                    AcShuffleRes[] aData = threads[0].getAcShuffleRes(op);
                    verifyAsRes(op, aData);
                } else {
                    BcShuffleRes[] bData = threads[0].getBcShuffleRes(op);
                    verifyBsRes(op, bData);
                }
            }
            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (bitParam = {}, longParam = {}) end, time:{}-----", parties[0].getPtoDesc().getPtoName(),
                Arrays.toString(bitParam), Arrays.toString(longParam), time);
            LOGGER.info("op:[{}] test pass", Arrays.toString(ops));
        } catch (InterruptedException | MpcAbortException e) {
            e.printStackTrace();
        }
    }

}
