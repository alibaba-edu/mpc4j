package edu.alibaba.mpc4j.work.db.sketch.CMS;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.ArrayList;

/**
 * CMS structure
 */
public class CMSTable extends AbstractCMSTable {

    public CMSTable(MpcLongVector[] data, ArrayList<MpcVector> buffer, int elementBitLen, int logSketchSize, HashParameters hashParameters) {
        super(data, buffer, elementBitLen, logSketchSize, hashParameters);
        MathPreconditions.checkEqual("data.length", "1", data.length, 1);
        MathPreconditions.checkEqual("buffer.length", "1", buffer.size(), 1);
    }

    public CMSTable(MpcLongVector[] data, int elementBitLen, int logSketchSize, HashParameters hashParameters) {
        super(data, elementBitLen, logSketchSize, hashParameters);
        MathPreconditions.checkEqual("data.length", "1", data.length, 1);
    }
}
