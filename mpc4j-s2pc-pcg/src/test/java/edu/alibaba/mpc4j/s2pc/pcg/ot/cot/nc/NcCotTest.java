package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.lpn.dual.silver.SilverCodeCreatorUtils.SilverCodeType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory.NcCotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.crr21.Crr21NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.direct.DirectNcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20.Ywl20NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.bcg19.Bcg19RegMspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20.Ywl20UniMspCotConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * no-choice COT test.
 *
 * @author Weiran Liu
 * @date 2022/01/13
 */
@RunWith(Parameterized.class)
public class NcCotTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(NcCotTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * default round
     */
    private static final int DEFAULT_ROUND = 2;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 18;
    /**
     * large round
     */
    private static final int LARGE_ROUND = 5;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // DIRECT
        configurations.add(new Object[]{
            NcCotType.DIRECT.name() + " (" + SecurityModel.MALICIOUS + ")",
            new DirectNcCotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        configurations.add(new Object[]{
            NcCotType.DIRECT.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new DirectNcCotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // YWL20 (Regular-Index)
        MspCotConfig maRegMspCotConfig = new Bcg19RegMspCotConfig.Builder(SecurityModel.MALICIOUS).build();
        configurations.add(new Object[]{
            NcCotType.YWL20.name() + " (" + SecurityModel.MALICIOUS + ", Regular-Index)",
            new Ywl20NcCotConfig.Builder(SecurityModel.MALICIOUS).setMspCotConfig(maRegMspCotConfig).build(),
        });
        MspCotConfig shRegMspCotConfig = new Bcg19RegMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build();
        configurations.add(new Object[]{
            NcCotType.YWL20.name() + " (" + SecurityModel.SEMI_HONEST + ", Regular-Index)",
            new Ywl20NcCotConfig.Builder(SecurityModel.SEMI_HONEST).setMspCotConfig(shRegMspCotConfig).build(),
        });
        // YWL20 (Unique-Index)
        MspCotConfig maUniMspCotConfig = new Ywl20UniMspCotConfig.Builder(SecurityModel.MALICIOUS).build();
        configurations.add(new Object[]{
            NcCotType.YWL20.name() + " (" + SecurityModel.MALICIOUS + ", Unique-Index)",
            new Ywl20NcCotConfig.Builder(SecurityModel.MALICIOUS).setMspCotConfig(maUniMspCotConfig).build(),
        });
        MspCotConfig shUniMspCotConfig = new Ywl20UniMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build();
        configurations.add(new Object[]{
            NcCotType.YWL20.name() + " (" + SecurityModel.SEMI_HONEST + ", Unique-Index)",
            new Ywl20NcCotConfig.Builder(SecurityModel.SEMI_HONEST).setMspCotConfig(shUniMspCotConfig).build(),
        });
        // CRR21 (Regular-Index, Silver11)
        configurations.add(new Object[]{
            NcCotType.CRR21.name() + " (" + SecurityModel.MALICIOUS + " (Regular-Index, Silver 11)",
            new Crr21NcCotConfig.Builder(SecurityModel.MALICIOUS).setCodeType(SilverCodeType.SILVER_11)
                .setMspCotConfig(new Bcg19RegMspCotConfig.Builder(SecurityModel.MALICIOUS).build())
                .build(),
        });
        // CRR21 (Regular-Index, Silver11)
        configurations.add(new Object[]{
            NcCotType.CRR21.name() + " (" + SecurityModel.SEMI_HONEST + ", Regular-Index, Silver 11)",
            new Crr21NcCotConfig.Builder(SecurityModel.SEMI_HONEST).setCodeType(SilverCodeType.SILVER_11)
                .setMspCotConfig(new Bcg19RegMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                .build(),
        });
        // CRR21 (Unique-Index, Silver11)
        configurations.add(new Object[]{
            NcCotType.CRR21.name() + " (" + SecurityModel.MALICIOUS + ", Unique-Index, Silver 11)",
            new Crr21NcCotConfig.Builder(SecurityModel.MALICIOUS).setCodeType(SilverCodeType.SILVER_11)
                .setMspCotConfig(new Ywl20UniMspCotConfig.Builder(SecurityModel.MALICIOUS).build())
                .build(),
        });
        configurations.add(new Object[]{
            NcCotType.CRR21.name() + " (" + SecurityModel.SEMI_HONEST + ", Unique-Index, Silver 11)",
            new Crr21NcCotConfig.Builder(SecurityModel.SEMI_HONEST).setCodeType(SilverCodeType.SILVER_11)
                .setMspCotConfig(new Ywl20UniMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                .build(),
        });
        // CRR21 (Regular-Index, Silver5)
        configurations.add(new Object[]{
            NcCotType.CRR21.name() + " (" + SecurityModel.MALICIOUS + ", Regular-Index, Silver 5)",
            new Crr21NcCotConfig.Builder(SecurityModel.MALICIOUS).setCodeType(SilverCodeType.SILVER_5)
                .setMspCotConfig(new Bcg19RegMspCotConfig.Builder(SecurityModel.MALICIOUS).build()).build(),
        });
        configurations.add(new Object[]{
            NcCotType.CRR21.name() + " (" + SecurityModel.SEMI_HONEST + ", Regular-Index, Silver 5)",
            new Crr21NcCotConfig.Builder(SecurityModel.SEMI_HONEST).setCodeType(SilverCodeType.SILVER_5)
                .setMspCotConfig(new Bcg19RegMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                .build(),
        });
        // CRR21 (Unique-Index, Silver5)
        configurations.add(new Object[]{
            NcCotType.CRR21.name() + " (" + SecurityModel.MALICIOUS + ", Unique-Index, Silver 5)",
            new Crr21NcCotConfig.Builder(SecurityModel.MALICIOUS).setCodeType(SilverCodeType.SILVER_5)
                .setMspCotConfig(new Ywl20UniMspCotConfig.Builder(SecurityModel.MALICIOUS).build()).build(),
        });
        configurations.add(new Object[]{
            NcCotType.CRR21.name() + " (" + SecurityModel.SEMI_HONEST + ", Unique-Index, Silver 5)",
            new Crr21NcCotConfig.Builder(SecurityModel.SEMI_HONEST).setCodeType(SilverCodeType.SILVER_5)
                .setMspCotConfig(new Ywl20UniMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build()).build(),
        });

        return configurations;
    }
    /**
     * config
     */
    private final NcCotConfig config;

    public NcCotTest(String name, NcCotConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1Round1Num() {
        testPto(1, 1, false);
    }

    @Test
    public void test2Round2Num() {
        testPto(2, 2, false);
    }

    @Test
    public void testDefaultRoundDefaultNum() {
        testPto(DEFAULT_NUM, DEFAULT_ROUND, false);
    }

    @Test
    public void testParallelDefaultRoundDefaultNum() {
        testPto(DEFAULT_NUM, DEFAULT_ROUND, true);
    }

    @Test
    public void test12LogNum() {
        testPto(1 << 12, DEFAULT_ROUND, false);
    }

    @Test
    public void test16LogNum() {
        testPto(1 << 16, DEFAULT_ROUND, false);
    }

    @Test
    public void testLargeRoundDefaultNum() {
        testPto(DEFAULT_NUM, LARGE_ROUND, false);
    }

    @Test
    public void testParallelLargeRoundDefaultNum() {
        testPto(DEFAULT_NUM, LARGE_ROUND, true);
    }

    @Test
    public void testDefaultRoundLargeNum() {
        testPto(LARGE_NUM, DEFAULT_ROUND, false);
    }

    @Test
    public void testParallelDefaultRoundLargeNum() {
        testPto(LARGE_NUM, DEFAULT_ROUND, true);
    }

    private void testPto(int num, int round, boolean parallel) {
        NcCotSender sender = NcCotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        NcCotReceiver receiver = NcCotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            NcCotSenderThread senderThread = new NcCotSenderThread(sender, delta, num, round);
            NcCotReceiverThread receiverThread = new NcCotReceiverThread(receiver, num, round);
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
            CotTestUtils.assertOutput(num * round, senderOutput, receiverOutput);
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
    public void testResetDelta() {
        NcCotSender sender = NcCotFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        NcCotReceiver receiver = NcCotFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        int round = DEFAULT_ROUND;
        int num = DEFAULT_NUM;
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            // first round
            NcCotSenderThread senderThread = new NcCotSenderThread(sender, delta, num, round);
            NcCotReceiverThread receiverThread = new NcCotReceiverThread(receiver, num, round);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long firstTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            CotSenderOutput firstSenderOutput = senderThread.getSenderOutput();
            CotReceiverOutput firstReceiverOutput = receiverThread.getReceiverOutput();
            CotTestUtils.assertOutput(num * round, firstSenderOutput, firstReceiverOutput);
            printAndResetRpc(firstTime);
            // second time, reset delta
            SECURE_RANDOM.nextBytes(delta);
            senderThread = new NcCotSenderThread(sender, delta, num, round);
            receiverThread = new NcCotReceiverThread(receiver, num, round);
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long secondTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            CotSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            CotReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            CotTestUtils.assertOutput(num * round, secondSenderOutput, secondReceiverOutput);
            // Δ should be different
            Assert.assertNotEquals(
                ByteBuffer.wrap(secondSenderOutput.getDelta()), ByteBuffer.wrap(firstSenderOutput.getDelta())
            );
            printAndResetRpc(secondTime);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            LOGGER.info("-----test {} (reset Δ) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
