package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.hzf22;

import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvOperations.ConvOp;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleOperations.ShuffleOp;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.ShuffleUtils;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.AbstractFillPermutationParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPerOperations.FillPerFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPermutationParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpOperations.PrpFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpOperations.PrpOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpParty;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.LongStream;

/**
 * HZF22 fill permutation party.
 *
 * @author Feng Han
 * @date 2025/2/26
 */
public class Hzf22FillPermutationParty extends AbstractFillPermutationParty implements FillPermutationParty {
    /**
     * soprp party
     */
    protected final SoprpParty soprpParty;

    public Hzf22FillPermutationParty(Abb3Party abb3Party, Hzf22FillPermutationConfig config) {
        super(Hzf22FillPermutationPtoDesc.getInstance(), abb3Party, config);
        soprpParty = SoprpFactory.createParty(abb3Party, config.getSoprpConfig());
        addMultiSubPto(soprpParty);
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        soprpParty.init();
        abb3Party.init();
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public long[] setUsage(FillPerFnParam... params) {
        if (!isMalicious()) {
            return new long[]{0, 0};
        }
        long[] tupleNums = new long[]{0, 0};
        for (FillPerFnParam param : params) {
            int indexDim = LongUtils.ceilLog2(param.outputLen);
            switch (param.op) {
                case FILL_ONE_PER_A: {
                    // shuffle
                    long shuffleBit = abb3Party.getShuffleParty().getTupleNum(ShuffleOp.B_SHUFFLE_COLUMN, param.outputLen, param.outputLen, indexDim);
                    // b2a
                    long[] b2aTuple = abb3Party.getConvParty().getTupleNum(ConvOp.B2A, param.outputLen, 1, 64);
                    // a2b
                    long[] a2bTuple = abb3Party.getConvParty().getTupleNum(ConvOp.A2B, param.inputLen[0], 1, indexDim);
                    abb3Party.updateNum(shuffleBit + b2aTuple[0] + a2bTuple[0], b2aTuple[0] + a2bTuple[1]);

                    long[] encTuple = soprpParty.setUsage(new PrpFnParam(PrpOp.ENC, param.outputLen + param.inputLen[0], Hzf22FillPermutationConfig.TARGET_DIM));
                    tupleNums[0] += b2aTuple[0] + a2bTuple[0] + encTuple[0] + shuffleBit;
                    tupleNums[1] += b2aTuple[1] + a2bTuple[1] + encTuple[1];
                    break;
                }
                case FILL_TWO_PER_A: {
                    // shuffle
                    long shuffleBit = abb3Party.getShuffleParty().getTupleNum(ShuffleOp.B_SHUFFLE_COLUMN, param.outputLen, param.outputLen, indexDim);
                    // a2b
                    long[] a2bTuple = abb3Party.getConvParty().getTupleNum(ConvOp.A2B, param.inputLen[0] + param.inputLen[1], 1, indexDim);
                    abb3Party.updateNum(shuffleBit + a2bTuple[0], a2bTuple[1]);

                    long[] encTuple = soprpParty.setUsage(
                        new PrpFnParam(PrpOp.ENC, param.outputLen + param.inputLen[0] + param.inputLen[1], Hzf22FillPermutationConfig.TARGET_DIM));
                    tupleNums[0] += a2bTuple[0] + encTuple[0] + shuffleBit;
                    tupleNums[1] += a2bTuple[1] + encTuple[1];
                    break;
                }
                default:
                    throw new IllegalArgumentException("illegal FillPerFnParam" + param.op.name());
            }
        }
        return tupleNums;
    }

    @Override
    public TripletLongVector permutationCompletion(TripletLongVector index, TripletLongVector equalSign, int m) throws MpcAbortException {
        checkInput(index, equalSign, m);
        logPhaseInfo(PtoState.PTO_BEGIN, "permutationCompletion");

        stopWatch.start();
        TripletZ2Vector[] fullPerm = (TripletZ2Vector[]) z2cParty.setPublicValues(Z2VectorUtils.getBinaryIndex(m));
        fullPerm = (TripletZ2Vector[]) abb3Party.getShuffleParty().shuffleColumn(fullPerm);
        logStepInfo(PtoState.PTO_STEP, 1, 4, resetAndGetTime(), "shuffle the full permutation");

        stopWatch.start();
        TripletZ2Vector[] originalIndex = abb3Party.getConvParty().a2b(index, fullPerm.length);
        logStepInfo(PtoState.PTO_STEP, 2, 4, resetAndGetTime(), "a2b");

        stopWatch.start();
        TripletZ2Vector[] oprpInput = new TripletZ2Vector[fullPerm.length];
        for (int i = 0; i < fullPerm.length; i++) {
            oprpInput[i] = (TripletZ2Vector) fullPerm[i].copy();
            oprpInput[i].merge(originalIndex[i]);
        }
        TripletZ2Vector[] oprpOutput = soprpParty.enc(oprpInput);
        BitVector[] openEnc = z2cParty.open(oprpOutput);
        BigInteger[] openEncBig = ZlDatabase.create(envType, parallel, openEnc).getBigIntegerData();
        logStepInfo(PtoState.PTO_STEP, 3, 4, resetAndGetTime(), "encoding and open");

        stopWatch.start();
        int[] pi = genPermutation(m, Arrays.copyOf(openEncBig, m), Arrays.copyOfRange(openEncBig, m, openEncBig.length));
        TripletZ2Vector[] resultB = ShuffleUtils.applyPermutationToRows(fullPerm, pi);
        TripletLongVector result = abb3Party.getConvParty().b2a(resultB);
        logStepInfo(PtoState.PTO_STEP, 4, 4, resetAndGetTime(), "encoding and open");

        logPhaseInfo(PtoState.PTO_END, "permutationCompletion");
        return result;
    }

    private static int[] genPermutation(int m, BigInteger[] allIndexEnc, BigInteger[] requiredIndexEnc) {
        int[] pi = new int[m];
        HashMap<BigInteger, Integer> map = new HashMap<>();
        for (int i = 0; i < requiredIndexEnc.length; i++) {
            map.put(requiredIndexEnc[i], i);
        }
        int otherStartIndex = requiredIndexEnc.length;
        for (int i = 0; i < m; i++) {
            if (map.containsKey(allIndexEnc[i])) {
                pi[map.get(allIndexEnc[i])] = i;
            } else {
                pi[otherStartIndex] = i;
                otherStartIndex++;
            }
        }
        return pi;
    }

    /**
     * 使用的方法是一次encoding，由两个不同的Party完成permutation的对齐，然后验证两个Party对齐permutation的操作是正确的
     */
    @Override
    public TripletLongVector[] twoPermutationCompletion(TripletLongVector leftIndex, TripletLongVector leftEqual, TripletLongVector rightIndex, TripletLongVector rightEqual, int m) throws MpcAbortException {
        checkInput(leftIndex, leftEqual, rightIndex, rightEqual, m);
        logPhaseInfo(PtoState.PTO_BEGIN, "twoPermutationCompletion");

        stopWatch.start();
        int[][] rand = abb3Party.getTripletProvider().getCrProvider().getRandIntArray(m);
        int[][] pi = Arrays.stream(rand).map(ShuffleUtils::permutationGeneration).toArray(int[][]::new);
        TripletZ2Vector[] fullPerm = (TripletZ2Vector[]) z2cParty.setPublicValues(Z2VectorUtils.getBinaryIndex(m));
        fullPerm = (TripletZ2Vector[]) abb3Party.getShuffleParty().shuffleColumn(pi, fullPerm);
        TripletLongVector fullPermLong = (TripletLongVector) zl64cParty.setPublicValue(LongVector.create(LongStream.range(0, m).toArray()));
        fullPermLong = (TripletLongVector) abb3Party.getShuffleParty().shuffle(pi, fullPermLong)[0];
        logStepInfo(PtoState.PTO_STEP, 1, 4, resetAndGetTime(), "shuffle the full permutation");

        stopWatch.start();
        TripletLongVector allOriginal = (TripletLongVector) leftIndex.copy();
        allOriginal.merge(rightIndex);
        TripletZ2Vector[] originalIndex = abb3Party.getConvParty().a2b(allOriginal, fullPerm.length);
        logStepInfo(PtoState.PTO_STEP, 2, 4, resetAndGetTime(), "a2b");

        stopWatch.start();
        TripletZ2Vector[] oprpInput = new TripletZ2Vector[fullPerm.length];
        for (int i = 0; i < fullPerm.length; i++) {
            oprpInput[i] = (TripletZ2Vector) fullPerm[i].copy();
            oprpInput[i].merge(originalIndex[i]);
        }
        TripletZ2Vector[] oprpOutput = soprpParty.enc(oprpInput);
        TripletZ2Vector[] allIndexEnc = Arrays.stream(oprpOutput)
            .map(ea -> ea.reduceShiftRight(leftIndex.getNum() + rightIndex.getNum()))
            .toArray(TripletZ2Vector[]::new);
        TripletZ2Vector[] leftIndexEnc = Arrays.stream(oprpOutput)
            .map(ea -> {
                TripletZ2Vector tmp = ea.reduceShiftRight(rightIndex.getNum());
                tmp.reduce(leftIndex.getNum());
                return tmp;
            })
            .toArray(TripletZ2Vector[]::new);
        TripletZ2Vector[] rightIndexEnc = Arrays.stream(oprpOutput)
            .map(ea -> {
                TripletZ2Vector tmp = (TripletZ2Vector) ea.copy();
                tmp.reduce(rightIndex.getNum());
                return tmp;
            })
            .toArray(TripletZ2Vector[]::new);
        Party firstParty = rpc.getParty(0);
        Party secondParty = rpc.getParty(1);
        Party thirdParty = rpc.getParty(2);
        int[] permuteLeft = null;
        int[] permuteRight = null;
        if (firstParty.getPartyId() == rpc.ownParty().getPartyId()) {
            BitVector[] allIndexPlain = z2cParty.revealOwn(allIndexEnc);
            z2cParty.revealOther(allIndexEnc, secondParty);
            BitVector[] leftIndexPlain = z2cParty.revealOwn(leftIndexEnc);
            z2cParty.revealOther(rightIndexEnc, secondParty);
            permuteLeft = genPermutation(m,
                ZlDatabase.create(envType, parallel, allIndexPlain).getBigIntegerData(),
                ZlDatabase.create(envType, parallel, leftIndexPlain).getBigIntegerData());
        } else if (secondParty.getPartyId() == rpc.ownParty().getPartyId()) {
            z2cParty.revealOther(allIndexEnc, firstParty);
            BitVector[] allIndexPlain = z2cParty.revealOwn(allIndexEnc);
            z2cParty.revealOther(leftIndexEnc, firstParty);
            BitVector[] rightIndexPlain = z2cParty.revealOwn(rightIndexEnc);
            permuteRight = genPermutation(m,
                ZlDatabase.create(envType, parallel, allIndexPlain).getBigIntegerData(),
                ZlDatabase.create(envType, parallel, rightIndexPlain).getBigIntegerData());
        } else {
            z2cParty.revealOther(allIndexEnc, firstParty);
            z2cParty.revealOther(allIndexEnc, secondParty);
            z2cParty.revealOther(leftIndexEnc, firstParty);
            z2cParty.revealOther(rightIndexEnc, secondParty);
        }
        logStepInfo(PtoState.PTO_STEP, 3, 4, resetAndGetTime(), "encoding and reveal");

        stopWatch.start();
        // permute
        TripletLongVector leftRes = (TripletLongVector) abb3Party.getShuffleParty().permuteNetwork(
            new TripletLongVector[]{fullPermLong}, permuteLeft, m, firstParty, secondParty, thirdParty)[0];
        TripletLongVector rightRes = (TripletLongVector) abb3Party.getShuffleParty().permuteNetwork(
            new TripletLongVector[]{fullPermLong}, permuteRight, m, secondParty, firstParty, thirdParty)[0];
        if (isMalicious) {
            TripletLongVector leftShouldBeZero = zl64cParty.sub(
                leftRes.copyOfRange(0, leftIndex.getNum()), leftIndex);
            TripletLongVector rightShouldBeZero = zl64cParty.sub(
                rightRes.copyOfRange(0, rightIndex.getNum()), rightIndex);
            zl64cParty.compareView4Zero(64, leftShouldBeZero, rightShouldBeZero);
        }
        logStepInfo(PtoState.PTO_STEP, 4, 4, resetAndGetTime(), "permute and verify");

        logPhaseInfo(PtoState.PTO_END, "twoPermutationCompletion");
        return new TripletLongVector[]{leftRes, rightRes};
    }
}
