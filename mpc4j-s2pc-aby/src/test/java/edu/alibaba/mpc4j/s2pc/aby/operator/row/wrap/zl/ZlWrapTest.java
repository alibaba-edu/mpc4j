package edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl.rrkc20.Rrkc20ZlWrapConfig;
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
 * Zl wrap protocol test.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
@RunWith(Parameterized.class)
public class ZlWrapTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlWrapTest.class);
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
    private static final int DEFAULT_L = 32;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RRKC20 (CryptFlow2)
        configurations.add(new Object[]{
            ZlWrapFactory.ZlWrapType.RRKC20 + " (" + SecurityModel.SEMI_HONEST + ")",
            new Rrkc20ZlWrapConfig.Builder(SecurityModel.SEMI_HONEST, false).build()
        });
        // RRKC20 (CryptFlow2 + silent OT)
        configurations.add(new Object[]{
            ZlWrapFactory.ZlWrapType.RRKC20 + " (" + SecurityModel.SEMI_HONEST + " + silent OT)",
            new Rrkc20ZlWrapConfig.Builder(SecurityModel.SEMI_HONEST, true).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final ZlWrapConfig config;

    public ZlWrapTest(String name, ZlWrapConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1Num() {
        testPto(DEFAULT_L, 1, false);
    }

    @Test
    public void test2Num() {
        testPto(DEFAULT_L, 2, false);
    }

    @Test
    public void test4Num() {
        testPto(DEFAULT_L, 4, false);
    }

    @Test
    public void test8Num() {
        testPto(DEFAULT_L, 8, false);
    }

    @Test
    public void test7Num() {
        testPto(DEFAULT_L, 7, false);
    }

    @Test
    public void test9Num() {
        testPto(DEFAULT_L, 9, false);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_L, DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_L, DEFAULT_NUM, true);
    }

    @Test
    public void test1L() {
        testPto(1, DEFAULT_NUM, false);
    }

    @Test
    public void test7L() {
        testPto(7, DEFAULT_NUM, false);
    }

    @Test
    public void test9L() {
        testPto(9, DEFAULT_NUM, false);
    }

    @Test
    public void test19L() {
        testPto(19, DEFAULT_NUM, false);
    }

    @Test
    public void testLargeNum() {
        testPto(DEFAULT_L, LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(DEFAULT_L, LARGE_NUM, true);
    }

    private void testPto(int l, int num, boolean parallel) {
        // create inputs
        byte[][] xs = genInputArray(l, num);
        byte[][] ys = genInputArray(l, num);
        // init the protocol
        Z2cConfig z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, false);
        Z2cParty z2cSender = Z2cFactory.createSender(firstRpc, secondRpc.ownParty(), z2cConfig);
        Z2cParty z2cReceiver = Z2cFactory.createReceiver(secondRpc, firstRpc.ownParty(), z2cConfig);
        ZlWrapParty sender = ZlWrapFactory.createSender(z2cSender, secondRpc.ownParty(), config);
        ZlWrapParty receiver = ZlWrapFactory.createReceiver(z2cReceiver, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlWrapPartyThread senderThread = new ZlWrapPartyThread(sender, z2cSender, l, xs);
            ZlWrapPartyThread receiverThread = new ZlWrapPartyThread(receiver, z2cReceiver, l, ys);
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
            SquareZ2Vector z0 = senderThread.getZi();
            SquareZ2Vector z1 = receiverThread.getZi();
            BitVector z = z0.getBitVector().xor(z1.getBitVector());
            assertOutput(l, num, xs, ys, z);
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(int l, int num, byte[][] xs, byte[][] ys, BitVector z) {
        BigInteger bound = BigInteger.ONE.shiftLeft(l).subtract(BigInteger.ONE);
        Assert.assertEquals(num, z.bitNum());
        for (int index = 0; index < num; index++) {
            BigInteger x = BigIntegerUtils.byteArrayToNonNegBigInteger(xs[index]);
            BigInteger y = BigIntegerUtils.byteArrayToNonNegBigInteger(ys[index]);
            boolean expectResult = x.add(y).compareTo(bound) > 0;
            Assert.assertEquals(z.get(index), expectResult);
        }
    }

    static byte[][] genInputArray(int l, int num) {
        int byteL = CommonUtils.getByteLength(l);
        return IntStream.range(0, num)
            .parallel()
            .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
            .toArray(byte[][]::new);
    }
}