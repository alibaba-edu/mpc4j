package edu.alibaba.mpc4j.work.db.dynamic.join.pkpk;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.work.db.dynamic.AbstractDynamicDbCircuit;
import edu.alibaba.mpc4j.work.db.dynamic.structure.OperationEnum;
import edu.alibaba.mpc4j.work.db.dynamic.structure.UpdateMessage;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * shortcut dynamic db pk-pk join circuit.
 *
 * @author Feng Han
 * @date 2025/3/10
 */
public class Zgc24DynamicDbPkPkJoinCircuit extends AbstractDynamicDbCircuit implements DynamicDbPkPkJoinCircuit {
    /**
     * PkPkJoin Materialized Table
     */
    private PkPkJoinMt pkPkJoinMt;
    /**
     * self payload indexes
     */
    private int[] selfPayloadIndexes;
    /**
     * other payload indexes
     */
    private int[] otherPayloadIndexes;
    /**
     * self input table
     */
    private JoinInputMt selfInputTab;
    /**
     * self input table
     */
    private JoinInputMt otherInputTab;


    public Zgc24DynamicDbPkPkJoinCircuit(Z2IntegerCircuit circuit) {
        super(circuit);
    }

    @Override
    public List<UpdateMessage> update(UpdateMessage updateMessage, PkPkJoinMt pkPkJoinMt, boolean updateFromLeft) throws MpcAbortException {
        setInputs(updateMessage, pkPkJoinMt);
        if (updateFromLeft) {
            MathPreconditions.checkEqual("updateMessage.getRowData().length", "materializedTable.getData().length",
                updateMessage.getRowData().length, pkPkJoinMt.getLeftMt().getData().length);
        } else {
            MathPreconditions.checkEqual("updateMessage.getRowData().length", "materializedTable.getData().length",
                updateMessage.getRowData().length, pkPkJoinMt.getRightMt().getData().length);
        }
        this.pkPkJoinMt = pkPkJoinMt;
        selfPayloadIndexes = updateFromLeft ? pkPkJoinMt.getLeftValueIndexes() : pkPkJoinMt.getRightValueIndexes();
        otherPayloadIndexes = updateFromLeft ? pkPkJoinMt.getRightValueIndexes() : pkPkJoinMt.getLeftValueIndexes();
        selfInputTab = updateFromLeft ? pkPkJoinMt.getLeftMt() : pkPkJoinMt.getRightMt();
        otherInputTab = updateFromLeft ? pkPkJoinMt.getRightMt() : pkPkJoinMt.getLeftMt();

        return switch (updateMessage.getOperation()) {
            case INSERT -> insert();
            case DELETE -> delete();
        };
    }

    private List<UpdateMessage> insert() throws MpcAbortException {
        List<UpdateMessage> result = new LinkedList<>();
        // add new tuple to the source input table
        for (int i = 0; i < selfInputTab.getData().length; i++) {
            selfInputTab.getData()[i].merge(updateMessage.getRowData()[i]);
        }
        // compare to get B_i[f]
        MpcZ2Vector[] otherKeys = Arrays.stream(otherInputTab.getKeyIndexes())
            .mapToObj(i -> otherInputTab.getData()[i])
            .toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] updateKeys = extendUpdateMsgData(otherKeys[0].bitNum(), selfInputTab.getKeyIndexes());
        MpcZ2Vector equalFlag = circuit.eq(otherKeys, updateKeys);
        equalFlag = z2cParty.and(equalFlag, otherInputTab.getData()[otherInputTab.getValidityIndex()]);
        // otherInputTab payload, get ur'[p_B]
        MpcZ2Vector[] otherPayloads = Arrays.stream(otherInputTab.getValueIndexes())
            .mapToObj(i -> otherInputTab.getData()[i])
            .toArray(MpcZ2Vector[]::new);
        otherPayloads = z2cParty.and(equalFlag, otherPayloads);
        for (int i = 0; i < otherPayloads.length; i++) {
            otherPayloads[i] = z2cParty.xorSelfAllElement(otherPayloads[i]);
        }
        // get ur'[v]
        MpcZ2Vector xorBf = z2cParty.xorSelfAllElement(equalFlag);
        MpcZ2Vector urv = z2cParty.and(xorBf, updateMessage.getRowData()[selfInputTab.getValidityIndex()]);
        // update join result
        MpcZ2Vector[] updateMsg = new MpcZ2Vector[dim];
        for (int i = 0; i < pkPkJoinMt.getKeyIndexes().length; i++) {
            updateMsg[pkPkJoinMt.getKeyIndexes()[i]] = updateMessage.getRowData()[selfInputTab.getKeyIndexes()[i]];
        }
        for (int i = 0; i < selfPayloadIndexes.length; i++) {
            updateMsg[selfPayloadIndexes[i]] = updateMessage.getRowData()[selfInputTab.getValueIndexes()[i]];
        }
        for (int i = 0; i < otherPayloadIndexes.length; i++) {
            updateMsg[otherPayloadIndexes[i]] = otherPayloads[i];
        }
        updateMsg[pkPkJoinMt.getValidityIndex()] = urv;
        for (int i = 0; i < pkPkJoinMt.getData().length; i++) {
            pkPkJoinMt.getData()[i].merge(updateMsg[i]);
        }

        // forward information
        if (!pkPkJoinMt.isOutputTable()) {
            result.add(new UpdateMessage(OperationEnum.INSERT, updateMsg));
        }
        return result;
    }

    private List<UpdateMessage> delete() throws MpcAbortException {
        List<UpdateMessage> result = new LinkedList<>();

        // compare to get A_i[v]
        MpcZ2Vector[] selfKeys = Arrays.stream(selfInputTab.getKeyIndexes())
            .mapToObj(i -> selfInputTab.getData()[i])
            .toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] updateKeys = extendUpdateMsgData(selfKeys[0].bitNum(), selfInputTab.getKeyIndexes());
        MpcZ2Vector equalFlag = circuit.eq(selfKeys, updateKeys);
        equalFlag = z2cParty.and(equalFlag, extendUpdateMsgData(equalFlag.bitNum(), selfInputTab.getValidityIndex())[0]);
        z2cParty.noti(equalFlag);
        equalFlag = z2cParty.and(equalFlag, selfInputTab.getData()[selfInputTab.getValidityIndex()]);
        selfInputTab.setColumnData(equalFlag, selfInputTab.getValidityIndex());

        // update J_i[f] in the join result
        MpcZ2Vector[] joinKeys = Arrays.stream(pkPkJoinMt.getKeyIndexes())
            .mapToObj(i -> pkPkJoinMt.getData()[i])
            .toArray(MpcZ2Vector[]::new);
        updateKeys = extendUpdateMsgData(joinKeys[0].bitNum(), selfInputTab.getKeyIndexes());
        MpcZ2Vector jf = circuit.eq(joinKeys, updateKeys);
        jf = z2cParty.and(jf, pkPkJoinMt.getData()[pkPkJoinMt.getValidityIndex()]);
        jf = z2cParty.and(jf, extendUpdateMsgData(jf.bitNum(), selfInputTab.getValidityIndex())[0]);

        if (!pkPkJoinMt.isOutputTable()) {
            MpcZ2Vector[] otherPayloads = Arrays.stream(otherPayloadIndexes)
                .mapToObj(i -> pkPkJoinMt.getData()[i])
                .toArray(MpcZ2Vector[]::new);
            otherPayloads = z2cParty.and(jf, otherPayloads);
            for (int i = 0; i < otherPayloads.length; i++) {
                otherPayloads[i] = z2cParty.xorSelfAllElement(otherPayloads[i]);
            }
            MpcZ2Vector[] updateMsg = new MpcZ2Vector[dim];
            for (int i = 0; i < pkPkJoinMt.getKeyIndexes().length; i++) {
                updateMsg[pkPkJoinMt.getKeyIndexes()[i]] = updateMessage.getRowData()[selfInputTab.getKeyIndexes()[i]];
            }
            for (int i = 0; i < selfPayloadIndexes.length; i++) {
                updateMsg[selfPayloadIndexes[i]] = updateMessage.getRowData()[selfInputTab.getValueIndexes()[i]];
            }
            for (int i = 0; i < otherPayloadIndexes.length; i++) {
                updateMsg[otherPayloadIndexes[i]] = otherPayloads[i];
            }
            updateMsg[pkPkJoinMt.getValidityIndex()] = z2cParty.xorSelfAllElement(jf);

            result.add(new UpdateMessage(OperationEnum.DELETE, updateMsg));
        }

        MpcZ2Vector jv = z2cParty.and(z2cParty.not(jf), pkPkJoinMt.getData()[pkPkJoinMt.getValidityIndex()]);
        pkPkJoinMt.setColumnData(jv, pkPkJoinMt.getValidityIndex());

        return result;
    }
}
