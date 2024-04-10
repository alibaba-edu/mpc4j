package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.ArrayList;
import java.util.Set;

/**
 * abstract unbalanced circuit PSI client.
 *
 * @author Liqiang Peng
 * @date 2023/4/18
 */
public abstract class AbstractUcpsiClient<T> extends AbstractTwoPartyPto implements UcpsiClient<T> {
    /**
     * max client element size
     */
    private int maxClientElementSize;
    /**
     * server element size
     */
    protected int serverElementSize;
    /**
     * client element list
     */
    protected ArrayList<T> clientElementArrayList;
    /**
     * client element size
     */
    protected int clientElementSize;

    protected AbstractUcpsiClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, UcpsiConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int maxClientElementSize, int serverElementSize) {
        MathPreconditions.checkPositive("maxClientElementSize", maxClientElementSize);
        this.maxClientElementSize = maxClientElementSize;
        MathPreconditions.checkPositive("serverElementSize", serverElementSize);
        this.serverElementSize = serverElementSize;

        initState();
    }

    protected void setPtoInput(Set<T> clientElementSet) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("clientElementSize", clientElementSet.size(), maxClientElementSize);
        clientElementArrayList = new ArrayList<>(clientElementSet);
        clientElementSize = clientElementSet.size();
        extraInfo++;
    }
}