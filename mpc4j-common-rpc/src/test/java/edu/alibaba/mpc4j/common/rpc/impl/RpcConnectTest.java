package edu.alibaba.mpc4j.common.rpc.impl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.file.FileRpc;
import edu.alibaba.mpc4j.common.rpc.impl.file.FileRpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpc;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.netty.SimpleNettyRpc;
import edu.alibaba.mpc4j.common.rpc.impl.netty.SimpleNettyRpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.RpcTestPtoDesc.PtoStep;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * RPC connect/disconnect 重连测试。
 * <p>
 * 测试 5 个参与方的 connect/disconnect 循环，验证以下场景：
 * <ul>
 *   <li>随机顺序并发 connect/disconnect</li>
 *   <li>从小到大顺序 connect/disconnect</li>
 *   <li>从大到小顺序 connect/disconnect</li>
 *   <li>重复 connect/disconnect 保护</li>
 *   <li>重连（disconnect 后再 connect）</li>
 * </ul>
 * </p>
 *
 * @author Weiran Liu
 * @date 2026/04/02
 */
@RunWith(Parameterized.class)
public class RpcConnectTest {
    /**
     * 参与方数量
     */
    private static final int PARTY_NUM = 5;
    /**
     * 起始端口（选择不常用的端口范围，避免与其他测试冲突）
     */
    private static final int START_PORT = 9500;
    /**
     * 连接超时时间（秒）
     */
    private static final int CONNECT_TIMEOUT_SECONDS = 15;
    /**
     * 重连间隔时间（毫秒），确保端口完全释放
     */
    private static final long RECONNECT_DELAY_MS = 500;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // MemoryRpc
        configurations.add(new Object[]{MemoryRpc.class.getSimpleName(), new MemoryRpcManager(PARTY_NUM),});
        // FileRpc
        configurations.add(new Object[]{FileRpc.class.getSimpleName(), new FileRpcManager(PARTY_NUM),});
        // NettyRpc
        configurations.add(new Object[]{SimpleNettyRpc.class.getSimpleName(), new SimpleNettyRpcManager(PARTY_NUM, START_PORT),});

        return configurations;
    }

    /**
     * RPC manager
     */
    private final RpcManager rpcManager;

    public RpcConnectTest(String name, RpcManager rpcManager) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.rpcManager = rpcManager;
    }

    @Test
    public void testSingleConnectDisconnect() throws InterruptedException {
        connectRandom();
        sendAndReceiveEmptyPackets();
        disconnectRandom();
    }

    @Test
    public void testDoubleConnect() throws InterruptedException {
        connectRandom();
        // 再次 connect 应该被忽略（只打印警告日志）
        connectRandom();
        sendAndReceiveEmptyPackets();
        disconnectRandom();
    }

    @Test
    public void testDoubleDisconnect() throws InterruptedException {
        connectRandom();
        sendAndReceiveEmptyPackets();
        disconnectRandom();
        // 再次 disconnect 应该被忽略（只打印警告日志）
        disconnectRandom();
    }

    @Test
    public void testReconnect() throws InterruptedException {
        // 核心测试：connect -> disconnect -> connect -> disconnect（并发随机顺序）
        connectRandom();
        sendAndReceiveEmptyPackets();
        disconnectRandom();
        // 等待端口完全释放
        Thread.sleep(RECONNECT_DELAY_MS);
        // 重连
        connectRandom();
        sendAndReceiveEmptyPackets();
        disconnectRandom();
    }

    @Test
    public void testAscendingConnectDisconnect() throws InterruptedException {
        connectAscending();
        sendAndReceiveEmptyPackets();
        disconnectAscending();
    }

    @Test
    public void testDescendingConnectDisconnect() throws InterruptedException {
        connectDescending();
        sendAndReceiveEmptyPackets();
        disconnectDescending();
    }

    @Test
    public void testAscendingReconnect() throws InterruptedException {
        // 从小到大顺序 connect -> disconnect -> connect -> disconnect
        connectAscending();
        sendAndReceiveEmptyPackets();
        disconnectAscending();
        Thread.sleep(RECONNECT_DELAY_MS);
        connectAscending();
        sendAndReceiveEmptyPackets();
        disconnectAscending();
    }

    @Test
    public void testDescendingReconnect() throws InterruptedException {
        // 从大到小顺序 connect -> disconnect -> connect -> disconnect
        connectDescending();
        sendAndReceiveEmptyPackets();
        disconnectDescending();
        Thread.sleep(RECONNECT_DELAY_MS);
        connectDescending();
        sendAndReceiveEmptyPackets();
        disconnectDescending();
    }

    /**
     * 所有参与方并发执行 connect（随机顺序）。
     */
    private void connectRandom() throws InterruptedException {
        int partyNum = rpcManager.getPartyNum();
        CountDownLatch latch = new CountDownLatch(partyNum);
        for (int partyId = 0; partyId < partyNum; partyId++) {
            Rpc rpc = rpcManager.getRpc(partyId);
            new Thread(() -> {
                try {
                    rpc.connect();
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        boolean success = latch.await(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertTrue("connect() timeout", success);
    }

    /**
     * 所有参与方并发执行 disconnect（随机顺序）。
     */
    private void disconnectRandom() throws InterruptedException {
        int partyNum = rpcManager.getPartyNum();
        CountDownLatch latch = new CountDownLatch(partyNum);
        for (int partyId = 0; partyId < partyNum; partyId++) {
            Rpc rpc = rpcManager.getRpc(partyId);
            new Thread(() -> {
                try {
                    rpc.disconnect();
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        boolean success = latch.await(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertTrue("disconnect() timeout", success);
    }

    /**
     * 从小到大顺序依次执行 connect（partyId 0, 1, 2, ..., partyNum-1）。
     * <p>
     * 顺序启动时，小 partyId（client）先启动，大 partyId（server）后启动。
     * 由于 connect() 的握手协议有重试机制，server 后启动不影响正确性。
     * </p>
     */
    private void connectAscending() throws InterruptedException {
        int partyNum = rpcManager.getPartyNum();
        CountDownLatch latch = new CountDownLatch(partyNum);
        for (int partyId = 0; partyId < partyNum; partyId++) {
            Rpc rpc = rpcManager.getRpc(partyId);
            new Thread(() -> {
                try {
                    rpc.connect();
                } finally {
                    latch.countDown();
                }
            }).start();
            // 每个参与方启动后稍等一会，形成先小后大的顺序
            Thread.sleep(50);
        }
        boolean success = latch.await(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertTrue("connect() ascending timeout", success);
    }

    /**
     * 从小到大顺序依次执行 disconnect（partyId 0, 1, 2, ..., partyNum-1）。
     */
    private void disconnectAscending() throws InterruptedException {
        int partyNum = rpcManager.getPartyNum();
        CountDownLatch latch = new CountDownLatch(partyNum);
        for (int partyId = 0; partyId < partyNum; partyId++) {
            Rpc rpc = rpcManager.getRpc(partyId);
            new Thread(() -> {
                try {
                    rpc.disconnect();
                } finally {
                    latch.countDown();
                }
            }).start();
            Thread.sleep(50);
        }
        boolean success = latch.await(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertTrue("disconnect() ascending timeout", success);
    }

    /**
     * 从大到小顺序依次执行 connect（partyId partyNum-1, ..., 1, 0）。
     * <p>
     * 逆序启动时，大 partyId（server）先启动，小 partyId（client）后启动。
     * server 先就绪，等待 client 连接，符合常见部署场景。
     * </p>
     */
    private void connectDescending() throws InterruptedException {
        int partyNum = rpcManager.getPartyNum();
        CountDownLatch latch = new CountDownLatch(partyNum);
        for (int partyId = partyNum - 1; partyId >= 0; partyId--) {
            Rpc rpc = rpcManager.getRpc(partyId);
            new Thread(() -> {
                try {
                    rpc.connect();
                } finally {
                    latch.countDown();
                }
            }).start();
            // 每个参与方启动后稍等一会，形成先大后小的顺序
            Thread.sleep(50);
        }
        boolean success = latch.await(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertTrue("connect() descending timeout", success);
    }

    /**
     * 从大到小顺序依次执行 disconnect（partyId partyNum-1, ..., 1, 0）。
     */
    private void disconnectDescending() throws InterruptedException {
        int partyNum = rpcManager.getPartyNum();
        CountDownLatch latch = new CountDownLatch(partyNum);
        for (int partyId = partyNum - 1; partyId >= 0; partyId--) {
            Rpc rpc = rpcManager.getRpc(partyId);
            new Thread(() -> {
                try {
                    rpc.disconnect();
                } finally {
                    latch.countDown();
                }
            }).start();
            Thread.sleep(50);
        }
        boolean success = latch.await(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertTrue("disconnect() descending timeout", success);
    }

    /**
     * 验证 connect 后通信正常：每个参与方给其他所有参与方各发一个空数据包并接收。
     * <p>
     * 每个参与方启动一个线程，先发送再接收，通过 CountDownLatch 等待全部完成。
     * </p>
     */
    private void sendAndReceiveEmptyPackets() throws InterruptedException {
        int partyNum = rpcManager.getPartyNum();
        // taskId 固定为 0，仅用于握手验证，不与业务 taskId 冲突
        int taskId = 0;
        CountDownLatch latch = new CountDownLatch(partyNum);
        for (int partyId = 0; partyId < partyNum; partyId++) {
            Rpc rpc = rpcManager.getRpc(partyId);
            new Thread(() -> {
                try {
                    // 向其他每个参与方发送一个空数据包
                    for (Party other : rpc.getPartySet()) {
                        if (!other.equals(rpc.ownParty())) {
                            DataPacketHeader sendHeader = new DataPacketHeader(
                                taskId, RpcTestPtoDesc.getInstance().getPtoId(), PtoStep.EMPTY.ordinal(),
                                rpc.ownParty().getPartyId(), other.getPartyId()
                            );
                            rpc.send(DataPacket.fromByteArrayList(sendHeader, new LinkedList<>()));
                        }
                    }
                    // 从其他每个参与方接收一个空数据包
                    for (Party other : rpc.getPartySet()) {
                        if (!other.equals(rpc.ownParty())) {
                            DataPacketHeader recvHeader = new DataPacketHeader(
                                taskId, RpcTestPtoDesc.getInstance().getPtoId(), PtoStep.EMPTY.ordinal(),
                                other.getPartyId(), rpc.ownParty().getPartyId()
                            );
                            DataPacket received = rpc.receive(recvHeader);
                            Assert.assertNotNull("received packet should not be null", received);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        boolean success = latch.await(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertTrue("sendAndReceiveEmptyPackets() timeout", success);
    }
}
