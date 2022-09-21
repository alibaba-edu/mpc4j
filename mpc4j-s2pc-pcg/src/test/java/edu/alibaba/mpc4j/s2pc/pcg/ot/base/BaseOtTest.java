package edu.alibaba.mpc4j.s2pc.pcg.ot.base;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.KyberEngineFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory.BaseOtType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.co15.Co15BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.csw20.Csw20BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19.Mr19EccBaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19.Mr19KyberBaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01.Np01BaseOtConfig;
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
 * 基础OT协议测试。
 *
 * @author Weiran Liu
 * @date 2019/07/12
 */
@RunWith(Parameterized.class)
public class BaseOtTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseOtTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认数量
     */
    private static final int DEFAULT_NUM = 1000;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // MR19_ECC (compress)
        configurationParams.add(new Object[]{
            BaseOtType.MR19_ECC.name() + " (compress)",
            new Mr19EccBaseOtConfig.Builder().setCompressEncode(true).build(),
        });
        // MR19_ECC (uncompress)
        configurationParams.add(new Object[]{
            BaseOtType.MR19_ECC.name() + " (uncompress)",
            new Mr19EccBaseOtConfig.Builder().setCompressEncode(false).build(),
        });
        // MR19_KYBER (CCA, k = 2)
        configurationParams.add(new Object[]{
            BaseOtType.MR19_KYBER.name() + "(CCA, k = 2)",
            new Mr19KyberBaseOtConfig.Builder().setParamsK(2).setKyberType(KyberEngineFactory.KyberType.KYBER_CCA).build(),
        });
        // MR19_KYBER (CCA, K = 3)
        configurationParams.add(new Object[]{
            BaseOtType.MR19_KYBER.name() + "(CCA, k = 3)",
            new Mr19KyberBaseOtConfig.Builder().setParamsK(3).setKyberType(KyberEngineFactory.KyberType.KYBER_CCA).build(),
        });
        // MR19_KYBER (CCA, K = 4)
        configurationParams.add(new Object[]{
            BaseOtType.MR19_KYBER.name() + "(CCA, k = 4)",
            new Mr19KyberBaseOtConfig.Builder().setParamsK(4).setKyberType(KyberEngineFactory.KyberType.KYBER_CCA).build(),
        });
        // MR19_KYBER (CPA, K = 2)
        configurationParams.add(new Object[]{
            BaseOtType.MR19_KYBER.name() + "(CPA, k = 2)",
            new Mr19KyberBaseOtConfig.Builder().setParamsK(2).setKyberType(KyberEngineFactory.KyberType.KYBER_CPA).build(),
        });
        // MR19_KYBER (CPA, K = 3)
        configurationParams.add(new Object[]{
            BaseOtType.MR19_KYBER.name() + "(CPA, k = 3)",
            new Mr19KyberBaseOtConfig.Builder().setParamsK(3).setKyberType(KyberEngineFactory.KyberType.KYBER_CPA).build(),
        });
        // MR19_KYBER (CPA, K = 4)
        configurationParams.add(new Object[]{
            BaseOtType.MR19_KYBER.name() + "(CPA, k = 4)",
            new Mr19KyberBaseOtConfig.Builder().setParamsK(4).setKyberType(KyberEngineFactory.KyberType.KYBER_CPA).build(),
        });
        // CO15 (compress)
        configurationParams.add(new Object[]{
            BaseOtType.CO15.name() + " (compress)",
            new Co15BaseOtConfig.Builder().setCompressEncode(true).build(),
        });
        // CO15 (uncompress)
        configurationParams.add(new Object[]{
            BaseOtType.CO15.name() + " (uncompress)",
            new Co15BaseOtConfig.Builder().setCompressEncode(false).build(),
        });
        // NP01 (compress)
        configurationParams.add(new Object[]{
            BaseOtType.NP01.name() + " (compress)",
            new Np01BaseOtConfig.Builder().setCompressEncode(true).build(),
        });
        // NP01 (uncompress)
        configurationParams.add(new Object[]{
            BaseOtType.NP01.name() + " (uncompress)",
            new Np01BaseOtConfig.Builder().setCompressEncode(false).build(),
        });
        // CSW20 (compress)
        configurationParams.add(new Object[]{
            BaseOtType.CSW20.name() + " (compress)",
            new Csw20BaseOtConfig.Builder().setCompressEncode(true).build(),
        });
        // CSW20 (uncompress)
        configurationParams.add(new Object[]{
            BaseOtType.CSW20.name() + " (uncompress)",
            new Csw20BaseOtConfig.Builder().setCompressEncode(false).build(),
        });

        return configurationParams;
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
    private final BaseOtConfig config;

    public BaseOtTest(String name, BaseOtConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        BaseOtSender sender = BaseOtFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BaseOtReceiver receiver = BaseOtFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1Num() {
        BaseOtSender sender = BaseOtFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BaseOtReceiver receiver = BaseOtFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1);
    }

    @Test
    public void test2Num() {
        BaseOtSender sender = BaseOtFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BaseOtReceiver receiver = BaseOtFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2);
    }

    @Test
    public void testDefaultNum() {
        BaseOtSender sender = BaseOtFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BaseOtReceiver receiver = BaseOtFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testParallelDefaultNum() {
        BaseOtSender sender = BaseOtFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BaseOtReceiver receiver = BaseOtFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    private void testPto(BaseOtSender sender, BaseOtReceiver receiver, int num) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            BaseOtSenderThread senderThread = new BaseOtSenderThread(sender, num);
            boolean[] choices = new boolean[num];
            IntStream.range(0, num).forEach(index -> choices[index] = SECURE_RANDOM.nextBoolean());
            BaseOtReceiverThread receiverThread = new BaseOtReceiverThread(receiver, choices);
            StopWatch stopWatch = new StopWatch();
            // 开始执行协议
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            // 等待线程停止
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long totalTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // 验证结果
            assertOutput(num, senderThread.getSenderOutput(), receiverThread.getReceiverOutput());
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderRpc.getSendByteLength(), receiverRpc.getSendByteLength(), totalTime
            );
            senderRpc.reset();
            receiverRpc.reset();
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int num, BaseOtSenderOutput senderOutput, BaseOtReceiverOutput receiverOutput) {
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        IntStream.range(0, num).forEach(index -> {
            if (receiverOutput.getChoice(index)) {
                Assert.assertEquals(
                    ByteBuffer.wrap(senderOutput.getR1(index)),
                    ByteBuffer.wrap(receiverOutput.getRb(index))
                );
            } else {
                Assert.assertEquals(
                    ByteBuffer.wrap(senderOutput.getR0(index)),
                    ByteBuffer.wrap(receiverOutput.getRb(index))
                );
            }
        });
    }
}