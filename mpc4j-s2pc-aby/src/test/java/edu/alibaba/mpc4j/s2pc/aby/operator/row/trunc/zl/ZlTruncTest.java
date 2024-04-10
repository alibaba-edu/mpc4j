package edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.gp23.Gp23ZlTruncConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.rrk20.Rrk20ZlTruncConfig;
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
 * Zl Truncation Test
 *
 * @author Liqiang Peng
 * @date 2023/10/2
 */
@RunWith(Parameterized.class)
public class ZlTruncTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlTruncTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 10000;
    /**
     * default Zl
     */
    private static final Zl DEFAULT_ZL = ZlFactory.createInstance(EnvType.STANDARD, Double.SIZE);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RRK+20 + silent OT
        configurations.add(new Object[]{
            ZlTruncFactory.ZlTruncType.RRK20.name() + " silent OT",
            new Rrk20ZlTruncConfig.Builder(true).build()
        });
        // GP23 + silent OT
        configurations.add(new Object[]{
            ZlTruncFactory.ZlTruncType.GP23.name() + " silent OT",
            new Gp23ZlTruncConfig.Builder(true).build()
        });
        // RRK+20
        configurations.add(new Object[]{
            ZlTruncFactory.ZlTruncType.RRK20.name(),
            new Rrk20ZlTruncConfig.Builder(false).build()
        });
        // GP23
        configurations.add(new Object[]{
            ZlTruncFactory.ZlTruncType.GP23.name(),
            new Gp23ZlTruncConfig.Builder(false).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final ZlTruncConfig config;

    public ZlTruncTest(String name, ZlTruncConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testDefaultNumShift1() {
        testPto(1, false);
    }

    @Test
    public void testDefaultNumShift3() {
        testPto(3, false);
    }

    @Test
    public void testDefaultNumShift4() {
        testPto(4, false);
    }

    @Test
    public void testParallelDefaultNumShift1() {
        testPto(1, true);
    }

    @Test
    public void testParallelDefaultNumShift3() {
        testPto(3, true);
    }

    @Test
    public void testParallelDefaultNumShift4() {
        testPto(4, true);
    }

    private void testPto(int s, boolean parallel) {
        // create inputs
        BigInteger n = ZlTruncTest.DEFAULT_ZL.getRangeBound();
        BigInteger bound = n.divide(BigInteger.valueOf(3));
        BigInteger[] x = new BigInteger[ZlTruncTest.DEFAULT_NUM];
        BigInteger[] x0 = new BigInteger[ZlTruncTest.DEFAULT_NUM];
        BigInteger[] x1 = new BigInteger[ZlTruncTest.DEFAULT_NUM];
        for (int i = 0; i < ZlTruncTest.DEFAULT_NUM; i++) {
            do {
                x[i] = new BigInteger(ZlTruncTest.DEFAULT_ZL.getL(), SECURE_RANDOM);
                x1[i] = new BigInteger(ZlTruncTest.DEFAULT_ZL.getL(), SECURE_RANDOM);
                x0[i] = new BigInteger(ZlTruncTest.DEFAULT_ZL.getL(), SECURE_RANDOM);
                x[i] = x0[i].add(x1[i]).mod(n);
            } while(x[i].compareTo(bound) >= 0 && x[i].subtract(n).abs().compareTo(bound) >= 0);
        }
        ZlVector x0Vector = ZlVector.create(ZlTruncTest.DEFAULT_ZL, x0);
        ZlVector x1Vector = ZlVector.create(ZlTruncTest.DEFAULT_ZL, x1);
        SquareZlVector shareX0 = SquareZlVector.create(x0Vector, false);
        SquareZlVector shareX1 = SquareZlVector.create(x1Vector, false);
        // init the protocol
        ZlTruncParty sender = ZlTruncFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        ZlTruncParty receiver = ZlTruncFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlTruncPartyThread senderThread = new ZlTruncPartyThread(sender, ZlTruncTest.DEFAULT_ZL.getL(), shareX0, s);
            ZlTruncPartyThread receiverThread = new ZlTruncPartyThread(receiver, ZlTruncTest.DEFAULT_ZL.getL(), shareX1, s);
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
            assertOutput(x0Vector, x1Vector, shareZ0, shareZ1, s);
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(ZlVector x0, ZlVector x1, SquareZlVector shareZ0, SquareZlVector shareZ1, int s) {
        int num = x0.getNum();
        Assert.assertEquals(num, shareZ0.getNum());
        Assert.assertEquals(num, shareZ1.getNum());
        ZlVector x = x0.add(x1);
        BigInteger[] xShift = rDiv(x.getElements(), ZlTruncTest.DEFAULT_ZL.getRangeBound(), s);
        ZlVector z = shareZ0.getZlVector().add(shareZ1.getZlVector());
        for (int index = 0; index < num; index++) {
            BigInteger a0 = x0.getElement(index).mod(BigInteger.ONE.shiftLeft(s));
            BigInteger a1 = x1.getElement(index).mod(BigInteger.ONE.shiftLeft(s));
            BigInteger error;
            if (a0.add(a1).compareTo(BigInteger.ONE.shiftLeft(s)) < 0) {
                error = BigInteger.ZERO;
            } else {
                error = BigInteger.ONE;
            }
            BigInteger r1 = z.getElement(index);
            BigInteger r2 = xShift[index].subtract(error).mod(ZlTruncTest.DEFAULT_ZL.getRangeBound());
            Assert.assertEquals(r1, r2);
        }
    }

    private BigInteger[] rDiv(BigInteger[] input, BigInteger n, int d) {
        int num = input.length;
        BigInteger nHalf = n.shiftRight(1);
        IntStream intStream = IntStream.range(0, num);
        return intStream.mapToObj(index -> {
            if (input[index].compareTo(nHalf) < 0) {
                return input[index].shiftRight(d).mod(n);
            } else {
                return input[index].subtract(n).shiftRight(d).mod(n);
            }
        }).toArray(BigInteger[]::new);
    }
}
