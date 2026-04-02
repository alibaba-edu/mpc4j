package edu.alibaba.mpc4j.common.rpc.impl.netty.simple;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.netty.NettyParty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Simple Netty RPC manager, used to create and maintain all SimpleNettyRpc instances.
 *
 * @author Feng Qing, Weiran Liu
 * @date 2020/10/12
 */
public class SimpleNettyRpcManager implements RpcManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleNettyRpcManager.class);
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
    private final Map<Integer, SimpleNettyRpc> nettyRpcMap;

    /**
     * 初始化Netty通信管理器。
     *
     * @param partyNum 参与方数量。
     * @param startPort 起始端口。
     */
    public SimpleNettyRpcManager(int partyNum, int startPort) {
        Preconditions.checkArgument(partyNum > 1, "Number of parties must be greater than 1");
        this.partyNum = partyNum;
        nettyPartySet = new HashSet<>(partyNum);
        nettyRpcMap = new HashMap<>(partyNum);
        // 初始化参与方
        IntStream.range(0, partyNum).forEach(partyId -> {
            NettyParty nettyParty = new NettyParty(
                partyId, getPartyName(partyId), DEFAULT_IP_ADDRESS, startPort + partyId
            );
            nettyPartySet.add(nettyParty);
        });
        // 将所有的NettyRpc对象放到一个集合里
        for (NettyParty nettyParty : nettyPartySet) {
            SimpleNettyRpc simpleNettyRpc = new SimpleNettyRpc(nettyParty, nettyPartySet);
            nettyRpcMap.put(simpleNettyRpc.ownParty().getPartyId(), simpleNettyRpc);
            LOGGER.debug("Add Netty party: {}", nettyParty);
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
