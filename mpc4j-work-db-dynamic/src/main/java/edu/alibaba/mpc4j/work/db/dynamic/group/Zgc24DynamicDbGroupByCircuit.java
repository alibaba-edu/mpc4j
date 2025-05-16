package edu.alibaba.mpc4j.work.db.dynamic.group;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.work.db.dynamic.AbstractDynamicDbCircuit;
import edu.alibaba.mpc4j.work.db.dynamic.structure.AggregateEnum;
import edu.alibaba.mpc4j.work.db.dynamic.structure.OperationEnum;
import edu.alibaba.mpc4j.work.db.dynamic.structure.UpdateMessage;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * shortcut dynamic db group by circuit.
 * <p>
 * Shortcut: Making MPC-based Collaborative Analytics Efficient on Dynamic Databases
 * Peizhao Zhou et el.
 * CCS 2024
 * </p>
 *
 * @author Feng Han
 * @date 2025/3/7
 */
public class Zgc24DynamicDbGroupByCircuit extends AbstractDynamicDbCircuit implements DynamicDbGroupByCircuit {
    /**
     * order-by Materialized Table
     */
    private GroupByMt groupByMt;
    /**
     * original Materialized Table
     */
    private MpcZ2Vector[] originalData;
    /**
     * extended update message, such that the row number equals to originalData
     */
    private MpcZ2Vector[] extendedUpDate;

    public Zgc24DynamicDbGroupByCircuit(Z2IntegerCircuit circuit) {
        super(circuit);
    }

    @Override
    public List<UpdateMessage> update(UpdateMessage updateMessage, GroupByMt groupByMt) throws MpcAbortException {
        setInputs(updateMessage, groupByMt);
        MathPreconditions.checkEqual("updateMessage.getRowData().length", "materializedTable.getData().length",
            updateMessage.getRowData().length, groupByMt.getData().length);
        this.groupByMt = groupByMt;
        int currentRowNum = groupByMt.getData()[0].bitNum();
        originalData = groupByMt.getData();
        extendedUpDate = extendUpdateMsgData(currentRowNum);
        return switch (updateMessage.getOperation()) {
            case INSERT -> insert();
            case DELETE -> delete();
        };
    }

    private List<UpdateMessage> insert() throws MpcAbortException {
        List<UpdateMessage> result = new LinkedList<>();

        // store the sum of value attributes before all operation
        MpcZ2Vector[] originalValuesXorRes = groupByMt.isOutputTable()
            ? null
            : Arrays.stream(groupByMt.getValueIndexes())
            .mapToObj(i -> originalData[i])
            .map(z2cParty::xorSelfAllElement)
            .toArray(MpcZ2Vector[]::new);

        // get the equality flag
        MpcZ2Vector idFlag = getIdFlag();

        // get the value attributes
        MpcZ2Vector[] tobeUpdateValue = Arrays.stream(groupByMt.getValueIndexes())
            .mapToObj(i -> extendedUpDate[i])
            .toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] originalValues = Arrays.stream(groupByMt.getValueIndexes())
            .mapToObj(i -> originalData[i])
            .toArray(MpcZ2Vector[]::new);
        // update the value attributes
        if (groupByMt.getAggType().equals(AggregateEnum.SUM)) {
            tobeUpdateValue = z2cParty.and(idFlag, tobeUpdateValue);
            originalValues = circuit.add(originalValues, tobeUpdateValue);
        } else {
            MpcZ2Vector compFlag = circuit.leq(originalValues, tobeUpdateValue);
            if (groupByMt.getAggType().equals(AggregateEnum.MIN)) {
                z2cParty.noti(compFlag);
            }
            // check whether extendIdFlag is true
            MpcZ2Vector updateFlag = z2cParty.and(idFlag, compFlag);
            originalValues = circuit.mux(originalValues, tobeUpdateValue, updateFlag);
        }
        // back to the original positions
        for (int i = 0; i < groupByMt.getValueIndexes().length; i++) {
            originalData[groupByMt.getValueIndexes()[i]] = originalValues[i];
        }

        if (!groupByMt.isOutputTable()) {
            // get the update message for the next level, should invoke before merge the last row
            result.addAll(getCommonUpdateMsg(idFlag, originalValuesXorRes));
        }

        // merge the update message into the last data
        MpcZ2Vector xorF = z2cParty.xorSelfAllElement(idFlag);
        MpcZ2Vector[] upData = updateMessage.getRowData();
        upData[groupByMt.getValidityIndex()] = z2cParty.and(z2cParty.not(xorF), upData[groupByMt.getValidityIndex()]);
        for (int i = 0; i < originalData.length; i++) {
            originalData[i].merge(upData[i]);
        }
        if (!groupByMt.isOutputTable()) {
            result.add(new UpdateMessage(OperationEnum.INSERT, upData));
        }
        return result;
    }

    private List<UpdateMessage> delete() throws MpcAbortException {
        if (groupByMt.getAggType().equals(AggregateEnum.MAX) || groupByMt.getAggType().equals(AggregateEnum.MIN)) {
            throw new MpcAbortException("group by extreme can not be updated");
        }

        // store the sum of value attributes before all operation
        MpcZ2Vector[] originalValuesXorRes = groupByMt.isOutputTable()
            ? null
            : Arrays.stream(groupByMt.getValueIndexes())
            .mapToObj(i -> originalData[i])
            .map(z2cParty::xorSelfAllElement)
            .toArray(MpcZ2Vector[]::new);

        // get the equality flag
        MpcZ2Vector idFlag = getIdFlag();

        // get the value attributes
        MpcZ2Vector[] tobeUpdateValue = Arrays.stream(groupByMt.getValueIndexes())
            .mapToObj(i -> extendedUpDate[i])
            .toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] originalValues = Arrays.stream(groupByMt.getValueIndexes())
            .mapToObj(i -> originalData[i])
            .toArray(MpcZ2Vector[]::new);
        // update the value attributes
        tobeUpdateValue = z2cParty.and(idFlag, tobeUpdateValue);
        originalValues = circuit.sub(originalValues, tobeUpdateValue);
        // back to the original positions
        for (int i = 0; i < groupByMt.getValueIndexes().length; i++) {
            originalData[groupByMt.getValueIndexes()[i]] = originalValues[i];
        }

        return groupByMt.isOutputTable()
            ? null
            : getCommonUpdateMsg(idFlag, originalValuesXorRes);
    }

    private MpcZ2Vector getIdFlag() throws MpcAbortException {
        // get the equality flag
        MpcZ2Vector idFlag = circuit.eq(
            Arrays.stream(groupByMt.getGroupKeyIndexes()).mapToObj(i -> originalData[i]).toArray(MpcZ2Vector[]::new),
            Arrays.stream(groupByMt.getGroupKeyIndexes()).mapToObj(i -> extendedUpDate[i]).toArray(MpcZ2Vector[]::new));
        idFlag = z2cParty.and(idFlag, originalData[groupByMt.getValidityIndex()]);
        idFlag = z2cParty.and(idFlag, extendedUpDate[groupByMt.getValidityIndex()]);
        return idFlag;
    }

    private List<UpdateMessage> getCommonUpdateMsg(MpcZ2Vector idFlag, MpcZ2Vector[] originalValuesXorRes) throws MpcAbortException {
        List<UpdateMessage> result = new LinkedList<>();
        // get the update message for the next level
        MpcZ2Vector[] insertMsg = z2cParty.and(idFlag, originalData);
        MpcZ2Vector[] ur1 = Arrays.stream(insertMsg)
            .map(z2cParty::xorSelfAllElement)
            .toArray(MpcZ2Vector[]::new);
        // we obtain ur2 in a different way.
        // if idFlag has 1, then original_row_updated = row_updated \oplus all_before \oplus all_after
        MpcZ2Vector[] afterValuesXorRes = Arrays.stream(groupByMt.getValueIndexes())
            .mapToObj(i -> originalData[i])
            .map(z2cParty::xorSelfAllElement)
            .toArray(MpcZ2Vector[]::new);
        assert originalValuesXorRes != null;
        // if no update. then it should be 0, if update, then it should be original \oplus after
        z2cParty.xori(afterValuesXorRes, originalValuesXorRes);
        // if updated, it should be original \oplus after \oplus after = original
        z2cParty.xori(afterValuesXorRes,
            Arrays.stream(groupByMt.getValueIndexes()).mapToObj(i -> ur1[i]).toArray(MpcZ2Vector[]::new));
        MpcZ2Vector[] ur2 = Arrays.stream(ur1).map(ea -> (MpcZ2Vector) ea.copy()).toArray(MpcZ2Vector[]::new);
        for (int i = 0; i < groupByMt.getValueIndexes().length; i++) {
            ur2[groupByMt.getValueIndexes()[i]] = afterValuesXorRes[i];
        }
        result.add(new UpdateMessage(OperationEnum.DELETE, ur2));
        result.add(new UpdateMessage(OperationEnum.INSERT, ur1));
        return result;
    }

}
