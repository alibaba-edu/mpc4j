package edu.alibaba.mpc4j.common.rpc.impl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.file.FileRpc;
import edu.alibaba.mpc4j.common.rpc.impl.file.FileRpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpc;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.netty.simple.SimpleNettyRpc;
import edu.alibaba.mpc4j.common.rpc.impl.netty.simple.SimpleNettyRpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.netty.robust.RobustNettyRpc;
import edu.alibaba.mpc4j.common.rpc.impl.netty.robust.RobustNettyRpcManager;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * RPC send/receive test.
 *
 * @author Weiran Liu
 * @date 2021/12/10
 */
@RunWith(Parameterized.class)
public class RpcSendRecvTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RobustNettyRpc
        configurations.add(new Object[] {RobustNettyRpc.class.getSimpleName(), new RobustNettyRpcManager(3, 10100),});
        // SimpleNettyRpc
        configurations.add(new Object[] {SimpleNettyRpc.class.getSimpleName(), new SimpleNettyRpcManager(3, 9100),});
        // FileRpc
        configurations.add(new Object[] {FileRpc.class.getSimpleName(), new FileRpcManager(3),});
        // MemoryRpc
        configurations.add(new Object[] {MemoryRpc.class.getSimpleName(), new MemoryRpcManager(3),});

        return configurations;
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * RPC manager
     */
    private final RpcManager rpcManager;

    public RpcSendRecvTest(String name, RpcManager rpcManager) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.rpcManager = rpcManager;
    }

    @Before
    public void connect() throws InterruptedException {
        int partyNum = rpcManager.getPartyNum();
        for (int partyId = 0; partyId < partyNum; partyId++) {
            Rpc partyRpc = rpcManager.getRpc(partyId);
            new Thread(partyRpc::connect).start();
            Thread.sleep(100);
        }
    }

    @After
    public void disconnect() {
        int partyNum = rpcManager.getPartyNum();
        for (int partyId = 0; partyId < partyNum; partyId++) {
            Rpc partyRpc = rpcManager.getRpc(partyId);
            new Thread(partyRpc::disconnect).start();
        }
    }

    @Test
    public void testData() throws InterruptedException {
        int partyNum = rpcManager.getPartyNum();
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        RpcSendRecvThread[] threads = new RpcSendRecvThread[partyNum];
        // start RPC threads
        for (int partyId = 0; partyId < partyNum; partyId++) {
            threads[partyId] = new RpcSendRecvThread(randomTaskId, rpcManager.getRpc(partyId));
            threads[partyId].start();
        }
        // join
        for (Thread thread : threads) {
            thread.join();
        }
        // check data packet
        Set<DataPacket> sendDataPacketSet = new HashSet<>();
        Set<DataPacket> receivedDataPacketSet = new HashSet<>();
        for (RpcSendRecvThread thread : threads) {
            sendDataPacketSet.addAll(thread.getSendDataPacketSet());
            receivedDataPacketSet.addAll(thread.getReceivedDataPacketSet());
        }
        Assert.assertTrue(sendDataPacketSet.containsAll(receivedDataPacketSet));
        Assert.assertTrue(receivedDataPacketSet.containsAll(sendDataPacketSet));
    }
}
