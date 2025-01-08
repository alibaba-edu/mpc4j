package edu.alibaba.mpc4j.s2pc.opf.osorter;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.osorter.ObSortFactory.ObSortType;
import edu.alibaba.mpc4j.s2pc.opf.osorter.bitonic.BitonicSorterConfig;
import edu.alibaba.mpc4j.s2pc.opf.osorter.quick.QuickSorterConfig;
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
import java.util.stream.IntStream;

/**
 * @author Feng Han
 * @date 2024/9/30
 */
@RunWith(Parameterized.class)
public class ObSorterTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ObSorterTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 999;
    /**
     * default num
     */
    private static final int PAYLOAD_DIM_UPPER = 100;
    /**
     * large num
     */
    private static final int LARGE_NUM = (1 << 12);
    /**
     * default l
     */
    private static final int DEFAULT_L = 64;
    /**
     * small l
     */
    private static final int SMALL_L = 3;
    /**
     * large l
     */
    private static final int LARGE_L = 127;

    @Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // quick sort
        configurations.add(new Object[]{
            ObSortType.QUICK.name() + " (" + SecurityModel.SEMI_HONEST.name() + ")",
            new QuickSorterConfig.Builder(false).build()
        });

        // bitonic sort
        configurations.add(new Object[]{
            ObSortType.BITONIC.name() + " (" + SecurityModel.SEMI_HONEST.name() + ")",
            new BitonicSorterConfig.Builder(false).build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final ObSortConfig config;

    public ObSorterTest(String name, ObSortConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test2Num() {
        testPto(2, DEFAULT_L, true, false, false);
        testPto(2, DEFAULT_L, true, false, true);
        testPto(2, DEFAULT_L, true, true, true);
    }

    @Test
    public void test7Num() {
        testPto(7, SMALL_L, true, false, false);
        testPto(7, SMALL_L, true, false, true);
        testPto(7, SMALL_L, true, true, true);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_NUM, DEFAULT_L, false, false, false);
        testPto(DEFAULT_NUM, DEFAULT_L, false, false, true);
        testPto(DEFAULT_NUM, DEFAULT_L, false, true, true);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_NUM, DEFAULT_L, true, false, false);
        testPto(DEFAULT_NUM, DEFAULT_L, true, false, true);
        testPto(DEFAULT_NUM, DEFAULT_L, true, true, true);
    }

    @Test
    public void testLargeNumDefaultDim() {
        testPto(LARGE_NUM, DEFAULT_L, true, false, false);
        testPto(LARGE_NUM, DEFAULT_L, true, false, true);
        testPto(LARGE_NUM, DEFAULT_L, true, true, true);
    }

    @Test
    public void testParallelLargeDim() {
        testPto(DEFAULT_NUM, LARGE_L, true, false, false);
        testPto(DEFAULT_NUM, LARGE_L, true, false, true);
        testPto(DEFAULT_NUM, LARGE_L, true, true, true);
    }

    private void testPto(int dataNum, int dimNum, boolean parallel, boolean needStable, boolean needPermutation) {
        testPtoWithoutPayload(dataNum, dimNum, parallel, needStable, needPermutation);
        testWithPayload(dataNum, dimNum, parallel, needStable, needPermutation);
    }

    private void testPtoWithoutPayload(int dataNum, int dimNum, boolean parallel, boolean needStable, boolean needPermutation) {
        // create inputs
        BitVector[] inputs = IntStream.range(0, dimNum).mapToObj(i -> BitVectorFactory.createRandom(dataNum, SECURE_RANDOM)).toArray(BitVector[]::new);
        BitVector[] senderPlain = IntStream.range(0, dimNum).mapToObj(i -> BitVectorFactory.createRandom(dataNum, SECURE_RANDOM)).toArray(BitVector[]::new);
        SquareZ2Vector[] senderInput = Arrays.stream(senderPlain).map(ea -> SquareZ2Vector.create(ea, false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] receiverInput = IntStream.range(0, dimNum).mapToObj(i -> SquareZ2Vector.create(inputs[i].xor(senderPlain[i]), false)).toArray(SquareZ2Vector[]::new);

        // init the protocol
        ObSorter sender = ObSortFactory.createSorter(firstRpc, secondRpc.ownParty(), config);
        ObSorter receiver = ObSortFactory.createSorter(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start, need permutation:{}, need stable:{} -----", sender.getPtoDesc().getPtoName(), needPermutation, needStable);
            ObSorterThread senderThread = new ObSorterThread(sender, senderInput, null,  needPermutation, needStable);
            ObSorterThread receiverThread = new ObSorterThread(receiver, receiverInput, null, needPermutation, needStable);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            BitVector[] permutation = null;
            if (!needPermutation) {
                assert senderThread.getRes() == null;
                assert receiverThread.getRes() == null;
            } else {
                SquareZ2Vector[] z0 = senderThread.getRes();
                SquareZ2Vector[] z1 = receiverThread.getRes();
                permutation = IntStream.range(0, z0.length).mapToObj(i -> z0[i].getBitVector().xor(z1[i].getBitVector())).toArray(BitVector[]::new);
            }
            BitVector[] res = IntStream.range(0, dimNum).mapToObj(i -> senderInput[i].getBitVector().xor(receiverInput[i].getBitVector())).toArray(BitVector[]::new);
            // verify
            assertOutput(inputs, res, null, null, permutation, needPermutation, needStable);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void testWithPayload(int dataNum, int dimNum, boolean parallel, boolean needStable, boolean needPermutation) {
        // create inputs
        BitVector[] inputs = IntStream.range(0, dimNum).mapToObj(i -> BitVectorFactory.createRandom(dataNum, SECURE_RANDOM)).toArray(BitVector[]::new);
        BitVector[] senderPlain = IntStream.range(0, dimNum).mapToObj(i -> BitVectorFactory.createRandom(dataNum, SECURE_RANDOM)).toArray(BitVector[]::new);
        SquareZ2Vector[] senderInput = Arrays.stream(senderPlain).map(ea -> SquareZ2Vector.create(ea, false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] receiverInput = IntStream.range(0, dimNum).mapToObj(i -> SquareZ2Vector.create(inputs[i].xor(senderPlain[i]), false)).toArray(SquareZ2Vector[]::new);
        // create payload
        int allDim = SECURE_RANDOM.nextInt(1, PAYLOAD_DIM_UPPER);
        BitVector[] payloads = IntStream.range(0, allDim).mapToObj(i -> BitVectorFactory.createRandom(dataNum, SECURE_RANDOM)).toArray(BitVector[]::new);
        BitVector[] senderPayloadPlain = IntStream.range(0, allDim).mapToObj(i -> BitVectorFactory.createRandom(dataNum, SECURE_RANDOM)).toArray(BitVector[]::new);
        SquareZ2Vector[] senderPayload = Arrays.stream(senderPayloadPlain).map(ea -> SquareZ2Vector.create(ea, false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] receiverPayload = IntStream.range(0, allDim).mapToObj(i -> SquareZ2Vector.create(payloads[i].xor(senderPayloadPlain[i]), false)).toArray(SquareZ2Vector[]::new);

        // init the protocol
        ObSorter sender = ObSortFactory.createSorter(firstRpc, secondRpc.ownParty(), config);
        ObSorter receiver = ObSortFactory.createSorter(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start, need permutation:{}, need stable:{} -----", sender.getPtoDesc().getPtoName(), needPermutation, needStable);
            ObSorterThread senderThread = new ObSorterThread(sender, senderInput, senderPayload, needPermutation, needStable);
            ObSorterThread receiverThread = new ObSorterThread(receiver, receiverInput, receiverPayload, needPermutation, needStable);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            BitVector[] permutation = null;
            if (!needPermutation) {
                assert senderThread.getRes() == null;
                assert receiverThread.getRes() == null;
            } else {
                SquareZ2Vector[] z0 = senderThread.getRes();
                SquareZ2Vector[] z1 = receiverThread.getRes();
                permutation = IntStream.range(0, z0.length).mapToObj(i -> z0[i].getBitVector().xor(z1[i].getBitVector())).toArray(BitVector[]::new);
            }
            BitVector[] res = IntStream.range(0, dimNum).mapToObj(i -> senderInput[i].getBitVector().xor(receiverInput[i].getBitVector())).toArray(BitVector[]::new);
            BitVector[] payloadRes = IntStream.range(0, allDim).mapToObj(i -> senderPayload[i].getBitVector().xor(receiverPayload[i].getBitVector())).toArray(BitVector[]::new);
            // verify
            assertOutput(inputs, res, payloads, payloadRes, permutation, needPermutation, needStable);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(BitVector[] inputs, BitVector[] res, BitVector[] payloads, BitVector[] payloadRes, BitVector[] permutation, boolean needPermutation, boolean needStable) {
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(inputs[i].bitNum(), res[i].bitNum());
        }
        BigInteger[] inpBig = ZlDatabase.create(EnvType.STANDARD, true, inputs).getBigIntegerData();
        BigInteger[] outBig = ZlDatabase.create(EnvType.STANDARD, true, res).getBigIntegerData();
        int[] perm = !needPermutation ? null : Arrays.stream(ZlDatabase.create(EnvType.STANDARD, true, permutation).getBigIntegerData())
            .mapToInt(BigInteger::intValue)
            .toArray();
        BigInteger[] inCopy = Arrays.copyOf(inpBig, inpBig.length);
        Arrays.sort(inCopy);
        for (int i = 0; i < inCopy.length; i++) {
//            if(!inCopy[i].equals(outBig[i])){
//                LOGGER.info("inCopy:{}", Arrays.toString(inCopy));
//                LOGGER.info("outBig:{}", Arrays.toString(outBig));
//            }
            assert inCopy[i].equals(outBig[i]);
        }
        BigInteger[] inpPayload = null, outPayload = null;
        if(payloads != null){
            assert payloads.length == payloadRes.length;
            inpPayload = ZlDatabase.create(EnvType.STANDARD, true, payloads).getBigIntegerData();
            outPayload = ZlDatabase.create(EnvType.STANDARD, true, payloadRes).getBigIntegerData();
        }
        if (needPermutation) {
            assert perm.length == inpBig.length;
            assert PermutationNetworkUtils.validPermutation(perm);
            for (int i = 0; i < perm.length; i++) {
                assert outBig[i].equals(inpBig[perm[i]]);
            }
            if (needStable) {
                for (int i = 1; i < perm.length; i++) {
                    assert outBig[i].compareTo(outBig[i - 1]) != 0 || perm[i] > perm[i - 1];
                }
            }
            // verify payload
            if(payloads != null){
                for (int i = 0; i < perm.length; i++) {
                    assert outPayload[i].equals(inpPayload[perm[i]]);
                }
            }
        }else if (payloads != null){
            // verify payload
            HashMap<BigInteger, List<BigInteger>> map = new HashMap<>();
            for (int i = 0; i < inpBig.length; i++) {
                if(map.containsKey(inpBig[i])){
                    map.get(inpBig[i]).add(inpPayload[i]);
                }else{
                    List<BigInteger> list = new ArrayList<>();
                    list.add(inpPayload[i]);
                    map.put(inpBig[i], list);
                }
            }
            for (int i = 0; i < inpBig.length; i++) {
                assert map.get(outBig[i]).contains(outPayload[i]);
            }
        }
    }
}
