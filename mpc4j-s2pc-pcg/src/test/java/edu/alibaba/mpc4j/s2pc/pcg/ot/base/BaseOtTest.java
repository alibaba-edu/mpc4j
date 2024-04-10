package edu.alibaba.mpc4j.s2pc.pcg.ot.base;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.KyberEngineFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory.BaseOtType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.co15.Co15BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.csw20.Csw20BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19.Mr19EccBaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19.Mr19KyberBaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01.Np01BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01.Np01ByteBaseOtConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
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
public class BaseOtTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseOtTest.class);
    /**
     * 默认数量
     */
    private static final int DEFAULT_NUM = 1000;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // NP01_BYTE
        configurations.add(new Object[]{
            BaseOtType.NP01_BYTE.name(),
            new Np01ByteBaseOtConfig.Builder().build(),
        });
        // MR19_ECC (compress)
        configurations.add(new Object[]{
            BaseOtType.MR19_ECC.name() + " (compress)",
            new Mr19EccBaseOtConfig.Builder().setCompressEncode(true).build(),
        });
        // MR19_ECC (uncompress)
        configurations.add(new Object[]{
            BaseOtType.MR19_ECC.name() + " (uncompress)",
            new Mr19EccBaseOtConfig.Builder().setCompressEncode(false).build(),
        });
        // MR19_KYBER (CCA, k = 2)
        configurations.add(new Object[]{
            BaseOtType.MR19_KYBER.name() + "(CCA, k = 2)",
            new Mr19KyberBaseOtConfig.Builder().setParamsK(2).setKyberType(KyberEngineFactory.KyberType.KYBER_CCA).build(),
        });
        // MR19_KYBER (CCA, K = 3)
        configurations.add(new Object[]{
            BaseOtType.MR19_KYBER.name() + "(CCA, k = 3)",
            new Mr19KyberBaseOtConfig.Builder().setParamsK(3).setKyberType(KyberEngineFactory.KyberType.KYBER_CCA).build(),
        });
        // MR19_KYBER (CCA, K = 4)
        configurations.add(new Object[]{
            BaseOtType.MR19_KYBER.name() + "(CCA, k = 4)",
            new Mr19KyberBaseOtConfig.Builder().setParamsK(4).setKyberType(KyberEngineFactory.KyberType.KYBER_CCA).build(),
        });
        // MR19_KYBER (CPA, K = 2)
        configurations.add(new Object[]{
            BaseOtType.MR19_KYBER.name() + "(CPA, k = 2)",
            new Mr19KyberBaseOtConfig.Builder().setParamsK(2).setKyberType(KyberEngineFactory.KyberType.KYBER_CPA).build(),
        });
        // MR19_KYBER (CPA, K = 3)
        configurations.add(new Object[]{
            BaseOtType.MR19_KYBER.name() + "(CPA, k = 3)",
            new Mr19KyberBaseOtConfig.Builder().setParamsK(3).setKyberType(KyberEngineFactory.KyberType.KYBER_CPA).build(),
        });
        // MR19_KYBER (CPA, K = 4)
        configurations.add(new Object[]{
            BaseOtType.MR19_KYBER.name() + "(CPA, k = 4)",
            new Mr19KyberBaseOtConfig.Builder().setParamsK(4).setKyberType(KyberEngineFactory.KyberType.KYBER_CPA).build(),
        });
        // CO15 (compress)
        configurations.add(new Object[]{
            BaseOtType.CO15.name() + " (compress)",
            new Co15BaseOtConfig.Builder().setCompressEncode(true).build(),
        });
        // CO15 (uncompress)
        configurations.add(new Object[]{
            BaseOtType.CO15.name() + " (uncompress)",
            new Co15BaseOtConfig.Builder().setCompressEncode(false).build(),
        });
        // NP01 (compress)
        configurations.add(new Object[]{
            BaseOtType.NP01.name() + " (compress)",
            new Np01BaseOtConfig.Builder().setCompressEncode(true).build(),
        });
        // NP01 (uncompress)
        configurations.add(new Object[]{
            BaseOtType.NP01.name() + " (uncompress)",
            new Np01BaseOtConfig.Builder().setCompressEncode(false).build(),
        });
        // CSW20 (compress)
        configurations.add(new Object[]{
            BaseOtType.CSW20.name() + " (compress)",
            new Csw20BaseOtConfig.Builder().setCompressEncode(true).build(),
        });
        // CSW20 (uncompress)
        configurations.add(new Object[]{
            BaseOtType.CSW20.name() + " (uncompress)",
            new Csw20BaseOtConfig.Builder().setCompressEncode(false).build(),
        });

        return configurations;
    }

    /**
     * 协议类型
     */
    private final BaseOtConfig config;

    public BaseOtTest(String name, BaseOtConfig config) {
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

    private void testPto(int num, boolean parallel) {
        BaseOtSender sender = BaseOtFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        BaseOtReceiver receiver = BaseOtFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            BaseOtSenderThread senderThread = new BaseOtSenderThread(sender, num);
            boolean[] choices = new boolean[num];
            IntStream.range(0, num).forEach(index -> choices[index] = SECURE_RANDOM.nextBoolean());
            BaseOtReceiverThread receiverThread = new BaseOtReceiverThread(receiver, choices);
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
            assertOutput(num, senderThread.getSenderOutput(), receiverThread.getReceiverOutput());
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
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