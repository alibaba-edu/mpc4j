package edu.alibaba.mpc4j.work.db.sketch.CMS;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTable;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTableType;

import java.util.LinkedList;
import java.util.List;

/**
 * abstract CMS table
 */
public class AbstractCMSTable implements SketchTable {
    /**
     * sketch table, we only save payload here
     */
    private MpcVector[] sketchTable;
    /**
     * buffer, we only save updated keys here, saved data is in the row form
     */
    private List<MpcVector> buffer;
    /**
     * log of sketch table size
     */
    protected final int logSketchSize;
    /**
     * input element bit length
     */
    private final int elementBitLen;
    /**
     * parameter for hash computation
     */
    private final HashParameters hashParameters;

    public AbstractCMSTable(MpcVector[] sketchTable, List<MpcVector> buffer, int elementBitLen, int logSketchSize, HashParameters hashParameters) {
        this.sketchTable = sketchTable;
        this.buffer = buffer;
        this.elementBitLen = elementBitLen;
        this.logSketchSize = logSketchSize;
        this.hashParameters = hashParameters;
    }

    public AbstractCMSTable(MpcVector[] sketchTable, int elementBitLen, int logSketchSize, HashParameters hashParameters) {
        this.sketchTable = sketchTable;
        this.buffer = new LinkedList<>();
        this.elementBitLen = elementBitLen;
        this.logSketchSize = logSketchSize;
        this.hashParameters = hashParameters;
    }

    public int getLogSketchSize() {
        return logSketchSize;
    }

    public int getElementBitLen() {
        return elementBitLen;
    }

    public HashParameters getHashParameters() {
        return hashParameters;
    }

    @Override
    public SketchTableType getSketchTableType() {
        return SketchTableType.CMS;
    }

    @Override
    public int getBufferIndex(){
        return buffer.size();
    }

    @Override
    public MpcVector[] getSketchTable() {
        return sketchTable;
    }

    @Override
    public int getTableSize() {
        return 1 << logSketchSize;
    }

    @Override
    public List<MpcVector> getBufferTable() {
        return buffer;
    }

    @Override
    public void clearBufferTable() {
        buffer.clear();
    }

    @Override
    public void setTableSize(int size) throws MpcAbortException {
        throw new MpcAbortException("Cannot set table size");
    }

    @Override
    public void updateSketchTable(MpcVector[] sketchTable) {
        this.sketchTable = sketchTable;
    }

    @Override
    public void updateBufferTable(List<MpcVector> bufferTable) {
        this.buffer = bufferTable;
    }

}
