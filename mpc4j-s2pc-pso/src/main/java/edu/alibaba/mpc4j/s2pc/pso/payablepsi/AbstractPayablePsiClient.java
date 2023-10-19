package edu.alibaba.mpc4j.s2pc.pso.payablepsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.ArrayList;
import java.util.Set;

/**
 * Abstract Payable PSI client.
 *
 * @author Liqiang Peng
 * @date 2023/9/15
 */
public abstract class AbstractPayablePsiClient<T> extends AbstractTwoPartyPto implements PayablePsiClient<T> {
    /**
     * max client element size
     */
    private int maxClientElementSize;
    /**
     * max server element size
     */
    private int maxServerElementSize;
    /**
     * client element list
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

    protected AbstractPayablePsiClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, PayablePsiConfig config) {
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
