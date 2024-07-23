package edu.alibaba.mpc4j.work.dpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.ArrayList;
import java.util.Set;

/**
 * abstract DPSI server.
 *
 * @author Weiran Liu
 * @date 2024/4/26
 */
public abstract class AbstractDpsiServer<T> extends AbstractTwoPartyPto implements DpsiServer<T> {
    /**
     * max server element size
     */
    protected int maxServerElementSize;
    /**
     * max client element size
     */
    protected int maxClientElementSize;
    /**
     * server element array list
     */
    protected ArrayList<T> serverElementArrayList;
    /**
     * server element size
     */
    protected int serverElementSize;
    /**
     * client element size
     */
    protected int clientElementSize;

    protected AbstractDpsiServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, DpsiConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(int maxServerElementSize, int maxClientElementSize) {
        MathPreconditions.checkPositive("maxServerElementSize", maxServerElementSize);
        this.maxServerElementSize = maxServerElementSize;
        MathPreconditions.checkPositive("maxClientElementSize", maxClientElementSize);
        this.maxClientElementSize = maxClientElementSize;
        initState();
    }

    protected void setPtoInput(Set<T> serverElementSet, int clientElementSize) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("serverElementSize", serverElementSet.size(), maxServerElementSize);
        serverElementSize = serverElementSet.size();
        serverElementArrayList = new ArrayList<>(serverElementSet);
        MathPreconditions.checkPositiveInRangeClosed("clientElementSize", clientElementSize, maxClientElementSize);
        this.clientElementSize = clientElementSize;
        extraInfo++;
    }
}
