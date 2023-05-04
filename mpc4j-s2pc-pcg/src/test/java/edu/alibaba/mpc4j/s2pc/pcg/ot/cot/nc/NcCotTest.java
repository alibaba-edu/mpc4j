package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.lpn.ldpc.LdpcCreatorUtils;
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

/**
 * no-choice COT test.
 *
 * @author Weiran Liu
 * @date 2022/01/13
 */
@RunWith(Parameterized.class)
public class NcCotTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(NcCotTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
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

        // DIRECT (Malicious)
        configurations.add(new Object[] {
            NcCotType.DIRECT.name() + " (" + SecurityModel.MALICIOUS + ")",
            new DirectNcCotConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        // DIRECT (Semi-honest)
        configurations.add(new Object[] {
            NcCotType.DIRECT.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new DirectNcCotConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // YWL20 (Malicious, Regular-Index)
        MspCotConfig maRegMspCotConfig = new Bcg19RegMspCotConfig.Builder(SecurityModel.MALICIOUS).build();
        configurations.add(new Object[] {
            NcCotType.YWL20.name() + " (Malicious, Regular-Index)",
            new Ywl20NcCotConfig.Builder(SecurityModel.MALICIOUS).setMspCotConfig(maRegMspCotConfig).build(),
        });
        // YWL20 (Malicious, Unique-Index)
        MspCotConfig maUniMspCotConfig = new Ywl20UniMspCotConfig.Builder(SecurityModel.MALICIOUS).build();
        configurations.add(new Object[] {
            NcCotType.YWL20.name() + " (Malicious, Unique-Index)",
            new Ywl20NcCotConfig.Builder(SecurityModel.MALICIOUS).setMspCotConfig(maUniMspCotConfig).build(),
        });
        // YWL20 (Semi-Honest, Regular-Index)
        MspCotConfig shRegMspCotConfig = new Bcg19RegMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build();
        configurations.add(new Object[] {
            NcCotType.YWL20.name() + " (Semi-honest, Regular-Index)",
            new Ywl20NcCotConfig.Builder(SecurityModel.SEMI_HONEST).setMspCotConfig(shRegMspCotConfig).build(),
        });
        // YWL20 (Semi-Honest, Unique-Index)
        MspCotConfig shUniMspCotConfig = new Ywl20UniMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build();
        configurations.add(new Object[] {
            NcCotType.YWL20.name() + " (Semi-honest, Unique-Index)",
            new Ywl20NcCotConfig.Builder(SecurityModel.SEMI_HONEST).setMspCotConfig(shUniMspCotConfig).build(),
        });
        // CRR21 (Semi-Honest, Regular-Index, Silver11)
        configurations.add(new Object[] {
            NcCotType.CRR21.name() + " (Semi-honest, Regular-Index, Silver 11)",
            new Crr21NcCotConfig.Builder().setCodeType(LdpcCreatorUtils.CodeType.SILVER_11)
                    .setMspCotConfig(new Bcg19RegMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                    .build(),
        });
        // CRR21 (Semi-Honest, Unique-Index, Silver11)
        configurations.add(new Object[] {
                NcCotType.CRR21.name() + " (Semi-honest, Unique-Index, Silver 11)",
                new Crr21NcCotConfig.Builder().setCodeType(LdpcCreatorUtils.CodeType.SILVER_11)
                        .setMspCotConfig(new Ywl20UniMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                        .build(),
        });
        // CRR21 (Malicious, Regular-Index, Silver11)
        configurations.add(new Object[] {
                NcCotType.CRR21.name() + " (Semi-honest, Regular-Index, Silver 11)",
                new Crr21NcCotConfig.Builder().setCodeType(LdpcCreatorUtils.CodeType.SILVER_11)
                        .setMspCotConfig(new Bcg19RegMspCotConfig.Builder(SecurityModel.MALICIOUS).build())
                        .build(),
        });
        // CRR21 (Malicious, Unique-Index, Silver11)
        configurations.add(new Object[] {
                NcCotType.CRR21.name() + " (Semi-honest, Unique-Index, Silver 11)",
                new Crr21NcCotConfig.Builder().setCodeType(LdpcCreatorUtils.CodeType.SILVER_11)
                        .setMspCotConfig(new Ywl20UniMspCotConfig.Builder(SecurityModel.MALICIOUS).build())
                        .build(),
        });
        // CRR21 (Semi-Honest, Regular-Index Silver5)
        configurations.add(new Object[] {
            NcCotType.CRR21.name() + " (Semi-honest, Regular, Silver 5)",
            new Crr21NcCotConfig.Builder().setCodeType(LdpcCreatorUtils.CodeType.SILVER_5)
                    .setMspCotConfig(new Bcg19RegMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                    .build(),
        });
        // CRR21 (Semi-honest, Unique-Index, Silver5)
        configurations.add(new Object[] {
                NcCotType.CRR21.name() + " (Semi-honest, Unique-index, Silver 5)",
                new Crr21NcCotConfig.Builder().setCodeType(LdpcCreatorUtils.CodeType.SILVER_5)
                        .setMspCotConfig(new Ywl20UniMspCotConfig.Builder(SecurityModel.SEMI_HONEST).build()).build(),
        });
        // CRR21 (Malicious, Regular-Index, Silver5)
        configurations.add(new Object[] {
                NcCotType.CRR21.name() + " (Malicious, Regular, Silver 5)",
                new Crr21NcCotConfig.Builder().setCodeType(LdpcCreatorUtils.CodeType.SILVER_5)
                        .setMspCotConfig(new Bcg19RegMspCotConfig.Builder(SecurityModel.MALICIOUS).build()).build(),
        });
        // CRR21 (Malicious, Unique-Index, Silver5)
        configurations.add(new Object[] {
                NcCotType.CRR21.name() + " (Malicious, Unique-index, Silver 5)",
                new Crr21NcCotConfig.Builder().setCodeType(LdpcCreatorUtils.CodeType.SILVER_5)
                        .setMspCotConfig(new Ywl20UniMspCotConfig.Builder(SecurityModel.MALICIOUS).build()).build(),
        });

        return configurations;
    }

    /**
     * sender RPC
     */
    private final Rpc senderRpc;
    /**
     * receiver RPC
     */
    private final Rpc receiverRpc;
    /**
     * config
     */
    private final NcCotConfig config;

    public NcCotTest(String name, NcCotConfig config) {
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
        NcCotSender sender = NcCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NcCotReceiver receiver = NcCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
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
            senderRpc.reset();
            long receiverByteLength = receiverRpc.getSendByteLength();
            receiverRpc.reset();
            CotSenderOutput senderOutput = senderThread.getSenderOutput();
            CotReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // verify
            CotTestUtils.assertOutput(num * round, senderOutput, receiverOutput);
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

    @Test
    public void testResetDelta() {
        NcCotSender sender = NcCotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NcCotReceiver receiver = NcCotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        int round = DEFAULT_ROUND;
        int num = DEFAULT_NUM;
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(delta);
            // first time
            NcCotSenderThread senderThread = new NcCotSenderThread(sender, delta, num, round);
            NcCotReceiverThread receiverThread = new NcCotReceiverThread(receiver, num, round);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long firstTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            long firstSenderByteLength = senderRpc.getSendByteLength();
            senderRpc.reset();
            long firstReceiverByteLength = receiverRpc.getSendByteLength();
            receiverRpc.reset();
            CotSenderOutput firstSenderOutput = senderThread.getSenderOutput();
            CotReceiverOutput firstReceiverOutput = receiverThread.getReceiverOutput();
            CotTestUtils.assertOutput(num * round, firstSenderOutput, firstReceiverOutput);
            // second time, reset delta
            SECURE_RANDOM.nextBytes(delta);
            senderThread = new NcCotSenderThread(sender, delta, num, round);
            receiverThread = new NcCotReceiverThread(receiver, num, round);
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long secondTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            long secondSenderByteLength = senderRpc.getSendByteLength();
            long secondReceiverByteLength = receiverRpc.getSendByteLength();
            senderRpc.reset();
            receiverRpc.reset();
            CotSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            CotReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            CotTestUtils.assertOutput(num * round, secondSenderOutput, secondReceiverOutput);
            // the communication should be equal
            Assert.assertEquals(secondSenderByteLength, firstSenderByteLength);
            Assert.assertEquals(secondReceiverByteLength, firstReceiverByteLength);
            // Δ should be not equal
            Assert.assertNotEquals(
                ByteBuffer.wrap(secondSenderOutput.getDelta()), ByteBuffer.wrap(firstSenderOutput.getDelta())
            );
            LOGGER.info("1st round, Send. {}B, Recv. {}B, {}ms",
                firstSenderByteLength, firstReceiverByteLength, firstTime
            );
            LOGGER.info("2nd round, Send. {}B, Recv. {}B, {}ms",
                secondSenderByteLength, secondReceiverByteLength, secondTime
            );
            LOGGER.info("-----test {} (reset Δ) end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sender.destroy();
        receiver.destroy();
    }
}
