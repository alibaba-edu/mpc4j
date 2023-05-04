package edu.alibaba.mpc4j.s2pc.opf.psm;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmFactory.PsmType;
import edu.alibaba.mpc4j.s2pc.opf.psm.cgs22.Cgs22LnotPsmConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.cgs22.Cgs22OpprfPsmConfig;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * private set membership test.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
@RunWith(Parameterized.class)
public class PsmTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsmTest.class);
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
    private static final int LARGE_NUM = 1 << 16;
    /**
     * default l
     */
    private static final int DEFAULT_L = 64;
    /**
     * default d
     */
    private static final int DEFAULT_D = 3;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CGS22_OPPRF (direct, semi-honest)
        configurations.add(new Object[]{
            PsmType.CGS22_OPPRF.name() + " (direct, semi-honest)",
            new Cgs22OpprfPsmConfig.Builder(SecurityModel.SEMI_HONEST, false).build()
        });
        // CGS22_OPPRF (silent, semi-honest)
        configurations.add(new Object[]{
            PsmType.CGS22_OPPRF.name() + " (silent, semi-honest)",
            new Cgs22OpprfPsmConfig.Builder(SecurityModel.SEMI_HONEST, true).build()
        });
        // CGS22_LNOT (direct, semi-honest)
        configurations.add(new Object[]{
            PsmType.CGS22_LNOT.name() + " (direct, semi-honest)",
            new Cgs22LnotPsmConfig.Builder(SecurityModel.SEMI_HONEST, false).build()
        });
        // CGS22_LNOT (silent, semi-honest)
        configurations.add(new Object[]{
            PsmType.CGS22_LNOT.name() + " (silent, semi-honest)",
            new Cgs22LnotPsmConfig.Builder(SecurityModel.SEMI_HONEST, true).build()
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
    private final PsmConfig config;

    public PsmTest(String name, PsmConfig config) {
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
    public void test2Num() {
        testPto(DEFAULT_L, DEFAULT_D, 2, false);
    }

    @Test
    public void test8Num() {
        testPto(DEFAULT_L, DEFAULT_D, 8, false);
    }

    @Test
    public void test7Num() {
        testPto(DEFAULT_L, DEFAULT_D, 7, false);
    }

    @Test
    public void test9Num() {
        testPto(DEFAULT_L, DEFAULT_D, 9, false);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_L, DEFAULT_D, DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_L, DEFAULT_D, DEFAULT_NUM, true);
    }

    @Test
    public void testSpecialL() {
        testPto(DEFAULT_L + 5, DEFAULT_D, DEFAULT_NUM, false);
    }

    @Test
    public void test1D() {
        testPto(DEFAULT_L, 1, DEFAULT_NUM, false);
    }

    @Test
    public void test2D() {
        testPto(DEFAULT_L, 2, DEFAULT_NUM, false);
    }

    @Test
    public void testLargeNum() {
        testPto(DEFAULT_L, DEFAULT_D, LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(DEFAULT_L, DEFAULT_D, LARGE_NUM, false);
    }

    private void testPto(int l, int d, int num, boolean parallel) {
        // create inputs
        byte[][][] senderInputArrays = PsmTestUtils.genSenderInputArrays(l, d, num, SECURE_RANDOM);
        byte[][] receiverInputArray = PsmTestUtils.genReceiverInputArray(l, d, senderInputArrays, SECURE_RANDOM);
        // init the protocol
        PsmSender sender = PsmFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        PsmReceiver receiver = PsmFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            PsmSenderThread senderThread = new PsmSenderThread(sender, l, d, senderInputArrays);
            PsmReceiverThread receiverThread = new PsmReceiverThread(receiver, l, d, receiverInputArray);
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
            long senderRound = senderRpc.getSendDataPacketNum();
            long receiverByteLength = receiverRpc.getSendByteLength();
            long receiverRound = receiverRpc.getSendDataPacketNum();
            senderRpc.reset();
            receiverRpc.reset();
            SquareZ2Vector z0 = senderThread.getZ0();
            SquareZ2Vector z1 = receiverThread.getZ1();
            BitVector z = z0.getBitVector().xor(z1.getBitVector());
            // verify
            assertOutput(num, senderInputArrays, receiverInputArray, z);
            LOGGER.info("Sender sends {}B / {} rounds, Receiver sends {}B / {} rounds, time = {}ms",
                senderByteLength, senderRound, receiverByteLength, receiverRound, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sender.destroy();
        receiver.destroy();
    }

    private void assertOutput(int num, byte[][][] senderInputArrays, byte[][] receiverInputArray, BitVector z) {
        Assert.assertEquals(num, z.bitNum());
        for (int index = 0; index < num; index++) {
            boolean equal = false;
            for (int i = 0; i < senderInputArrays[index].length; i++) {
                equal |= Arrays.equals(senderInputArrays[index][i], receiverInputArray[index]);
            }
            if (!equal) {
                // not equal
                Assert.assertFalse(z.get(index));
            } else {
                // equal
                Assert.assertTrue(z.get(index));
            }
        }
    }
}
