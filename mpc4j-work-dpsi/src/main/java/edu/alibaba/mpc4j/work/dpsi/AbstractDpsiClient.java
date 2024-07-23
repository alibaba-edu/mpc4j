package edu.alibaba.mpc4j.work.dpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.ArrayList;
import java.util.Set;

/**
 * abstract DPSI client.
 *
 * @author Weiran Liu
 * @date 2024/4/26
 */
public abstract class AbstractDpsiClient<T> extends AbstractTwoPartyPto implements DpsiClient<T> {
    /**
     * max client element size
     */
    protected int maxClientElementSize;
    /**
     * max server element size
     */
    protected int maxServerElementSize;
    /**
     * client element array list
     */
    protected ArrayList<T> clientElementArrayList;
    /**
     * client element size
     */
    protected int clientElementSize;
    /**
     * server element size
     */
    protected int serverElementSize;

    protected AbstractDpsiClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, DpsiConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int maxClientElementSize, int maxServerElementSize) {
        MathPreconditions.checkPositive("maxClientElementSize", maxClientElementSize);
        this.maxClientElementSize = maxClientElementSize;
        MathPreconditions.checkPositive("maxServerElementSize", maxServerElementSize);
        this.maxServerElementSize = maxServerElementSize;
        initState();
    }

    protected void setPtoInput(Set<T> clientElementSet, int serverElementSize) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("clientElementSize", clientElementSet.size(), maxClientElementSize);
        clientElementSize = clientElementSet.size();
        clientElementArrayList = new ArrayList<>(clientElementSet);
        MathPreconditions.checkPositiveInRangeClosed("serverElementSize", serverElementSize, maxServerElementSize);
        this.serverElementSize = serverElementSize;
        extraInfo++;
    }
}
