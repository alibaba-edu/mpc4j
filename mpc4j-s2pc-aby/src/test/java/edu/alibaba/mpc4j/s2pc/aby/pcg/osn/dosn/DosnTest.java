package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.OsnTestUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.lll24.Lll24DosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory.RosnType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.cgp20.Cgp20CstRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.gmr21.Gmr21NetRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.lll24.Lll24CstRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.lll24.Lll24NetRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.ms13.Ms13NetRosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.prrs24.Prrs24OprfRosnConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Factory.Conv32Type;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Decision OSN tests.
 *
 * @author Weiran Liu
 * @date 2022/02/10
 */
@RunWith(Parameterized.class)
public class DosnTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(DosnTest.class);
    /**
     * 默认批处理数量
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * 性能测试置换表大小
     */
    private static final int LARGE_N = (1 << 16) + 1;
    /**
     * 统计字节长度
     */
    private static final int STATS_BYTE_LENGTH = CommonConstants.STATS_BYTE_LENGTH;
    /**
     * 默认字节长度
     */
    private static final int DEFAULT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * 较大字节长度
     */
    private static final int LARGE_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH * 2;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // PRRS24_OPRF
        configurations.add(new Object[]{
            RosnType.PRRS24_OPRF.name() + " (" + Conv32Type.SVODE + ")",
            new Lll24DosnConfig.Builder(new Prrs24OprfRosnConfig.Builder(Conv32Type.SVODE).build()).build()
        });
        configurations.add(new Object[]{
            RosnType.PRRS24_OPRF.name() + " (" + Conv32Type.SCOT + ")",
            new Lll24DosnConfig.Builder(new Prrs24OprfRosnConfig.Builder(Conv32Type.SCOT).build()).build()
        });

        // PRRS24_OPRF
        configurations.add(new Object[]{
            RosnType.LLL24_NET.name(),
            new Lll24DosnConfig.Builder(new Lll24NetRosnConfig.Builder(false).build()).build()
        });

        // LLL24_CST
        configurations.add(new Object[]{
            RosnType.LLL24_CST.name() + " (T = 32)",
            new Lll24DosnConfig.Builder(new Lll24CstRosnConfig.Builder(32, false).build()).build()
        });
        configurations.add(new Object[]{
            RosnType.LLL24_CST.name() + " (T = 16)",
            new Lll24DosnConfig.Builder(new Lll24CstRosnConfig.Builder(16, false).build()).build()
        });
        // CGP20_CST
        configurations.add(new Object[]{
            RosnType.CGP20_CST.name() + " (T = 32)",
            new Lll24DosnConfig.Builder(new Cgp20CstRosnConfig.Builder(32, false).build()).build()
        });
        configurations.add(new Object[]{
            RosnType.CGP20_CST.name() + " (T = 16)",
            new Lll24DosnConfig.Builder(new Cgp20CstRosnConfig.Builder(16, false).build()).build()
        });

        // GMR21_NET
        configurations.add(new Object[]{
            RosnType.GMR21_NET.name(),
            new Lll24DosnConfig.Builder(new Gmr21NetRosnConfig.Builder(false).build()).build()
        });
        // MS13_NET
        configurations.add(new Object[]{
            RosnType.MS13_NET.name(),
            new Lll24DosnConfig.Builder(new Ms13NetRosnConfig.Builder(false).build()).build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final DosnConfig config;

    public DosnTest(String name, DosnConfig config) {
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
    public void testStatsByteLength() {
        testPto(DEFAULT_NUM, STATS_BYTE_LENGTH, false);
    }

    @Test
    public void testLargeByteLength() {
        testPto(DEFAULT_NUM, LARGE_BYTE_LENGTH, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_NUM, DEFAULT_BYTE_LENGTH, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_NUM, DEFAULT_BYTE_LENGTH, true);
    }

    @Test
    public void testLarge() {
        testPto(LARGE_N, LARGE_BYTE_LENGTH, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(LARGE_N, LARGE_BYTE_LENGTH, true);
    }

    private void testPto(int num, int byteLength, boolean parallel) {
        DosnSender sender = DosnFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        DosnReceiver receiver = DosnFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}, num = {}, byte_length = {}-----", sender.getPtoDesc().getPtoName(), num, byteLength);
            byte[][] inputVector = BytesUtils.randomByteArrayVector(num, byteLength, SECURE_RANDOM);
            int[] pi = PermutationNetworkUtils.randomPermutation(num, SECURE_RANDOM);
            DosnSenderThread senderThread = new DosnSenderThread(sender, inputVector, byteLength);
            DosnReceiverThread receiverThread = new DosnReceiverThread(receiver, pi, byteLength);
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
            DosnPartyOutput senderOutput = senderThread.getSenderOutput();
            DosnPartyOutput receiverOutput = receiverThread.getReceiverOutput();
            OsnTestUtils.assertOutput(inputVector, pi, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
