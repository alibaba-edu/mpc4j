package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.mixed.opt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.AbstractPgSortParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations.PgSortFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.quick.QuickPgSortParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.radix.RadixPgSortParty;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * oblivious mixed sorting party using somewhat-opt strategy
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class OptPgSortParty extends AbstractPgSortParty implements PgSortParty {
    /**
     * radix sorting
     */
    protected final RadixPgSortParty radixPgSortParty;
    /**
     * quick sorting
     */
    protected final QuickPgSortParty quickPgSortParty;

    public OptPgSortParty(Abb3Party abb3Party, OptPgSortConfig config) {
        super(OptPgSortPtoDesc.getInstance(), abb3Party, config);
        radixPgSortParty = new RadixPgSortParty(abb3Party, config.getRadixPgSortConfig());
        quickPgSortParty = new QuickPgSortParty(abb3Party, config.getQuickPgSortConfig());
        addMultiSubPto(radixPgSortParty, quickPgSortParty);
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        quickPgSortParty.init();
        radixPgSortParty.init();
        initState();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public long[] setUsage(PgSortFnParam... params) {
        long[] res = new long[]{0, 0};
        for (PgSortFnParam funParam : params) {
            long[] tempRes;
            int sumBitNum = funParam.op.name().endsWith("_A") ? Arrays.stream(funParam.dims).sum() : funParam.dims[0];
            boolean useQuick = useQuickSort(funParam.dataNum, sumBitNum);
            if (useQuick) {
                tempRes = quickPgSortParty.setUsage(funParam);
            } else {
                tempRes = radixPgSortParty.setUsage(funParam);
            }
            res[0] += tempRes[0];
            res[1] += tempRes[1];
        }
        return res;
    }

    boolean useQuickSort(int dataNum, int dataDim) {
        // not exactly
        if (isMalicious) {
            return LongUtils.ceilLog2(dataNum) < 0.5 * dataDim;
        } else {
            return LongUtils.ceilLog2(dataNum) < dataDim;
        }
    }

    @Override
    public TripletLongVector perGen4MultiDim(TripletLongVector[] input, int[] bitLens) throws MpcAbortException {
        MathPreconditions.checkEqual("input.length", "bitLens.length", input.length, bitLens.length);
        if (!useQuickSort(input[0].getNum(), Arrays.stream(bitLens).sum())) {
            return radixPgSortParty.perGen4MultiDim(input, bitLens);
        } else {
            return quickPgSortParty.perGen4MultiDim(input, bitLens);
        }
    }

    @Override
    public TripletLongVector perGen4MultiDimWithOrigin(TripletLongVector[] input, int[] bitLens, TripletZ2Vector[] saveSortRes) throws MpcAbortException {
        MathPreconditions.checkEqual("input.length", "bitLens.length", input.length, bitLens.length);
        if (!useQuickSort(input[0].getNum(), Arrays.stream(bitLens).sum())) {
            return radixPgSortParty.perGen4MultiDimWithOrigin(input, bitLens, saveSortRes);
        } else {
            return quickPgSortParty.perGen4MultiDimWithOrigin(input, bitLens, saveSortRes);
        }
    }

    @Override
    public TripletZ2Vector[] perGen(TripletZ2Vector[] input) throws MpcAbortException {
        if (!useQuickSort(input[0].getNum(), input.length)) {
            return radixPgSortParty.perGen(input);
        } else {
            return quickPgSortParty.perGen(input);
        }
    }

    @Override
    public TripletZ2Vector[] perGenAndSortOrigin(TripletZ2Vector[] input) throws MpcAbortException {
        if (!useQuickSort(input[0].getNum(), input.length)) {
            return radixPgSortParty.perGenAndSortOrigin(input);
        } else {
            return quickPgSortParty.perGenAndSortOrigin(input);
        }
    }
}

