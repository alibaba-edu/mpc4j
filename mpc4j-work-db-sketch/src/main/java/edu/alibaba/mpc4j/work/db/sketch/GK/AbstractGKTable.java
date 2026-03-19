package edu.alibaba.mpc4j.work.db.sketch.GK;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTable;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTableType;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractGKTable implements SketchTable {
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
    protected int sketchSize;

    protected double epsilon;

    protected int dataSize;

    public AbstractGKTable(MpcVector[] data, List<MpcVector> buffer) {
        this.data = data;
        this.buffer = buffer;
    }

    public AbstractGKTable(MpcVector[] data) {
        this.data = data;
        this.buffer = new LinkedList<>();
    }

    @Override
    public SketchTableType getSketchTableType(){
        return SketchTableType.GK;
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
        return sketchSize;
    }

    /**
     * @param updateSize
     * @return adjusted new size
     */
    public int resize(int updateSize){
        this.dataSize += updateSize;
        int newSize = (int) (2 * (Math.log(epsilon*dataSize + 2) / epsilon)) + 2;
        this.sketchSize = newSize > this.sketchSize ? newSize : this.sketchSize;
        return this.sketchSize;
    }

    public int getDataSize(){
        return dataSize;
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
        this.sketchSize = size;
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
