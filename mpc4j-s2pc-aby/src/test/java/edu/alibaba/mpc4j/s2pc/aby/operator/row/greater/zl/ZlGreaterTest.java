package edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.rrk20.Rrk20ZlGreaterConfig;
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
 * Zl Greater Test.
 *
 * @author Li Peng
 * @date 2023/5/24
 */
@RunWith(Parameterized.class)
public class ZlGreaterTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlGreaterTest.class);
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
    private static final Zl SMALL_ZL = ZlFactory.createInstance(EnvType.STANDARD, 3);
    /**
     * default Zl
     */
    private static final Zl DEFAULT_ZL = ZlFactory.createInstance(EnvType.STANDARD, Integer.SIZE);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RRK+20, default zl
        configurations.add(new Object[]{
            ZlGreaterFactory.ZlGreaterType.RRK20.name() + " (l = " + DEFAULT_ZL.getL() + ")",
            new Rrk20ZlGreaterConfig.Builder(DEFAULT_ZL).build()
        });
        // RRK+20, small zl
        configurations.add(new Object[]{
            ZlGreaterFactory.ZlGreaterType.RRK20.name() + " (l = " + SMALL_ZL.getL() + ")",
            new Rrk20ZlGreaterConfig.Builder(SMALL_ZL).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final ZlGreaterConfig config;
    /**
     * Zl instance
     */
    private final Zl zl;

    public ZlGreaterTest(String name, ZlGreaterConfig config) {
        super(name);
        this.config = config;
        zl = config.getZl();
    }

    @Test
    public void test1Num() {
        testPto(1, false);
    }

    @Test
    public void test2Num() {
        testPto(2, false);
    }

    @Test
    public void test8Num() {
        testPto(8, false);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_NUM, true);
    }

    @Test
    public void testLargeNum() {
        testPto(LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(LARGE_NUM, true);
    }

    private void testPto(int num, boolean parallel) {
        // make sure bit length of zl > 2
        Assert.assertTrue(zl.getL() > 2);
        // create inputs, making sure that the plain value is positive under 2' complement notation in zl.
        BigInteger[] randomsX0 = IntStream.range(0, num)
            .mapToObj(i -> new BigInteger(zl.getL() - 2, SECURE_RANDOM)).toArray(BigInteger[]::new);
        BigInteger[] randomsX1 = IntStream.range(0, num)
            .mapToObj(i -> new BigInteger(zl.getL() - 2, SECURE_RANDOM)).toArray(BigInteger[]::new);
        BigInteger[] randomsY0 = IntStream.range(0, num)
            .mapToObj(i -> new BigInteger(zl.getL() - 2, SECURE_RANDOM)).toArray(BigInteger[]::new);
        BigInteger[] randomsY1 = IntStream.range(0, num)
            .mapToObj(i -> new BigInteger(zl.getL() - 2, SECURE_RANDOM)).toArray(BigInteger[]::new);
        ZlVector x0 = ZlVector.create(zl, randomsX0);
        ZlVector x1 = ZlVector.create(zl, randomsX1);
        ZlVector y0 = ZlVector.create(zl, randomsY0);
        ZlVector y1 = ZlVector.create(zl, randomsY1);
        SquareZlVector shareX0 = SquareZlVector.create(x0, false);
        SquareZlVector shareX1 = SquareZlVector.create(x1, false);
        SquareZlVector shareY0 = SquareZlVector.create(y0, false);
        SquareZlVector shareY1 = SquareZlVector.create(y1, false);
        // init the protocol
        ZlGreaterParty sender = ZlGreaterFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        ZlGreaterParty receiver = ZlGreaterFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlGreaterPartyThread senderThread = new ZlGreaterPartyThread(sender, shareX0, shareY0);
            ZlGreaterPartyThread receiverThread = new ZlGreaterPartyThread(receiver, shareX1, shareY1);
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

    private void assertOutput(ZlVector x0, ZlVector x1, ZlVector y0, ZlVector y1,
                              SquareZlVector shareZ0, SquareZlVector shareZ1) {
        int num = x0.getNum();
        Assert.assertEquals(num, shareZ0.getNum());
        Assert.assertEquals(num, shareZ1.getNum());
        ZlVector x = x0.add(x1);
        ZlVector y = y0.add(y1);
        ZlVector z = shareZ0.getZlVector().add(shareZ1.getZlVector());
        for (int index = 0; index < num; index++) {
            boolean xi = x.getElement(index).compareTo(y.getElement(index)) > 0;
            if (xi) {
                // x > y
                Assert.assertEquals(z.getElement(index), x.getElement(index));
            } else {
                // x <= y
                Assert.assertEquals(z.getElement(index), y.getElement(index));
            }
        }
    }
}
