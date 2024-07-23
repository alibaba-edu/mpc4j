package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.OtTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory.CoreCotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.alsz13.Alsz13CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.iknp03.Iknp03CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.kos15.Kos15CoreCotConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 核COT协议测试。
 *
 * @author Weiran Liu
 * @date 2022/01/13
 */
@RunWith(Parameterized.class)
public class CoreCotTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreCotTest.class);
    /**
     * 默认数量
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * 较大数量
     */
    private static final int LARGE_NUM = 1 << 20;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // KOS15
        configurations.add(new Object[] {
            CoreCotType.KOS15.name(), new Kos15CoreCotConfig.Builder().build(),
        });
        // ALSZ13
        configurations.add(new Object[] {
            CoreCotType.ALSZ13.name(), new Alsz13CoreCotConfig.Builder().build(),
        });
        // IKNP03
        configurations.add(new Object[] {
            CoreCotType.IKNP03.name(), new Iknp03CoreCotConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * 协议类型
     */
    private final CoreCotConfig config;

    public CoreCotTest(String name, CoreCotConfig config) {
        super(name);
        this.config = config;
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
        CoreCotSender sender = CoreCotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        CoreCotReceiver receiver = CoreCotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, SECURE_RANDOM);
            boolean[] choices = BinaryUtils.randomBinary(num, SECURE_RANDOM);
            CoreCotSenderThread senderThread = new CoreCotSenderThread(sender, delta, num);
            CoreCotReceiverThread receiverThread = new CoreCotReceiverThread(receiver, choices);
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
            CotSenderOutput senderOutput = senderThread.getSenderOutput();
            CotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
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
