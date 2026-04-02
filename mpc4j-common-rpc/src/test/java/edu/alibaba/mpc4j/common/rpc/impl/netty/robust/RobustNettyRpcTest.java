package edu.alibaba.mpc4j.common.rpc.impl.netty.robust;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.impl.RpcImplTestUtils;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RobustNettyRpc专项测试。
 * <p>
 * 测试场景：
 * <ol>
 *   <li>正常connect/disconnect验证：5方成功握手、收发空包、断连</li>
 *   <li>TCP闪断模拟：在发送管道插入故障注入Handler，前N次write强制失败，
 *       验证RobustDataSendManager的指数退避重传最终成功完成数据收发</li>
 * </ol>
 * </p>
 *
 * @author Weiran Liu
 * @date 2026/04/02
 */
public class RobustNettyRpcTest {
    /**
     * 参与方数量
     */
    private static final int PARTY_NUM = 3;
    /**
     * 起始端口（与其他测试文件的端口错开，避免端口冲突）
     */
    private static final int START_PORT = 10500;
    /**
     * 超时时间（秒）
     */
    private static final int TIMEOUT_SECONDS = 15;
    /**
     * 长超时时间（秒），用于大数据传输或大量重试的测试
     */
    private static final int LONG_TIMEOUT_SECONDS = 30;
    /**
     * 数据完整性测试的数据大小（2MB）
     */
    private static final int DATA_SIZE = 2 * 1024 * 1024;

    /**
     * 故障注入Handler：前N次write请求强制失败（模拟TCP写入失败）。
     * <p>
     * 通过{@code ChannelDuplexHandler.write()}拦截出站写操作，
     * 前{@code failCount}次调用{@code promise.tryFailure()}使写入失败，
     * 触发RobustDataSendManager的指数退避重传逻辑。
     * N次之后放行，让重传成功。
     * </p>
     */
    @ChannelHandler.Sharable
    static class FaultInjectionHandler extends ChannelDuplexHandler {
        /**
         * 剩余失败次数（原子操作，线程安全）
         */
        private final AtomicInteger remainingFailures;
        /**
         * 记录实际触发的失败次数（用于断言验证）
         */
        private final AtomicInteger failureTriggered;

        FaultInjectionHandler(int failCount) {
            this.remainingFailures = new AtomicInteger(failCount);
            this.failureTriggered = new AtomicInteger(0);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (remainingFailures.getAndDecrement() > 0) {
                // 模拟写入失败：不向下传递msg，直接以失败状态完成promise
                failureTriggered.incrementAndGet();
                promise.tryFailure(new java.io.IOException("Simulated write failure (fault injection)"));
            } else {
                // 放行：正常发送
                super.write(ctx, msg, promise);
            }
        }

        int getFailureTriggered() {
            return failureTriggered.get();
        }
    }

    /**
     * 周期性故障注入Handler：每第{@code period}次write请求强制失败。
     * <p>
     * 与{@link FaultInjectionHandler}的连续N次失败不同，此Handler的故障是散布式的，
     * 模拟间歇性网络抖动场景。第1, 2, ..., period-1次write正常，第period次失败，
     * 第period+1次正常，依此类推。
     * </p>
     */
    @ChannelHandler.Sharable
    static class PeriodicFaultHandler extends ChannelDuplexHandler {
        /**
         * 周期（每period次write失败一次）
         */
        private final int period;
        /**
         * 写入计数器（从0开始递增）
         */
        private final AtomicInteger writeCount;
        /**
         * 实际触发的失败次数（用于断言验证）
         */
        private final AtomicInteger failureTriggered;

        PeriodicFaultHandler(int period) {
            this.period = period;
            this.writeCount = new AtomicInteger(0);
            this.failureTriggered = new AtomicInteger(0);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (writeCount.incrementAndGet() % period == 0) {
                failureTriggered.incrementAndGet();
                promise.tryFailure(new IOException("Simulated periodic write failure (every " + period + "th)"));
            } else {
                super.write(ctx, msg, promise);
            }
        }

        int getFailureTriggered() {
            return failureTriggered.get();
        }
    }

    /**
     * 验证：在发送管道中注入写失败（前3次write强制失败），RobustDataSendManager指数退避重传，
     * 最终数据仍能正确收发完成。
     * <p>
     * 测试步骤：
     * <ol>
     *   <li>正常connect</li>
     *   <li>通过RobustNettyRpcManager向各方的发送pipeline注入FaultInjectionHandler</li>
     *   <li>发送空包（触发分片发送，故障Handler使前3次write失败）</li>
     *   <li>验证故障确实被触发（failureTriggered > 0）</li>
     *   <li>验证最终收发成功（数据正确到达）</li>
     *   <li>disconnect</li>
     * </ol>
     * </p>
     */
    @Test
    public void testWriteFailureRetransmission() throws InterruptedException {
        // 故障注入Handler：前3次write强制失败
        FaultInjectionHandler faultHandler = new FaultInjectionHandler(3);
        // 创建带故障注入的RpcManager
        RobustNettyRpcManager rpcManager = new RobustNettyRpcManager(PARTY_NUM, START_PORT + 10, faultHandler);
        RpcImplTestUtils.connectRandom(rpcManager, TIMEOUT_SECONDS);
        RpcImplTestUtils.sendAndReceiveEmptyPackets(rpcManager, TIMEOUT_SECONDS);
        // 验证故障确实被触发（如果从未触发，说明测试环境有问题）
        Assert.assertTrue(
            "FaultInjectionHandler should have triggered at least once during connect+send",
            faultHandler.getFailureTriggered() > 0
        );
        RpcImplTestUtils.disconnectRandom(rpcManager, TIMEOUT_SECONDS);
    }

    @Test
    public void testPartialDisconnectTimeout() throws InterruptedException {
        RobustNettyRpcManager rpcManager = new RobustNettyRpcManager(PARTY_NUM, START_PORT + 20);
        int halfNum = PARTY_NUM / 2;
        RpcImplTestUtils.connectRandom(rpcManager, TIMEOUT_SECONDS);
        RpcImplTestUtils.sendAndReceiveEmptyPackets(rpcManager, TIMEOUT_SECONDS);
        // 只让前半部分disconnect，后半部分不响应FINISH包 → 前半部分超时
        CountDownLatch latch = new CountDownLatch(halfNum);
        for (int partyId = 0; partyId < halfNum; partyId++) {
            final int id = partyId;
            Thread t = new Thread(() -> {
                try {
                    rpcManager.getRpc(id).disconnect();
                } finally {
                    latch.countDown();
                }
            });
            t.setDaemon(true);
            t.start();
        }
        boolean timedOut = !latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertTrue(
            "Expected partial disconnect() to timeout when other parties are still connected",
            timedOut
        );
    }

    /**
     * 间歇性写失败测试：每第3次write强制失败，验证散布式故障下RobustDataSendManager仍能正确完成数据收发。
     * <p>
     * 与{@link #testWriteFailureRetransmission()}的连续失败不同，此测试模拟更真实的网络抖动场景：
     * 故障随机散布在整个通信过程中（包括connect、send、disconnect阶段），
     * 每次失败只影响1次write，随后的重传即可成功。
     * </p>
     */
    @Test
    public void testIntermittentWriteFailure() throws InterruptedException {
        PeriodicFaultHandler faultHandler = new PeriodicFaultHandler(3);
        RobustNettyRpcManager rpcManager = new RobustNettyRpcManager(PARTY_NUM, START_PORT + 30, faultHandler);
        RpcImplTestUtils.connectRandom(rpcManager, TIMEOUT_SECONDS);
        RpcImplTestUtils.sendAndReceiveEmptyPackets(rpcManager, TIMEOUT_SECONDS);
        Assert.assertTrue(
            "PeriodicFaultHandler should have triggered at least once",
            faultHandler.getFailureTriggered() > 0
        );
        RpcImplTestUtils.disconnectRandom(rpcManager, TIMEOUT_SECONDS);
    }

    /**
     * 重试次数边界测试：前5次write连续失败（恰好等于MAX_RETRY=5的最大可恢复次数），
     * 验证RobustDataSendManager在第6次尝试时成功恢复。
     * <p>
     * 退避时间：100+200+400+800+1600=3100ms。
     * 若failCount≥6，则超过MAX_RETRY，发送将永久失败。
     * </p>
     */
    @Test
    public void testBoundaryRetryExhaustion() throws InterruptedException {
        // 恰好5次连续失败：消耗全部MAX_RETRY次重试机会，第6次尝试成功
        FaultInjectionHandler faultHandler = new FaultInjectionHandler(5);
        RobustNettyRpcManager rpcManager = new RobustNettyRpcManager(PARTY_NUM, START_PORT + 40, faultHandler);
        RpcImplTestUtils.connectRandom(rpcManager, LONG_TIMEOUT_SECONDS);
        RpcImplTestUtils.sendAndReceiveEmptyPackets(rpcManager, LONG_TIMEOUT_SECONDS);
        Assert.assertEquals(
            "All 5 failure slots should have been triggered",
            5, faultHandler.getFailureTriggered()
        );
        RpcImplTestUtils.disconnectRandom(rpcManager, LONG_TIMEOUT_SECONDS);
    }

    /**
     * 数据完整性测试：在周期性写失败故障注入下发送2MB随机数据，验证接收方收到的数据与发送方完全一致。
     * <p>
     * 测试步骤：
     * <ol>
     *   <li>创建带PeriodicFaultHandler(4)的RPC管理器（每4次write失败一次，持续整个通信周期）</li>
     *   <li>connect</li>
     *   <li>每个参与方生成2MB随机byte[]，发送给其他所有参与方</li>
     *   <li>每个参与方接收并验证数据完整性（assertArrayEquals）</li>
     *   <li>disconnect</li>
     * </ol>
     * </p>
     */
    @Test
    public void testDataIntegrityUnderWriteFailure() throws InterruptedException {
        PeriodicFaultHandler faultHandler = new PeriodicFaultHandler(4);
        RobustNettyRpcManager rpcManager = new RobustNettyRpcManager(PARTY_NUM, START_PORT + 50, faultHandler);
        RpcImplTestUtils.connectRandom(rpcManager, LONG_TIMEOUT_SECONDS);
        // 发送2MB随机数据并验证完整性
        sendAndReceiveRandomPayload(rpcManager);
        Assert.assertTrue(
            "PeriodicFaultHandler should have triggered at least once during data transfer",
            faultHandler.getFailureTriggered() > 0
        );
        RpcImplTestUtils.disconnectRandom(rpcManager, LONG_TIMEOUT_SECONDS);
    }

    /**
     * 每个参与方发送指定大小的随机数据给其他所有参与方，接收后验证数据完整性。
     * <p>
     * 使用独立的taskId=42和ptoId=99999，避免与{@code sendAndReceiveEmptyPackets}的协议ID冲突。
     * 线程内的断言错误通过{@code AtomicReference}传播到主线程。
     * </p>
     *
     * @param rpcManager RPC管理器
     */
    private void sendAndReceiveRandomPayload(RobustNettyRpcManager rpcManager) throws InterruptedException {
        int partyNum = rpcManager.getPartyNum();
        long taskId = 42L;
        int ptoId = 99999;
        int stepId = 0;
        // 为每个参与方生成随机数据
        SecureRandom secureRandom = new SecureRandom();
        byte[][] partyData = new byte[partyNum][];
        for (int i = 0; i < partyNum; i++) {
            partyData[i] = new byte[DATA_SIZE];
            secureRandom.nextBytes(partyData[i]);
        }
        AtomicReference<Throwable> firstError = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(partyNum);
        for (int partyId = 0; partyId < partyNum; partyId++) {
            Rpc rpc = rpcManager.getRpc(partyId);
            int myId = partyId;
            new Thread(() -> {
                try {
                    // 向其他每个参与方发送随机数据
                    for (Party other : rpc.getPartySet()) {
                        if (!other.equals(rpc.ownParty())) {
                            DataPacketHeader header = new DataPacketHeader(
                                taskId, ptoId, stepId, myId, other.getPartyId()
                            );
                            rpc.send(DataPacket.fromByteArrayList(
                                header, Collections.singletonList(partyData[myId])
                            ));
                        }
                    }
                    // 从其他每个参与方接收数据并验证完整性
                    for (Party other : rpc.getPartySet()) {
                        if (!other.equals(rpc.ownParty())) {
                            DataPacketHeader header = new DataPacketHeader(
                                taskId, ptoId, stepId, other.getPartyId(), myId
                            );
                            DataPacket received = rpc.receive(header);
                            Assert.assertNotNull("received packet should not be null", received);
                            List<byte[]> payload = received.getPayload();
                            Assert.assertEquals("payload should contain exactly 1 element", 1, payload.size());
                            Assert.assertArrayEquals(
                                "data integrity failed: party " + other.getPartyId() + " -> " + myId,
                                partyData[other.getPartyId()], payload.get(0)
                            );
                        }
                    }
                } catch (Throwable t) {
                    firstError.compareAndSet(null, t);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        boolean success = latch.await(LONG_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (firstError.get() != null) {
            Assert.fail("Thread assertion failed: " + firstError.get().getMessage());
        }
        Assert.assertTrue("sendAndReceiveRandomPayload() timeout", success);
    }
}
