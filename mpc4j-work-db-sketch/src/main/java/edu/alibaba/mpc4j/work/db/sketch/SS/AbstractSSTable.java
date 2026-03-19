package edu.alibaba.mpc4j.work.db.sketch.SS;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTable;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTableType;

import java.util.LinkedList;
import java.util.List;

/**
 * abstract MG table
 */
public abstract class AbstractSSTable implements SketchTable {
    /**
     * data in sketch table
     */
    private MpcVector[] data;
    /**
     * data in sketch buffer
     */
    private List<MpcVector> buffer;
    /**
     * log of size for sketch table
     */
    protected int logSketchSize;

    public AbstractSSTable(MpcVector[] data, List<MpcVector> buffer) {
        this.data = data;
        this.buffer = buffer;
    }

    public AbstractSSTable(MpcVector[] data) {
        this.data = data;
        this.buffer = new LinkedList<>();
    }

    @Override
    public SketchTableType getSketchTableType(){
        return SketchTableType.MG;
    }

    @Override
    public int getBufferIndex(){
        return buffer == null ? 0 : buffer.size();
    }

    @Override
    public MpcVector[] getSketchTable(){
        return data;
    }

    @Override
    public int getTableSize(){
        return 1 << logSketchSize;
    }

    public int getLogTableSize(){
        return logSketchSize;
    }

    @Override
    public List<MpcVector> getBufferTable(){
        return buffer;
    }

    @Override
    public void clearBufferTable() {
        buffer.clear();
    }

    @Override
    public void setTableSize(int size){
        this.logSketchSize = size;
    }

    @Override
    public void updateSketchTable(MpcVector[] sketchTable){
        this.data = sketchTable;
    }

    @Override
    public void updateBufferTable(List<MpcVector> bufferTable){
        this.buffer = bufferTable;
    }
}
