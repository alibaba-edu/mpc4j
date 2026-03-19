package edu.alibaba.mpc4j.work.db.sketch.CMS;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;

import java.util.ArrayList;

/**
 * Z2 CMS table
 */
public class Z2CMSTable extends AbstractCMSTable {
    /**
     * bit number for payload (i.e. the max count number is 1<<payloadBitLen)
     */
    private final int payloadBitLen;

    public Z2CMSTable(MpcZ2Vector[] data, ArrayList<MpcVector> buffer, int payloadBitLen, int elementBitLen, int logSketchSize, HashParameters hashParameters) {
        super(data, buffer, elementBitLen, logSketchSize, hashParameters);
        this.payloadBitLen = payloadBitLen;
    }

    public Z2CMSTable(MpcZ2Vector[] data, int payloadBitLen, int elementBitLen, int logSketchSize, HashParameters hashParameters) {
        super(data, elementBitLen, logSketchSize, hashParameters);
        this.payloadBitLen = payloadBitLen;
    }

    public int getPayloadBitLen() {return payloadBitLen;}

}
