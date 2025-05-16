package edu.alibaba.mpc4j.work.db.dynamic.select;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.work.db.dynamic.AbstractDynamicDbCircuit;
import edu.alibaba.mpc4j.work.db.dynamic.structure.UpdateMessage;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * shortcut dynamic db select circuit.
 *
 * @author Feng Han
 * @date 2025/3/10
 */
public class Zgc24DynamicDbSelectCircuit extends AbstractDynamicDbCircuit implements DynamicDbSelectCircuit {
    /**
     * PkPkJoin Materialized Table
     */
    private SelectMt selectMt;

    public Zgc24DynamicDbSelectCircuit(Z2IntegerCircuit circuit) {
        super(circuit);
    }

    @Override
    public List<UpdateMessage> update(UpdateMessage updateMessage, SelectMt selectMt) throws MpcAbortException {
        setInputs(updateMessage, selectMt);
        MathPreconditions.checkEqual("updateMessage.getRowData().length", "materializedTable.getData().length",
            updateMessage.getRowData().length, selectMt.getData().length);
        this.selectMt = selectMt;
        return switch (updateMessage.getOperation()) {
            case INSERT -> insert();
            case DELETE -> delete();
        };
    }

    private List<UpdateMessage> insert() throws MpcAbortException {
        List<UpdateMessage> result = new LinkedList<>();

        MpcZ2Vector[] funInput = new MpcZ2Vector[selectMt.getValueIndexes().length + 1];
        IntStream.range(0, selectMt.getValueIndexes().length).forEach(i -> funInput[i] = updateMessage.getRowData()[selectMt.getValueIndexes()[i]]);
        funInput[selectMt.getValueIndexes().length] = updateMessage.getRowData()[selectMt.getValidityIndex()];
        MpcZ2Vector flag = selectMt.getFunction().apply(circuit, funInput);

        flag = z2cParty.and(flag, updateMessage.getRowData()[selectMt.getValidityIndex()]);
        updateMessage.getRowData()[selectMt.getValidityIndex()] = flag;

        for (int i = 0; i < dim; i++) {
            selectMt.getData()[i].merge(updateMessage.getRowData()[i]);
        }
        if (!selectMt.isOutputTable()) {
            result.add(updateMessage);
        }
        return result;
    }

    private List<UpdateMessage> delete() throws MpcAbortException {
        List<UpdateMessage> result = new LinkedList<>();

        // compare id key
        MpcZ2Vector[] selfKeys = Arrays.stream(selectMt.getIdIndexes())
            .mapToObj(i -> selectMt.getData()[i])
            .toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] updateKeys = extendUpdateMsgData(selfKeys[0].bitNum(), selectMt.getIdIndexes());
        MpcZ2Vector equalFlag = circuit.eq(selfKeys, updateKeys);
        z2cParty.noti(equalFlag);
        equalFlag = z2cParty.and(equalFlag, selectMt.getData()[selectMt.getValidityIndex()]);
        selectMt.setColumnData(equalFlag, selectMt.getValidityIndex());
        if (!selectMt.isOutputTable()) {
            result.add(updateMessage);
        }

        return result;
    }
}
