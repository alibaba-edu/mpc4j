package edu.alibaba.mpc4j.s2pc.opf.psm;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmFactory.PsmType;
import edu.alibaba.mpc4j.s2pc.opf.psm.cgs22.Cgs22LnotPsmConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.cgs22.Cgs22OpprfPsmConfig;
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
 * private set membership test.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
@RunWith(Parameterized.class)
public class PsmTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsmTest.class);
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
            PsmType.CGS22_OPPRF.name() + " (" + SecurityModel.SEMI_HONEST.name() + ")",
            new Cgs22OpprfPsmConfig.Builder(SecurityModel.SEMI_HONEST, false).build()
        });
        // CGS22_LNOT
        configurations.add(new Object[]{
            PsmType.CGS22_LNOT.name() + " (" + SecurityModel.SEMI_HONEST.name() + ")",
            new Cgs22LnotPsmConfig.Builder(SecurityModel.SEMI_HONEST, false).build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final PsmConfig config;

    public PsmTest(String name, PsmConfig config) {
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
        byte[][][] senderInputArrays = PsmTestUtils.genSenderInputArrays(l, d, num, SECURE_RANDOM);
        byte[][] receiverInputArray = PsmTestUtils.genReceiverInputArray(l, d, senderInputArrays, SECURE_RANDOM);
        // init the protocol
        PsmSender sender = PsmFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        PsmReceiver receiver = PsmFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            PsmSenderThread senderThread = new PsmSenderThread(sender, l, d, senderInputArrays);
            PsmReceiverThread receiverThread = new PsmReceiverThread(receiver, l, d, receiverInputArray);
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
