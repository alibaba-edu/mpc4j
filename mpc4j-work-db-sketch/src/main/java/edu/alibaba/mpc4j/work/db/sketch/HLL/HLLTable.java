package edu.alibaba.mpc4j.work.db.sketch.HLL;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.ArrayList;

public class HLLTable extends AbstractHLLTable {

    // the length of each counter
    private final int payloadBitLen;

    private final int hashBitLen;

    public HLLTable(MpcZ2Vector[] data, ArrayList<MpcVector> buffer, int hashBitLen, int elementBitLen, int logSketchSize, MpcZ2Vector encKey) {
        super(data, buffer, elementBitLen, logSketchSize, encKey);
        this.hashBitLen = hashBitLen;
        this.payloadBitLen = LongUtils.ceilLog2(hashBitLen);
    }

    public HLLTable(MpcZ2Vector[] data, int hashBitLen, int elementBitLen, int logSketchSize, MpcZ2Vector encKey) {
        super(data, elementBitLen, logSketchSize, encKey);
        this.hashBitLen = hashBitLen;
        this.payloadBitLen = LongUtils.ceilLog2(hashBitLen);
    }

    public int getPayloadBitLen() {
        return payloadBitLen;
    }

    public int getHashBitLen() {
        return hashBitLen;
    }

}
