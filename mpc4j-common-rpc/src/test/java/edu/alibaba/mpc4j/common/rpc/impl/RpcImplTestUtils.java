package edu.alibaba.mpc4j.common.rpc.impl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.RpcImplTestPtoDesc.PtoStep;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import org.junit.Assert;

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * RPC implementation test utility class.
 *
 * @author Weiran Liu
 */
public class RpcImplTestUtils {
    /**
     * private constructor.
     */
    private RpcImplTestUtils() {
        // empty
    }

    /**
     * 所有参与方并发执行 connect（随机顺序）。
     *
     * @param rpcManager     RPC 管理器。
     * @param timeoutSeconds 超时时间（秒）。
     * @throws InterruptedException 线程被中断。
     */
    public static void connectRandom(RpcManager rpcManager, int timeoutSeconds) throws InterruptedException {
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
        boolean success = latch.await(timeoutSeconds, TimeUnit.SECONDS);
        Assert.assertTrue("connect() timeout", success);
    }

    /**
     * 所有参与方并发执行 disconnect（随机顺序）。
     *
     * @param rpcManager     RPC 管理器。
     * @param timeoutSeconds 超时时间（秒）。
     * @throws InterruptedException 线程被中断。
     */
    public static void disconnectRandom(RpcManager rpcManager, int timeoutSeconds) throws InterruptedException {
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
        boolean success = latch.await(timeoutSeconds, TimeUnit.SECONDS);
        Assert.assertTrue("disconnect() timeout", success);
    }

    /**
     * 从小到大顺序依次执行 connect（partyId 0, 1, 2, ..., partyNum-1）。
     * <p>
     * 顺序启动时，小 partyId（client）先启动，大 partyId（server）后启动。
     * 由于 connect() 的握手协议有重试机制，server 后启动不影响正确性。
     * </p>
     *
     * @param rpcManager     RPC 管理器。
     * @param timeoutSeconds 超时时间（秒）。
     * @throws InterruptedException 线程被中断。
     */
    public static void connectAscending(RpcManager rpcManager, int timeoutSeconds) throws InterruptedException {
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
        boolean success = latch.await(timeoutSeconds, TimeUnit.SECONDS);
        Assert.assertTrue("connect() ascending timeout", success);
    }

    /**
     * 从小到大顺序依次执行 disconnect（partyId 0, 1, 2, ..., partyNum-1）。
     *
     * @param rpcManager     RPC 管理器。
     * @param timeoutSeconds 超时时间（秒）。
     * @throws InterruptedException 线程被中断。
     */
    public static void disconnectAscending(RpcManager rpcManager, int timeoutSeconds) throws InterruptedException {
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
        boolean success = latch.await(timeoutSeconds, TimeUnit.SECONDS);
        Assert.assertTrue("disconnect() ascending timeout", success);
    }

    /**
     * 从大到小顺序依次执行 connect（partyId partyNum-1, ..., 1, 0）。
     * <p>
     * 逆序启动时，大 partyId（server）先启动，小 partyId（client）后启动。
     * server 先就绪，等待 client 连接，符合常见部署场景。
     * </p>
     *
     * @param rpcManager     RPC 管理器。
     * @param timeoutSeconds 超时时间（秒）。
     * @throws InterruptedException 线程被中断。
     */
    public static void connectDescending(RpcManager rpcManager, int timeoutSeconds) throws InterruptedException {
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
        boolean success = latch.await(timeoutSeconds, TimeUnit.SECONDS);
        Assert.assertTrue("connect() descending timeout", success);
    }

    /**
     * 从大到小顺序依次执行 disconnect（partyId partyNum-1, ..., 1, 0）。
     *
     * @param rpcManager     RPC 管理器。
     * @param timeoutSeconds 超时时间（秒）。
     * @throws InterruptedException 线程被中断。
     */
    public static void disconnectDescending(RpcManager rpcManager, int timeoutSeconds) throws InterruptedException {
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
        boolean success = latch.await(timeoutSeconds, TimeUnit.SECONDS);
        Assert.assertTrue("disconnect() descending timeout", success);
    }

    /**
     * 验证 connect 后通信正常：每个参与方给其他所有参与方各发一个空数据包并接收。
     * <p>
     * 每个参与方启动一个线程，先发送再接收，通过 CountDownLatch 等待全部完成。
     * taskId 固定为 0，仅用于连通性验证，不与业务 taskId 冲突。
     * </p>
     *
     * @param rpcManager     RPC 管理器。
     * @param timeoutSeconds 超时时间（秒）。
     * @throws InterruptedException 线程被中断。
     */
    public static void sendAndReceiveEmptyPackets(RpcManager rpcManager, int timeoutSeconds) throws InterruptedException {
        int partyNum = rpcManager.getPartyNum();
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
                                taskId, RpcImplTestPtoDesc.getInstance().getPtoId(), PtoStep.EMPTY.ordinal(),
                                rpc.ownParty().getPartyId(), other.getPartyId()
                            );
                            rpc.send(DataPacket.fromByteArrayList(sendHeader, new LinkedList<>()));
                        }
                    }
                    // 从其他每个参与方接收一个空数据包
                    for (Party other : rpc.getPartySet()) {
                        if (!other.equals(rpc.ownParty())) {
                            DataPacketHeader recvHeader = new DataPacketHeader(
                                taskId, RpcImplTestPtoDesc.getInstance().getPtoId(), PtoStep.EMPTY.ordinal(),
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
        boolean success = latch.await(timeoutSeconds, TimeUnit.SECONDS);
        Assert.assertTrue("sendAndReceiveEmptyPackets() timeout", success);
    }
}
