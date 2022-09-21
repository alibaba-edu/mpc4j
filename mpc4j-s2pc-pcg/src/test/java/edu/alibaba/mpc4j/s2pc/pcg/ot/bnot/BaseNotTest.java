package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.KyberEngineFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.co15.Co15BaseNotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19.Mr19EccBaseNotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19.Mr19KyberBaseNotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np01.Np01BaseNotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np99.Np99BaseNotConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 基础n选1-OT协议测试。
 *
 * @author Hanwen Feng
 * @date 2022/07/22
 */
@RunWith(Parameterized.class)
public class BaseNotTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseNotTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认数量
     */
    private static final int DEFAULT_NUM = 256;
    /**
     * 默认最大选择值
     */
    private static final int DEFAULT_MAX_CHOICE = 33;
    /**
     * 较小最大选择值
     */
    private static final int SMALL_MAX_CHOICE = 6;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // MR19_KYBER (CCA, k = 2)
        configurations.add(new Object[]{
            BaseNotFactory.BaseNotType.MR19_KYBER.name() + " (CCA, k = 2)",
            new Mr19KyberBaseNotConfig.Builder().setParamsK(2).setKyberType(KyberEngineFactory.KyberType.KYBER_CCA).build(),
        });
        // MR19_KYBER (CCA, k = 3)
        configurations.add(new Object[]{
            BaseNotFactory.BaseNotType.MR19_KYBER.name() + " (CCA, k = 3)",
            new Mr19KyberBaseNotConfig.Builder().setParamsK(3).setKyberType(KyberEngineFactory.KyberType.KYBER_CCA).build(),
        });
        // MR19_KYBER (CCA, k = 4)
        configurations.add(new Object[]{
            BaseNotFactory.BaseNotType.MR19_KYBER.name() + " (CCA, k = 4)",
            new Mr19KyberBaseNotConfig.Builder().setParamsK(4).setKyberType(KyberEngineFactory.KyberType.KYBER_CCA).build(),
        });
        // MR19_KYBER (CPA, k = 2)
        configurations.add(new Object[]{
            BaseNotFactory.BaseNotType.MR19_KYBER.name() + " (CPA, k = 2)",
            new Mr19KyberBaseNotConfig.Builder().setParamsK(2).setKyberType(KyberEngineFactory.KyberType.KYBER_CPA).build(),
        });
        // MR19_KYBER (CPA, k = 3)
        configurations.add(new Object[]{
            BaseNotFactory.BaseNotType.MR19_KYBER.name() + " (CPA, k = 3)",
            new Mr19KyberBaseNotConfig.Builder().setParamsK(3).setKyberType(KyberEngineFactory.KyberType.KYBER_CPA).build(),
        });
        // MR19_KYBER (CPA, k = 4)
        configurations.add(new Object[]{
            BaseNotFactory.BaseNotType.MR19_KYBER.name() + " (CPA, k = 4)",
            new Mr19KyberBaseNotConfig.Builder().setParamsK(4).setKyberType(KyberEngineFactory.KyberType.KYBER_CPA).build(),
        });
        // NP99
        configurations.add(new Object[]{
            BaseNotFactory.BaseNotType.NP99.name(), new Np99BaseNotConfig.Builder().build(),
        });
        // CO15 (compress)
        configurations.add(new Object[]{
            BaseNotFactory.BaseNotType.CO15.name() + " (compress)",
            new Co15BaseNotConfig.Builder().setCompressEncode(true).build(),
        });
        // CO15 (uncompress)
        configurations.add(new Object[]{
            BaseNotFactory.BaseNotType.CO15.name() + " (uncompress)",
            new Co15BaseNotConfig.Builder().setCompressEncode(false).build(),
        });
        // NP01 (compress)
        configurations.add(new Object[]{
            BaseNotFactory.BaseNotType.NP01.name() + " (compress)",
            new Np01BaseNotConfig.Builder().setCompressEncode(true).build(),
        });
        // NP01 (uncompress)
        configurations.add(new Object[]{
            BaseNotFactory.BaseNotType.NP01.name() + " (uncompress)",
            new Np01BaseNotConfig.Builder().setCompressEncode(false).build(),
        });
        // MR19_ECC (compress)
        configurations.add(new Object[]{
            BaseNotFactory.BaseNotType.MR19_ECC.name() + " (compress)",
            new Mr19EccBaseNotConfig.Builder().setCompressEncode(true).build(),
        });
        // MR19_ECC (uncompress)
        configurations.add(new Object[]{
            BaseNotFactory.BaseNotType.MR19_ECC.name() + " (uncompress)",
            new Mr19EccBaseNotConfig.Builder().setCompressEncode(false).build(),
        });

        return configurations;
    }

    /**
     * 发送方
     */
    private final Rpc senderRpc;
    /**
     * 接收方
     */
    private final Rpc receiverRpc;
    /**
     * 协议类型
     */
    private final BaseNotConfig config;

    public BaseNotTest(String name, BaseNotConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        BaseNotSender sender = BaseNotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BaseNotReceiver receiver = BaseNotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1() {
        BaseNotSender sender = BaseNotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BaseNotReceiver receiver = BaseNotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1, DEFAULT_MAX_CHOICE, false);
    }

    @Test
    public void test2() {
        BaseNotSender sender = BaseNotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BaseNotReceiver receiver = BaseNotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2, DEFAULT_MAX_CHOICE, false);
    }

    @Test
    public void testSmallMaxChoice() {
        BaseNotSender sender = BaseNotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BaseNotReceiver receiver = BaseNotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM, SMALL_MAX_CHOICE, false);
    }

    @Test
    public void testDefault() {
        BaseNotSender sender = BaseNotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BaseNotReceiver receiver = BaseNotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM, DEFAULT_MAX_CHOICE, false);
    }

    @Test
    public void testParallelDefault() {
        BaseNotSender sender = BaseNotFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BaseNotReceiver receiver = BaseNotFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM, DEFAULT_MAX_CHOICE, true);
    }

    private void testPto(BaseNotSender sender, BaseNotReceiver receiver, int num, int maxChoice, boolean parallel) {
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            int[] choices = new int[num];
            IntStream.range(0, num).forEach(index -> choices[index] = SECURE_RANDOM.nextInt(maxChoice));
            BaseNotSenderThread senderThread = new BaseNotSenderThread(sender, num, maxChoice);
            BaseNotReceiverThread receiverThread = new BaseNotReceiverThread(receiver, choices, maxChoice);
            StopWatch stopWatch = new StopWatch();
            // 开始执行协议
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            // 等待线程停止
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long onlineTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // 验证结果
            assertOutput(num, senderThread.getSenderOutput(), receiverThread.getReceiverOutput());
            LOGGER.info("Sender sends {}B, Receiver sends {}B, Online time = {}ms",
                senderRpc.getSendByteLength(), receiverRpc.getSendByteLength(), onlineTime
            );
            senderRpc.reset();
            receiverRpc.reset();
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int num, BaseNotSenderOutput senderOutput, BaseNotReceiverOutput receiverOutput) {
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        Assert.assertEquals(senderOutput.getMaxChoice(), receiverOutput.getMaxChoice());
        int maxChoice = senderOutput.getMaxChoice();
        IntStream.range(0, num).forEach(index -> {
            for (int choice = 0; choice < maxChoice; choice++) {
                ByteBuffer rb = ByteBuffer.wrap(receiverOutput.getRb(index));
                if (choice == receiverOutput.getChoice(index)) {
                    Assert.assertEquals(ByteBuffer.wrap(senderOutput.getRi(index, choice)), rb);
                } else {
                    Assert.assertNotEquals(ByteBuffer.wrap(senderOutput.getRi(index, choice)), rb);
                }
            }
        });
    }
}