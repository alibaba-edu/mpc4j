package edu.alibaba.mpc4j.s2pc.aby.circuit.mux.zl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.ac.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.circuit.mux.zl.ZlMuxFactory.ZlMuxType;
import edu.alibaba.mpc4j.s2pc.aby.circuit.mux.zl.rrg21.Rrg21ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.circuit.mux.zl.rrk20.Rrk20ZlMuxConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
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
public class ZlMuxTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlMuxTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
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
     * the sender RPC
     */
    private final Rpc senderRpc;
    /**
     * the receiver RPC
     */
    private final Rpc receiverRpc;
    /**
     * the config
     */
    private final ZlMuxConfig config;

    public ZlMuxTest(String name, ZlMuxConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Before
    public void connect() {
        senderRpc.connect();
        receiverRpc.connect();
    }

    @After
    public void disconnect() {
        senderRpc.disconnect();
        receiverRpc.disconnect();
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
        testPto(DEFAULT_ZL, LARGE_NUM, false);
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
        ZlMuxParty sender = ZlMuxFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZlMuxParty receiver = ZlMuxFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
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
            long senderByteLength = senderRpc.getSendByteLength();
            long receiverByteLength = receiverRpc.getSendByteLength();
            senderRpc.reset();
            receiverRpc.reset();
            SquareZlVector shareZ0 = senderThread.getShareZ0();
            SquareZlVector shareZ1 = receiverThread.getShareZ1();
            // verify
            assertOutput(x0, x1, y0, y1, shareZ0, shareZ1);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sender.destroy();
        receiver.destroy();
    }

    private void assertOutput(BitVector x0, BitVector x1, ZlVector y0, ZlVector y1,
                              SquareZlVector shareZ0, SquareZlVector shareZ1) {
        int num = x0.bitNum();
        Assert.assertEquals(num, shareZ0.getNum());
        Assert.assertEquals(num, shareZ1.getNum());
        Zl zl = y0.getZl();
        BitVector x = x0.xor(x1);
        ZlVector y = y0.add(y1);
        ZlVector z = shareZ0.add(shareZ1, true).getVector();
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
