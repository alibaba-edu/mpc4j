package edu.alibaba.mpc4j.common.rpc;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.impl.netty.NettyParty;
import edu.alibaba.mpc4j.common.rpc.impl.netty.NettyRpc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;

import java.util.*;

/**
 * 通信接口设置工具类。
 *
 * @author Weiran Liu
 * @date 2022/8/28
 */
public class RpcPropertiesUtils {

    private RpcPropertiesUtils() {
        // empty
    }

    /**
     * reads and sets Netty RPC.
     *
     * @param properties  properties.
     * @param ownName     own name.
     * @param partyPrefix the prefixes of the parties.
     * @return Netty RPC.
     */
    public static Rpc readNettyRpcWithOwnName(Properties properties, String ownName, String... partyPrefix) {
        MathPreconditions.checkGreater("# of parties", partyPrefix.length, 1);
        int partyNum = partyPrefix.length;
        Set<NettyParty> nettyPartySet = new HashSet<>(partyNum);
        Map<String, NettyParty> nettyPartyMap = new HashMap<>(partyNum);
        for (int partyIndex = 0; partyIndex < partyNum; partyIndex++) {
            String name = PropertiesUtils.readString(properties, partyPrefix[partyIndex] + "_name");
            String ip = PropertiesUtils.readString(properties, partyPrefix[partyIndex] + "_ip");
            int port = PropertiesUtils.readInt(properties, partyPrefix[partyIndex] + "_port");
            NettyParty nettyParty = new NettyParty(partyIndex, name, ip, port);
            nettyPartySet.add(nettyParty);
            nettyPartyMap.put(name, nettyParty);
        }
        NettyParty ownParty = Preconditions.checkNotNull(
            nettyPartyMap.get(ownName), "ownName must be in %s: %s", Arrays.toString(partyPrefix), ownName
        );
        return new NettyRpc(ownParty, nettyPartySet);
    }
}
