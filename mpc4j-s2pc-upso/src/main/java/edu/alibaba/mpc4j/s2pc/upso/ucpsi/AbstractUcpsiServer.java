package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.ArrayList;
import java.util.Set;

/**
 * abstract unbalanced circuit PSI server.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public abstract class AbstractUcpsiServer<T> extends AbstractTwoPartyPto implements UcpsiServer<T> {
    /**
     * max client element size
     */
    protected int maxClientElementSize;
    /**
     * server element list
     */
    protected ArrayList<T> serverElementArrayList;
    /**
     * server element size
     */
    protected int serverElementSize;

    protected AbstractUcpsiServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, UcpsiConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(Set<T> serverElementSet, int maxClientElementSize) {
        MathPreconditions.checkPositive("serverElementSize", serverElementSet.size());
        this.serverElementSize = serverElementSet.size();
        serverElementArrayList = new ArrayList<>(serverElementSet);
        MathPreconditions.checkPositive("maxClientElementSize", maxClientElementSize);
        this.maxClientElementSize = maxClientElementSize;
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}
