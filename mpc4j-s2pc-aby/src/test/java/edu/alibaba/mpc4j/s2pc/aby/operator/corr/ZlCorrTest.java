package edu.alibaba.mpc4j.s2pc.aby.operator.corr;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.ZlCorrConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.ZlCorrFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.ZlCorrParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.gp23.Gp23ZlCorrConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.rrk20.Rrk20ZlCorrConfig;
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
 * Zl Corr Test
 *
 * @author Liqiang Peng
 * @date 2023/10/2
 */
@RunWith(Parameterized.class)
public class ZlCorrTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlCorrTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 10000;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RRK+20
        configurations.add(new Object[]{
            ZlCorrFactory.ZlCorrType.RRK20.name(),
            new Rrk20ZlCorrConfig.Builder(SecurityModel.SEMI_HONEST, true).build()
        });
        // GP23
        configurations.add(new Object[]{
            ZlCorrFactory.ZlCorrType.GP23.name(),
            new Gp23ZlCorrConfig.Builder(SecurityModel.SEMI_HONEST, true).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final ZlCorrConfig config;
    /**
     * Zl
     */
    private final Zl zl;

    public ZlCorrTest(String name, ZlCorrConfig config) {
        super(name);
        this.config = config;
        zl = ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L_FOR_MODULE_N - 1);
    }

    @Test
    public void testParallelDefault() {
        testPto(true);
    }

    @Test
    public void testDefault() {
        testPto(false);
    }

    private void testPto(boolean parallel) {
        // create inputs
        BigInteger n = zl.getRangeBound();
        BigInteger bound = n.divide(BigInteger.valueOf(3));
        BigInteger[] x = new BigInteger[ZlCorrTest.DEFAULT_NUM];
        BigInteger[] x0 = new BigInteger[ZlCorrTest.DEFAULT_NUM];
        BigInteger[] x1 = new BigInteger[ZlCorrTest.DEFAULT_NUM];
        for (int i = 0; i < ZlCorrTest.DEFAULT_NUM; i++) {
            do {
                x[i] = zl.createRandom(SECURE_RANDOM);
                x1[i] = zl.createRandom(SECURE_RANDOM);
                x0[i] = zl.createRandom(SECURE_RANDOM);
                x[i] = zl.add(x0[i], x1[i]);
            } while (x[i].compareTo(bound) >= 0 && x[i].subtract(n).abs().compareTo(bound) >= 0);
        }
        ZlVector x0Vector = ZlVector.create(zl, x0);
        ZlVector x1Vector = ZlVector.create(zl, x1);
        SquareZlVector shareX0 = SquareZlVector.create(x0Vector, false);
        SquareZlVector shareX1 = SquareZlVector.create(x1Vector, false);
        // init z2c
        Z2cConfig z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
        Z2cParty z2cSender = Z2cFactory.createSender(firstRpc, secondRpc.ownParty(), z2cConfig);
        Z2cParty z2cReceiver = Z2cFactory.createReceiver(secondRpc, firstRpc.ownParty(), z2cConfig);
        // init the protocol
        ZlCorrParty sender = ZlCorrFactory.createSender(z2cSender, secondRpc.ownParty(), config);
        ZlCorrParty receiver = ZlCorrFactory.createReceiver(z2cReceiver, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlCorrPartyThread senderThread = new ZlCorrPartyThread(sender, z2cSender, shareX0);
            ZlCorrPartyThread receiverThread = new ZlCorrPartyThread(receiver, z2cReceiver, shareX1);
            StopWatch stopWatch = new StopWatch();
            // execute the protocol
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            SquareZlVector shareZ0 = senderThread.getShareZ();
            SquareZlVector shareZ1 = receiverThread.getShareZ();
            assertOutput(x0Vector, x1Vector, shareZ0, shareZ1);
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(ZlVector x0, ZlVector x1, SquareZlVector shareZ0, SquareZlVector shareZ1) {
        int num = x0.getNum();
        Assert.assertEquals(num, shareZ0.getNum());
        Assert.assertEquals(num, shareZ1.getNum());
        ZlVector shareCorr = shareZ0.getZlVector().add(shareZ1.getZlVector());
        BigInteger n = x0.getZl().getRangeBound();
        BigInteger nPrime = n.shiftRight(1);
        ZlVector x = x0.add(x1);
        for (int index = 0; index < num; index++) {
            BigInteger value = x.getElement(index);
            BigInteger value0 = x0.getElement(index);
            BigInteger value1 = x1.getElement(index);
            BigInteger expectedValue = BigInteger.ZERO;
            if (value.compareTo(nPrime) >= 0 && value0.compareTo(nPrime) < 0 && value1.compareTo(nPrime) < 0) {
                expectedValue = n.subtract(BigInteger.ONE);
            } else if (value.compareTo(nPrime) < 0 && value0.compareTo(nPrime) >= 0 && value1.compareTo(nPrime) >= 0) {
                expectedValue = BigInteger.ONE;
            }
            Assert.assertEquals(shareCorr.getElement(index), expectedValue);
        }
    }
}
