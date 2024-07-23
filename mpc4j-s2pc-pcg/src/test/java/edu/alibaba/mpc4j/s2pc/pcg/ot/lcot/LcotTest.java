package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.OtTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.kk13.Kk13OptLcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.kk13.Kk13OriLcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.oos17.Oos17LcotConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 2^l选1-COT协议测试。
 *
 * @author Weiran Liu
 * @date 2022/6/8
 */
@RunWith(Parameterized.class)
public class LcotTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(LcotTest.class);
    /**
     * 默认数量
     */
    private static final int DEFAULT_NUM = 1 << 10;
    /**
     * 较大数量
     */
    private static final int LARGE_NUM = 1 << 16;
    /**
     * 较小输入比特长度
     */
    private static final int SMALL_INPUT_BIT_LENGTH = 1;
    /**
     * 默认输入比特长度
     */
    private static final int DEFAULT_INPUT_BIT_LENGTH = 8;
    /**
     * 较大输入比特长度
     */
    private static final int LARGE_INPUT_BIT_LENGTH = 64;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // OOS17
        configurations.add(new Object[] {
            LcotFactory.LcotType.OOS17.name(), new Oos17LcotConfig.Builder().build(),
        });
        // KK13_OPT
        configurations.add(new Object[] {
            LcotFactory.LcotType.KK13_OPT.name(), new Kk13OptLcotConfig.Builder().build(),
        });
        // KK13_ORI
        configurations.add(new Object[] {
            LcotFactory.LcotType.KK13_ORI.name(), new Kk13OriLcotConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * 协议类型
     */
    private final LcotConfig config;

    public LcotTest(String name, LcotConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1() {
        testPto(DEFAULT_INPUT_BIT_LENGTH, 1, false);
    }

    @Test
    public void test2() {
        testPto(DEFAULT_INPUT_BIT_LENGTH, 2, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_INPUT_BIT_LENGTH, DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_INPUT_BIT_LENGTH, DEFAULT_NUM, true);
    }

    @Test
    public void testSmallInputBitLength() {
        testPto(SMALL_INPUT_BIT_LENGTH, DEFAULT_NUM, false);
    }

    @Test
    public void testLargeInputBitLength() {
        testPto(LARGE_INPUT_BIT_LENGTH, DEFAULT_NUM, false);
    }

    @Test
    public void testLarge() {
        testPto(DEFAULT_INPUT_BIT_LENGTH, LARGE_NUM, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(DEFAULT_INPUT_BIT_LENGTH, LARGE_NUM, true);
    }

    private void testPto(int inputBitLength, int num, boolean parallel) {
        LcotSender sender = LcotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        LcotReceiver receiver = LcotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            int inputByteLength = CommonUtils.getByteLength(inputBitLength);
            byte[][] choices = IntStream.range(0, num)
                .mapToObj(index -> {
                    byte[] choice = new byte[inputByteLength];
                    SECURE_RANDOM.nextBytes(choice);
                    BytesUtils.reduceByteArray(choice, inputBitLength);
                    return choice;
                })
                .toArray(byte[][]::new);
            LcotSenderThread senderThread = new LcotSenderThread(sender, inputBitLength, num);
            LcotReceiverThread receiverThread = new LcotReceiverThread(receiver, inputBitLength, choices);
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
            LcotSenderOutput senderOutput = senderThread.getSenderOutput();
            LcotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            OtTestUtils.assertOutput(num, senderOutput, receiverOutput);
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
