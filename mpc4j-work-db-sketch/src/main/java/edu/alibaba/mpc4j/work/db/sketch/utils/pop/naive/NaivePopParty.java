package edu.alibaba.mpc4j.work.db.sketch.utils.pop.naive;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.AbstractThreePartyOpfPto;
import edu.alibaba.mpc4j.work.db.sketch.utils.pop.PopFnParam;
import edu.alibaba.mpc4j.work.db.sketch.utils.pop.PopParty;

import java.util.Arrays;

/**
 * Naive implementation of Pop (Permute-and-Open) protocol.
 * Uses basic Z2 circuit operations to remove elements from secret-shared arrays.
 */
public class NaivePopParty extends AbstractThreePartyOpfPto implements PopParty {
    /**
     * Z2 integer circuit for boolean operations
     */
    public final Z2IntegerCircuit z2IntegerCircuit;

    /**
     * Constructor for NaivePopParty
     *
     * @param abb3Party the underlying ABB3 party
     * @param config    the protocol configuration
     */
    public NaivePopParty(Abb3Party abb3Party, NaivePopConfig config) {
        super(NaivePopPtoDesc.getInstance(), abb3Party, config);
        z2IntegerCircuit = new Z2IntegerCircuit(abb3Party.getZ2cParty(), config.getCircuitConfig());
    }

    /**
     * Initialize the protocol
     *
     * @throws MpcAbortException if initialization fails
     */
    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        abb3Party.init();
        initState();
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    /**
     * Set the resource usage for the protocol
     *
     * @param params array of function parameters
     * @return array of required tuple numbers [z2Tuples, z64Tuples]
     */
    @Override
    public long[] setUsage(PopFnParam... params) {
        long z2Tuples = 0;
        for (PopFnParam param : params) {
            z2Tuples += (long) param.inputDim * param.inputSize;
            if (param.popFromIndex) {
                z2Tuples += (long) param.inputSize * LongUtils.ceilLog2(param.inputSize);
            }
        }
        abb3Party.updateNum(z2Tuples, 0);
        return new long[]{z2Tuples, 0};
    }

    /**
     * Pop operation with index-based selection
     * Removes the element at the specified index and returns the remaining values
     *
     * @param input Input table of secret-shared values
     * @param index Target index indicating which element should be popped
     * @return array of remaining secret-shared values after popping
     * @throws MpcAbortException if the protocol execution fails
     */
    @Override
    public TripletZ2Vector[] pop(TripletZ2Vector[] input, TripletZ2Vector[] index) throws MpcAbortException {
        MathPreconditions.checkEqual("index.length", "LongUtils.ceilLog2(input[0].bitNum())", index.length, LongUtils.ceilLog2(input[0].bitNum()));
        MathPreconditions.checkGreater("input[0].bitNum() > 1", input[0].bitNum(), 1);
        logPhaseInfo(PtoState.PTO_BEGIN, "pop");

        stopWatch.start();
        TripletZ2Vector[] allIndex = (TripletZ2Vector[]) z2cParty.setPublicValues(Z2VectorUtils.getBinaryIndex(input[0].bitNum()));
        TripletZ2Vector[] extendTargetIndex = Arrays.stream(index)
            .map(ea -> ea.extendSizeWithSameEle(input[0].bitNum()))
            .toArray(TripletZ2Vector[]::new);
        TripletZ2Vector flag = (TripletZ2Vector) z2IntegerCircuit.eq(allIndex, extendTargetIndex);
        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime(), "get flag");

        stopWatch.start();
        TripletZ2Vector[] output = popCircuit(input, flag);
        logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime(), "popCircuit");

        logPhaseInfo(PtoState.PTO_END, "pop");
        return output;
    }

    /**
     * Pop operation with flag-based selection
     * Removes elements indicated by the flag and returns the remaining values
     *
     * @param input Input table of secret-shared values
     * @param flag  Indicator flag, where each bit indicates whether the corresponding element should be popped
     * @return array of remaining secret-shared values after popping
     */
    @Override
    public TripletZ2Vector[] pop(TripletZ2Vector[] input, TripletZ2Vector flag) {
        MathPreconditions.checkGreater("input[0].bitNum() > 1", input[0].bitNum(), 1);
        logPhaseInfo(PtoState.PTO_BEGIN, "pop");

        stopWatch.start();
        TripletZ2Vector[] output = popCircuit(input, flag);
        logStepInfo(PtoState.PTO_STEP, 1, 1, resetAndGetTime(), "popCircuit");

        logPhaseInfo(PtoState.PTO_END, "pop");
        return output;
    }

    /**
     * Core circuit implementation of the pop operation
     * Uses XOR and MUX operations to remove elements based on the flag
     *
     * @param input Input table of secret-shared values
     * @param flag  Indicator flag for elements to pop
     * @return array of remaining secret-shared values
     */
    private TripletZ2Vector[] popCircuit(TripletZ2Vector[] input, TripletZ2Vector flag) {
        MathPreconditions.checkEqual("input[0].bitNum()", "flag.length", input[0].bitNum(), flag.bitNum());
        int num = flag.getNum();
        TripletZ2Vector xorBeforeFlag = (TripletZ2Vector) z2cParty.xorAllBeforeElement(flag);
        xorBeforeFlag = xorBeforeFlag.reduceShiftRight(1);
        TripletZ2Vector[] rightOne = Arrays.stream(input).map(each -> {
            TripletZ2Vector tmp = (TripletZ2Vector) each.copy();
            tmp.reduce(num - 1);
            return tmp;
        }).toArray(TripletZ2Vector[]::new);
        TripletZ2Vector[] originalOne = Arrays.stream(input).map(each -> each.reduceShiftRight(1)).toArray(TripletZ2Vector[]::new);
        return z2cParty.mux(originalOne, rightOne, xorBeforeFlag);
    }
}
