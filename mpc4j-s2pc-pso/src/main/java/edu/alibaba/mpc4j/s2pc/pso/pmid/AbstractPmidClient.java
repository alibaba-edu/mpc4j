package edu.alibaba.mpc4j.s2pc.pso.pmid;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pso.pmid.PmidFactory.PmidType;

import java.util.ArrayList;
import java.util.Map;

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
    private int maxClientSetSize;
    /**
     * 服务端集合最大数量
     */
    private int maxServerSetSize;
    /**
     * 客户端最大重复元素上界
     */
    private int maxK;
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
     * 服务端元素数量
     */
    protected int serverSetSize;
    /**
     * 客户端重复元素上界
     */
    protected int k;

    protected AbstractPmidClient(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, PmidConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    @Override
    public PmidType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxClientSetSize, int maxServerSetSize, int maxK) {
        assert maxClientSetSize > 1 : "max(ClientSetSize) must be greater than 1";
        this.maxClientSetSize = maxClientSetSize;
        assert maxServerSetSize > 1 : "max(ServerSetSize) must be greater than 1";
        this.maxServerSetSize = maxServerSetSize;
        assert maxK >= 1 : "max(K) must be greater than or equal to 1";
        this.maxK = maxK;
        initialized = false;
    }

    protected void setPtoInput(Map<T, Integer> clientElementMap, int serverSetSize) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert clientElementMap.keySet().size() > 1 && clientElementMap.keySet().size() <= maxClientSetSize :
            "ClientSetSize must be in range (1, " + maxClientSetSize + "]";
        this.clientElementMap = clientElementMap;
        clientElementArrayList = new ArrayList<>(clientElementMap.keySet());
        clientSetSize = clientElementArrayList.size();
        k = clientElementMap.keySet().stream()
            .mapToInt(clientElementMap::get)
            .peek(dy -> {
                assert dy >= 1 : "ky must be greater than or equal to 1";
            })
            .max()
            .orElse(0);
        assert k <= maxK : "K must be in range [1, " + maxK + "]";
        assert serverSetSize > 1 && serverSetSize <= maxServerSetSize :
            "ServerSetSize must be in range (1, " + maxServerSetSize + "]";
        this.serverSetSize = serverSetSize;
        extraInfo++;
    }
}
