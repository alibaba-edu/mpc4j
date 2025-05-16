package edu.alibaba.mpc4j.work.db.dynamic;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.work.db.dynamic.join.pkpk.PkPkJoinMt;
import edu.alibaba.mpc4j.work.db.dynamic.structure.MaterializedTable;
import edu.alibaba.mpc4j.work.db.dynamic.structure.MaterializedTableType;
import edu.alibaba.mpc4j.work.db.dynamic.structure.UpdateMessage;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Mpc dynamic DB circuit party thread.
 *
 * @author Feng Han
 * @date 2025/3/10
 */
public class DynamicDbCircuitPartyThread extends Thread{
    /**
     * db circuit
     */
    private final DynamicDbCircuitConfig circuitConfig;
    /**
     * z2 party
     */
    private final MpcZ2cParty z2cParty;
    /**
     * update input
     */
    private final UpdateMessage updateMessage;
    /**
     * materialized table
     */
    private final MaterializedTable materializedTable;
    /**
     * update from left table
     */
    private boolean updateFromLeft;
    /**
     * update from left table
     */
    private List<UpdateMessage> updateResult;

    public DynamicDbCircuitPartyThread(DynamicDbCircuitConfig circuitConfig, MpcZ2cParty z2cParty,
                                       UpdateMessage updateMessage, MaterializedTable materializedTable) {
        this.circuitConfig = circuitConfig;
        this.z2cParty = z2cParty;
        this.updateMessage = updateMessage;
        this.materializedTable = materializedTable;
    }

    public void setUpdateFromLeft(boolean updateFromLeft) {
        this.updateFromLeft = updateFromLeft;
    }

    public List<UpdateMessage> getPlainUpdateResult() throws MpcAbortException {
        return updateResult;
    }

    @Override
    public void run() {
        try {
            z2cParty.init();
            DynamicDbCircuit circuit = new DynamicDbCircuit(circuitConfig, z2cParty);

            List<UpdateMessage> shareRes;
            switch (materializedTable.getMaterializedTableType()) {
                case PK_PK_JOIN_MT: {
                    shareRes = circuit.twoTabUpdate(updateMessage, materializedTable, updateFromLeft);
                    break;
                }
                case GLOBAL_AGG_MT, SELECT_MT, GROUP_BY_MT, ORDER_BY_MT: {
                    shareRes = circuit.oneTabUpdate(updateMessage, materializedTable);
                    break;
                }
                default:
                    throw new IllegalStateException("Invalid " + MaterializedTableType.class.getName() + ": " + materializedTable.getMaterializedTableType().name());
            }

            // open the result to obtain the plain data
            PlainZ2Vector[] afterData = Arrays.stream(z2cParty.open(materializedTable.getData()))
                .map(PlainZ2Vector::create)
                .toArray(PlainZ2Vector[]::new);
            materializedTable.updateData(afterData);
            // 如果是pk-pk join
            if(materializedTable instanceof PkPkJoinMt tmpMt){
                PlainZ2Vector[] leftPlain = Arrays.stream(z2cParty.open(tmpMt.getLeftMt().getData()))
                    .map(PlainZ2Vector::create)
                    .toArray(PlainZ2Vector[]::new);
                PlainZ2Vector[] rightPlain = Arrays.stream(z2cParty.open(tmpMt.getRightMt().getData()))
                    .map(PlainZ2Vector::create)
                    .toArray(PlainZ2Vector[]::new);
                tmpMt.getLeftMt().updateData(leftPlain);
                tmpMt.getRightMt().updateData(rightPlain);
            }
            if(shareRes != null){
                updateResult = new LinkedList<>();
                for (UpdateMessage update : shareRes) {
                    PlainZ2Vector[] columnData = Arrays.stream(z2cParty.open(update.getRowData()))
                        .map(PlainZ2Vector::create)
                        .toArray(PlainZ2Vector[]::new);
                    updateResult.add(new UpdateMessage(update.getOperation(), columnData));
                }
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }

}
