package edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.AbstractThreePartyDbPto;

import java.util.stream.IntStream;

/**
 * abstract PkPk semi join party
 *
 * @author Feng Han
 * @date 2025/2/25
 */
public abstract class AbstractPkPkSemiJoinParty extends AbstractThreePartyDbPto implements PkPkSemiJoinParty {
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

    protected AbstractPkPkSemiJoinParty(PtoDesc ptoDesc, Abb3Party abb3Party, PkPkSemiJoinConfig config) {
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

        newLeft = new TripletZ2Vector[leftKeyIndex.length + 1];
        IntStream.range(0, leftKeyIndex.length).forEach(i -> newLeft[i] = left[leftKeyIndex[i]]);
        newLeft[leftKeyIndex.length] = left[left.length - 1];
        newRight = new TripletZ2Vector[rightKeyIndex.length + 1];
        IntStream.range(0, rightKeyIndex.length).forEach(i -> newRight[i] = right[rightKeyIndex[i]]);
        newRight[rightKeyIndex.length] = right[right.length - 1];
    }
}
