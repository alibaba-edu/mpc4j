package edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.quick;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvOperations.ConvOp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.quick.QuickPgSortParty;
import edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.AbstractOrderSelectParty;
import edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.OrderSelectOperations.OrderSelectFnParam;
import edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.OrderSelectParty;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;

/**
 * oblivious order select party using quick sorting
 */
public class QuickOrderSelectParty extends AbstractOrderSelectParty implements OrderSelectParty {

    /**
     * quick sorting party
     */
    private final QuickPgSortParty sortParty;

    public QuickOrderSelectParty(Abb3Party abb3Party, QuickOrderSelectConfig config) {
        super(QuickOrderSelectPtoDesc.getInstance(), abb3Party, config);
        sortParty = (QuickPgSortParty) PgSortFactory.createParty(abb3Party, config.getQuickPgSortConfig());
        addSubPto(sortParty);
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        sortParty.init();
        initState();
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public long[] setUsage(OrderSelectFnParam... params) {
        if (!isMalicious) {
            return new long[]{0, 0};
        }
        long bitTupleNum = 0, longTupleNum = 0;
        for (OrderSelectFnParam funParam : params) {
            int dataNum = CommonUtils.getByteLength(funParam.dataNum) << 3;
            int sumBitNum = funParam.op.name().endsWith("_A") ? Arrays.stream(funParam.dims).sum() : funParam.dims[0];
            int logM = LongUtils.ceilLog2(dataNum);
            // estimated number of comparisons
            long compareNum = (long) dataNum * logM + 7L * logM * (funParam.range[1] - funParam.range[0]);

            bitTupleNum += compareNum * (sumBitNum + logM + ComparatorFactory.getAndGateNum(sortParty.comparatorType, sumBitNum + logM));
            // permute operation
            sortParty.permuteParty.setUsage(new PermuteFnParam(PermuteOp.APPLY_INV_B_B, dataNum, logM, logM));
            if (funParam.op.name().endsWith("_A")) {
                // 1. the cost of a2b
                for (int bit : funParam.dims) {
                    bitTupleNum += abb3Party.getConvParty().getTupleNum(ConvOp.A2B, dataNum, 1, bit)[0];
                }
                bitTupleNum += abb3Party.getConvParty().getTupleNum(ConvOp.B2A, dataNum, 1, 64)[0];
            }
        }
        abb3Party.updateNum(bitTupleNum, longTupleNum);
        return new long[]{bitTupleNum, longTupleNum};
    }

    @Override
    public Pair<TripletZ2Vector[], TripletLongVector[]> orderSelect(TripletLongVector[] input, int[] bitLens, int[] range) throws MpcAbortException {
        return arithmeticInputSelect(input, bitLens, range, true);
    }

    @Override
    public Pair<TripletZ2Vector[], TripletLongVector[]> selectRangeNoOrder(TripletLongVector[] input, int[] bitLens, int[] range) throws MpcAbortException {
        return arithmeticInputSelect(input, bitLens, range, false);
    }

    private Pair<TripletZ2Vector[], TripletLongVector[]> arithmeticInputSelect(TripletLongVector[] input, int[] bitLens, int[] range, boolean stillSorted) throws MpcAbortException {
        checkInput(input, bitLens, range);
        if (input[0].getNum() == 1) {
            return Pair.of(
                (TripletZ2Vector[]) z2cParty.setPublicValues(new BitVector[]{BitVectorFactory.createZeros(1)}),
                Arrays.stream(input).map(TripletLongVector::copy).toArray(TripletLongVector[]::new)
            );
        }
        logPhaseInfo(PtoState.PTO_BEGIN, stillSorted ? "orderSelect" : "selectRangeNoOrder");

        stopWatch.start();
        int totalBitNum = Arrays.stream(bitLens).sum();
        TripletZ2Vector[] bShareInput = new TripletZ2Vector[totalBitNum];
        for (int i = 0, start = 0; i < input.length; i++) {
            System.arraycopy(abb3Party.getConvParty().a2b(input[i], bitLens[i]), 0, bShareInput, start, bitLens[i]);
            start += bitLens[i];
        }
        logStepInfo(PtoState.PTO_STEP, 1, 3, resetAndGetTime(), "a2b time");

        stopWatch.start();
        TripletZ2Vector[] res = sortParty.sortAll(bShareInput, range, stillSorted);
        logStepInfo(PtoState.PTO_STEP, 2, 3, resetAndGetTime(), "sort time");

        stopWatch.start();
        TripletZ2Vector[] invPai = sortParty.getPerWithIndex(Arrays.copyOfRange(res, bShareInput.length, res.length));
        TripletZ2Vector[] data = getPart(Arrays.copyOfRange(res, 0, bShareInput.length), range);
        TripletLongVector[] valuesInRange = new TripletLongVector[input.length];
        for (int i = 0, start = 0; i < input.length; i++) {
            valuesInRange[i] = abb3Party.getConvParty().b2a(Arrays.copyOfRange(data, start, start + bitLens[i]));
            start += bitLens[i];
        }
        logStepInfo(PtoState.PTO_STEP, 3, 3, resetAndGetTime(), "inv perm and b2a time");

        logPhaseInfo(PtoState.PTO_END, stillSorted ? "orderSelect" : "selectRangeNoOrder");
        return Pair.of(invPai, valuesInRange);
    }

    @Override
    public Pair<TripletZ2Vector[], TripletZ2Vector[]> orderSelect(TripletZ2Vector[] input, int[] range) throws MpcAbortException {
        return binaryInputSelect(input, range, true);
    }

    @Override
    public Pair<TripletZ2Vector[], TripletZ2Vector[]> selectRangeNoOrder(TripletZ2Vector[] input, int[] range) throws MpcAbortException {
        return binaryInputSelect(input, range, false);
    }

    private Pair<TripletZ2Vector[], TripletZ2Vector[]> binaryInputSelect(TripletZ2Vector[] input, int[] range, boolean stillSorted) throws MpcAbortException {
        checkInput(input, range);
        if (input[0].bitNum() == 1) {
            return Pair.of(
                (TripletZ2Vector[]) z2cParty.setPublicValues(new BitVector[]{BitVectorFactory.createZeros(1)}),
                Arrays.stream(input).map(TripletZ2Vector::copy).toArray(TripletZ2Vector[]::new)
            );
        }
        logPhaseInfo(PtoState.PTO_BEGIN, stillSorted ? "orderSelect" : "selectRangeNoOrder");

        stopWatch.start();
        // we need to set this sortAll function as a public function
        TripletZ2Vector[] allTransBack = sortParty.sortAll(input, range, stillSorted);
        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime(), "sort time");

        stopWatch.start();
        // we need to set this getPerWithIndex function as a public function
        TripletZ2Vector[] invPai = sortParty.getPerWithIndex(Arrays.copyOfRange(allTransBack, input.length, allTransBack.length));
        TripletZ2Vector[] res = getPart(Arrays.copyOfRange(allTransBack, 0, input.length), range);
        logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime(), "inverse permute time");

        logPhaseInfo(PtoState.PTO_END, stillSorted ? "orderSelect" : "selectRangeNoOrder");
        return Pair.of(invPai, res);
    }

    /**
     * return the data whose index after sorting is in [from, to)
     *
     * @param wires input data
     * @param range [from, to)
     */
    private TripletZ2Vector[] getPart(TripletZ2Vector[] wires, int[] range) throws MpcAbortException {
        MathPreconditions.checkEqual("range.length", "2", range.length, 2);
        MathPreconditions.checkNonNegativeInRange("range[0]", range[0], wires[0].bitNum());
        MathPreconditions.checkInRangeClosed("range[1]", range[1], range[0], wires[0].bitNum());
        int originalBitLen = wires[0].bitNum();
        int validBitLen = range[1] - range[0];
        return Arrays.stream(wires).map(wire -> {
            TripletZ2Vector tmp = wire.reduceShiftRight(originalBitLen - range[1]);
            tmp.reduce(validBitLen);
            return tmp;
        }).toArray(TripletZ2Vector[]::new);
    }
}
