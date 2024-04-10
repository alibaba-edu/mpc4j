package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory.ZlMuxType;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.rrg21.Rrg21ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.rrk20.Rrk20ZlMuxConfig;
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

/**
 * Zl mux test.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
@RunWith(Parameterized.class)
public class ZlMuxTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlMuxTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 18;
    /**
     * small Zl
     */
    private static final Zl SMALL_ZL = ZlFactory.createInstance(EnvType.STANDARD, 1);
    /**
     * default Zl
     */
    private static final Zl DEFAULT_ZL = ZlFactory.createInstance(EnvType.STANDARD, Integer.SIZE);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RRG+21
        configurations.add(new Object[]{
            ZlMuxType.RRG21.name(), new Rrg21ZlMuxConfig.Builder().build()
        });
        // RRK+20
        configurations.add(new Object[]{
            ZlMuxType.RRK20.name(), new Rrk20ZlMuxConfig.Builder().build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final ZlMuxConfig config;

    public ZlMuxTest(String name, ZlMuxConfig config) {
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
        BitVector x0 = BitVectorFactory.createRandom(num, SECURE_RANDOM);
        BitVector x1 = BitVectorFactory.createRandom(num, SECURE_RANDOM);
        SquareZ2Vector shareX0 = SquareZ2Vector.create(x0, false);
        SquareZ2Vector shareX1 = SquareZ2Vector.create(x1, false);
        ZlVector y0 = ZlVector.createRandom(zl, num, SECURE_RANDOM);
        ZlVector y1 = ZlVector.createRandom(zl, num, SECURE_RANDOM);
        SquareZlVector shareY0 = SquareZlVector.create(y0, false);
        SquareZlVector shareY1 = SquareZlVector.create(y1, false);
        // init the protocol
        ZlMuxParty sender = ZlMuxFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        ZlMuxParty receiver = ZlMuxFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlMuxSenderThread senderThread = new ZlMuxSenderThread(sender, shareX0, shareY0);
            ZlMuxReceiverThread receiverThread = new ZlMuxReceiverThread(receiver, shareX1, shareY1);
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
            SquareZlVector shareZ0 = senderThread.getShareZ0();
            SquareZlVector shareZ1 = receiverThread.getShareZ1();
            assertOutput(x0, x1, y0, y1, shareZ0, shareZ1);
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(BitVector x0, BitVector x1, ZlVector y0, ZlVector y1,
                              SquareZlVector shareZ0, SquareZlVector shareZ1) {
        int num = x0.bitNum();
        Assert.assertEquals(num, shareZ0.getNum());
        Assert.assertEquals(num, shareZ1.getNum());
        Zl zl = y0.getZl();
        BitVector x = x0.xor(x1);
        ZlVector y = y0.add(y1);
        ZlVector z = shareZ0.getZlVector().add(shareZ1.getZlVector());
        for (int index = 0; index < num; index++) {
            boolean xi = x.get(index);
            if (!xi) {
                // xi = 0
                Assert.assertEquals(zl.createZero(), z.getElement(index));
            } else {
                // x1 = 1
                Assert.assertEquals(y.getElement(index), z.getElement(index));
            }
        }
    }
}
