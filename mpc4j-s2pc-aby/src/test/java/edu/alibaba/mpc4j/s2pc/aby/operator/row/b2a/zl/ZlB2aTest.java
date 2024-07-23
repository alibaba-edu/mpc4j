package edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl;

import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.rrkc20.Rrkc20ZlB2aConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Zl boolean to arithmetic protocol test.
 *
 * @author Liqiang Peng
 * @date 2024/6/4
 */
@RunWith(Parameterized.class)
public class ZlB2aTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlB2aTest.class);
    /**
     * default number of bits
     */
    private static final int DEFAULT_BIT_NUM = 1001;
    /**
     * large number of bits
     */
    private static final int LARGE_BIT_NUM = (1 << 18) - 1;
    /**
     * zl
     */
    private static final Zl zl = ZlFactory.createInstance(EnvType.STANDARD, 32);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RRKC20 (CryptFlow2)
        configurations.add(new Object[]{
            ZlB2aFactory.ZlB2aType.RRKC20 + " (" + SecurityModel.SEMI_HONEST + ")",
            new Rrkc20ZlB2aConfig.Builder(SecurityModel.SEMI_HONEST, false).build()
        });
        // RRKC20 (CryptFlow2 + silent OT)
        configurations.add(new Object[]{
            ZlB2aFactory.ZlB2aType.RRKC20 + " (" + SecurityModel.SEMI_HONEST + " + silent OT)",
            new Rrkc20ZlB2aConfig.Builder(SecurityModel.SEMI_HONEST, true).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final ZlB2aConfig config;

    public ZlB2aTest(String name, ZlB2aConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1BitNum() {
        testPto(1, false);
    }

    @Test
    public void test2BitNum() {
        testPto(2, false);
    }

    @Test
    public void test8BitNum() {
        testPto(8, false);
    }

    @Test
    public void test15BitNum() {
        testPto(15, false);
    }

    @Test
    public void testDefaultBitNum() {
        testPto(DEFAULT_BIT_NUM, false);
    }

    @Test
    public void testParallelDefaultBitNum() {
        testPto(DEFAULT_BIT_NUM, true);
    }

    @Test
    public void testLargeBitNum() {
        testPto(LARGE_BIT_NUM, false);
    }

    @Test
    public void testParallelLargeBitNum() {
        testPto(LARGE_BIT_NUM, true);
    }

    private void testPto(int bitNum, boolean parallel) {
        ZlB2aParty sender = ZlB2aFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        ZlB2aParty receiver = ZlB2aFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate x
        BitVector xVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        // generate y
        BitVector yVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlB2aPartyThread senderThread = new ZlB2aPartyThread(sender, zl, SquareZ2Vector.create(xVector, false));
            ZlB2aPartyThread receiverThread = new ZlB2aPartyThread(receiver, zl, SquareZ2Vector.create(yVector, false));
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
            MpcZlVector z0 = senderThread.getZi();
            MpcZlVector z1 = receiverThread.getZi();
            BitVector expectValue = xVector.xor(yVector);
            IntStream.range(0, bitNum).forEach(i -> {
                int actualValue = zl.add(z0.getZlVector().getElement(i), z1.getZlVector().getElement(i)).intValueExact();
                Assert.assertEquals(actualValue, expectValue.get(i) ? 1 : 0);
            });
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}