package edu.alibaba.mpc4j.common.rpc.impl.netty.simple;

import edu.alibaba.mpc4j.common.rpc.impl.RpcImplTestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * SimpleNettyRpc 专项测试。
 *
 * @author Weiran Liu
 * @date 2026/04/02
 */
public class SimpleNettyRpcTest {
    /**
     * 参与方数量
     */
    private static final int PARTY_NUM = 5;
    /**
     * 起始端口
     */
    private static final int START_PORT = 9500;
    /**
     * 超时时间（秒）
     */
    private static final int TIMEOUT_SECONDS = 15;

    /**
     * 验证：所有参与方全部 connect 后，只让前半部分调用 disconnect()，预期 disconnect() 超时。
     * <p>
     * disconnect() 握手协议要求配对方响应 FINISH 包（CLIENT_FINISH / SERVER_FINISH）。
     * 后半部分参与方仍处于通信状态，不会发出 FINISH 包，导致前半部分的 disconnect() 握手
     * 永久阻塞，直到超时。
     * </p>
     * <p>
     * 由于测试结束时挂起的线程无法被干净地终止，此测试类不包含其他测试用例，由 JVM 在
     * 测试结束后自然回收所有资源。
     * </p>
     */
    @Test
    public void testPartialDisconnectTimeout() throws InterruptedException {
        SimpleNettyRpcManager rpcManager = new SimpleNettyRpcManager(PARTY_NUM, START_PORT);
        int halfNum = PARTY_NUM / 2;
        // 第一步：所有参与方全部 connect，并验证连通性
        RpcImplTestUtils.connectRandom(rpcManager, TIMEOUT_SECONDS);
        RpcImplTestUtils.sendAndReceiveEmptyPackets(rpcManager, TIMEOUT_SECONDS);
        // 第二步：只让前半部分（partyId 0 .. halfNum-1）disconnect
        // 后半部分还连着，不会响应 CLIENT_FINISH，所以前半部分的 disconnect() 会挂住
        CountDownLatch disconnectLatch = new CountDownLatch(halfNum);
        List<Thread> disconnectThreads = new ArrayList<>();
        for (int partyId = 0; partyId < halfNum; partyId++) {
            final int id = partyId;
            Thread t = new Thread(() -> {
                try {
                    rpcManager.getRpc(id).disconnect();
                } finally {
                    disconnectLatch.countDown();
                }
            });
            disconnectThreads.add(t);
            t.start();
        }
        // 预期：前半部分的 disconnect() 超时（后半部分不会响应 FINISH 包）
        boolean partialDisconnectTimedOut = !disconnectLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertTrue(
            "Expected partial disconnect() to timeout when other parties are still connected, but it succeeded",
            partialDisconnectTimedOut
        );
        // 测试结论已验证，中断挂起的线程，让 JVM 在测试结束后回收资源
        for (Thread t : disconnectThreads) {
            t.interrupt();
        }
    }
}
