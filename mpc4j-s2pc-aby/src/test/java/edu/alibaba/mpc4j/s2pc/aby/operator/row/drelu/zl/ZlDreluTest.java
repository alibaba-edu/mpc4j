package edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.rrk20.Rrk20ZlDreluConfig;
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
 * Zl DReLU Test
 *
 * @author Li Peng
 * @date 2023/5/23
 */
@RunWith(Parameterized.class)
public class ZlDreluTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlDreluTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;
    /**
     * small Zl
     */
    private static final Zl SMALL_ZL = ZlFactory.createInstance(EnvType.STANDARD, 1);
    /**
     * default Zl
     */
    private static final Zl DEFAULT_ZL = ZlFactory.createInstance(EnvType.STANDARD, Double.SIZE);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RRK+20
        configurations.add(new Object[]{
            ZlDreluFactory.ZlDreluType.RRK20.name(),
            new Rrk20ZlDreluConfig.Builder(false).build()
        });

        configurations.add(new Object[]{
            ZlDreluFactory.ZlDreluType.RRK20.name() + " silent OT",
            new Rrk20ZlDreluConfig.Builder(true).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final ZlDreluConfig config;

    public ZlDreluTest(String name, ZlDreluConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1Num() {
        testPto(DEFAULT_ZL, 1, false);
    }

    @Test
    public void test2Num() {
        testPto(DEFAULT_ZL, 2, false);
    }

    @Test
    public void test8Num() {
        testPto(DEFAULT_ZL, 8, false);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_ZL, DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_ZL, DEFAULT_NUM, true);
    }

    @Test
    public void testSmallZl() {
        testPto(SMALL_ZL, DEFAULT_NUM, false);
    }

    @Test
    public void testLargeNum() {
        testPto(DEFAULT_ZL, LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(DEFAULT_ZL, LARGE_NUM, true);
    }

    private void testPto(Zl zl, int num, boolean parallel) {
        // create inputs
        ZlVector x0 = ZlVector.createRandom(zl, num, SECURE_RANDOM);
        ZlVector x1 = ZlVector.createRandom(zl, num, SECURE_RANDOM);
        SquareZlVector shareX0 = SquareZlVector.create(x0, false);
        SquareZlVector shareX1 = SquareZlVector.create(x1, false);
        // init the protocol
        ZlDreluParty sender = ZlDreluFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        ZlDreluParty receiver = ZlDreluFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlDreluPartyThread senderThread = new ZlDreluPartyThread(sender, zl.getL(), shareX0);
            ZlDreluPartyThread receiverThread = new ZlDreluPartyThread(receiver, zl.getL(), shareX1);
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
            SquareZ2Vector shareZ0 = senderThread.getShareZ();
            SquareZ2Vector shareZ1 = receiverThread.getShareZ();
            assertOutput(x0, x1, shareZ0, shareZ1);
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(ZlVector x0, ZlVector x1, SquareZ2Vector shareZ0, SquareZ2Vector shareZ1) {
        int num = x0.getNum();
        int l = x0.getZl().getL();
        Assert.assertEquals(num, shareZ0.getNum());
        Assert.assertEquals(num, shareZ1.getNum());
        ZlVector x = x0.add(x1);
        BitVector z = shareZ0.getBitVector().xor(shareZ1.getBitVector());
        for (int index = 0; index < num; index++) {
            // >= 0
            boolean xi = x.getElement(index).compareTo(BigInteger.ONE.shiftLeft(l - 1)) < 0;
            Assert.assertEquals(xi, z.get(index));
        }
    }
}
