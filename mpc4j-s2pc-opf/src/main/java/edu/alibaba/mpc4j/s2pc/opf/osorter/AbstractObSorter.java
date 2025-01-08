package edu.alibaba.mpc4j.s2pc.opf.osorter;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * @author Feng Han
 * @date 2024/9/27
 */
public abstract class AbstractObSorter extends AbstractTwoPartyPto implements ObSorter {
    /**
     * the number of input
     */
    protected int inputNum;
    /**
     * the number of input dimension
     */
    protected int inputDim;
    /**
     * need permutation
     */
    protected boolean needPermutation;
    /**
     * need stable
     */
    protected boolean needStable;
    /**
     * data
     */
    protected SquareZ2Vector[] data;
    /**
     * payload
     */
    protected SquareZ2Vector[] flatPayload;
    /**
     * orderEnum
     */
    protected SquareZ2Vector[] resultPermutation;


    protected AbstractObSorter(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ObSortConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setPtoInput(SquareZ2Vector[] xiArray, boolean needPermutation, boolean needStable) {
        inputNum = xiArray[0].bitNum();
        inputDim = xiArray.length;
        this.needPermutation = needPermutation;
        this.needStable = needStable;
        for (int i = 1; i < xiArray.length; i++) {
            MathPreconditions.checkEqual("data num", "inputVectors[i].bitNum()", xiArray[0].bitNum(), xiArray[i].bitNum());
        }
        data = xiArray;
    }

    protected void setPtoInputWithPayload(SquareZ2Vector[] xiArray, SquareZ2Vector[] payloads,  boolean needPermutation, boolean needStable) {
        setPtoInput(xiArray, needPermutation, needStable);
        this.flatPayload = payloads;
    }
}
