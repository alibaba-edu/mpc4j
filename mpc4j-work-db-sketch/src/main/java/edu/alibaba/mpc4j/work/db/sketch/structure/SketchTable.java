package edu.alibaba.mpc4j.work.db.sketch.structure;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.List;

/**
 * Interface for sketch data structures in the S³ (Sketch-based Secure Streaming) framework.
 * <p>
 * A sketch table consists of two parts:
 * <ul>
 *   <li><b>Sketch (data)</b>: a compact summary of the stream data with bounded size s, stored as secret-shared MPC vectors.</li>
 *   <li><b>Buffer</b>: a temporary storage for incoming stream updates before they are batch-merged into the sketch.</li>
 * </ul>
 * When the buffer is full (i.e., buffer size reaches the table size s), a Merge protocol is triggered
 * to incorporate the buffered updates into the sketch using the group-then-process strategy
 * (create table → sort → segmented prefix operation → compact).
 * <p>
 * Supported sketch types include CMS (Count-Min Sketch), HLL (HyperLogLog), SS (SpaceSaving), and GK (Greenwald-Khanna).
 *
 * @see AbstractSketchTable
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
