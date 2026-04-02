package edu.alibaba.mpc4j.common.rpc.impl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.file.FileRpc;
import edu.alibaba.mpc4j.common.rpc.impl.file.FileRpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpc;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.netty.robust.RobustNettyRpc;
import edu.alibaba.mpc4j.common.rpc.impl.netty.robust.RobustNettyRpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.netty.simple.SimpleNettyRpc;
import edu.alibaba.mpc4j.common.rpc.impl.netty.simple.SimpleNettyRpcManager;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

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
     * 连接超时时间（秒）
     */
    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    /**
     * 重连间隔时间（毫秒），确保端口完全释放
     */
    private static final long RECONNECT_DELAY_MS = 500;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RobustNettyRpc
        configurations.add(new Object[] {RobustNettyRpc.class.getSimpleName(), new RobustNettyRpcManager(PARTY_NUM, 10000),});
        // SimpleNettyRpc
        configurations.add(new Object[] {SimpleNettyRpc.class.getSimpleName(), new SimpleNettyRpcManager(PARTY_NUM, 9000),});
        // FileRpc
        configurations.add(new Object[]{FileRpc.class.getSimpleName(), new FileRpcManager(PARTY_NUM),});
        // MemoryRpc
        configurations.add(new Object[]{MemoryRpc.class.getSimpleName(), new MemoryRpcManager(PARTY_NUM),});

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
        RpcImplTestUtils.connectRandom(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.sendAndReceiveEmptyPackets(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.disconnectRandom(rpcManager, CONNECT_TIMEOUT_SECONDS);
    }

    @Test
    public void testDoubleConnect() throws InterruptedException {
        RpcImplTestUtils.connectRandom(rpcManager, CONNECT_TIMEOUT_SECONDS);
        // 再次 connect 应该被忽略（只打印警告日志）
        RpcImplTestUtils.connectRandom(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.sendAndReceiveEmptyPackets(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.disconnectRandom(rpcManager, CONNECT_TIMEOUT_SECONDS);
    }

    @Test
    public void testDoubleDisconnect() throws InterruptedException {
        RpcImplTestUtils.connectRandom(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.sendAndReceiveEmptyPackets(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.disconnectRandom(rpcManager, CONNECT_TIMEOUT_SECONDS);
        // 再次 disconnect 应该被忽略（只打印警告日志）
        RpcImplTestUtils.disconnectRandom(rpcManager, CONNECT_TIMEOUT_SECONDS);
    }

    @Test
    public void testReconnect() throws InterruptedException {
        // 核心测试：connect -> disconnect -> connect -> disconnect（并发随机顺序）
        RpcImplTestUtils.connectRandom(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.sendAndReceiveEmptyPackets(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.disconnectRandom(rpcManager, CONNECT_TIMEOUT_SECONDS);
        // 等待端口完全释放
        Thread.sleep(RECONNECT_DELAY_MS);
        // 重连
        RpcImplTestUtils.connectRandom(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.sendAndReceiveEmptyPackets(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.disconnectRandom(rpcManager, CONNECT_TIMEOUT_SECONDS);
    }

    @Test
    public void testAscendingConnectDisconnect() throws InterruptedException {
        RpcImplTestUtils.connectAscending(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.sendAndReceiveEmptyPackets(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.disconnectAscending(rpcManager, CONNECT_TIMEOUT_SECONDS);
    }

    @Test
    public void testDescendingConnectDisconnect() throws InterruptedException {
        RpcImplTestUtils.connectDescending(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.sendAndReceiveEmptyPackets(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.disconnectDescending(rpcManager, CONNECT_TIMEOUT_SECONDS);
    }

    @Test
    public void testAscendingReconnect() throws InterruptedException {
        // 从小到大顺序 connect -> disconnect -> connect -> disconnect
        RpcImplTestUtils.connectAscending(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.sendAndReceiveEmptyPackets(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.disconnectAscending(rpcManager, CONNECT_TIMEOUT_SECONDS);
        Thread.sleep(RECONNECT_DELAY_MS);
        RpcImplTestUtils.connectAscending(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.sendAndReceiveEmptyPackets(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.disconnectAscending(rpcManager, CONNECT_TIMEOUT_SECONDS);
    }

    @Test
    public void testDescendingReconnect() throws InterruptedException {
        // 从大到小顺序 connect -> disconnect -> connect -> disconnect
        RpcImplTestUtils.connectDescending(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.sendAndReceiveEmptyPackets(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.disconnectDescending(rpcManager, CONNECT_TIMEOUT_SECONDS);
        Thread.sleep(RECONNECT_DELAY_MS);
        RpcImplTestUtils.connectDescending(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.sendAndReceiveEmptyPackets(rpcManager, CONNECT_TIMEOUT_SECONDS);
        RpcImplTestUtils.disconnectDescending(rpcManager, CONNECT_TIMEOUT_SECONDS);
    }
}
