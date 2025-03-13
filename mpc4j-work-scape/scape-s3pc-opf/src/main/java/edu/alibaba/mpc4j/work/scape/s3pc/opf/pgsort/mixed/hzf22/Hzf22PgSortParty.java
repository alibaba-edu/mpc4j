package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.mixed.hzf22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.AbstractPgSortParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations.PgSortFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.bitonic.BitonicPgSortParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.mixed.opt.OptPgSortPtoDesc;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.radix.RadixPgSortParty;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * oblivious mixed sorting party using strategy in HZF22
 *
 * @author Feng Han
 * @date 2024/02/28
 */
public class Hzf22PgSortParty extends AbstractPgSortParty implements PgSortParty {
    /**
     * radix sorting
     */
    protected final RadixPgSortParty radixPgSortParty;
    /**
     * quick sorting
     */
    protected final BitonicPgSortParty bitonicPgSortParty;

    public Hzf22PgSortParty(Abb3Party abb3Party, Hzf22PgSortConfig config) {
        super(OptPgSortPtoDesc.getInstance(), abb3Party, config);
        radixPgSortParty = new RadixPgSortParty(abb3Party, config.getRadixPgSortConfig());
        bitonicPgSortParty = new BitonicPgSortParty(abb3Party, config.getBitonicPgSortConfig());
        addMultiSubPto(radixPgSortParty, bitonicPgSortParty);
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        radixPgSortParty.init();
        bitonicPgSortParty.init();
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
            long[] tmpRes;
            int sumBitNum = funParam.op.name().endsWith("_A") ? Arrays.stream(funParam.dims).sum() : funParam.dims[0];
            if (sumBitNum <= 3) {
                tmpRes = radixPgSortParty.setUsage(funParam);
            } else {
                tmpRes = bitonicPgSortParty.setUsage(funParam);
            }
            res[0] += tmpRes[0];
            res[1] += tmpRes[1];
        }
        return res;
    }

    @Override
    public TripletLongVector perGen4MultiDim(TripletLongVector[] input, int[] bitLens) throws MpcAbortException {
        MathPreconditions.checkEqual("input.length", "bitLens.length", input.length, bitLens.length);
        int sumBits = Arrays.stream(bitLens).sum();
        if (sumBits <= 3) {
            return radixPgSortParty.perGen4MultiDim(input, bitLens);
        } else {
            return bitonicPgSortParty.perGen4MultiDim(input, bitLens);
        }
    }

    @Override
    public TripletLongVector perGen4MultiDimWithOrigin(TripletLongVector[] input, int[] bitLens, TripletZ2Vector[] saveSortRes) throws MpcAbortException {
        MathPreconditions.checkEqual("input.length", "bitLens.length", input.length, bitLens.length);
        int sumBits = Arrays.stream(bitLens).sum();
        if (sumBits <= 3) {
            return radixPgSortParty.perGen4MultiDimWithOrigin(input, bitLens, saveSortRes);
        } else {
            return bitonicPgSortParty.perGen4MultiDimWithOrigin(input, bitLens, saveSortRes);
        }
    }

    @Override
    public TripletZ2Vector[] perGen(TripletZ2Vector[] input) throws MpcAbortException {
        if (input.length <= 3) {
            return radixPgSortParty.perGen(input);
        } else {
            return bitonicPgSortParty.perGen(input);
        }
    }

    @Override
    public TripletZ2Vector[] perGenAndSortOrigin(TripletZ2Vector[] input) throws MpcAbortException {
        if (input.length <= 3) {
            return radixPgSortParty.perGenAndSortOrigin(input);
        } else {
            return bitonicPgSortParty.perGenAndSortOrigin(input);
        }
    }
}

