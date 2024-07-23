package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.OsnTestUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory.RosnType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.cgp20.Cgp20CstRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.gmr21.Gmr21NetRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.lll24.Lll24CstRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.lll24.Lll24NetRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.ms13.Ms13NetRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.prrs24.Prrs24OprfRosnConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Factory.Conv32Type;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Random OSN test.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
@RunWith(Parameterized.class)
public class RosnTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(RosnTest.class);
    /**
     * default min num
     */
    private static final int DEFAULT_MIN_NUM = 15;
    /**
     * default max num
     */
    private static final int DEFAULT_MAX_NUM = 33;
    /**
     * large num
     */
    private static final int LARGE_NUM = (1 << 16) + 1;
    /**
     * default element byte length
     */
    private static final int DEFAULT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * short element byte length
     */
    private static final int SHORT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH / 2 - 1;
    /**
     * large element byte length
     */
    private static final int LARGE_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH * 2 + 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // PRRS24_OPRF
        configurations.add(new Object[]{
            RosnType.PRRS24_OPRF.name() + " (" + Conv32Type.SVODE + ")",
            new Prrs24OprfRosnConfig.Builder(Conv32Type.SVODE).build(),
        });
        configurations.add(new Object[]{
            RosnType.PRRS24_OPRF.name() + " (" + Conv32Type.SCOT + ")",
            new Prrs24OprfRosnConfig.Builder(Conv32Type.SCOT).build(),
        });
        // LLL24_NET
        configurations.add(new Object[]{
            RosnType.LLL24_NET.name(), new Lll24NetRosnConfig.Builder(false).build(),
        });
        // LLL24_CST
        configurations.add(new Object[]{
            RosnType.LLL24_CST.name() + " (T = 32)", new Lll24CstRosnConfig.Builder(32, false).build(),
        });
        configurations.add(new Object[]{
            RosnType.LLL24_CST.name() + " (T = 16)", new Lll24CstRosnConfig.Builder(16, false).build(),
        });
        // CGP20_CST
        configurations.add(new Object[]{
            RosnType.CGP20_CST.name() + " (T = 32)", new Cgp20CstRosnConfig.Builder(32, false).build(),
        });
        configurations.add(new Object[]{
            RosnType.CGP20_CST.name() + " (T = 16)", new Cgp20CstRosnConfig.Builder(16, false).build(),
        });
        // GMR21_NET
        configurations.add(new Object[]{
            RosnType.GMR21_NET.name(), new Gmr21NetRosnConfig.Builder(false).build(),
        });
        // MS13_NET
        configurations.add(new Object[]{
            RosnType.MS13_NET.name(), new Ms13NetRosnConfig.Builder(false).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final RosnConfig config;

    public RosnTest(String name, RosnConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test2N() {
        testPto(2, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void test3N() {
        testPto(3, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void test4N() {
        testPto(4, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void test5N() {
        testPto(5, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void testDefault() {
        for (int num = DEFAULT_MIN_NUM; num <= DEFAULT_MAX_NUM; num++) {
            testPto(num, DEFAULT_BYTE_LENGTH, false);
        }
    }

    @Test
    public void testDefaultParallel() {
        for (int num = DEFAULT_MIN_NUM; num <= DEFAULT_MAX_NUM; num++) {
            testPto(num, DEFAULT_BYTE_LENGTH, true);
        }
    }

    @Test
    public void testShortByteLength() {
        testPto(DEFAULT_MAX_NUM, SHORT_BYTE_LENGTH, true);
    }

    @Test
    public void testLargeByteLength() {
        testPto(DEFAULT_MAX_NUM, LARGE_BYTE_LENGTH, true);
    }

    @Test
    public void testLarge() {
        testPto(LARGE_NUM, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void testLargeParallel() {
        testPto(LARGE_NUM, DEFAULT_BYTE_LENGTH, true);
    }

    private void testPto(int num, int byteLength, boolean parallel) {
        RosnSender sender = RosnFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        RosnReceiver receiver = RosnFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            int[] pi = PermutationNetworkUtils.randomPermutation(num, SECURE_RANDOM);
            RosnSenderThread senderThread = new RosnSenderThread(sender, num, byteLength);
            RosnReceiverThread receiverThread = new RosnReceiverThread(receiver, pi, byteLength);
            STOP_WATCH.start();
            // start
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            // verify
            RosnSenderOutput senderOutput = senderThread.getSenderOutput();
            RosnReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            OsnTestUtils.assertOutput(pi, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
