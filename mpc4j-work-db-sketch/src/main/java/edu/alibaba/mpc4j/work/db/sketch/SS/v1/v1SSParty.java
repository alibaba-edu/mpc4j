package edu.alibaba.mpc4j.work.db.sketch.SS.v1;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.db.sketch.SS.AbstractSSParty;
import edu.alibaba.mpc4j.work.db.sketch.SS.SSParty;
import edu.alibaba.mpc4j.work.db.sketch.SS.SSTable;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTable;
import edu.alibaba.mpc4j.work.db.sketch.utils.agg.AggFactory;
import edu.alibaba.mpc4j.work.db.sketch.utils.agg.AggParty;
import edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.OrderSelectFactory;
import edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.OrderSelectParty;
import edu.alibaba.mpc4j.work.db.sketch.utils.pop.PopFactory;
import edu.alibaba.mpc4j.work.db.sketch.utils.pop.PopParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortParty;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * v1 MG computing party.
 */
public class v1SSParty extends AbstractSSParty implements SSParty {
    /**
     * permute party
     */
    public final PermuteParty permuteParty;
    /**
     * group-by-sum party
     */
    public final GroupSumParty groupSumParty;
    /**
     * sorting party
     */
    public final PgSortParty pgSortParty;
    /**
     * selection party
     */
    public final OrderSelectParty orderSelectParty;
    /**
     * agg party
     */
    public final AggParty aggParty;
    /**
     * pop party
     */
    public final PopParty popParty;

    public v1SSParty(Abb3Party abb3Party, v1SSConfig config) {
        super(v1SSPtoDesc.getInstance(), abb3Party, config);
        permuteParty = PermuteFactory.createParty(abb3Party, config.getPermuteConfig());
        groupSumParty = GroupSumFactory.createParty(abb3Party, config.getGroupSumConfig());
        pgSortParty = PgSortFactory.createParty(abb3Party, config.getPgSortConfig());
        orderSelectParty = OrderSelectFactory.createParty(abb3Party, config.getOrderSelectConfig());
        aggParty = AggFactory.createParty(abb3Party, config.getAggConfig());
        popParty = PopFactory.createParty(abb3Party, config.getPopConfig());
        addMultiSubPto(permuteParty, groupSumParty, pgSortParty);
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        abb3Party.init();
        permuteParty.init();
        groupSumParty.init();
        pgSortParty.init();
        orderSelectParty.init();
        aggParty.init();
        popParty.init();
        initState();
        logStepInfo(PtoState.INIT_STEP, 1, 2, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void update(SketchTable ssTable, MpcVector[] newData) throws MpcAbortException {
        setssTable((SSTable) ssTable);
        int bIndex = ssTable.getBufferIndex();
        // simple insert the new data
        ssTable.getBufferTable().add(newData[0]);
        // merge buffer if need
        if (bIndex == ssTable.getTableSize() - 1) {
            int sketchSize = ssTable.getTableSize();
            int payloadBitLen = ((SSTable) ssTable).getPayloadBitLen();
            int keyBitLen = ((SSTable) ssTable).getKeyBitLen();
//            int bufferSize = bufferData[0].getNum();
            MathPreconditions.checkEqual("keyBitLen + payloadBitLen", "ssTable.getSketchTable().length",
                keyBitLen + payloadBitLen, ssTable.getSketchTable().length);
            logPhaseInfo(PtoState.PTO_BEGIN);

            stopWatch.start();
            TripletZ2Vector[] keyAndPayload = new TripletZ2Vector[keyBitLen + payloadBitLen];
            TripletZ2Vector[] buffer = ssTable.getBufferTable().toArray(TripletZ2Vector[]::new);
            buffer = z2cParty.matrixTranspose(buffer);
            // merge two table, new data in the front and old data in the end
            for (int i = 0; i < keyBitLen; i++) {
                keyAndPayload[i] = buffer[i];
                keyAndPayload[i].merge(ssTable.getSketchTable()[i]);
            }
            for (int i = keyBitLen; i < keyAndPayload.length - 1; i++) {
                keyAndPayload[i] = (TripletZ2Vector) ssTable.getSketchTable()[i];
                keyAndPayload[i].extendLength(sketchSize * 2);
            }
            keyAndPayload[keyAndPayload.length - 1] = z2cParty.createShareZeros(sketchSize);
            z2cParty.noti(keyAndPayload[keyAndPayload.length - 1]);
            keyAndPayload[keyAndPayload.length - 1].merge(ssTable.getSketchTable()[keyAndPayload.length - 1]);
            // order by the index key, we use PgSort to enable flexible selection of sorting algorithms
            TripletZ2Vector[] keys = Arrays.copyOf(keyAndPayload, keyBitLen);
            TripletZ2Vector[] countValues = Arrays.copyOfRange(keyAndPayload, keyBitLen, keyBitLen + payloadBitLen);
            TripletZ2Vector[] perm = pgSortParty.perGenAndSortOrigin(keys);
            countValues = permuteParty.applyInvPermutation(perm, countValues);
            logStepInfo(PtoState.PTO_STEP, 1, 4, resetAndGetTime(), "sort values in the order of count");

            stopWatch.start();
            // group by aggregation
            TripletZ2Vector[] groupInput = new TripletZ2Vector[keyBitLen + 1];
            System.arraycopy(keys, 0, groupInput, 0, keyBitLen);
            // add flag
            groupInput[keyBitLen] = z2cParty.createShareZeros(2 * sketchSize);
            z2cParty.noti(groupInput[keyBitLen]);
            int[] keyIndex = IntStream.range(0, keyBitLen).toArray();
            TripletZ2Vector flag = groupSumParty.getGroupFlag(groupInput, keyIndex);
            TripletLongVector aFlag = abb3Party.getConvParty().bit2a(flag);
            TripletLongVector aCount = abb3Party.getConvParty().b2a(countValues);
            TripletLongVector[] sum = groupSumParty.groupSum(new TripletLongVector[]{aCount}, aFlag);
            TripletZ2Vector sumFlag = abb3Party.getConvParty().a2b(sum[1], 1)[0];
            TripletZ2Vector[] groupSum = abb3Party.getConvParty().a2b(sum[0], payloadBitLen);
            groupSum = z2cParty.and(sumFlag, groupSum);
            logStepInfo(PtoState.PTO_STEP, 2, 4, resetAndGetTime(), "group by sum");

            stopWatch.start();
            // remove the least k-1 element
            System.arraycopy(keys, 0, keyAndPayload, 0, keyBitLen);
            System.arraycopy(groupSum, 0, keyAndPayload, keyBitLen, payloadBitLen);
            int[] kAnd1Range = new int[]{0, sketchSize};
            Pair<TripletZ2Vector[], TripletZ2Vector[]> select = orderSelectParty.selectRangeNoOrder(groupSum, kAnd1Range);
            keyAndPayload = permuteParty.applyInvPermutation(select.getLeft(), keyAndPayload);
            // keep the last k+1 elements (k+1 largest element)
            Arrays.stream(keyAndPayload).forEach(each -> each.reduce(sketchSize));

//            stopWatch.start();
            // get minimum count and remove it, other values sub this value
//            Pair<TripletZ2Vector[], TripletZ2Vector> minPair = aggParty.extremeIndicator(Arrays.copyOfRange(keyAndPayload, keyBitLen, keyAndPayload.length), AggOp.MIN);
//            TripletZ2Vector[] popResult = popParty.pop(keyAndPayload, minPair.getRight());
//            TripletLongVector min = abb3Party.getConvParty().b2a(minPair.getLeft());
//            TripletLongVector extendMin = min.extendSizeWithSameEle(ssTable.getTableSize());
//            TripletLongVector sumA = abb3Party.getConvParty().b2a(Arrays.copyOfRange(popResult, keyBitLen, popResult.length));
//            sumA = zl64cParty.sub(sumA, extendMin);
//            countValues = abb3Party.getConvParty().a2b(sumA, payloadBitLen);
//            System.arraycopy(countValues, 0, popResult, keyBitLen, payloadBitLen);
            ssTable.updateSketchTable(keyAndPayload);
            ssTable.clearBufferTable();
            logStepInfo(PtoState.PTO_STEP, 3, 4, resetAndGetTime(), "last step");

            logPhaseInfo(PtoState.PTO_END);
        }

    }

    private void mergeAndGroupBy(SketchTable ssTable) throws MpcAbortException {
        TripletZ2Vector[] table = (TripletZ2Vector[]) ssTable.getSketchTable();
        TripletZ2Vector[] buffer = z2cParty.matrixTranspose(ssTable.getBufferTable().toArray(TripletZ2Vector[]::new));
        TripletZ2Vector[] dataCopy = Arrays.copyOf(table, table.length);
        TripletZ2Vector[] bufferCopy = Arrays.copyOf(buffer, buffer.length);
        int payloadBitLen = ((SSTable) ssTable).getPayloadBitLen();
        int keyBitLen = ((SSTable) ssTable).getKeyBitLen();
        // merge two table
        for (int i = 0; i < bufferCopy.length; i++) {
            dataCopy[i].merge(bufferCopy[i]);
        }
        for (int i = keyBitLen; i < dataCopy.length - 1; i++) {
            dataCopy[i].merge(z2cParty.createShareZeros(bufferCopy[0].bitNum()));
        }
        TripletZ2Vector shareOne = (TripletZ2Vector) z2cParty.setPublicValues(new BitVector[]{BitVectorFactory.createOnes(bufferCopy[0].bitNum())})[0];
        dataCopy[dataCopy.length - 1].merge(shareOne);

        TripletZ2Vector[] indexCopy = Arrays.copyOf(dataCopy, keyBitLen);
        TripletZ2Vector[] countCopy = Arrays.copyOfRange(dataCopy, keyBitLen, dataCopy.length);
        // order by the index key, we use PgSort to enable flexible selection of sorting algorithms
        TripletZ2Vector[] perm = pgSortParty.perGenAndSortOrigin(indexCopy);
        countCopy = permuteParty.applyInvPermutation(perm, countCopy);

        // group by flag
        TripletZ2Vector[] groupKeyInput = new TripletZ2Vector[keyBitLen + 1];
        System.arraycopy(indexCopy, 0, groupKeyInput, 0, keyBitLen);
        groupKeyInput[keyBitLen] = (TripletZ2Vector) z2cParty.setPublicValues(new BitVector[]{BitVectorFactory.createOnes(indexCopy[0].bitNum())})[0];
        int[] keys = IntStream.range(0, keyBitLen).toArray();
        TripletZ2Vector flag = groupSumParty.getGroupFlag(groupKeyInput, keys);

        // group by sum
        TripletLongVector convFlag = abb3Party.getConvParty().bit2a(flag);
        TripletLongVector convCount = abb3Party.getConvParty().b2a(countCopy);
        TripletLongVector[] sum = groupSumParty.groupSum(new TripletLongVector[]{convCount}, convFlag);
        TripletZ2Vector[] sumFlag = abb3Party.getConvParty().a2b(sum[1], 1);
        TripletZ2Vector[] groupSum = abb3Party.getConvParty().a2b(sum[0], payloadBitLen);
        IntStream.range(0, groupSum.length).forEach(i -> groupSum[i] = z2cParty.and(groupSum[i], sumFlag[0]));

        // get result
        System.arraycopy(indexCopy, 0, dataCopy, 0, keyBitLen);
        System.arraycopy(groupSum, 0, dataCopy, keyBitLen, payloadBitLen);
        int sketchSize = ssTable.getTableSize();
        int[] kAnd1Range = new int[]{0, sketchSize};
        Pair<TripletZ2Vector[], TripletZ2Vector[]> select = orderSelectParty.selectRangeNoOrder(groupSum, kAnd1Range);
        dataCopy = permuteParty.applyInvPermutation(select.getLeft(), dataCopy);
        // keep the last k+1 elements (k+1 largest element)
        Arrays.stream(dataCopy).forEach(each -> each.reduce(sketchSize));

        ssTable.updateSketchTable(dataCopy);
        ssTable.clearBufferTable();
//        return dataCopy;
    }

    @Override
    public MpcVector[] getQuery(SketchTable ssTable, int k) throws MpcAbortException {
        TripletZ2Vector[] tableCopy = (TripletZ2Vector[]) ssTable.getSketchTable();
        int logPayload = ((SSTable) ssTable).getPayloadBitLen();
        int keyBitLen = ((SSTable) ssTable).getKeyBitLen();
        int tableSize = ssTable.getTableSize();
        // merge the buffer
        if (ssTable.getBufferIndex() > 0) {
//            TripletZ2Vector[] bufferCopy = ssTable.getBufferTable().toArray(TripletZ2Vector[]::new);
//            bufferCopy = z2cParty.matrixTranspose(bufferCopy);
            mergeAndGroupBy(ssTable);
        }
        TripletZ2Vector[] countCopy = new TripletZ2Vector[logPayload];
        System.arraycopy(tableCopy, keyBitLen, countCopy, 0, logPayload);
        int[] kRange = new int[]{tableSize - k, tableSize};
        Pair<TripletZ2Vector[], TripletZ2Vector[]> selectK = orderSelectParty.orderSelect(countCopy, kRange);
        tableCopy = permuteParty.applyInvPermutation(selectK.getLeft(), tableCopy);
        Arrays.stream(tableCopy).forEach(each -> each.reduce(k));
        return tableCopy;
    }
}
