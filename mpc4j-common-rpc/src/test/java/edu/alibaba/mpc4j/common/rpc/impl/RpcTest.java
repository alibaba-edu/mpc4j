package edu.alibaba.mpc4j.common.rpc.impl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.file.FileRpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.netty.NettyRpcManager;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 通信接口测试。
 *
 * @author Weiran Liu
 * @date 2021/12/10
 */
@RunWith(Parameterized.class)
public class RpcTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // MemoryRpc
        configurationParams.add(new Object[] {"MemoryRpc", new MemoryRpcManager(3),});
        // FileRpc
        configurationParams.add(new Object[] {"FileRpc", new FileRpcManager(3),});
        // NettyRpc
        configurationParams.add(new Object[] {"NettyRpc", new NettyRpcManager(3, 8800),});

        return configurationParams;
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 待测试的通信接口管理器
     */
    private final RpcManager rpcManager;

    public RpcTest(String name, RpcManager rpcManager) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.rpcManager = rpcManager;
    }

    @Test
    public void testData() throws InterruptedException {
        int partyNum = rpcManager.getPartyNum();
        // 随机选取一个taskID
        long taskId = Math.abs(SECURE_RANDOM.nextLong());
        // 构建通信线程
        RpcDataThread[] threads = new RpcDataThread[partyNum];
        // 初始化并启动每个RPC线程
        for (int partyId = 0; partyId < partyNum; partyId++) {
            threads[partyId] = new RpcDataThread(taskId, rpcManager.getRpc(partyId));
            threads[partyId].start();
        }
        // 等待线程停止
        for (Thread thread : threads) {
            thread.join();
        }
        // 空数据包验证
        Set<DataPacket> emptySendDataPacketSet = new HashSet<>();
        Set<DataPacket> emptyReceivedDataPacketSet = new HashSet<>();
        for (RpcDataThread thread : threads) {
            emptySendDataPacketSet.addAll(thread.getEmptySendDataPacketSet());
            emptyReceivedDataPacketSet.addAll(thread.getEmptyReceivedDataPacketSet());
        }
        Assert.assertTrue(emptySendDataPacketSet.containsAll(emptyReceivedDataPacketSet));
        Assert.assertTrue(emptyReceivedDataPacketSet.containsAll(emptySendDataPacketSet));
        // 长度为0数据包验证
        Set<DataPacket> zeroLengthSendDataPacketSet = new HashSet<>();
        Set<DataPacket> zeroLengthReceivedDataPacketSet = new HashSet<>();
        for (RpcDataThread thread : threads) {
            zeroLengthSendDataPacketSet.addAll(thread.getZeroLengthSendDataPacketSet());
            zeroLengthReceivedDataPacketSet.addAll(thread.getZeroLengthReceivedDataPacketSet());
        }
        Assert.assertTrue(zeroLengthSendDataPacketSet.containsAll(zeroLengthReceivedDataPacketSet));
        Assert.assertTrue(zeroLengthReceivedDataPacketSet.containsAll(zeroLengthSendDataPacketSet));
        // 单条数据包验证
        Set<DataPacket> singleSendDataPacketSet = new HashSet<>();
        Set<DataPacket> singleReceivedDataPacketSet = new HashSet<>();
        for (RpcDataThread thread : threads) {
            singleSendDataPacketSet.addAll(thread.getSingleSendDataPacketSet());
            singleReceivedDataPacketSet.addAll(thread.getSingleReceivedDataPacketSet());
        }
        Assert.assertTrue(singleSendDataPacketSet.containsAll(singleReceivedDataPacketSet));
        Assert.assertTrue(singleReceivedDataPacketSet.containsAll(singleSendDataPacketSet));
        // 额外信息数据包验证
        Set<DataPacket> extraInfoSendDataPacketSet = new HashSet<>();
        Set<DataPacket> extraInfoReceivedDataPacketSet = new HashSet<>();
        for (RpcDataThread thread : threads) {
            extraInfoSendDataPacketSet.addAll(thread.getExtraInfoSendDataPacketSet());
            extraInfoReceivedDataPacketSet.addAll(thread.getExtraInfoReceivedDataPacketSet());
        }
        Assert.assertTrue(extraInfoSendDataPacketSet.containsAll(extraInfoReceivedDataPacketSet));
        Assert.assertTrue(extraInfoReceivedDataPacketSet.containsAll(extraInfoSendDataPacketSet));
    }
}
