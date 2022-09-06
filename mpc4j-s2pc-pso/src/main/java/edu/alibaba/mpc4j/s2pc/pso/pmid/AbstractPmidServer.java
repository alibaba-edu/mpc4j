package edu.alibaba.mpc4j.s2pc.pso.pmid;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;

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
public abstract class AbstractPmidServer<T> extends AbstractSecureTwoPartyPto implements PmidServer<T> {
    /**
     * 配置项
     */
    private final PmidConfig config;
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
        this.config = config;
    }

    @Override
    public PmidFactory.PmidType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxServerSetSize, int maxServerU, int maxClientSetSize, int maxClientU) {
        assert maxServerSetSize > 1 : "max(ServerSetSize) must be greater than 1";
        this.maxServerSetSize = maxServerSetSize;
        assert maxServerU >= 1 : "max(ServerU) must be greater than or equal to 1";
        this.maxServerU = maxServerU;
        assert maxClientSetSize > 1 : "max(ClientSetSize) must be greater than 1";
        this.maxClientSetSize = maxClientSetSize;
        assert maxClientU >= 1 : "max(ClientU) must be greater than or equal to 1";
        this.maxClientU = maxClientU;
        initialized = false;
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
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        Set<T> serverElementSet = serverElementMap.keySet();
        assert serverElementSet.size() > 1 && serverElementSet.size() <= maxServerSetSize :
            "ServerSetSize must be in range (1, " + maxServerSetSize + "]";
        serverElementArrayList = new ArrayList<>(serverElementSet);
        serverSetSize = serverElementSet.size();
        this.serverElementMap = serverElementMap;
        serverU = serverElementSet.stream()
            .mapToInt(serverElementMap::get)
            .peek(ui -> {
                assert ui >= 1 : "ui must be greater than or equal to 1: " + ui;
            })
            .max()
            .orElse(0);
        assert serverU >= 1 && serverU <= maxServerU : "ServerU must be in range [1, " + maxServerU + "]: " + serverU;
        assert clientSetSize > 1 && clientSetSize <= maxClientSetSize :
            "ClientSetSize must be in range (1, " + maxClientSetSize + "]";
        this.clientSetSize = clientSetSize;
        assert clientU >= 1 && clientU <= maxClientU : "ClientU must be in range [1, " + maxClientU + "]: " + clientU;
        this.clientU = clientU;
        extraInfo++;
    }
}
