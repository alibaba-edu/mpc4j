package edu.alibaba.mpc4j.s2pc.opf.psm.pdsm;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.PdsmFactory.PdsmType;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.cgs22.Cgs22NaivePdsmConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.cgs22.Cgs22OpprfPdsmConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * private (distinct) set membership test.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
@RunWith(Parameterized.class)
public class PdsmTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdsmTest.class);
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

        // CGS22_OPPRF
        configurations.add(new Object[]{
            PdsmType.CGS22_OPPRF.name() + " (" + SecurityModel.SEMI_HONEST.name() + ")",
            new Cgs22OpprfPdsmConfig.Builder(SecurityModel.SEMI_HONEST, false).build()
        });
        // CGS22_LNOT
        configurations.add(new Object[]{
            PdsmType.CGS22_NAIVE.name() + " (" + SecurityModel.SEMI_HONEST.name() + ")",
            new Cgs22NaivePdsmConfig.Builder(SecurityModel.SEMI_HONEST, false).build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final PdsmConfig config;

    public PdsmTest(String name, PdsmConfig config) {
        super(name);
        this.config = config;
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
        byte[][][] senderInputArrays = PdsmTestUtils.genSenderInputArrays(l, d, num, SECURE_RANDOM);
        byte[][] receiverInputArray = PdsmTestUtils.genReceiverInputArray(l, d, senderInputArrays, SECURE_RANDOM);
        // init the protocol
        PdsmSender sender = PdsmFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        PdsmReceiver receiver = PdsmFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            PdsmSenderThread senderThread = new PdsmSenderThread(sender, l, d, senderInputArrays);
            PdsmReceiverThread receiverThread = new PdsmReceiverThread(receiver, l, d, receiverInputArray);
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
            SquareZ2Vector z0 = senderThread.getZ0();
            SquareZ2Vector z1 = receiverThread.getZ1();
            BitVector z = z0.getBitVector().xor(z1.getBitVector());
            // verify
            assertOutput(num, senderInputArrays, receiverInputArray, z);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
