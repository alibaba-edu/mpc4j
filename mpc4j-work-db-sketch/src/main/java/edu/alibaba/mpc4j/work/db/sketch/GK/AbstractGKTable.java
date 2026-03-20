package edu.alibaba.mpc4j.work.db.sketch.GK;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTable;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTableType;

import java.util.LinkedList;
import java.util.List;

/**
 * Abstract base class for GK (Greenwald-Khanna) sketch tables in the S³ framework.
 * 
 * This class manages the core data structures for the GK sketch algorithm, including:
 * - The main sketch table containing ordered tuples (k_i, g1_i, g2_i, delta1_i, delta2_i)
 * - A buffer for accumulating new elements before merging
 * - Dynamic sizing based on the running size formula: s' = 2*ln(n/s + 2)/ε + 2
 * 
 * The sketch table grows dynamically with the stream size n to maintain the ε-approximation
 * guarantee for quantile queries.
 * 
 * Reference: "Sketch-based Secure Query Processing for Streaming Data" (S³ framework)
 */
public abstract class AbstractGKTable implements SketchTable {
    /**
     * The main sketch table data containing GK tuples.
     * Organized as [key, g1, g2, delta1, delta2, t, dummy] where:
     * - key: sorted data value
     * - g1, g2: gap values for rank tracking
     * - delta1, delta2: rank uncertainty bounds
     * - t: binary index/timestamp
     * - dummy: flag indicating valid/invalid entries
     */
    private MpcVector[] data;
    
    /**
     * Buffer for accumulating new elements before merging with the main table.
     * Elements are added to the buffer during updates and merged when full.
     * This batching approach improves efficiency by reducing merge operations.
     */
    private List<MpcVector> buffer;
    
    /**
     * Current size of the sketch table (number of tuples).
     * This size grows dynamically based on the running size formula.
     */
    protected int sketchSize;

    /**
     * The error parameter ε for the GK sketch.
     * Defines the approximation guarantee: rank error ≤ ε*n
     */
    protected double epsilon;

    /**
     * Total number of data elements processed so far (stream size n).
     * Used for dynamic sizing and rank calculations.
     */
    protected int dataSize;

    /**
     * Constructs a GK table with initial data and buffer.
     * 
     * @param data the initial sketch table data
     * @param buffer the initial buffer containing pending elements
     */
    public AbstractGKTable(MpcVector[] data, List<MpcVector> buffer) {
        this.data = data;
        this.buffer = buffer;
    }

    /**
     * Constructs a GK table with initial data and empty buffer.
     * 
     * @param data the initial sketch table data
     */
    public AbstractGKTable(MpcVector[] data) {
        this.data = data;
        this.buffer = new LinkedList<>();
    }

    @Override
    public SketchTableType getSketchTableType() {
        return SketchTableType.GK;
    }

    @Override
    public int getBufferIndex() {
        return buffer == null ? 0 : buffer.size();
    }

    @Override
    public MpcVector[] getSketchTable() {
        return data;
    }

    @Override
    public int getTableSize() {
        return sketchSize;
    }

    /**
     * Resizes the sketch table based on the running size formula.
     * 
     * The GK sketch uses dynamic sizing: s' = 2*ln(n/s + 2)/ε + 2
     * where n is the stream size and s is the current size.
     * This ensures the sketch maintains ε-approximation guarantee.
     * 
     * @param updateSize the number of new elements being added
     * @return the new (possibly increased) sketch size
     */
    public int resize(int updateSize){
        this.dataSize += updateSize;
        int newSize = (int) (2 * (Math.log(epsilon*dataSize + 2) / epsilon)) + 2;
        this.sketchSize = Math.max(newSize, this.sketchSize);
        return this.sketchSize;
    }

    /**
     * Gets the total number of data elements processed.
     * 
     * @return the stream size n
     */
    public int getDataSize() {
        return dataSize;
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
    public void setTableSize(int size) {
        this.sketchSize = size;
    }

    @Override
    public void updateSketchTable(MpcVector[] sketchTable) {
        this.data = sketchTable;
    }

    @Override
    public void updateBufferTable(List<MpcVector> bufferTable) {
        this.buffer = bufferTable;
    }
}
