package edu.alibaba.mpc4j.work.scape.s3pc.db.join.general;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.AbstractThreePartyDbPto;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.InputProcessUtils;

import java.util.Arrays;

/**
 * Abstract general Join Party
 *
 * @author Feng Han
 * @date 2025/2/20
 */
public abstract class AbstractGeneralJoinParty extends AbstractThreePartyDbPto implements GeneralJoinParty {
    /**
     * new left table after re-organization
     */
    protected TripletLongVector[] newLeft;
    /**
     * new right table after re-organization
     */
    protected TripletLongVector[] newRight;
    /**
     * the key dimension
     */
    protected int keyDim;
    /**
     * the input is sorted in the order of join_key and valid_flag
     */
    protected boolean inputIsSorted;

    protected AbstractGeneralJoinParty(PtoDesc ptoDesc, Abb3Party abb3Party, GeneralJoinConfig config) {
        super(ptoDesc, abb3Party, config);
    }

    protected void setPtoInput(TripletLongVector[] left, TripletLongVector[] right, int[] leftKeyIndex, int[] rightKeyIndex, int m, boolean inputIsSorted) {
        Preconditions.checkArgument(left != null && right != null && leftKeyIndex.length == rightKeyIndex.length);
        MathPreconditions.checkGreaterOrEqual("m", m, Math.max(left[0].getNum(), right[0].getNum()));
        // 认为输入表格的最后一维是F，即合法位
        for (int i = 0; i < leftKeyIndex.length; i++) {
            MathPreconditions.checkGreater("left.length - 1 > leftKeyIndex[i]", left.length - 1, leftKeyIndex[i]);
            MathPreconditions.checkGreater("right.length - 1 > rightKeyIndex[i]", right.length - 1, rightKeyIndex[i]);
        }
        removeDummy(left, right);
        newLeft = InputProcessUtils.reshapeInput(left, leftKeyIndex);
        newRight = InputProcessUtils.reshapeInput(right, rightKeyIndex);
        keyDim = leftKeyIndex.length;
        this.inputIsSorted = inputIsSorted;
    }

    /**
     * set the values of the dummy rows as 0
     *
     * @param left  left table, the last column is valid indicator flag
     * @param right right table, the last column is valid indicator flag
     */
    public void removeDummy(TripletLongVector[] left, TripletLongVector[] right) {
        TripletLongVector[] leftData = Arrays.copyOf(left, left.length - 1);
        TripletLongVector[] leftFlag = new TripletLongVector[leftData.length];
        Arrays.fill(leftFlag, left[leftData.length]);
        TripletLongVector[] rightData = Arrays.copyOf(right, right.length - 1);
        TripletLongVector[] rightFlag = new TripletLongVector[rightData.length];
        Arrays.fill(rightFlag, right[rightData.length]);
        System.arraycopy(zl64cParty.mul(leftData, leftFlag), 0, left, 0, leftData.length);
        System.arraycopy(zl64cParty.mul(rightData, rightFlag), 0, right, 0, rightData.length);
    }
}
