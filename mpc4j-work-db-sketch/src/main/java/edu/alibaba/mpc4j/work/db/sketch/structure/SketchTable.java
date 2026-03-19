package edu.alibaba.mpc4j.work.db.sketch.structure;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.List;

/**
 * sketch table interface
 */
public interface SketchTable {
    /**
     * get sketch table type
     *
     * @return sketch table type
     */
    SketchTableType getSketchTableType();
    /**
     * get the current buffer index, in fact is the current buffer size
     *
     * @return buffer index
     */
    int getBufferIndex();
    /**
     * get the sketch table size
     *
     * @return sketch table size
     */
    int getTableSize();
    /**
     * get the sketch table
     *
     * @return sketch table
     */
    MpcVector[] getSketchTable();
    /**
     * get the buffer table
     *
     * @return buffer table
     */
    List<MpcVector> getBufferTable();
    /**
     * clear the buffer table
     */
    void clearBufferTable();
    /**
     * set the maximum sketch table size
     *
     * @param tableSize maximum sketch table size
     */
    void setTableSize(int tableSize) throws MpcAbortException;
    /**
     * update the sketch table
     *
     * @param sketchTable sketch table
     */
    void updateSketchTable(MpcVector[] sketchTable);
    /**
     * update the buffer table
     *
     * @param bufferTable buffer table
     */
    void updateBufferTable(List<MpcVector> bufferTable);
}
