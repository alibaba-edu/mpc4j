package edu.alibaba.mpc4j.s2pc.aby.bc.or;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcSquareVector;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.bc.bea91.Bea91BcConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * OR-BC协议测试。
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
@RunWith(Parameterized.class)
public class OrBcTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrBcTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认运算数量
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * 较大运算数量
     */
    private static final int LARGE_NUM = 1 << 18;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // Beaver91 (x public, y public)
        configurations.add(new Object[] {
            BcFactory.BcType.BEA91.name() + " (x public, y public)", new Bea91BcConfig.Builder().build(), true, true
        });
        // Beaver91 (x public, y secret)
        configurations.add(new Object[] {
            BcFactory.BcType.BEA91.name() + " (x public, y secret)", new Bea91BcConfig.Builder().build(), true, false
        });
        // Beaver91 (x secret, y public)
        configurations.add(new Object[] {
            BcFactory.BcType.BEA91.name() + " (x secret, y public)", new Bea91BcConfig.Builder().build(), false, true
        });
        // Beaver91 (x secret, y secret)
        configurations.add(new Object[] {
            BcFactory.BcType.BEA91.name() + " (x secret, y secret)", new Bea91BcConfig.Builder().build(), false, false
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
    private final BcConfig config;
    /**
     * x是否为公开导线
     */
    private final boolean xPublic;
    /**
     * y是否为公开导线
     */
    private final boolean yPublic;

    public OrBcTest(String name, BcConfig config, boolean xPublic, boolean yPublic) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
        this.xPublic = xPublic;
        this.yPublic = yPublic;
    }

    @Test
    public void testPtoType() {
        BcParty sender = BcFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BcParty receiver = BcFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1Num() {
        BcParty sender = BcFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BcParty receiver = BcFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1);
    }

    @Test
    public void test2Num() {
        BcParty sender = BcFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BcParty receiver = BcFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2);
    }

    @Test
    public void test8Num() {
        BcParty sender = BcFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BcParty receiver = BcFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 8);
    }

    @Test
    public void testDefaultNum() {
        BcParty sender = BcFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BcParty receiver = BcFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testParallelDefaultNum() {
        BcParty sender = BcFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BcParty receiver = BcFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum() {
        BcParty sender = BcFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BcParty receiver = BcFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, LARGE_NUM);
    }

    @Test
    public void testParallelLargeNum() {
        BcParty sender = BcFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BcParty receiver = BcFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, LARGE_NUM);
    }

    private void testPto(BcParty sender, BcParty receiver, int num) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);

        int byteLength = CommonUtils.getByteLength(num);
        // 生成x0
        byte[] x0Bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(x0Bytes);
        BytesUtils.reduceByteArray(x0Bytes, num);
        BcSquareVector x0 = BcSquareVector.create(x0Bytes, num, xPublic);
        // 生成x1
        byte[] x1Bytes;
        if (xPublic) {
            x1Bytes = BytesUtils.clone(x0Bytes);
        } else {
            x1Bytes = new byte[byteLength];
            SECURE_RANDOM.nextBytes(x1Bytes);
            BytesUtils.reduceByteArray(x1Bytes, num);
        }
        BcSquareVector x1 = BcSquareVector.create(x1Bytes, num, xPublic);
        // 生成y0
        byte[] y0Bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(y0Bytes);
        BytesUtils.reduceByteArray(y0Bytes, num);
        BcSquareVector y0 = BcSquareVector.create(y0Bytes, num, yPublic);
        // 生成y1
        byte[] y1Bytes;
        if (yPublic) {
            y1Bytes = BytesUtils.clone(y0Bytes);
        } else {
            y1Bytes = new byte[byteLength];
            SECURE_RANDOM.nextBytes(y1Bytes);
            BytesUtils.reduceByteArray(y1Bytes, num);
        }
        BcSquareVector y1 = BcSquareVector.create(y1Bytes, num, yPublic);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            OrBcPartyThread senderThread = new OrBcPartyThread(sender, x0, y0);
            OrBcPartyThread receiverThread = new OrBcPartyThread(receiver, x1, y1);
            StopWatch stopWatch = new StopWatch();
            // 开始执行协议
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            long senderByteLength = senderRpc.getSendByteLength();
            long receiverByteLength = receiverRpc.getSendByteLength();
            senderRpc.reset();
            receiverRpc.reset();
            BcSquareVector z0 = senderThread.getPartyOutput();
            BcSquareVector z1 = receiverThread.getPartyOutput();
            // 验证结果
            assertOutput(x0, x1, y0, y1, z0, z1);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(BcSquareVector x0, BcSquareVector x1, BcSquareVector y0, BcSquareVector y1,
                              BcSquareVector z0, BcSquareVector z1) {
        byte[] x = xPublic ? x0.getBytes() : BytesUtils.xor(x0.getBytes(), x1.getBytes());
        byte[] y = yPublic ? y0.getBytes() : BytesUtils.xor(y0.getBytes(), y1.getBytes());
        //noinspection SuspiciousNameCombination
        byte[] expectZ = BytesUtils.or(x, y);
        byte[] actualZ = (xPublic && yPublic) ? z0.getBytes() : BytesUtils.xor(z0.getBytes(), z1.getBytes());
        Assert.assertArrayEquals(expectZ, actualZ);
    }
}
