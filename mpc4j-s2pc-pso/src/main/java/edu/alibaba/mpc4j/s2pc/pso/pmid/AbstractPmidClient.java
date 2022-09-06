package edu.alibaba.mpc4j.s2pc.pso.pmid;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pso.pmid.PmidFactory.PmidType;

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
public abstract class AbstractPmidClient<T> extends AbstractSecureTwoPartyPto implements PmidClient<T> {
    /**
     * 配置项
     */
    private final PmidConfig config;
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
        this.config = config;
    }

    @Override
    public PmidType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxClientSetSize, int maxClientU, int maxServerSetSize, int maxServerU) {
        assert maxClientSetSize > 1 : "max(ClientSetSize) must be greater than 1";
        this.maxClientSetSize = maxClientSetSize;
        assert maxClientU >= 1 : "max(ClientU) must be greater than or equal to 1";
        this.maxClientU = maxClientU;
        assert maxServerSetSize > 1 : "max(ServerSetSize) must be greater than 1";
        this.maxServerSetSize = maxServerSetSize;
        assert maxServerU >= 1 : "max(ServerU) must be greater than or equal to 1";
        this.maxServerU = maxServerU;

        initialized = false;
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
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        Set<T> clientElementSet = clientElementMap.keySet();
        assert clientElementSet.size() > 1 && clientElementSet.size() <= maxClientSetSize :
            "ClientSetSize must be in range (1, " + maxClientSetSize + "]";
        clientElementArrayList = new ArrayList<>(clientElementSet);
        clientSetSize = clientElementSet.size();
        this.clientElementMap = clientElementMap;
        clientU = clientElementSet.stream()
            .mapToInt(clientElementMap::get)
            .peek(uy -> {
                assert uy >= 1 : "uy must be greater than or equal to 1";
            })
            .max()
            .orElse(0);
        assert clientU >= 1 && clientU <= maxClientU : "ClientK must be in range [1, " + maxClientU + "]: " + clientU;
        assert serverSetSize > 1 && serverSetSize <= maxServerSetSize :
            "ServerSetSize must be in range (1, " + maxServerSetSize + "]";
        this.serverSetSize = serverSetSize;
        assert serverU >= 1 && serverU <= maxServerU : "ServerK must be in range [1, " + maxServerU + "]: " + serverU;
        this.serverU = serverU;
        extraInfo++;
    }
}
