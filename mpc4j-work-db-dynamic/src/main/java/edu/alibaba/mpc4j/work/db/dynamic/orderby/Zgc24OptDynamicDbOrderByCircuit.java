package edu.alibaba.mpc4j.work.db.dynamic.orderby;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.work.db.dynamic.AbstractDynamicDbCircuit;
import edu.alibaba.mpc4j.work.db.dynamic.structure.OperationEnum;
import edu.alibaba.mpc4j.work.db.dynamic.structure.UpdateMessage;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * our optimized shortcut dynamic db order by circuit.
 */
public class Zgc24OptDynamicDbOrderByCircuit extends AbstractDynamicDbCircuit implements DynamicDbOrderByCircuit {
    /**
     * order-by Materialized Table
     */
    private OrderByMt orderByMt;

    public Zgc24OptDynamicDbOrderByCircuit(Z2IntegerCircuit circuit) {
        super(circuit);
    }

    @Override
    public List<UpdateMessage> update(UpdateMessage updateMessage, OrderByMt orderByMt) throws MpcAbortException {
        setInputs(updateMessage, orderByMt);
        MathPreconditions.checkEqual("updateMessage.getRowData().length", "materializedTable.getData().length",
            updateMessage.getRowData().length, orderByMt.getData().length);
        this.orderByMt = orderByMt;
        return switch (updateMessage.getOperation()) {
            case INSERT -> insert();
            case DELETE -> delete();
        };
    }

    private List<UpdateMessage> insert() throws MpcAbortException {
        int currentRowNum = orderByMt.getData()[0].bitNum();
        MpcZ2Vector[] originalData = orderByMt.getData();
        if(originalData[0].bitNum()  == 0) {
            // if there is no record yet
            for (int i = 0; i < originalData.length; i++) {
                originalData[i].merge(updateMessage.getRowData()[i]);
            }
            return List.of(new UpdateMessage(OperationEnum.INSERT, originalData));
        }

        MpcZ2Vector[] extendedUpDate = extendUpdateMsgData(currentRowNum);
        List<UpdateMessage> result = new LinkedList<>();

        if (!orderByMt.isOutputTable()) {
            // if the size of the current table does not exceed the limit num, we can directly insert the new record
            // but it only happens in running the benchmark
            if(currentRowNum < orderByMt.getLimitNum()) {
                MpcZ2Vector[] dummyUr1 = Arrays.stream(originalData)
                    .map(ea -> z2cParty.createShareZeros(1))
                    .toArray(MpcZ2Vector[]::new);
                result.add(new UpdateMessage(OperationEnum.DELETE, dummyUr1));
                result.add(new UpdateMessage(OperationEnum.INSERT, updateMessage.getRowData()));
            }else{
                MpcZ2Vector[] originalLast = Arrays.stream(originalData)
                    .map(ea ->
                        z2cParty.create(ea.isPlain(),
                            Arrays.stream(ea.getBitVectors())
                                .map(one -> one.get(orderByMt.getLimitNum() - 1) ? BitVectorFactory.createOnes(1) : BitVectorFactory.createZeros(1))
                                .toArray(BitVector[]::new)
                        ))
                    .toArray(MpcZ2Vector[]::new);
                MpcZ2Vector[] leftCompInput = new MpcZ2Vector[orderByMt.getOrderKeyIndexes().length + 1];
                MpcZ2Vector[] rightCompInput = new MpcZ2Vector[orderByMt.getOrderKeyIndexes().length + 1];
                leftCompInput[0] = originalLast[orderByMt.getValidityIndex()];
                rightCompInput[0] = updateMessage.getRowData()[orderByMt.getValidityIndex()];
                for (int i = 0; i < orderByMt.getOrderKeyIndexes().length; i++) {
                    int ind = orderByMt.getOrderKeyIndexes()[i];
                    leftCompInput[i + 1] = originalLast[ind];
                    rightCompInput[i + 1] = updateMessage.getRowData()[ind];
                }
                // original < new?
                MpcZ2Vector flag = circuit.lessThan(leftCompInput, rightCompInput);

                MpcZ2Vector[] ur1 = z2cParty.and(flag, originalLast);
                MpcZ2Vector[] ur2 = z2cParty.and(flag, updateMessage.getRowData());
                result.add(new UpdateMessage(OperationEnum.DELETE, ur1));
                result.add(new UpdateMessage(OperationEnum.INSERT, ur2));
            }
        }

        // CDIC
        MpcZ2Vector[] leftCompInput = new MpcZ2Vector[orderByMt.getOrderKeyIndexes().length + 1];
        MpcZ2Vector[] rightCompInput = new MpcZ2Vector[orderByMt.getOrderKeyIndexes().length + 1];

        leftCompInput[0] = originalData[orderByMt.getValidityIndex()];
        rightCompInput[0] = extendedUpDate[orderByMt.getValidityIndex()];
        for (int i = 0; i < orderByMt.getOrderKeyIndexes().length; i++) {
            int ind = orderByMt.getOrderKeyIndexes()[i];
            leftCompInput[i + 1] = originalData[ind];
            rightCompInput[i + 1] = extendedUpDate[ind];
        }
        // original < new?
        MpcZ2Vector compResult = circuit.lessThan(leftCompInput, rightCompInput);
        MpcZ2Vector[] dPrime = circuit.mux(originalData, extendedUpDate, compResult);

        MpcZ2Vector[] d2Prime = Arrays.stream(dPrime)
            .map(ea -> ea.reduceShiftRight(currentRowNum - 1))
            .toArray(MpcZ2Vector[]::new);
        for (int i = 0; i < dim; i++) {
            dPrime[i].merge(updateMessage.getRowData()[i]);
            dPrime[i].reduce(currentRowNum);
        }
        MpcZ2Vector[] muxRes = circuit.mux(dPrime, originalData, compResult);
        for (int i = 0; i < dim; i++) {
            d2Prime[i].merge(muxRes[i]);
        }
        // if the current view size is too big, remove the last one
        if(d2Prime[0].bitNum() > orderByMt.getLimitNum() + orderByMt.getDeletionThreshold()) {
            for (int i = 0; i < dim; i++) {
                d2Prime[i].reduceShiftRighti(1);
            }
        }
        orderByMt.updateData(d2Prime);

        return result;
    }

    private List<UpdateMessage> delete() throws MpcAbortException {
        int currentRowNum = orderByMt.getData()[0].bitNum();
        if(currentRowNum == 0) {
            throw new MpcAbortException("empty table can not delete row");
        }
        if (orderByMt.getCurrentDeleteNum() >= orderByMt.getDeletionThreshold()) {
            throw new MpcAbortException("currentDeleteNum >= deletionThreshold");
        }
        MpcZ2Vector[] originalData = orderByMt.getData();
        MpcZ2Vector[] extendedUpDate = extendUpdateMsgData(currentRowNum);
        List<UpdateMessage> result = new LinkedList<>();

        // get the equality flag
        MpcZ2Vector idFlag = circuit.eq(
            Arrays.stream(orderByMt.getIdIndexes()).mapToObj(i -> originalData[i]).toArray(MpcZ2Vector[]::new),
            Arrays.stream(orderByMt.getIdIndexes()).mapToObj(i -> extendedUpDate[i]).toArray(MpcZ2Vector[]::new));
        idFlag = z2cParty.and(idFlag, originalData[orderByMt.getValidityIndex()]);
        idFlag = z2cParty.and(idFlag, extendedUpDate[orderByMt.getValidityIndex()]);

        if (!orderByMt.isOutputTable()) {
            MpcZ2Vector lFlags = idFlag.reduceShiftRight(currentRowNum - orderByMt.getLimitNum());
            // xor first l flag result
            MpcZ2Vector fs = z2cParty.xorSelfAllElement(lFlags);
            MpcZ2Vector[] originalLast = Arrays.stream(originalData)
                .map(ea -> z2cParty.create(ea.isPlain(),
                    Arrays.stream(ea.getBitVectors())
                        .map(one -> one.get(orderByMt.getLimitNum()) ? BitVectorFactory.createOnes(1) : BitVectorFactory.createZeros(1))
                        .toArray(BitVector[]::new)
                ))
                .toArray(MpcZ2Vector[]::new);
            MpcZ2Vector[] ur1 = z2cParty.and(fs, updateMessage.getRowData());
            MpcZ2Vector[] ur2 = z2cParty.and(fs, originalLast);
            result.add(new UpdateMessage(OperationEnum.DELETE, ur1));
            result.add(new UpdateMessage(OperationEnum.INSERT, ur2));
        }
        // d'[f]
        MpcZ2Vector dfPrime = z2cParty.xorAllBeforeElement(idFlag);
        // d[i + 1]
        MpcZ2Vector[] dShiftRight1 = Arrays.stream(originalData)
            .map(ea -> ea.reduceShiftRight(1))
            .toArray(MpcZ2Vector[]::new);
        // original data keep last currentRowNum - 1 data
        Arrays.stream(originalData).forEach(ea -> ea.reduce(currentRowNum - 1));
        // process first currentRowNum - 1 data
        MpcZ2Vector[] muxRes = circuit.mux(dShiftRight1, originalData, dfPrime.reduceShiftRight(1));
        // process the last element
        for (int i = 0; i < dim; i++) {
            originalData[i].reduce(1);
            if (i == orderByMt.getValidityIndex()) {
                dfPrime.reduce(1);
                originalData[i] = z2cParty.and(originalData[i], z2cParty.not(dfPrime));
            }
            // add last element, only the flag can be possible changed
            muxRes[i].merge(originalData[i]);
        }

        orderByMt.updateData(muxRes);
        orderByMt.increaseDeleteNum();
        return result;
    }
}
