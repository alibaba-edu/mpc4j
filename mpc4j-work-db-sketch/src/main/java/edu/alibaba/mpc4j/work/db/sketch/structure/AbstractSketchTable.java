package edu.alibaba.mpc4j.work.db.sketch.structure;

import edu.alibaba.mpc4j.common.circuit.MpcVector;

import java.util.LinkedList;
import java.util.List;

/**
 * abstract sketch table
 */
public abstract class AbstractSketchTable implements SketchTable {

    // The type of sketch table
    private final SketchTableType sketchTableType;

    // data in sketch table
    private MpcVector[] data;

    // data in sketch buffer
    private List<MpcVector> buffer;

    // size for sketch table
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
    public SketchTableType getSketchTableType(){
        return sketchTableType;
    }

    @Override
    public int getBufferIndex(){
        return buffer.size();
    }

    @Override
    public MpcVector[] getSketchTable(){
        return data;
    }

    @Override
    public int getTableSize(){
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
    public void setTableSize(int size){
        this.size = size;
    }

    @Override
    public void updateSketchTable(MpcVector[] sketchTable){
        this.data = sketchTable;
    }

    @Override
    public void updateBufferTable(List<MpcVector> bufferTable) {
        this.buffer = bufferTable;
    }

}
