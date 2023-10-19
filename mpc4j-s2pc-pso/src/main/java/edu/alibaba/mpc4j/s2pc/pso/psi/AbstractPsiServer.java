package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.ArrayList;
import java.util.Set;

/**
 * PSI abstract client
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public abstract class AbstractPsiServer<T> extends AbstractTwoPartyPto implements PsiServer<T> {
    /**
     * the max size of server's elements
     */
    private int maxServerElementSize;
    /**
     * the max size of client's elements
     */
    private int maxClientElementSize;
    /**
     * the set of server's element
     */
    protected ArrayList<T> serverElementArrayList;
    /**
     * the real input size of server
     */
    protected int serverElementSize;
    /**
     * the real input size of client
     */
    protected int clientElementSize;

    protected AbstractPsiServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, PsiConfig config) {
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
