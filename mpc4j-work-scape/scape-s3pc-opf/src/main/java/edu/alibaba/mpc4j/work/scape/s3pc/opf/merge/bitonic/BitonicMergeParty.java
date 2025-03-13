package edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.bitonic;

import edu.alibaba.mpc4j.common.circuit.z2.Z2CircuitConfig;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory;
import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.AbstractThreePartyOpfPto;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.MergeFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.MergeParty;

/**
 * merge two sorted inputs with bitonic merge
 *
 * @author Feng Han
 * @date 2025/2/21
 */
public class BitonicMergeParty extends AbstractThreePartyOpfPto implements MergeParty {
    /**
     * adder type
     */
    public final ComparatorType comparatorType;
    /**
     * merge sorter
     */
    private final BitonicMergeCircuit mergeCircuit;

    public BitonicMergeParty(Abb3Party abb3Party, BitonicMergeConfig config) {
        super(BitonicMergePtoDesc.getInstance(), abb3Party, config);
        comparatorType = config.getComparatorTypes();
        mergeCircuit = new BitonicMergeCircuit(new Z2IntegerCircuit(z2cParty,
            new Z2CircuitConfig.Builder().setComparatorType(comparatorType).build()));
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
    public long[] setUsage(MergeFnParam... params) {
        if (!isMalicious) {
            return new long[]{0, 0};
        }
        long bitTupleNum = 0;
        for (MergeFnParam funParam : params) {
            int halfMax = BitonicMergeCircuit.smallestPowerOfTwoBiggerEqualThan(Math.max(funParam.leftDataNum, funParam.rightDataNum));
            int fixedTotalLen = halfMax << 1;
            boolean noExtraBit = halfMax == funParam.leftDataNum && funParam.leftDataNum == funParam.rightDataNum;
            int bitLen = noExtraBit ? funParam.dim + 1 : funParam.dim;
            bitTupleNum += (long) (bitLen + ComparatorFactory.getAndGateNum(comparatorType, bitLen))
                * LongUtils.ceilLog2(fixedTotalLen) * fixedTotalLen / 2;
        }
        abb3Party.updateNum(bitTupleNum, 0);
        return new long[]{bitTupleNum, 0};
    }

    @Override
    public TripletZ2Vector[] merge(TripletZ2Vector[] first, TripletZ2Vector[] second) throws MpcAbortException {
        MathPreconditions.checkEqual("first.length", "second.length", first.length, second.length);
        logPhaseInfo(PtoState.PTO_BEGIN, "merge");

        stopWatch.start();
        TripletZ2Vector[] mergeRes = (TripletZ2Vector[]) mergeCircuit.fromTwoSorted(first, second);
        logStepInfo(PtoState.PTO_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END, "merge");
        return mergeRes;
    }

}
