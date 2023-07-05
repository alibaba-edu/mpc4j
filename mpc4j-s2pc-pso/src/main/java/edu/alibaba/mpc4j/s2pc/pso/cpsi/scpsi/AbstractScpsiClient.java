package edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.ArrayList;
import java.util.Set;

/**
 * abstract server-payload circuit PSI client.
 *
 * @author Weiran Liu
 * @date 2023/4/18
 */
public abstract class AbstractScpsiClient<T> extends AbstractTwoPartyPto implements ScpsiClient<T> {
    /**
     * max client element size
     */
    private int maxClientElementSize;
    /**
     * max server element size
     */
    private int maxServerElementSize;
    /**
     * client element array list
     */
    protected ArrayList<T> clientElementArrayList;
    /**
     * client element sie
     */
    protected int clientElementSize;
    /**
     * server element size
     */
    protected int serverElementSize;

    protected AbstractScpsiClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, ScpsiConfig config) {
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
