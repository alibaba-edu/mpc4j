package edu.alibaba.mpc4j.s2pc.pso.aidpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.Set;

/**
 * abstract aid PSI party.
 *
 * @author Weiran Liu
 * @date 2023/5/4
 */
public abstract class AbstractAidPsiParty<T> extends AbstractThreePartyPto implements AidPsiParty<T> {
    /**
     * max own element size
     */
    private int maxOwnElementSize;
    /**
     * max other element size
     */
    private int maxOtherElementSize;
    /**
     * own element size
     */
    protected int ownElementSize;
    /**
     * other element size
     */
    protected int otherElementSize;

    protected AbstractAidPsiParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, Party aiderParty, AidPsiConfig config) {
        super(ptoDesc, ownRpc, otherParty, aiderParty, config);
    }

    protected void setInitInput(int maxOwnElementSize, int maxOtherElementSize) {
        MathPreconditions.checkPositive("maxOwnElementSize", maxOwnElementSize);
        this.maxOwnElementSize = maxOwnElementSize;
        MathPreconditions.checkPositive("maxOtherElementSize", maxOtherElementSize);
        this.maxOtherElementSize = maxOtherElementSize;
        initState();
    }

    protected void setPtoInput(Set<T> ownElementSet, int otherElementSize) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("ownElementSize", ownElementSet.size(), maxOwnElementSize);
        ownElementSize = ownElementSet.size();
        MathPreconditions.checkPositiveInRangeClosed("otherElementSize", otherElementSize, maxOtherElementSize);
        this.otherElementSize = otherElementSize;
        extraInfo++;
    }
}
