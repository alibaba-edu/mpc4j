package edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.naive;

import edu.alibaba.mpc4j.common.circuit.z2.Z2CircuitConfig;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory;
import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvOperations.ConvOp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.AbstractOrderByParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.OrderByFnParam;

import java.util.Arrays;

/**
 * naive order-by protocol, where the payload is involved during sorting
 *
 * @author Feng Han
 * @date 2025/3/4
 */
public class NaiveOrderByParty extends AbstractOrderByParty {
    /**
     * adder type
     */
    public final ComparatorType comparatorType;
    /**
     * whether the sorting process is stable
     */
    private final boolean isStable;
    /**
     * circuit
     */
    public Z2IntegerCircuit circuit;

    public NaiveOrderByParty(Abb3Party abb3Party, NaiveOrderByConfig config) {
        super(NaiveOrderByPtoDesc.getInstance(), abb3Party, config);
        comparatorType = config.getComparatorTypes();
        circuit = new Z2IntegerCircuit(z2cParty, new Z2CircuitConfig.Builder().setComparatorType(comparatorType).build());
        isStable = config.isStable();
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        abb3Party.init();
        initState();
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public long[] setUsage(OrderByFnParam... params) {
        if (!isMalicious) {
            return new long[]{0, 0};
        }
        long bitTupleNum = 0, longTupleNum = 0;
        for (OrderByFnParam funParam : params) {
            int dataNum = CommonUtils.getByteLength(funParam.inputSize) << 3;
            int logDataNum = LongUtils.ceilLog2(dataNum);
            int sortDim, payloadDim;
            if (funParam.inputInBinaryForm) {
                sortDim = funParam.keyDim + 1;
                payloadDim = funParam.totalDim - 1 - funParam.keyDim;
            } else {
                sortDim = funParam.keyDim * 64 + 1;
                payloadDim = (funParam.totalDim - 1 - funParam.keyDim) * 64;
            }
            bitTupleNum += (long) dataNum * logDataNum * logDataNum / 2 *
                (ComparatorFactory.getAndGateNum(comparatorType, sortDim) + payloadDim + sortDim);
            if (!funParam.inputInBinaryForm) {
                long[] a2bTuple = abb3Party.getConvParty().getTupleNum(ConvOp.A2B, dataNum, funParam.keyDim, 64);
                long[] a2bitTuple = abb3Party.getConvParty().getTupleNum(ConvOp.A2B, dataNum, 1, 1);
                long[] b2aTuple = abb3Party.getConvParty().getTupleNum(ConvOp.B2A, dataNum, funParam.keyDim, 64);
                long[] bit2aTuple = abb3Party.getConvParty().getTupleNum(ConvOp.BIT2A, dataNum, 1, 1);
                bitTupleNum += a2bTuple[0] + a2bitTuple[0] + b2aTuple[0] + bit2aTuple[0];
                longTupleNum += a2bTuple[1] + a2bitTuple[1] + b2aTuple[1] + bit2aTuple[1];
            }
        }
        abb3Party.updateNum(bitTupleNum, longTupleNum);
        return new long[]{bitTupleNum, longTupleNum};
    }

    @Override
    public void orderBy(TripletZ2Vector[] table, int[] keyIndex) throws MpcAbortException {
        preprocess(table, keyIndex);
        logPhaseInfo(PtoState.PTO_BEGIN, "orderBy for binary input");

        stopWatch.start();
        TripletZ2Vector[] sortInput = new TripletZ2Vector[keyIndex.length + 1];
        sortInput[0] = bInput[bInput.length - 1];
        z2cParty.noti(sortInput[0]);
        System.arraycopy(bInput, 0, sortInput, 1, keyIndex.length);

        TripletZ2Vector[] payload = Arrays.copyOfRange(bInput, keyIndex.length, bInput.length - 1);
        if (payload.length > 0) {
            circuit.psort(new TripletZ2Vector[][]{sortInput}, new TripletZ2Vector[][]{payload}, null, false, isStable);
        } else {
            circuit.psort(new TripletZ2Vector[][]{sortInput}, null, null, false, isStable);
        }
        z2cParty.noti(sortInput[0]);
        logStepInfo(PtoState.PTO_STEP, 1, 1, resetAndGetTime());

        postprocess(sortInput, payload, table);
        logPhaseInfo(PtoState.PTO_END, "orderBy for binary input");
    }

    @Override
    public void orderBy(TripletLongVector[] table, int[] keyIndex) throws MpcAbortException {
        preprocess(table, keyIndex);
        int keyDim = keyIndex.length;
        int payloadDim = table.length - 1 - keyDim;
        logPhaseInfo(PtoState.PTO_BEGIN, "orderBy for arithmetic input");

        stopWatch.start();
        TripletZ2Vector[][] bAll = abb3Party.getConvParty().a2b(Arrays.copyOf(aInput, aInput.length - 1), 64);
        TripletZ2Vector[] sortKey = new TripletZ2Vector[keyDim * 64 + 1];
        sortKey[0] = abb3Party.getConvParty().a2b(aInput[aInput.length - 1], 1)[0];
        z2cParty.noti(sortKey[0]);
        logStepInfo(PtoState.PTO_STEP, 1, 3, resetAndGetTime(), "a2b");

        stopWatch.start();
        for (int i = 0; i < keyDim; i++) {
            System.arraycopy(bAll[i], 0, sortKey, i * 64 + 1, 64);
        }
        TripletZ2Vector[] bPayload = null;
        if (payloadDim > 0) {
            bPayload = Arrays.stream(bAll, keyDim, aInput.length - 1)
                .flatMap(Arrays::stream)
                .toArray(TripletZ2Vector[]::new);
            circuit.psort(new TripletZ2Vector[][]{sortKey}, new TripletZ2Vector[][]{bPayload}, null, false, isStable);
        } else {
            circuit.psort(new TripletZ2Vector[][]{sortKey}, null, null, false, isStable);
        }
        logStepInfo(PtoState.PTO_STEP, 2, 3, resetAndGetTime(), "sort");

        stopWatch.start();
        TripletLongVector[] aKey = new TripletLongVector[keyDim + 1];
        z2cParty.noti(sortKey[0]);
        aKey[0] = abb3Party.getConvParty().bit2a(sortKey[0]);
        TripletZ2Vector[][] bTransInput = new TripletZ2Vector[table.length - 1][];
        for (int i = 0; i < keyDim; i++) {
            bTransInput[i] = Arrays.copyOfRange(sortKey, 1 + i * 64, 1 + (i + 1) * 64);
        }
        for (int i = 0; i < payloadDim; i++) {
            bTransInput[i + keyDim] = Arrays.copyOfRange(bPayload, i * 64, (i + 1) * 64);
        }
        TripletLongVector[] transBack = abb3Party.getConvParty().b2a(bTransInput);
        System.arraycopy(transBack, 0, aKey, 1, keyDim);
        TripletLongVector[] payload = Arrays.copyOfRange(transBack, keyIndex.length, transBack.length);
        logStepInfo(PtoState.PTO_STEP, 3, 3, resetAndGetTime(), "b2a");

        postprocess(aKey, payload, table);
        logPhaseInfo(PtoState.PTO_END, "orderBy for arithmetic input");
    }
}
