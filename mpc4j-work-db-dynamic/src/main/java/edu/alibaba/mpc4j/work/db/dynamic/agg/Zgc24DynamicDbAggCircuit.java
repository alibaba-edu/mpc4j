package edu.alibaba.mpc4j.work.db.dynamic.agg;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.work.db.dynamic.AbstractDynamicDbCircuit;
import edu.alibaba.mpc4j.work.db.dynamic.structure.AggregateEnum;
import edu.alibaba.mpc4j.work.db.dynamic.structure.UpdateMessage;

import java.util.Arrays;
import java.util.List;

/**
 * dynamic db agg circuit.
 *
 * @author Feng Han
 * @date 2025/3/10
 */
public class Zgc24DynamicDbAggCircuit extends AbstractDynamicDbCircuit implements DynamicDbAggCircuit {

    /**
     * order-by Materialized Table
     */
    private AggMt aggMt;

    public Zgc24DynamicDbAggCircuit(Z2IntegerCircuit circuit) {
        super(circuit);
    }

    @Override
    public List<UpdateMessage> update(UpdateMessage updateMessage, AggMt aggMt) throws MpcAbortException {
        setInputs(updateMessage, aggMt);
        MathPreconditions.checkEqual("updateMessage.getRowData().length", "materializedTable.getData().length",
            updateMessage.getRowData().length, aggMt.getData().length);
        this.aggMt = aggMt;
        return switch (updateMessage.getOperation()) {
            case INSERT -> insert();
            case DELETE -> delete();
        };
    }

    private List<UpdateMessage> insert() throws MpcAbortException {
        MpcZ2Vector[] xs = Arrays.stream(aggMt.getValueIndexes())
            .mapToObj(i -> updateMessage.getRowData()[i])
            .toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] ys = Arrays.stream(aggMt.getValueIndexes())
            .mapToObj(i -> aggMt.getData()[i])
            .toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] z;
        if(aggMt.getAggType().equals(AggregateEnum.SUM)) {
            xs = z2cParty.and(updateMessage.getRowData()[aggMt.getValidityIndex()], xs);
            MpcZ2Vector[] originalFlags = new MpcZ2Vector[aggMt.getValueIndexes().length];
            Arrays.fill(originalFlags, aggMt.getData()[aggMt.getValidityIndex()]);
            ys = z2cParty.and(originalFlags, ys);
            z = circuit.add(xs, ys);
        }else{
            MpcZ2Vector leqFlag = circuit.leq(ys, xs);
            if (aggMt.getAggType().equals(AggregateEnum.MAX)) {
                z2cParty.noti(leqFlag);
            }
            leqFlag = z2cParty.and(aggMt.getData()[aggMt.getValidityIndex()], leqFlag);
            leqFlag = z2cParty.or(z2cParty.not(updateMessage.getRowData()[aggMt.getValidityIndex()]), leqFlag);
            z = circuit.mux(xs, ys, leqFlag);
        }
        for(int i = 0; i < aggMt.getValueIndexes().length; i++) {
            aggMt.setColumnData(z[i], aggMt.getValueIndexes()[i]);
        }
        aggMt.setColumnData(z2cParty.or(updateMessage.getRowData()[aggMt.getValidityIndex()], aggMt.getData()[aggMt.getValidityIndex()]),
            aggMt.getValidityIndex());
        return null;
    }

    private List<UpdateMessage> delete() throws MpcAbortException {
        if (aggMt.getAggType().equals(AggregateEnum.MAX) || aggMt.getAggType().equals(AggregateEnum.MIN)) {
            throw new MpcAbortException("group by extreme can not be updated");
        }
        MpcZ2Vector[] xs = Arrays.stream(aggMt.getValueIndexes())
            .mapToObj(i -> updateMessage.getRowData()[i])
            .toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] ys = Arrays.stream(aggMt.getValueIndexes())
            .mapToObj(i -> aggMt.getData()[i])
            .toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] flags = new MpcZ2Vector[aggMt.getValueIndexes().length];
        Arrays.fill(flags, updateMessage.getRowData()[aggMt.getValidityIndex()]);
        xs = z2cParty.and(flags, xs);
        MpcZ2Vector[] z = circuit.sub(ys, xs);
        for(int i = 0; i < aggMt.getValueIndexes().length; i++) {
            aggMt.setColumnData(z[i], aggMt.getValueIndexes()[i]);
        }
        return null;
    }
}
