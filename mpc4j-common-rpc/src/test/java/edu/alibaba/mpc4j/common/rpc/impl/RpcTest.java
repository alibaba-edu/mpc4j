package edu.alibaba.mpc4j.common.rpc.impl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.file.FileRpc;
import edu.alibaba.mpc4j.common.rpc.impl.file.FileRpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpc;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.netty.NettyRpc;
import edu.alibaba.mpc4j.common.rpc.impl.netty.NettyRpcManager;
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
 * RPC unit test.
 *
 * @author Weiran Liu
 * @date 2021/12/10
 */
@RunWith(Parameterized.class)
public class RpcTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // MemoryRpc
        configurations.add(new Object[] {MemoryRpc.class.getSimpleName(), new MemoryRpcManager(3),});
        // FileRpc
        configurations.add(new Object[] {FileRpc.class.getSimpleName(), new FileRpcManager(3),});
        // NettyRpc
        configurations.add(new Object[] {NettyRpc.class.getSimpleName(), new NettyRpcManager(3, 8800),});

        return configurations;
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * RPC manager
     */
    private final RpcManager rpcManager;

    public RpcTest(String name, RpcManager rpcManager) {
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
        RpcDataThread[] threads = new RpcDataThread[partyNum];
        // start RPC threads
        for (int partyId = 0; partyId < partyNum; partyId++) {
            threads[partyId] = new RpcDataThread(randomTaskId, rpcManager.getRpc(partyId));
            threads[partyId].start();
        }
        // join
        for (Thread thread : threads) {
            thread.join();
        }
        // empty data packet
        Set<DataPacket> emptySendDataPacketSet = new HashSet<>();
        Set<DataPacket> emptyReceivedDataPacketSet = new HashSet<>();
        for (RpcDataThread thread : threads) {
            emptySendDataPacketSet.addAll(thread.getEmptySendDataPacketSet());
            emptyReceivedDataPacketSet.addAll(thread.getEmptyReceivedDataPacketSet());
        }
        Assert.assertTrue(emptySendDataPacketSet.containsAll(emptyReceivedDataPacketSet));
        Assert.assertTrue(emptyReceivedDataPacketSet.containsAll(emptySendDataPacketSet));
        // length-0 data packet
        Set<DataPacket> zeroLengthSendDataPacketSet = new HashSet<>();
        Set<DataPacket> zeroLengthReceivedDataPacketSet = new HashSet<>();
        for (RpcDataThread thread : threads) {
            zeroLengthSendDataPacketSet.addAll(thread.getZeroLengthSendDataPacketSet());
            zeroLengthReceivedDataPacketSet.addAll(thread.getZeroLengthReceivedDataPacketSet());
        }
        Assert.assertTrue(zeroLengthSendDataPacketSet.containsAll(zeroLengthReceivedDataPacketSet));
        Assert.assertTrue(zeroLengthReceivedDataPacketSet.containsAll(zeroLengthSendDataPacketSet));
        // singleton data packet
        Set<DataPacket> singleSendDataPacketSet = new HashSet<>();
        Set<DataPacket> singleReceivedDataPacketSet = new HashSet<>();
        for (RpcDataThread thread : threads) {
            singleSendDataPacketSet.addAll(thread.getSingleSendDataPacketSet());
            singleReceivedDataPacketSet.addAll(thread.getSingleReceivedDataPacketSet());
        }
        Assert.assertTrue(singleSendDataPacketSet.containsAll(singleReceivedDataPacketSet));
        Assert.assertTrue(singleReceivedDataPacketSet.containsAll(singleSendDataPacketSet));
        // equal-length data packet
        Set<DataPacket> equalLengthSendDataPacketSet = new HashSet<>();
        Set<DataPacket> equalLengthReceivedDataPacketSet = new HashSet<>();
        for (RpcDataThread thread : threads) {
            equalLengthSendDataPacketSet.addAll(thread.getEqualLengthSendDataPacketSet());
            equalLengthReceivedDataPacketSet.addAll(thread.getEqualLengthReceivedDataPacketSet());
        }
        Assert.assertTrue(equalLengthSendDataPacketSet.containsAll(equalLengthReceivedDataPacketSet));
        Assert.assertTrue(equalLengthReceivedDataPacketSet.containsAll(equalLengthSendDataPacketSet));
        // data packet with extra information
        Set<DataPacket> extraInfoSendDataPacketSet = new HashSet<>();
        Set<DataPacket> extraInfoReceivedDataPacketSet = new HashSet<>();
        for (RpcDataThread thread : threads) {
            extraInfoSendDataPacketSet.addAll(thread.getExtraInfoSendDataPacketSet());
            extraInfoReceivedDataPacketSet.addAll(thread.getExtraInfoReceivedDataPacketSet());
        }
        Assert.assertTrue(extraInfoSendDataPacketSet.containsAll(extraInfoReceivedDataPacketSet));
        Assert.assertTrue(extraInfoReceivedDataPacketSet.containsAll(extraInfoSendDataPacketSet));
        // verify the data in memory are different
        DataPacket[] sendAll = extraInfoSendDataPacketSet.toArray(new DataPacket[0]);
        sendAll[0].getPayload().get(0)[0] = (byte) (sendAll[0].getPayload().get(0)[0] + 1);
        Assert.assertFalse(extraInfoReceivedDataPacketSet.contains(sendAll[0]));
        // take any data packet verification
        Set<DataPacket> takeAnySendDataPacketSet = new HashSet<>();
        Set<DataPacket> takeAnyReceivedDataPacketSet = new HashSet<>();
        for (RpcDataThread thread : threads) {
            takeAnySendDataPacketSet.addAll(thread.getTakeAnySendDataPacketSet());
            takeAnyReceivedDataPacketSet.addAll(thread.getTakeAnyReceivedDataPacketSet());
        }
        Assert.assertTrue(takeAnySendDataPacketSet.containsAll(takeAnyReceivedDataPacketSet));
        Assert.assertTrue(takeAnyReceivedDataPacketSet.containsAll(takeAnySendDataPacketSet));
    }
}
