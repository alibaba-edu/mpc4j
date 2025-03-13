package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.AbstractThreePartyDbPto;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.InputProcessUtils;

/**
 * Abstract PkPk Join Party
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public abstract class AbstractPkPkJoinParty extends AbstractThreePartyDbPto implements PkPkJoinParty {
    /**
     * the tables have dummy padded rows
     */
    protected boolean withDummy;
    /**
     * the input tables are already sorted by keys
     */
    protected boolean inputIsSorted;
    /**
     * the dimension of key
     */
    protected int keyDim;
    /**
     * size of the left table
     */
    protected int leftNum;
    /**
     * size of the right table
     */
    protected int rightNum;
    /**
     * new left table after re-organization
     */
    protected TripletZ2Vector[] newLeft;
    /**
     * new right table after re-organization
     */
    protected TripletZ2Vector[] newRight;

    protected AbstractPkPkJoinParty(PtoDesc ptoDesc, Abb3Party abb3Party, PkPkJoinConfig config) {
        super(ptoDesc, abb3Party, config);
    }

    protected void inputProcess(TripletZ2Vector[] left, TripletZ2Vector[] right,
                                int[] leftKeyIndex, int[] rightKeyIndex,
                                boolean withDummy, boolean inputIsSorted) {
        MathPreconditions.checkEqual("leftKeyIndex.length", "rightKeyIndex.length", leftKeyIndex.length, rightKeyIndex.length);
        this.withDummy = withDummy;
        this.inputIsSorted = inputIsSorted;
        keyDim = leftKeyIndex.length;
        leftNum = left[0].bitNum();
        rightNum = right[0].bitNum();
        newLeft = InputProcessUtils.reshapeInput(left, leftKeyIndex);
        newRight = InputProcessUtils.reshapeInput(right, rightKeyIndex);
    }
}
