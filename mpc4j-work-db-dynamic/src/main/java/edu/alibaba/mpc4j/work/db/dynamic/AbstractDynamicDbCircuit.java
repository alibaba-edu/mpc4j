package edu.alibaba.mpc4j.work.db.dynamic;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.work.db.dynamic.structure.MaterializedTable;
import edu.alibaba.mpc4j.work.db.dynamic.structure.UpdateMessage;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * abstract dynamic db party
 *
 * @author Feng Han
 * @date 2025/3/6
 */
public abstract class AbstractDynamicDbCircuit {
    /**
     * z2c party
     */
    protected final MpcZ2cParty z2cParty;
    /**
     * z2 circuit
     */
    protected final Z2IntegerCircuit circuit;
    /**
     * input data to be updated
     */
    protected UpdateMessage updateMessage;
    /**
     * dimension of the database
     */
    protected int dim;

    public AbstractDynamicDbCircuit(DynamicDbCircuitConfig config, MpcZ2cParty z2cParty) {
        this.z2cParty = z2cParty;
        this.circuit = new Z2IntegerCircuit(z2cParty, config.getCircuitConfig());
    }

    public AbstractDynamicDbCircuit(Z2IntegerCircuit circuit) {
        this.z2cParty = circuit.getParty();
        this.circuit = circuit;
    }

    protected void setInputs(UpdateMessage updateMessage, MaterializedTable materializedTable) {
        this.updateMessage = updateMessage;
        dim = materializedTable.getData().length;
    }

    /**
     * extend update message data into the dimension and length of the materialized table
     *
     * @param num the number of rows in the materialized table
     * @param indexes the indexes of column to be extended
     * @return the extended update message data
     */
    protected MpcZ2Vector[] extendUpdateMsgData(int num, int... indexes) {
        Preconditions.checkArgument(indexes.length > 0);
        MpcZ2Vector[] updateMsgData = updateMessage.getRowData();
        BitVector[][] shareData = Arrays.stream(indexes)
            .mapToObj(i -> updateMsgData[i])
            .map(MpcZ2Vector::getBitVectors)
            .map(ea -> Arrays.stream(ea)
                .map(one -> one.get(0) ? BitVectorFactory.createOnes(num) : BitVectorFactory.createZeros(num))
                .toArray(BitVector[]::new)
            )
            .toArray(BitVector[][]::new);
        return Arrays.stream(shareData).map(ea -> z2cParty.create(updateMsgData[0].isPlain(), ea)).toArray(MpcZ2Vector[]::new);
    }

    /**
     * extend update message data into the dimension and length of the materialized table
     *
     * @param num the number of rows in the materialized table
     * @return the extended update message data
     */
    protected MpcZ2Vector[] extendUpdateMsgData(int num) {
        return extendUpdateMsgData(num, IntStream.range(0, updateMessage.getRowData().length).toArray());
    }
}
