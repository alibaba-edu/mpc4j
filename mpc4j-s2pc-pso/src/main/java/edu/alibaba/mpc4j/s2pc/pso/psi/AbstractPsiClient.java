package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.ArrayList;
import java.util.Set;

/**
 * PSI abstract server
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public abstract class AbstractPsiClient<T> extends AbstractTwoPartyPto implements PsiClient<T> {
    /**
     * the max size of client's elements
     */
    private int maxClientElementSize;
    /**
     * the max size of server's elements
     */
    private int maxServerElementSize;
    /**
     * the set of client's element
     */
    protected ArrayList<T> clientElementArrayList;
    /**
     * the real input size of client
     */
    protected int clientElementSize;
    /**
     * the real input size of server
     */
    protected int serverElementSize;

    protected AbstractPsiClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, PsiConfig config) {
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
