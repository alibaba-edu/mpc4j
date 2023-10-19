package edu.alibaba.mpc4j.s2pc.pso.payablepsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.ArrayList;
import java.util.Set;

/**
 * Abstract Payable PSI server.
 *
 * @author Liqiang Peng
 * @date 2023/9/15
 */
public abstract class AbstractPayablePsiServer<T> extends AbstractTwoPartyPto implements PayablePsiServer<T> {
    /**
     * max server element size
     */
    private int maxServerElementSize;
    /**
     * max client element size
     */
    private int maxClientElementSize;
    /**
     * server element list
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

    protected AbstractPayablePsiServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, PayablePsiConfig config) {
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
