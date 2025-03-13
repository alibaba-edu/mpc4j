package edu.alibaba.mpc4j.work.scape.s3pc.db.orderby;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.hzf22.Hzf22OrderByConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.hzf22.Hzf22OrderByPtoDesc;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.naive.NaiveOrderByConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.naive.NaiveOrderByPtoDesc;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * @author Feng Han
 * @date 2025/3/4
 */
@RunWith(Parameterized.class)
public class OrderByTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderByTest.class);
    /**
     * use simulate mtp or not
     */
    private static final boolean USE_MT_TEST_MODE = true;
    /**
     * bit length of key to be sorted in our test
     */
    private static final int[] B_KEY_DIM = new int[]{4, 64};
    /**
     * bit length of key to be sorted in our test
     */
    private static final int[] A_KEY_DIM = new int[]{2, 3};
    /**
     * payload bit length of binary input
     */
    private static final int B_PAYLOAD_DIM = 20;
    /**
     * payload bit length of arithmetic input
     */
    private static final int A_PAYLOAD_DIM = 3;
    /**
     * small input size
     */
    private static final int SMALL_SIZE = 1 << 2;
    /**
     * middle input size
     */
    private static final int MIDDLE_SIZE = 333;
    /**
     * large input size
     */
    private static final int LARGE_SIZE = 3375;

    @Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // naive
        configurations.add(new Object[]{
            NaiveOrderByPtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new NaiveOrderByConfig.Builder(false).build(), false
        });
        configurations.add(new Object[]{
            NaiveOrderByPtoDesc.getInstance().getPtoName() + "(malicious, tuple)",
            new NaiveOrderByConfig.Builder(true).build(), false
        });
        configurations.add(new Object[]{
            NaiveOrderByPtoDesc.getInstance().getPtoName() + "(malicious, mac)",
            new NaiveOrderByConfig.Builder(true).build(), true
        });

        // hzf22
        configurations.add(new Object[]{
            Hzf22OrderByPtoDesc.getInstance().getPtoName() + "(semi-honest)",
            new Hzf22OrderByConfig.Builder(false).build(), false
        });
        configurations.add(new Object[]{
            Hzf22OrderByPtoDesc.getInstance().getPtoName() + "(malicious, tuple)",
            new Hzf22OrderByConfig.Builder(true).build(), false
        });
        configurations.add(new Object[]{
            Hzf22OrderByPtoDesc.getInstance().getPtoName() + "(malicious, mac)",
            new Hzf22OrderByConfig.Builder(true).build(), true
        });

        return configurations;
    }

    /**
     * sort configure
     */
    private final OrderByConfig config;
    /**
     * verify with mac or not
     */
    private final boolean baseUseMac;

    public OrderByTest(String name, OrderByConfig config, boolean baseUseMac) {
        super(name);
        this.config = config;
        this.baseUseMac = baseUseMac;
    }

    @Test
    public void test1() {
        testEach(false, 1, A_KEY_DIM[0], B_KEY_DIM[0], A_PAYLOAD_DIM, B_PAYLOAD_DIM);
    }

    @Test
    public void test7() {
        testEach(false, 7, A_KEY_DIM[0], B_KEY_DIM[0], A_PAYLOAD_DIM, B_PAYLOAD_DIM);
    }

    @Test
    public void testSmallSize() {
        testEach(false, SMALL_SIZE, A_KEY_DIM[0], B_KEY_DIM[0], A_PAYLOAD_DIM, B_PAYLOAD_DIM);
    }

    @Test
    public void testMiddleSize() {
        testEach(true, MIDDLE_SIZE, A_KEY_DIM[0], B_KEY_DIM[0], A_PAYLOAD_DIM, B_PAYLOAD_DIM);
    }

    @Test
    public void testLargeSize() {
        testEach(true, LARGE_SIZE, A_KEY_DIM[1], B_KEY_DIM[1], A_PAYLOAD_DIM, B_PAYLOAD_DIM);
    }

    private void testEach(boolean parallel, int dataNum, int aKeyDim, int bKeyDim, int aPayloadDim, int bPayloadDim) {
        testOpi(parallel, dataNum, true, false, aKeyDim, bKeyDim, aPayloadDim, bPayloadDim);
        testOpi(parallel, dataNum, false, true, aKeyDim, bKeyDim, aPayloadDim, bPayloadDim);
    }

    private OrderByParty[] getParties(boolean parallel) {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        boolean isMalicious = config.getSecurityModel().equals(SecurityModel.MALICIOUS);

        Abb3RpConfig abb3RpConfig = (isMalicious && USE_MT_TEST_MODE)
            ? new Abb3RpConfig.Builder(isMalicious, baseUseMac)
            .setTripletProviderConfig(new TripletProviderConfig.Builder(true)
                .setRpZ2MtpConfig(RpMtProviderFactory.createZ2MtpConfigTestMode())
                .setRpZl64MtpConfig(RpMtProviderFactory.createZl64MtpConfigTestMode())
                .build()).build()
            : new Abb3RpConfig.Builder(isMalicious, baseUseMac).build();
        Abb3Party[] parties = IntStream.range(0, 3).mapToObj(i ->
                new Abb3RpParty(rpcAll[i], abb3RpConfig))
            .toArray(Abb3RpParty[]::new);

        OrderByParty[] orderByParties = Arrays.stream(parties).map(each ->
            OrderByFactory.createParty(each, config)).toArray(OrderByParty[]::new);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(orderByParties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return orderByParties;
    }

    private void testOpi(boolean parallel, int dataNum, boolean testArithmetic, boolean testBinary,
                         int aKeyDim, int bKeyDim, int aPayloadDim, int bPayloadDim) {
        LongVector[] aInput = testArithmetic ? genInputData(dataNum, aKeyDim, aPayloadDim) : null;
        int[] aKeyInd = testArithmetic ? shuffleInputDim(aInput, aKeyDim) : null;
        BitVector[] bInput = testBinary
            ? IntStream.range(0, bKeyDim + bPayloadDim + 1)
            .mapToObj(i -> BitVectorFactory.createRandom(dataNum, SECURE_RANDOM))
            .toArray(BitVector[]::new)
            : null;
        int[] bKeyInd = testBinary ? shuffleInputDim(bInput, bKeyDim) : null;

        OrderByParty[] parties = getParties(parallel);
        LOGGER.info("-----test {}, (dataNum = {}) start-----",
            parties[0].getPtoDesc().getPtoName(), dataNum);
        try {
            OrderByPartyThread[] threads = Arrays.stream(parties).map(p ->
                new OrderByPartyThread(p, bInput, bKeyInd, aInput, aKeyInd)).toArray(OrderByPartyThread[]::new);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for (OrderByPartyThread t : threads) {
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            // verify
            if (testBinary) {
                verifyBinary(bInput, threads[0].getbOutput(), bKeyInd);
            }
            if (testArithmetic) {
                verifyArithmetic(aInput, threads[0].getaOutput(), aKeyInd);
            }
            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, (dataNum = {}) end, time:{}-----", parties[0].getPtoDesc().getPtoName(),
                dataNum, time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private LongVector[] genInputData(int size, int keyDim, int payloadDim) {
        LongVector[] res = new LongVector[keyDim + payloadDim + 1];
        for (int i = 0; i < keyDim + payloadDim; i++) {
            long[] tmp = IntStream.range(0, size).mapToLong(j -> Math.abs(SECURE_RANDOM.nextLong())).toArray();
            res[i] = LongVector.create(tmp);
        }
        res[keyDim + payloadDim] = LongVector.create(LongStream.range(0, size).map(i -> SECURE_RANDOM.nextBoolean() ? 1 : 0).toArray());
        return res;
    }

    private int[] shuffleInputDim(Object[] input, int keyDim) {
//        return IntStream.range(0, keyDim).toArray();
        Object[] tmp = Arrays.copyOf(input, input.length);
        // 得到0到input.length-1的随机keyDim个数据
        TIntList keyInd = new TIntLinkedList();
        while (keyInd.size() < keyDim) {
            int r = SECURE_RANDOM.nextInt(input.length - 1);
            if (!keyInd.contains(r)) {
                keyInd.add(r);
            }
        }
        keyInd.sort();
        int[] keyIndArray = keyInd.toArray();
        for (int i = 0; i < keyDim; i++) {
            input[keyIndArray[i]] = tmp[i];
        }
        int sourceInd = keyDim;
        for (int targetInd = 0; targetInd < input.length - 1; targetInd++) {
            if (!keyInd.contains(targetInd)) {
                input[targetInd] = tmp[sourceInd];
                sourceInd++;
            }
        }
        return keyIndArray;
    }

    private void shuffleBack(Object[] input, int[] keyInd) {
        Object[] tmp = Arrays.copyOf(input, input.length);
        IntStream.range(0, keyInd.length).forEach(i -> input[i] = tmp[keyInd[i]]);
        HashSet<Integer> hashSet = Arrays.stream(keyInd).boxed().collect(Collectors.toCollection(HashSet::new));
        int index = 0;
        for (int i = keyInd.length; i < input.length; i++) {
            while (hashSet.contains(index)) {
                index++;
            }
            input[i] = tmp[index];
            index++;
        }
    }

    private void verifyArithmetic(LongVector[] plainInput, LongVector[] output, int[] keyInd) {
        int num = plainInput[0].getNum();
        Assert.assertEquals(plainInput.length, output.length);
        shuffleBack(plainInput, keyInd);
        shuffleBack(output, keyInd);

        BigInteger[] inputKey = new BigInteger[num];
        BigInteger[] inputPayload = new BigInteger[num];
        BigInteger[] outputKey = new BigInteger[num];
        BigInteger[] outputPayload = new BigInteger[num];
        IntStream.range(0, num).parallel().forEach(i -> {
            inputKey[i] = BigInteger.valueOf(1 - plainInput[plainInput.length - 1].getElement(i));
            outputKey[i] = BigInteger.valueOf(1 - output[output.length - 1].getElement(i));
            for (int j = 0; j < keyInd.length; j++) {
                inputKey[i] = inputKey[i].shiftLeft(64).add(BigInteger.valueOf(plainInput[j].getElement(i)));
                outputKey[i] = outputKey[i].shiftLeft(64).add(BigInteger.valueOf(output[j].getElement(i)));
            }
            inputPayload[i] = BigInteger.ZERO;
            outputPayload[i] = BigInteger.ZERO;
            for (int j = keyInd.length; j < plainInput.length - 1; j++) {
                inputPayload[i] = inputPayload[i].shiftLeft(64).add(BigInteger.valueOf(plainInput[j].getElement(i)));
                outputPayload[i] = outputPayload[i].shiftLeft(64).add(BigInteger.valueOf(output[j].getElement(i)));
            }
        });

        verifyOrder(inputKey, inputPayload, outputKey, outputPayload);
    }

    private void verifyBinary(BitVector[] plainInput, BitVector[] output, int[] keyInd) {
        Assert.assertEquals(plainInput.length, output.length);
        shuffleBack(plainInput, keyInd);
        shuffleBack(output, keyInd);
        BitVector[] inputKeyBit = new BitVector[keyInd.length + 1];
        inputKeyBit[0] = plainInput[plainInput.length - 1].not();
        System.arraycopy(plainInput, 0, inputKeyBit, 1, keyInd.length);
        BigInteger[] inputKey = ZlDatabase.create(EnvType.STANDARD, true, inputKeyBit).getBigIntegerData();
        BigInteger[] inputPayload = ZlDatabase.create(EnvType.STANDARD, true,
            Arrays.copyOfRange(plainInput, keyInd.length, plainInput.length - 1)).getBigIntegerData();

        BitVector[] outputKeyBit = new BitVector[keyInd.length + 1];
        outputKeyBit[0] = output[output.length - 1].not();
        System.arraycopy(output, 0, outputKeyBit, 1, keyInd.length);
        BigInteger[] outputKey = ZlDatabase.create(EnvType.STANDARD, true, outputKeyBit).getBigIntegerData();
        BigInteger[] outputPayload = ZlDatabase.create(EnvType.STANDARD, true,
            Arrays.copyOfRange(output, keyInd.length, output.length - 1)).getBigIntegerData();

        verifyOrder(inputKey, inputPayload, outputKey, outputPayload);
    }

    private void verifyOrder(BigInteger[] inputKey, BigInteger[] inputPayload, BigInteger[] outputKey, BigInteger[] outputPayload) {
        Map<BigInteger, List<BigInteger>> inputMap = new HashMap<>();
        for (int i = 0; i < inputKey.length; i++) {
            inputMap.computeIfAbsent(inputKey[i], k -> new LinkedList<>()).add(inputPayload[i]);
        }
        for (int i = 0; i < outputPayload.length - 1; i++) {
            Assert.assertTrue(outputKey[i].compareTo(outputKey[i + 1]) <= 0);
            Assert.assertTrue(inputMap.containsKey(outputKey[i]));
            Assert.assertTrue(inputMap.get(outputKey[i]).contains(outputPayload[i]));
            if (inputMap.get(outputKey[i]).size() == 1) {
                inputMap.remove(outputKey[i]);
            }
        }
    }
}
