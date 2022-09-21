package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;

import java.util.ArrayList;
import java.util.Set;

/**
 * PSI协议服务端。
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public abstract class AbstractPsiServer<T> extends AbstractSecureTwoPartyPto implements PsiServer<T> {
    /**
     * 配置项
     */
    private final PsiConfig config;
    /**
     * 服务端最大元素数量
     */
    private int maxServerElementSize;
    /**
     * 客户端最大元素数量
     */
    private int maxClientElementSize;
    /**
     * 服务端元素数组
     */
    protected ArrayList<T> serverElementArrayList;
    /**
     * 服务端元素数量
     */
    protected int serverElementSize;
    /**
     * 客户端元素数量
     */
    protected int clientElementSize;

    protected AbstractPsiServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, PsiConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
        this.config = config;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxServerElementSize, int maxClientElementSize) {
        assert maxServerElementSize > 0 : "max server element size must be greater than 0: " + maxServerElementSize;
        this.maxServerElementSize = maxServerElementSize;
        assert maxClientElementSize > 0 : "mac client element size must be greater than 0: " + maxClientElementSize;
        this.maxClientElementSize = maxClientElementSize;
        extraInfo++;
        initialized = false;
    }

    protected void setPtoInput(Set<T> serverElementSet, int clientElementSize) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert serverElementSet.size() > 0 && serverElementSet.size() <= maxServerElementSize
            : "server element size must be in range (0, " + maxServerElementSize + "]: " + serverElementSet.size();
        serverElementSize = serverElementSet.size();
        serverElementArrayList = new ArrayList<>(serverElementSet);
        assert clientElementSize > 0 && clientElementSize <= maxClientElementSize
            : "client element size must be in range (0, " + maxClientElementSize + "]: " + clientElementSize;
        this.clientElementSize = clientElementSize;
        extraInfo++;
    }
}
