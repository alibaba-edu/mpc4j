package edu.alibaba.mpc4j.s2pc.opf.shuffle;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory.RosnType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.lll24.Lll24FlatNetRosnConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory.ShuffleType;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.cgp20.Cgp20ShuffleConfig;
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
 * shuffle test
 *
 * @author Feng Han
 * @date 2024/9/27
 */
@RunWith(Parameterized.class)
public class ShuffleTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShuffleTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;
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
    private static final int LARGE_L = 197;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CGP20 + LL24_NET
        configurations.add(new Object[]{
            ShuffleType.CGP20.name() + " (" + SecurityModel.SEMI_HONEST.name() + "," + RosnType.LLL24_FLAT_NET + ")",
            new Cgp20ShuffleConfig.Builder(false).setRosnConfig(new Lll24FlatNetRosnConfig.Builder(false).build()).build()
        });

        // CGP20 + LLL24_MATRIX
        configurations.add(new Object[]{
            ShuffleType.CGP20.name() + " (" + SecurityModel.SEMI_HONEST.name() + "," + RosnType.LLL24_CST + ")",
            new Cgp20ShuffleConfig.Builder(false).setRosnConfig(new Lll24FlatNetRosnConfig.Builder(false).build()).build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final ShuffleConfig config;

    public ShuffleTest(String name, ShuffleConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test2Num() {
        testPto(2, DEFAULT_L, true, false);
    }

    @Test
    public void test7Num() {
        testPto(7, SMALL_L, true, false);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_NUM, DEFAULT_L, true, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_NUM, DEFAULT_L, false, true);
    }

    @Test
    public void testLargeNumLargeDim() {
        testPto(LARGE_NUM, LARGE_L, false, false);
    }

    @Test
    public void testParallelLargeDim() {
        testPto(DEFAULT_NUM, LARGE_L, true, true);
    }

    private void testPto(int dataNum, int dimNum, boolean shareData, boolean parallel) {
        // create inputs
        BitVector[] inputs = IntStream.range(0, dimNum).mapToObj(i -> BitVectorFactory.createRandom(dataNum, SECURE_RANDOM)).toArray(BitVector[]::new);
        MpcZ2Vector[] senderInput, receiverInput;
        if (shareData) {
            BitVector[] senderPlain = IntStream.range(0, dimNum).mapToObj(i -> BitVectorFactory.createRandom(dataNum, SECURE_RANDOM)).toArray(BitVector[]::new);
            senderInput = Arrays.stream(senderPlain).map(ea -> SquareZ2Vector.create(ea, false)).toArray(MpcZ2Vector[]::new);
            receiverInput = IntStream.range(0, dimNum).mapToObj(i -> SquareZ2Vector.create(inputs[i].xor(senderPlain[i]), false)).toArray(MpcZ2Vector[]::new);
        } else {
            senderInput = Arrays.stream(inputs).map(PlainZ2Vector::create).toArray(PlainZ2Vector[]::new);
            receiverInput = null;
        }
        // init the protocol
        ShuffleParty sender = ShuffleFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        ShuffleParty receiver = ShuffleFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ShuffleSenderThread senderThread = new ShuffleSenderThread(sender, dataNum, dimNum, senderInput);
            ShuffleReceiverThread receiverThread = new ShuffleReceiverThread(receiver, dataNum, dimNum, receiverInput);
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
            SquareZ2Vector[] z0 = senderThread.getRes();
            SquareZ2Vector[] z1 = receiverThread.getRes();
            BitVector[] z = IntStream.range(0, dimNum).mapToObj(i -> z0[i].getBitVector().xor(z1[i].getBitVector())).toArray(BitVector[]::new);
            // verify
            assertOutput(inputs, z);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(BitVector[] inputs, BitVector[] res) {
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(inputs[i].bitNum(), res[i].bitNum());
        }
        BigInteger[] inpBig = ZlDatabase.create(EnvType.STANDARD, true, inputs).getBigIntegerData();
        BigInteger[] outBig = ZlDatabase.create(EnvType.STANDARD, true, res).getBigIntegerData();

        Map<BigInteger, Integer> inputMap = new HashMap<>();
        for (BigInteger i : inpBig) {
            if (inputMap.containsKey(i)) {
                inputMap.put(i, inputMap.get(i) + 1);
            } else {
                inputMap.put(i, 1);
            }
        }
        for (BigInteger i : outBig) {
            assert inputMap.containsKey(i);
            assert inputMap.get(i) >= 1;
            inputMap.put(i, inputMap.get(i) - 1);
        }
    }
}
