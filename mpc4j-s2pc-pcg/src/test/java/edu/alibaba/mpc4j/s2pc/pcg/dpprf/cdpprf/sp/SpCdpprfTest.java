package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfFactory.SpCdpprfType;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.gyw23.Gyw23SpCdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import org.junit.Assert;
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
 * SP-CDPPRF tests.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
@RunWith(Parameterized.class)
public class SpCdpprfTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpCdpprfTest.class);
    /**
     * default num, must be in format 2^k
     */
    private static final int DEFAULT_NUM = 1 << 10;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // GYW23
        configurations.add(new Object[]{
            SpCdpprfType.GYW23.name(), new Gyw23SpCdpprfConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final SpCdpprfConfig config;

    public SpCdpprfTest(String name, SpCdpprfConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testFirstAlpha() {
        //noinspection UnnecessaryLocalVariable
        int num = DEFAULT_NUM;
        int alpha = 0;
        testPto(alpha, num, false);
    }

    @Test
    public void testLastAlpha() {
        int num = DEFAULT_NUM;
        int alpha = num - 1;
        testPto(alpha, num, false);
    }

    @Test
    public void test1Num() {
        int num = 1;
        int alpha = 0;
        testPto(alpha, num, false);
    }

    @Test
    public void test2Num() {
        int num = 2;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, false);
    }

    @Test
    public void testDefault() {
        int num = DEFAULT_NUM;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, false);
    }

    @Test
    public void testParallelDefault() {
        int num = DEFAULT_NUM;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, true);
    }

    @Test
    public void testLargeNum() {
        int num = LARGE_NUM;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, false);
    }

    @Test
    public void testParallelLargeNum() {
        int num = LARGE_NUM;
        int alpha = SECURE_RANDOM.nextInt(num);
        testPto(alpha, num, true);
    }

    private void testPto(int alpha, int num, boolean parallel) {
        SpCdpprfSender sender = SpCdpprfFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        SpCdpprfReceiver receiver = SpCdpprfFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, SECURE_RANDOM);
            SpCdpprfSenderThread senderThread = new SpCdpprfSenderThread(sender, delta, num);
            SpCdpprfReceiverThread receiverThread = new SpCdpprfReceiverThread(receiver, alpha, num);
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
            SpCdpprfSenderOutput senderOutput = senderThread.getSenderOutput();
            SpCdpprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(num, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPrecompute() {
        SpCdpprfSender sender = SpCdpprfFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        SpCdpprfReceiver receiver = SpCdpprfFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int num = DEFAULT_NUM;
        try {
            LOGGER.info("-----test {} (precompute) start-----", sender.getPtoDesc().getPtoName());
            byte[] initDelta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(initDelta);
            byte[] actualDelta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(actualDelta);
            int alpha = SECURE_RANDOM.nextInt(num);
            CotSenderOutput preSenderOutput = CotSenderOutput.createRandom(
                SpCdpprfFactory.getPrecomputeNum(config, num), actualDelta, SECURE_RANDOM
            );
            CotReceiverOutput preReceiverOutput = CotReceiverOutput.createRandom(preSenderOutput, SECURE_RANDOM);
            SpCdpprfSenderThread senderThread = new SpCdpprfSenderThread(sender, initDelta, num, preSenderOutput);
            SpCdpprfReceiverThread receiverThread = new SpCdpprfReceiverThread(receiver, alpha, num, preReceiverOutput);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            SpCdpprfSenderOutput senderOutput = senderThread.getSenderOutput();
            SpCdpprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(num, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} (precompute) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int num, SpCdpprfSenderOutput senderOutput, SpCdpprfReceiverOutput receiverOutput) {
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        byte[] actualDelta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        IntStream.range(0, num).forEach(index -> {
            if (index == receiverOutput.getAlpha()) {
                Assert.assertNull(receiverOutput.getV1(index));
            } else {
                Assert.assertArrayEquals(senderOutput.getV0(index), receiverOutput.getV1(index));
            }
            BytesUtils.xori(actualDelta, senderOutput.getV0(index));
        });
        // verify correlation
        Assert.assertArrayEquals(senderOutput.getDelta(), actualDelta);
    }
}
