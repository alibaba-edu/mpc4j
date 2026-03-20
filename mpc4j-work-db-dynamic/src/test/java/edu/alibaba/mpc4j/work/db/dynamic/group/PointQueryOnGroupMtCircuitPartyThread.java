package edu.alibaba.mpc4j.work.db.dynamic.group;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.circuit.z2.Z2CircuitConfig;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * Point Query On GroupMt Circuit Party Thread
 */
public class PointQueryOnGroupMtCircuitPartyThread extends Thread {
    /**
     * db circuit
     */
    private final Z2CircuitConfig circuitConfig;
    /**
     * z2 party
     */
    private final MpcZ2cParty z2cParty;
    /**
     * materialized table
     */
    private final GroupByMt materializedTable;
    /**
     * required keys
     */
    private final MpcZ2Vector[][] keys;
    /**
     * result
     */
    private BitVector[][] queryResult;


    public PointQueryOnGroupMtCircuitPartyThread(Z2CircuitConfig circuitConfig, MpcZ2cParty z2cParty,
                                                 GroupByMt materializedTable, MpcZ2Vector[][] keys) {
        this.circuitConfig = circuitConfig;
        this.z2cParty = z2cParty;
        this.materializedTable = materializedTable;
        this.keys = keys;
    }

    public BitVector[][] getPlainResult() throws MpcAbortException {
        return queryResult;
    }

    @Override
    public void run() {
        try {
            z2cParty.init();
            PointQueryOnGroupMtCircuit circuit = new PointQueryOnGroupMtCircuit(circuitConfig, z2cParty);

            queryResult = new BitVector[keys.length][];
            for (int i = 0; i < keys.length; i++) {
                MpcZ2Vector[] tmp = circuit.pointQuery(materializedTable, keys[i]);
                queryResult[i] = z2cParty.open(tmp);
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
