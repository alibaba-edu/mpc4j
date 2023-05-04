package edu.alibaba.mpc4j.common.rpc.impl.memory;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketBuffer;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * 内存通信管理器。
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
public class MemoryRpcManager implements RpcManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryRpcManager.class);
    /**
     * 参与方数量
     */
    private final int partyNum;
    /**
     * 参与方集合
     */
    private final Set<MemoryParty> memoryPartySet;
    /**
     * 所有参与方RPC
     */
    private final Map<Integer, MemoryRpc> memoryRpcMap;

    /**
     * 初始化内存通信管理器。
     *
     * @param partyNum 参与方数量。
     */
    public MemoryRpcManager(int partyNum) {
        MathPreconditions.checkGreater("partyNum", partyNum, 1);
        this.partyNum = partyNum;
        // 构建一个统一的数据包缓存区
        DataPacketBuffer dataPacketBuffer = new DataPacketBuffer();
        // 初始化所有参与方
        memoryPartySet = new HashSet<>(partyNum);
        IntStream.range(0, partyNum).forEach(partyId -> {
            MemoryParty memoryParty = new MemoryParty(partyId, getPartyName(partyId));
            memoryPartySet.add(memoryParty);
        });
        // 初始化所有参与方的内存通信
        memoryRpcMap = new HashMap<>(partyNum);
        for (MemoryParty memoryParty : memoryPartySet) {
            MemoryRpc memoryRpc = new MemoryRpc(memoryParty, memoryPartySet, dataPacketBuffer);
            memoryRpcMap.put(memoryRpc.ownParty().getPartyId(), memoryRpc);
            LOGGER.debug("Add memory party: {}", memoryParty);
        }
    }

    @Override
    public Rpc getRpc(int partyId) {
        MathPreconditions.checkNonNegativeInRange("partyId", partyId, partyNum);
        return memoryRpcMap.get(partyId);
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
        return new HashSet<>(memoryPartySet);
    }
}
