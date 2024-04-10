package edu.alibaba.mpc4j.s2pc.opf.pmpeqt;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * Permuted Matrix Private Equality Test abstract sender.
 *
 * @author Liqiang Peng
 * @date 2024/3/5
 */
public abstract class AbstractPmPeqtSender extends AbstractTwoPartyPto implements PmPeqtSender {

    /**
     * max row num
     */
    protected int maxRow;
    /**
     * max column num
     */
    protected int maxColumn;
    /**
     * byte length
     */
    protected int byteLength;
    /**
     * row
     */
    protected int row;
    /**
     * column
     */
    protected int column;

    protected AbstractPmPeqtSender(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, PmPeqtConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(int maxRow, int maxColumn) {
        MathPreconditions.checkGreater("maxRow * maxColumn", maxRow * maxColumn, 1);
        this.maxRow = maxRow;
        this.maxColumn = maxColumn;
        extraInfo++;
        initState();
    }

    protected void setPtoInput(byte[][][] inputMatrix, int[] rowPermutationMap, int[] columnPermutationMap,
                               int byteLength) {
        checkInitialized();
        MathPreconditions.checkGreaterOrEqual("byteLength", byteLength, CommonConstants.STATS_BYTE_LENGTH);
        this.byteLength = byteLength;
        MathPreconditions.checkGreaterOrEqual("row", rowPermutationMap.length, 1);
        MathPreconditions.checkLessOrEqual("row", rowPermutationMap.length, maxRow);
        this.row = rowPermutationMap.length;
        MathPreconditions.checkGreaterOrEqual("column", columnPermutationMap.length, 1);
        MathPreconditions.checkLessOrEqual("column", columnPermutationMap.length, maxColumn);
        this.column = columnPermutationMap.length;
        MathPreconditions.checkGreater("row * column", row * column, 1);
        MathPreconditions.checkEqual("expected matrix row", "matrix row", row, inputMatrix.length);
        for (int i = 0; i < row; i++) {
            MathPreconditions.checkEqual("expected matrix column", "matrix column", column, inputMatrix[i].length);
        }
        extraInfo++;
    }
}
