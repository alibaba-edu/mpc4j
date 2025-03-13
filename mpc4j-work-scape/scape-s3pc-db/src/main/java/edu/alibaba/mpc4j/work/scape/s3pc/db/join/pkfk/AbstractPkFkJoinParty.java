package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.AbstractThreePartyDbPto;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.InputProcessUtils;

import java.util.Arrays;

/**
 * abstract PkFk join party
 *
 * @author Feng Han
 * @date 2025/2/20
 */
public abstract class AbstractPkFkJoinParty extends AbstractThreePartyDbPto implements PkFkJoinParty {
    /**
     * new left table after re-organization
     */
    protected TripletLongVector[] processedUTab;
    /**
     * new right table after re-organization
     */
    protected TripletLongVector[] processedNuTab;
    /**
     * the key dimension
     */
    protected int keyDim;
    /**
     * the input is sorted in the order of join_key and valid_flag
     */
    protected boolean inputIsSorted;

    protected AbstractPkFkJoinParty(PtoDesc ptoDesc, Abb3Party abb3Party, PkFkJoinConfig config) {
        super(ptoDesc, abb3Party, config);
    }

    protected void setPtoInput(TripletLongVector[] uTable, TripletLongVector[] nuTable,
                               int[] uKeyIndex, int[] nuKeyIndex, boolean inputIsSorted) {
        Preconditions.checkArgument(uTable != null && nuTable != null && uKeyIndex.length == nuKeyIndex.length);
        // 认为输入表格的最后一维是F，即合法位
        for (int i = 0; i < uKeyIndex.length; i++) {
            MathPreconditions.checkGreater("uTable.length - 1 > uKeyIndex[i]", uTable.length - 1, uKeyIndex[i]);
            MathPreconditions.checkGreater("nuTable.length - 1 > nuKeyIndex[i]", nuTable.length - 1, nuKeyIndex[i]);
        }
        removeDummy(uTable, nuTable);
        processedUTab = InputProcessUtils.reshapeInput(uTable, uKeyIndex);
        processedNuTab = InputProcessUtils.reshapeInput(nuTable, nuKeyIndex);
        keyDim = uKeyIndex.length;
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
