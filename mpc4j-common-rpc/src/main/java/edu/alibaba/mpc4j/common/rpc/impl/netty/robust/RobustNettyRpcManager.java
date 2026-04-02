package edu.alibaba.mpc4j.common.rpc.impl.netty.robust;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.netty.NettyParty;
import io.netty.channel.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Robust Netty RPC manager, used to create and maintain all RobustNettyRpc instances.
 *
 * @author Weiran Liu
 * @date 2026/04/02
 */
public class RobustNettyRpcManager implements RpcManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RobustNettyRpcManager.class);
    /**
     * 默认IP地址，本地测试时都使用本地地址
     */
    private static final String DEFAULT_IP_ADDRESS = "127.0.0.1";
    /**
     * 总参与方数量
     */
    private final int partyNum;
    /**
     * 参与方集合
     */
    private final Set<NettyParty> nettyPartySet;
    /**
     * 所有参与方RPC
     */
    private final Map<Integer, RobustNettyRpc> nettyRpcMap;

    /**
     * 初始化Robust Netty通信管理器（不带额外Handler）。
     *
     * @param partyNum  参与方数量。
     * @param startPort 起始端口。
     */
    public RobustNettyRpcManager(int partyNum, int startPort) {
        this(partyNum, startPort, null);
    }

    /**
     * 初始化Robust Netty通信管理器。
     *
     * @param partyNum     参与方数量。
     * @param startPort    起始端口。
     * @param extraHandler 可选的发送管道额外Handler（如测试用的故障注入Handler）；传null则不注入。
     */
    public RobustNettyRpcManager(int partyNum, int startPort, ChannelHandler extraHandler) {
        Preconditions.checkArgument(partyNum > 1, "Number of parties must be greater than 1");
        this.partyNum = partyNum;
        nettyPartySet = new HashSet<>(partyNum);
        nettyRpcMap = new HashMap<>(partyNum);
        IntStream.range(0, partyNum).forEach(partyId -> {
            NettyParty nettyParty = new NettyParty(
                partyId, getPartyName(partyId), DEFAULT_IP_ADDRESS, startPort + partyId
            );
            nettyPartySet.add(nettyParty);
        });
        for (NettyParty nettyParty : nettyPartySet) {
            RobustNettyRpc robustNettyRpc = new RobustNettyRpc(nettyParty, nettyPartySet, extraHandler);
            nettyRpcMap.put(robustNettyRpc.ownParty().getPartyId(), robustNettyRpc);
            LOGGER.debug("Add Robust Netty party: {}", nettyParty);
        }
    }

    @Override
    public Rpc getRpc(int partyId) {
        Preconditions.checkArgument(
            partyId >= 0 && partyId < partyNum, "Party ID must be in range [0, %s)", partyNum
        );
        return nettyRpcMap.get(partyId);
    }

    private String getPartyName(int partyId) {
        return "P_" + (partyId + 1);
    }

    @Override
    public int getPartyNum() {
        return partyNum;
    }

    @Override
    public Set<Party> getPartySet() {
        return new HashSet<>(nettyPartySet);
    }
}
