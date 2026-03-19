package edu.alibaba.mpc4j.work.db.sketch.GK;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;

import java.util.List;

public class GKTable extends AbstractGKTable {
    /**
     * key bit length
     */
    private final int keyBitLen;

    private final int attributeBitLen;


    public GKTable(MpcZ2Vector[] data, List<MpcVector> buffer, int sketchSize, int keyBitLen, int attributeBitLen, double epsilon) {
        super(data, buffer);
        this.sketchSize = sketchSize;
        this.keyBitLen = keyBitLen;
        this.attributeBitLen = attributeBitLen;
        this.epsilon = epsilon;
    }

    public GKTable(MpcZ2Vector[] data, int sketchSize, int keyBitLen, int attributeBitLen, double epsilon) {
        super(data);
        this.sketchSize = sketchSize;
        this.keyBitLen = keyBitLen;
        this.attributeBitLen = attributeBitLen;
        this.epsilon = epsilon;
    }

    public int getKeyBitLen() {return keyBitLen;}

    public int getAttributeBitLen() {return attributeBitLen;}

    public int getThreshold() {return (int) (epsilon*dataSize);}
}
