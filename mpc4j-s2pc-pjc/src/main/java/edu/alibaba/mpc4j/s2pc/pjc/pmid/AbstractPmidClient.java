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
 * PMID协议客户端抽象类。
 *
 * @author Weiran Liu
 * @date 2022/5/10
 */
public abstract class AbstractPmidClient<T> extends AbstractTwoPartyPto implements PmidClient<T> {
    /**
     * 客户端集合最大数量
     */
    protected int maxClientSetSize;
    /**
     * 客户端最大重复元素上界
     */
    protected int maxClientU;
    /**
     * 服务端集合最大数量
     */
    protected int maxServerSetSize;
    /**
     * 服务端最大重复元素上界
     */
    protected int maxServerU;
    /**
     * 客户端元素映射
     */
    protected Map<T, Integer> clientElementMap;
    /**
     * 客户端元素列表
     */
    protected ArrayList<T> clientElementArrayList;
    /**
     * 客户端元素数量
     */
    protected int clientSetSize;
    /**
     * 客户端重复元素上界
     */
    protected int clientU;
    /**
     * 服务端元素数量
     */
    protected int serverSetSize;
    /**
     * 服务端重复元素上界
     */
    protected int serverU;

    protected AbstractPmidClient(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, PmidConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxClientSetSize, int maxClientU, int maxServerSetSize, int maxServerU) {
        MathPreconditions.checkGreater("maxClientSetSize", maxClientSetSize, 1);
        this.maxClientSetSize = maxClientSetSize;
        MathPreconditions.checkPositive("maxClientU", maxClientU);
        this.maxClientU = maxClientU;
        MathPreconditions.checkGreater("maxServerSetSize", maxServerSetSize, 1);
        this.maxServerSetSize = maxServerSetSize;
        MathPreconditions.checkPositive("maxServerU", maxServerU);
        this.maxServerU = maxServerU;
        initState();
    }

    protected void setPtoInput(Set<T> clientElementSet, int serverSetSize) {
        Map<T, Integer> clientElementMap = clientElementSet.stream()
            .collect(Collectors.toMap(
                    element -> element,
                    element -> 1
                )
            );
        setPtoInput(clientElementMap, serverSetSize, 1);
    }

    protected void setPtoInput(Map<T, Integer> clientElementMap, int serverSetSize) {
        setPtoInput(clientElementMap, serverSetSize, 1);
    }

    protected void setPtoInput(Set<T> clientElementSet, int serverSetSize, int serverU) {
        Map<T, Integer> clientElementMap = clientElementSet.stream()
            .collect(Collectors.toMap(
                    element -> element,
                    element -> 1
                )
            );
        setPtoInput(clientElementMap, serverSetSize, serverU);
    }

    protected void setPtoInput(Map<T, Integer> clientElementMap, int serverSetSize, int serverU) {
        checkInitialized();
        Set<T> clientElementSet = clientElementMap.keySet();
        MathPreconditions.checkGreater("clientSetSize", clientElementSet.size(), 1);
        MathPreconditions.checkLessOrEqual("clientSetSize", clientElementSet.size(), maxClientSetSize);
        clientElementArrayList = new ArrayList<>(clientElementSet);
        clientSetSize = clientElementSet.size();
        this.clientElementMap = clientElementMap;
        clientU = clientElementSet.stream()
            .mapToInt(clientElementMap::get)
            .peek(uy -> MathPreconditions.checkPositive("uy", uy))
            .max()
            .orElse(0);
        MathPreconditions.checkPositiveInRangeClosed("clientU", clientU, maxClientU);
        MathPreconditions.checkGreater("serverSetSize", serverSetSize, 1);
        MathPreconditions.checkLessOrEqual("serverSetSize", serverSetSize, maxServerSetSize);
        this.serverSetSize = serverSetSize;
        MathPreconditions.checkPositiveInRangeClosed("serverU", serverU, maxServerU);
        this.serverU = serverU;
        extraInfo++;
    }
}
