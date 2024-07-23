package edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm.zl;

import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm.ZlMatCrossTermConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm.ZlMatCrossTermFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm.ZlMatCrossTermParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm.rrgg21.Rrgg21ZlMatCrossTermConfig;
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

/**
 * Zl Matrix Cross Term Multiplication protocol test.
 *
 * @author Liqiang Peng
 * @date 2024/6/12
 */
@RunWith(Parameterized.class)
public class ZlMatCrossTermTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlMatCrossTermTest.class);
    /**
     * default d1
     */
    private static final int DEFAULT_D_1 = 2;
    /**
     * default d2
     */
    private static final int DEFAULT_D_2 = 3;
    /**
     * default d3
     */
    private static final int DEFAULT_D_3 = 2;
    /**
     * default m
     */
    private static final int DEFAULT_M = 10;
    /**
     * default n
     */
    private static final int DEFAULT_N = 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RRGG21 (SIRNN)
        configurations.add(new Object[]{
            ZlMatCrossTermFactory.ZlMatCrossTermType.RRGG21.name(),
            new Rrgg21ZlMatCrossTermConfig.Builder(SecurityModel.SEMI_HONEST, false).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final ZlMatCrossTermConfig config;

    public ZlMatCrossTermTest(String name, ZlMatCrossTermConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_M, DEFAULT_N, DEFAULT_D_1, DEFAULT_D_2, DEFAULT_D_3, false);
    }

    @Test
    public void testSpecialBitLength() {
        testPto(7, 9, DEFAULT_D_1, DEFAULT_D_2, DEFAULT_D_3, false);
    }

    @Test
    public void testSpecialBitLengthSpecialDimension() {
        testPto(7, 9, 5, 7, 9, false);
    }

    @Test
    public void testSpecialDimension() {
        testPto(DEFAULT_M, DEFAULT_N, 5, 1, 9, false);
    }

    @Test
    public void testDefaultParallel() {
        testPto(DEFAULT_M, DEFAULT_N, DEFAULT_D_1, DEFAULT_D_2, DEFAULT_D_3, true);
    }


    private void testPto(int m, int n, int d1, int d2, int d3, boolean parallel) {
        Z2cConfig z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, false);
        Z2cParty z2cSender = Z2cFactory.createSender(firstRpc, secondRpc.ownParty(), z2cConfig);
        Z2cParty z2cReceiver = Z2cFactory.createReceiver(secondRpc, firstRpc.ownParty(), z2cConfig);
        // create instances
        ZlMatCrossTermParty sender = ZlMatCrossTermFactory.createSender(z2cSender, secondRpc.ownParty(), config);
        ZlMatCrossTermParty receiver = ZlMatCrossTermFactory.createReceiver(z2cReceiver, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate x
        SquareZlVector xVector = SquareZlVector.createRandom(
            ZlFactory.createInstance(EnvType.STANDARD, m), d1 * d2, SECURE_RANDOM
        );
        // generate y
        SquareZlVector temp = SquareZlVector.createRandom(
            ZlFactory.createInstance(EnvType.STANDARD, n - BigInteger.valueOf(d2).bitLength()), d2 * d3, SECURE_RANDOM
        );
        SquareZlVector yVector = SquareZlVector.create(
            ZlFactory.createInstance(EnvType.STANDARD, n), temp.getZlVector().getElements(), true);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlMatCrossTermPartyThread senderThread = new ZlMatCrossTermPartyThread(sender, z2cSender, xVector, m, n, d1, d2, d3);
            ZlMatCrossTermPartyThread receiverThread = new ZlMatCrossTermPartyThread(receiver, z2cReceiver, yVector, m, n, d1, d2, d3);
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
            verifyResult(actualResult, xVector.getZlVector(), yVector.getZlVector(), d1, d2, d3, m + n);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void verifyResult(ZlVector actualResult, ZlVector xVector, ZlVector yVector, int d1, int d2, int d3, int l) {
        Assert.assertEquals(actualResult.getNum(), d1 * d3);
        Assert.assertEquals(actualResult.getZl().getL(), l);
        Zl zl = actualResult.getZl();
        BigInteger expectResult;
        for (int i = 0; i < d1; i++) {
            for (int j = 0; j < d3; j++) {
                expectResult = BigInteger.ZERO;
                for (int k = 0; k < d2; k++) {
                    BigInteger t = zl.mul(xVector.getElement(i * d2 + k), yVector.getElement(k * d3 + j));
                    expectResult = zl.add(expectResult, t);
                }
                Assert.assertEquals(expectResult, actualResult.getElement(i * d3 + j));
            }
        }

    }
}