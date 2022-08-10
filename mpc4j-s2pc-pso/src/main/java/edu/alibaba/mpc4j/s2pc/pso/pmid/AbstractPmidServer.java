package edu.alibaba.mpc4j.s2pc.pso.pmid;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;

import java.util.ArrayList;
import java.util.Set;

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
    private int maxServerSetSize;
    /**
     * 客户端集合最大数量
     */
    private int maxClientSetSize;
    /**
     * 客户端最大重复元素上界
     */
    private int maxK;
    /**
     * 服务端元素列表
     */
    protected ArrayList<T> serverElementArrayList;
    /**
     * 服务端元素数量
     */
    protected int serverSetSize;
    /**
     * 客户端元素数量
     */
    protected int clientSetSize;
    /**
     * 客户端重复元素上界
     */
    protected int k;

    protected AbstractPmidServer(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, PmidConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    @Override
    public PmidFactory.PmidType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxServerSetSize, int maxClientSetSize, int maxK) {
        assert maxServerSetSize > 1 : "max(ServerSetSize) must be greater than 1";
        this.maxServerSetSize = maxServerSetSize;
        assert maxClientSetSize > 1 : "max(ClientSetSize) must be greater than 1";
        this.maxClientSetSize = maxClientSetSize;
        assert maxK >= 1 : "max(K) must be greater than or equal to 1";
        this.maxK = maxK;
        initialized = false;
    }

    protected void setPtoInput(Set<T> serverElementSet, int clientSetSize, int k) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert serverElementSet.size() > 1 && serverElementSet.size() <= maxServerSetSize :
            "ServerSetSize must be in range (1, " + maxServerSetSize + "]";
        serverElementArrayList = new ArrayList<>(serverElementSet);
        serverSetSize = serverElementArrayList.size();
        assert clientSetSize > 1 && clientSetSize <= maxClientSetSize :
            "ClientSetSize must be in range (1, " + maxClientSetSize + "]";
        this.clientSetSize = clientSetSize;
        assert k <= maxK : "K must be in range [1, " + maxK + "]";
        this.k = k;
        extraInfo++;
    }
}
