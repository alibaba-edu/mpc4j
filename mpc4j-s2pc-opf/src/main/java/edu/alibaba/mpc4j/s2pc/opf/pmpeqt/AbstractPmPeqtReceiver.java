package edu.alibaba.mpc4j.s2pc.opf.pmpeqt;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * Permuted Matrix Private Equality Test abstract receiver.
 *
 * @author Liqiang Peng
 * @date 2024/3/5
 */
public abstract class AbstractPmPeqtReceiver extends AbstractTwoPartyPto implements PmPeqtReceiver {

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

    protected AbstractPmPeqtReceiver(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, PmPeqtConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int maxRow, int maxColumn) {
        MathPreconditions.checkGreater("maxRow * maxColumn", maxRow * maxColumn, 1);
        this.maxRow = maxRow;
        this.maxColumn = maxColumn;
        extraInfo++;
        initState();
    }

    protected void setPtoInput(byte[][][] inputMatrix, int byteLength, int row, int column) {
        checkInitialized();
        MathPreconditions.checkGreaterOrEqual("byteLength", byteLength, CommonConstants.STATS_BYTE_LENGTH);
        this.byteLength = byteLength;
        MathPreconditions.checkGreaterOrEqual("row", row, 1);
        MathPreconditions.checkLessOrEqual("row", row, maxRow);
        this.row = row;
        MathPreconditions.checkGreaterOrEqual("column", column, 1);
        MathPreconditions.checkLessOrEqual("column", column, maxColumn);
        this.column = column;
        MathPreconditions.checkGreater("row * column", row * column, 1);
        MathPreconditions.checkEqual("expected matrix row", "matrix row", row, inputMatrix.length);
        for (int i = 0; i < row; i++) {
            MathPreconditions.checkEqual("expected matrix column", "matrix column", column, inputMatrix[i].length);
        }
        extraInfo++;
    }
}
