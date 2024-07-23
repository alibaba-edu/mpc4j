package edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl;

import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.rrgg21.Rrgg21ZlCrossTermConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Zl Cross Term Multiplication protocol test.
 *
 * @author Liqiang Peng
 * @date 2024/6/5
 */
@RunWith(Parameterized.class)
public class ZlCrossTermTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlCrossTermTest.class);
    /**
     * default number
     */
    private static final int DEFAULT_NUM = 10;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RRGG21 (SIRNN)
        configurations.add(new Object[]{
            ZlCrossTermFactory.ZlCrossTermType.RRGG21.name(),
            new Rrgg21ZlCrossTermConfig.Builder(SecurityModel.SEMI_HONEST, false).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final ZlCrossTermConfig config;

    public ZlCrossTermTest(String name, ZlCrossTermConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testM1N8() {
        testPto(1, 8, false);
    }

    @Test
    public void testM1N16() {
        testPto(1, 16, false);
    }

    @Test
    public void testM1N32() {
        testPto(1, 32, false);
    }

    @Test
    public void testM2N8() {
        testPto(2, 8, false);
    }

    @Test
    public void testM2N16() {
        testPto(2, 16, false);
    }

    @Test
    public void testM2N16Parallel() {
        testPto(2, 16, true);
    }

    @Test
    public void testM5N7() {
        testPto(5, 7, false);
    }

    private void testPto(int m, int n, boolean parallel) {
        Z2cConfig z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, false);
        Z2cParty z2cSender = Z2cFactory.createSender(firstRpc, secondRpc.ownParty(), z2cConfig);
        Z2cParty z2cReceiver = Z2cFactory.createReceiver(secondRpc, firstRpc.ownParty(), z2cConfig);
        // create instances
        ZlCrossTermParty sender = ZlCrossTermFactory.createSender(z2cSender, secondRpc.ownParty(), config);
        ZlCrossTermParty receiver = ZlCrossTermFactory.createReceiver(z2cReceiver, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate x
        SquareZlVector xVector = SquareZlVector.createRandom(
            ZlFactory.createInstance(EnvType.STANDARD, m), DEFAULT_NUM, SECURE_RANDOM
        );
        // generate y
        SquareZlVector yVector = SquareZlVector.createRandom(
            ZlFactory.createInstance(EnvType.STANDARD, n), DEFAULT_NUM, SECURE_RANDOM
        );
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlCrossTermPartyThread senderThread = new ZlCrossTermPartyThread(sender, z2cSender, xVector, m, n);
            ZlCrossTermPartyThread receiverThread = new ZlCrossTermPartyThread(receiver, z2cReceiver, yVector, m, n);
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
            ZlVector actualResult = z0.getZlVector().add(z1.getZlVector());
            IntStream.range(0, DEFAULT_NUM).forEach(i -> {
                BigInteger expectValue = xVector.getZlVector().getElement(i)
                    .multiply(yVector.getZlVector().getElement(i))
                    .mod(BigInteger.ONE.shiftLeft(m + n));
                Assert.assertEquals(actualResult.getElement(i), expectValue);
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