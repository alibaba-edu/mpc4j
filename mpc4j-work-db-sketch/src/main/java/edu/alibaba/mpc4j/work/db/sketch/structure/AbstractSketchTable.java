package edu.alibaba.mpc4j.work.db.sketch.structure;

import edu.alibaba.mpc4j.common.circuit.MpcVector;

import java.util.LinkedList;
import java.util.List;

/**
 * Abstract base implementation of the {@link SketchTable} interface.
 * <p>
 * This class manages the two core components of a sketch in the S³ framework:
 * <ul>
 *   <li><b>data</b>: the sketch table itself, stored as an array of secret-shared {@link MpcVector}s.
 *       Each element represents a column of the sketch (e.g., for CMS: hash-indexed counters;
 *       for GK: key, g1, g2, delta1, delta2, t, dummy columns).</li>
 *   <li><b>buffer</b>: a list of incoming stream updates waiting to be batch-merged into the sketch.
 *       Each {@link MpcVector} in the buffer represents one column of buffered data.</li>
 * </ul>
 * The {@code size} field represents the maximum number of rows in the sketch table,
 * which determines when the buffer triggers a Merge operation.
 *
 * @see SketchTable
 */
public abstract class AbstractSketchTable implements SketchTable {

    /**
     * the type of this sketch table (CMS, HLL, SS, or GK)
     */
    private final SketchTableType sketchTableType;

    /**
     * the sketch data array, where each MpcVector represents a column of the sketch table
     * (stored in secret-shared form for secure computation)
     */
    private MpcVector[] data;

    /**
     * the buffer for incoming stream updates, where each MpcVector represents a column of buffered data.
     * When buffer size reaches {@code size}, a Merge protocol is triggered.
     */
    private List<MpcVector> buffer;

    /**
     * the maximum number of rows in the sketch table.
     * For CMS/HLL, this equals the sketch size s.
     * For SS/GK, this may grow dynamically based on the stream size n.
     */
    protected int size;

    public AbstractSketchTable(SketchTableType sketchTableType, MpcVector[] data, List<MpcVector> buffer) {
        this.sketchTableType = sketchTableType;
        this.data = data;
        this.buffer = buffer;
    }

    public AbstractSketchTable(SketchTableType sketchTableType, MpcVector[] data) {
        this.sketchTableType = sketchTableType;
        this.data = data;
        this.buffer = new LinkedList<>();
    }

    @Override
    public SketchTableType getSketchTableType() {
        return sketchTableType;
    }

    @Override
    public int getBufferIndex() {
        return buffer.size();
    }

    @Override
    public MpcVector[] getSketchTable() {
        return data;
    }

    @Override
    public int getTableSize() {
        return size;
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
        this.size = size;
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
