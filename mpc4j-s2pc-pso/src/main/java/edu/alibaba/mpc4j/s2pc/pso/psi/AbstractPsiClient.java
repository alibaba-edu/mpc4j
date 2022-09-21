package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;

import java.util.ArrayList;
import java.util.Set;

/**
 * PSI协议客户端。
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public abstract class AbstractPsiClient<T> extends AbstractSecureTwoPartyPto implements PsiClient<T> {
    /**
     * 配置项
     */
    private final PsiConfig config;
    /**
     * 客户端最大元素数量
     */
    private int maxClientElementSize;
    /**
     * 服务端最大元素数量
     */
    private int maxServerElementSize;
    /**
     * 客户端元素集合
     */
    protected ArrayList<T> clientElementArrayList;
    /**
     * 客户端元素数量
     */
    protected int clientElementSize;
    /**
     * 服务端元素数量
     */
    protected int serverElementSize;

    protected AbstractPsiClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, PsiConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
        this.config = config;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxClientElementSize, int maxServerElementSize) {
        assert maxClientElementSize > 0 : "max client element size must be greater than 0: " + maxClientElementSize;
        this.maxClientElementSize = maxClientElementSize;
        assert maxServerElementSize > 0 : "max server element size must be greater than 0: " + maxServerElementSize;
        this.maxServerElementSize = maxServerElementSize;
        extraInfo++;
        initialized = false;
    }

    protected void setPtoInput(Set<T> clientElementSet, int serverElementSize) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert clientElementSet.size() > 0 && clientElementSet.size() <= maxClientElementSize
            : "client element size must be in range (0, " + maxServerElementSize + "]: " + clientElementSet.size();
        clientElementSize = clientElementSet.size();
        clientElementArrayList = new ArrayList<>(clientElementSet);
        assert serverElementSize > 0 && serverElementSize <= maxServerElementSize
            : "server element size must be in range (0, " + maxServerElementSize + "]: " + serverElementSize;
        this.serverElementSize = serverElementSize;
        extraInfo++;
    }
}
