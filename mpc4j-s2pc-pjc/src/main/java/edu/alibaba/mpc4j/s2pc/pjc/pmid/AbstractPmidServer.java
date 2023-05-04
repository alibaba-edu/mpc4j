package edu.alibaba.mpc4j.s2pc.pjc.pmid;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PMID协议服务端抽象类。
 *
 * @author Weiran Liu
 * @date 2022/5/6
 */
public abstract class AbstractPmidServer<T> extends AbstractTwoPartyPto implements PmidServer<T> {
    /**
     * 服务端集合最大数量
     */
    protected int maxServerSetSize;
    /**
     * 服务端最大重复元素上界
     */
    protected int maxServerU;
    /**
     * 客户端集合最大数量
     */
    protected int maxClientSetSize;
    /**
     * 客户端最大重复元素上界
     */
    protected int maxClientU;
    /**
     * 服务端元素映射
     */
    protected Map<T, Integer> serverElementMap;
    /**
     * 服务端元素列表
     */
    protected ArrayList<T> serverElementArrayList;
    /**
     * 服务端元素数量
     */
    protected int serverSetSize;
    /**
     * 服务端重复元素上界（只用于服务端有重复元素的情况）
     */
    protected int serverU;
    /**
     * 客户端元素数量
     */
    protected int clientSetSize;
    /**
     * 客户端重复元素上界
     */
    protected int clientU;

    protected AbstractPmidServer(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, PmidConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxServerSetSize, int maxServerU, int maxClientSetSize, int maxClientU) {
        MathPreconditions.checkGreater("maxServerSetSize", maxServerSetSize, 1);
        this.maxServerSetSize = maxServerSetSize;
        MathPreconditions.checkPositive("maxServerU", maxServerU);
        this.maxServerU = maxServerU;
        MathPreconditions.checkGreater("maxClientSetSize", maxClientSetSize, 1);
        this.maxClientSetSize = maxClientSetSize;
        MathPreconditions.checkPositive("maxClientU", maxClientU);
        this.maxClientU = maxClientU;
        initState();
    }

    protected void setPtoInput(Set<T> serverElementSet, int clientSetSize) {
        setPtoInput(serverElementSet, clientSetSize, 1);
    }

    protected void setPtoInput(Set<T> serverElementSet, int clientSetSize, int clientU) {
        Map<T, Integer> serverElementMap = serverElementSet.stream()
            .collect(Collectors.toMap(
                    element -> element,
                    element -> 1
                )
            );
        setPtoInput(serverElementMap, clientSetSize, clientU);
    }

    protected void setPtoInput(Map<T, Integer> serverElementMap, int clientSetSize) {
        setPtoInput(serverElementMap, clientSetSize, 1);
    }

    protected void setPtoInput(Map<T, Integer> serverElementMap, int clientSetSize, int clientU) {
        checkInitialized();
        Set<T> serverElementSet = serverElementMap.keySet();
        MathPreconditions.checkGreater("serverSetSize", serverElementSet.size(), 1);
        MathPreconditions.checkLessOrEqual("serverSetSize", serverElementSet.size(), maxServerSetSize);
        serverElementArrayList = new ArrayList<>(serverElementSet);
        serverSetSize = serverElementSet.size();
        this.serverElementMap = serverElementMap;
        serverU = serverElementSet.stream()
            .mapToInt(serverElementMap::get)
            .peek(ux -> MathPreconditions.checkPositive("ux", ux))
            .max()
            .orElse(0);
        MathPreconditions.checkPositiveInRangeClosed("serverU", serverU, maxServerU);
        MathPreconditions.checkGreater("clientSetSize", clientSetSize, 1);
        MathPreconditions.checkLessOrEqual("clientSetSize", clientSetSize, maxClientSetSize);
        this.clientSetSize = clientSetSize;
        MathPreconditions.checkPositiveInRangeClosed("clientU", clientU, maxClientU);
        this.clientU = clientU;
        extraInfo++;
    }
}
