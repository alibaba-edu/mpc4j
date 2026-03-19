package edu.alibaba.mpc4j.work.db.sketch.SS;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;

import java.util.List;

/**
 * MG table.
 */
public class SSTable extends AbstractSSTable {
    /**
     * payload bit length
     */
    private final int payloadBitLen;
    /**
     * key bit length
     */
    private final int keyBitLen;

    public SSTable(MpcZ2Vector[] data, List<MpcVector> buffer, int logSketchSize, int keyBitLen, int payloadBitLen) {
        super(data, buffer);
        this.logSketchSize = logSketchSize;
        this.keyBitLen = keyBitLen;
        this.payloadBitLen = payloadBitLen;
    }

    public SSTable(MpcZ2Vector[] data, int logSketchSize, int keyBitLen, int payloadBitLen) {
        super(data);
        this.logSketchSize = logSketchSize;
        this.keyBitLen = keyBitLen;
        this.payloadBitLen = payloadBitLen;
    }

    public int getPayloadBitLen() {return payloadBitLen;}

    public int getKeyBitLen() {return keyBitLen;}
}
