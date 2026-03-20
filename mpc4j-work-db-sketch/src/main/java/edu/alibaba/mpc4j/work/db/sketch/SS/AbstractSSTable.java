package edu.alibaba.mpc4j.work.db.sketch.SS;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTable;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTableType;

import java.util.LinkedList;
import java.util.List;

/**
 * Abstract SpaceSaving (SS) sketch table.
 * 
 * <p>This class represents the data structure for SS sketch in the S³ framework.
 * It maintains two main components:
 * <ul>
 *   <li><b>Sketch Table:</b> Stores up to s (key, value) pairs with highest estimated frequencies</li>
 *   <li><b>Buffer:</b> Temporarily stores new stream elements before merging into the sketch table</li>
 * </ul>
 * 
 * <p>The buffer accumulates elements and triggers the Merge protocol when it reaches capacity,
 * following Algorithm 4 to efficiently update the sketch table while maintaining the space constraint.
 */
public abstract class AbstractSSTable implements SketchTable {
    /**
     * Data in the sketch table.
     * 
     * <p>This array contains the key-value pairs stored in the sketch.
     * The structure is [key_bits..., value_bits...] where:
     * - key_bits: bit vectors representing the key (item identifier)
     * - value_bits: bit vectors representing the frequency/count value
     * 
     * <p>The table maintains at most s entries, where s = 2^logSketchSize.
     */
    private MpcVector[] data;
    
    /**
     * Data in the sketch buffer.
     * 
     * <p>This buffer temporarily stores incoming stream elements before they are merged
     * into the main sketch table. Elements are added one at a time, and when the buffer
     * reaches capacity (size s), the Merge protocol is triggered to flush the buffer.
     * 
     * <p>Buffering improves efficiency by batching merge operations rather than performing
     * them for every single update.
     */
    private List<MpcVector> buffer;
    
    /**
     * Logarithm of the sketch table size.
     * 
     * <p>The actual table size is s = 2^logSketchSize. Using log representation allows
     * efficient size calculations and bit-level operations.
     * 
     * <p>This parameter determines the space bound and error guarantee:
     * - Larger s → better accuracy but more storage
     * - Error guarantee: estimation error ≤ n/s
     */
    protected int logSketchSize;

    /**
     * Constructor creating an SS table with both data and buffer.
     *
     * @param data   the initial sketch table data
     * @param buffer the initial buffer data
     */
    public AbstractSSTable(MpcVector[] data, List<MpcVector> buffer) {
        this.data = data;
        this.buffer = buffer;
    }

    /**
     * Constructor creating an SS table with data only (empty buffer).
     *
     * @param data the initial sketch table data
     */
    public AbstractSSTable(MpcVector[] data) {
        this.data = data;
        this.buffer = new LinkedList<>();
    }

    @Override
    public SketchTableType getSketchTableType() {
        return SketchTableType.SS;
    }

    /**
     * Get the current buffer index (number of elements in buffer).
     * 
     * <p>When this reaches the table size, the buffer is full and Merge should be triggered.
     *
     * @return the number of elements currently in the buffer
     */
    @Override
    public int getBufferIndex() {
        return buffer == null ? 0 : buffer.size();
    }

    /**
     * Get the sketch table data array.
     *
     * @return the key-value pairs in the sketch table
     */
    @Override
    public MpcVector[] getSketchTable() {
        return data;
    }

    /**
     * Get the actual table size.
     * 
     * @return the table size s = 2^logSketchSize
     */
    @Override
    public int getTableSize() {
        return 1 << logSketchSize;
    }

    /**
     * Get the log of the table size.
     *
     * @return logSketchSize where table size = 2^logSketchSize
     */
    public int getLogTableSize() {
        return logSketchSize;
    }

    /**
     * Get the buffer table.
     *
     * @return the list of buffered elements
     */
    @Override
    public List<MpcVector> getBufferTable() {
        return buffer;
    }

    /**
     * Clear all elements from the buffer.
     * 
     * <p>This is called after the Merge protocol flushes the buffer into the sketch table.
     */
    @Override
    public void clearBufferTable() {
        buffer.clear();
    }

    /**
     * Set the log table size.
     *
     * @param size the log of the table size to set
     */
    @Override
    public void setTableSize(int size) {
        this.logSketchSize = size;
    }

    /**
     * Update the sketch table with new data.
     * 
     * <p>This is called after the Merge protocol to replace the old table with
     * the merged and compacted version.
     *
     * @param sketchTable the new sketch table data
     */
    @Override
    public void updateSketchTable(MpcVector[] sketchTable) {
        this.data = sketchTable;
    }

    /**
     * Update the buffer table with new data.
     *
     * @param bufferTable the new buffer data
     */
    @Override
    public void updateBufferTable(List<MpcVector> bufferTable) {
        this.buffer = bufferTable;
    }
}
