package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.hzf22;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvOperations.ConvOp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.AbstractThreePartyDbPto;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.SortSignFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.SortSignParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.MergeFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.MergeFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.MergeParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations.PgSortFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortParty;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * HZF22 SortSign Party
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public class Hzf22SortSignParty extends AbstractThreePartyDbPto implements SortSignParty {
    /**
     * z2 circuit
     */
    protected final Z2IntegerCircuit z2IntegerCircuit;
    /**
     * sorting party
     */
    protected final PgSortParty sortParty;
    /**
     * sorting party
     */
    protected final MergeParty mergeParty;
    /**
     * permute party
     */
    protected final PermuteParty permuteParty;
    /**
     * key dimension
     */
    private int aKeyDim;
    /**
     * leftTableSize
     */
    private int leftSize;
    /**
     * rightTableSize
     */
    private int rightSize;

    public Hzf22SortSignParty(Abb3Party abb3Party, Hzf22SortSignConfig config) {
        super(Hzf22SortSignPtoDesc.getInstance(), abb3Party, config);
        z2IntegerCircuit = new Z2IntegerCircuit(z2cParty);
        sortParty = PgSortFactory.createParty(abb3Party, config.getPgSortConfig());
        mergeParty = MergeFactory.createParty(abb3Party, config.getMergeConfig());
        permuteParty = PermuteFactory.createParty(abb3Party, config.getPermuteConfig());
        addMultiSubPto(sortParty, mergeParty);
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        sortParty.init();
        mergeParty.init();
        permuteParty.init();
        abb3Party.init();
        initState();
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public long[] setUsage(SortSignFnParam... params) {
        long[] res = new long[]{0, 0};
        List<long[]> abbTuples = new LinkedList<>();
        List<long[]> funTuples = new LinkedList<>();
        for (SortSignFnParam param : params) {
            int totalDataNum = param.leftTableLen + param.rightTableLen;
            if (param.inputIsSorted) {
                int logLen = LongUtils.ceilLog2(totalDataNum);
                abbTuples.add(abb3Party.getConvParty().getTupleNum(ConvOp.A2B, totalDataNum, 1, 1));
                abbTuples.add(abb3Party.getConvParty().getTupleNum(ConvOp.A2B, totalDataNum, param.keyDim, 64));
                abbTuples.add(abb3Party.getConvParty().getTupleNum(ConvOp.B2A, totalDataNum, 1, logLen));
                funTuples.add(mergeParty.setUsage(new MergeFnParam(param.leftTableLen, param.rightTableLen, logLen + 2 * 64 * param.keyDim)));
                funTuples.add(permuteParty.setUsage(new PermuteFnParam(PermuteOp.APPLY_INV_A_A, totalDataNum, 1, 64)));
            } else {
                int[] eachBits = new int[param.keyDim + 2];
                eachBits[0] = 1;
                eachBits[eachBits.length - 1] = 1;
                Arrays.fill(eachBits, 1, eachBits.length - 1, 64);
                funTuples.add(sortParty.setUsage(new PgSortFnParam(PgSortOperations.PgSortOp.SORT_A, totalDataNum, eachBits)));
            }
            abbTuples.add(abb3Party.getConvParty().getTupleNum(ConvOp.BIT2A, totalDataNum, 4, 64));
            abbTuples.add(new long[]{totalDataNum * (64L + 3), 0});
        }
        long[] funTuple = funTuples.stream().reduce(new long[]{0, 0}, (a, b) -> new long[]{a[0] + b[0], a[1] + b[1]});
        long[] abbTuple = abbTuples.stream().reduce(new long[]{0, 0}, (a, b) -> new long[]{a[0] + b[0], a[1] + b[1]});
        abb3Party.updateNum(abbTuple[0], abbTuple[1]);
        res[0] += funTuple[0] + abbTuple[0];
        res[1] += funTuple[1] + abbTuple[1];
        return res;
    }

    @Override
    public TripletLongVector[] preSort(TripletLongVector[] leftKeys, TripletLongVector[] rightKeys,
                                       TripletLongVector leftValidFlag, TripletLongVector rightValidFlag, boolean isInputSorted) throws MpcAbortException {
        checkInput(leftKeys, rightKeys, leftValidFlag, rightValidFlag);

        TripletLongVector[] result;
        logPhaseInfo(PtoState.PTO_BEGIN, "preSort");
        if (isInputSorted) {
            stopWatch.start();
            // generate the input for merge
            BitVector[] index = Z2VectorUtils.getBinaryIndex(leftSize + rightSize);
            TripletZ2Vector[] shareIndex = (TripletZ2Vector[]) z2cParty.setPublicValues(index);
            TripletZ2Vector[] leftMergeInput = new TripletZ2Vector[aKeyDim * 64 + 2 + index.length];
            TripletZ2Vector[] rightMergeInput = new TripletZ2Vector[aKeyDim * 64 + 2 + index.length];
            leftMergeInput[0] = abb3Party.getConvParty().a2b(leftValidFlag, 1)[0];
            z2cParty.noti(leftMergeInput[0]);
            rightMergeInput[0] = abb3Party.getConvParty().a2b(rightValidFlag, 1)[0];
            z2cParty.noti(rightMergeInput[0]);
            TripletZ2Vector[][] leftKeyBinary = abb3Party.getConvParty().a2b(leftKeys, 64);
            TripletZ2Vector[][] rightKeyBinary = abb3Party.getConvParty().a2b(rightKeys, 64);
            for (int i = 0; i < aKeyDim; i++) {
                System.arraycopy(leftKeyBinary[i], 0, leftMergeInput, 1 + i * 64, 64);
                System.arraycopy(rightKeyBinary[i], 0, rightMergeInput, 1 + i * 64, 64);
            }
            leftMergeInput[aKeyDim * 64 + 1] = (TripletZ2Vector) z2cParty.setPublicValues(
                new BitVector[]{BitVectorFactory.createZeros(leftSize)})[0];
            rightMergeInput[aKeyDim * 64 + 1] = (TripletZ2Vector) z2cParty.setPublicValues(
                new BitVector[]{BitVectorFactory.createOnes(rightSize)})[0];
            for (int i = 0; i < shareIndex.length; i++) {
                leftMergeInput[aKeyDim * 64 + 2 + i] = shareIndex[i].reduceShiftRight(rightSize);
                rightMergeInput[aKeyDim * 64 + 2 + i] = shareIndex[i];
                rightMergeInput[aKeyDim * 64 + 2 + i].reduce(rightSize);
            }
            TripletZ2Vector[] mergeRes = mergeParty.merge(leftMergeInput, rightMergeInput);
            logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime(), "merge");

            stopWatch.start();
            TripletLongVector[] signs = getSign(Arrays.copyOf(mergeRes, mergeRes.length - index.length));
            TripletLongVector shuffledId = abb3Party.getConvParty().bit2a(mergeRes[1 + aKeyDim * 64]);
            TripletLongVector invPi = abb3Party.getConvParty().b2a(Arrays.copyOfRange(mergeRes, 2 + aKeyDim * 64, mergeRes.length));
            TripletLongVector pi = permuteParty.applyInvPermutation(invPi,
                (TripletLongVector) zl64cParty.setPublicValue(LongVector.create(LongStream.range(0, leftSize + rightSize).toArray())))[0];
            result = new TripletLongVector[]{signs[0], signs[1], signs[2], shuffledId, pi};
            logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime(), "get signs");
        } else {
            stopWatch.start();
            // 排序的输入是 invFlag，keys，Id
            TripletLongVector[] all = new TripletLongVector[aKeyDim + 2];
            all[0] = (TripletLongVector) leftValidFlag.copy();
            all[0].merge(rightValidFlag);
            zl64cParty.negi(all[0]);
            zl64cParty.addi(all[0], 1L);
            for (int i = 0; i < aKeyDim; i++) {
                all[i + 1] = (TripletLongVector) leftKeys[i].copy();
                all[i + 1].merge(rightKeys[i]);
            }
            long[] tmp = new long[leftSize + rightSize];
            IntStream.range(leftSize, tmp.length).forEach(i -> tmp[i] = 1L);
            all[aKeyDim + 1] = (TripletLongVector) zl64cParty.setPublicValue(LongVector.create(tmp));
            // 输入的有效bits
            int[] eachBits = new int[aKeyDim + 2];
            eachBits[0] = 1;
            eachBits[eachBits.length - 1] = 1;
            Arrays.fill(eachBits, 1, eachBits.length - 1, 64);
            TripletZ2Vector[] saveSortRes = new TripletZ2Vector[64 * aKeyDim + 2];
            TripletLongVector kPai = sortParty.perGen4MultiDimWithOrigin(all, eachBits, saveSortRes);
            logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime(), "sort");

            stopWatch.start();
            // 根据排序选择的方法得到结果
            TripletLongVector shuffledId = abb3Party.getConvParty().bit2a(saveSortRes[saveSortRes.length - 1]);
            TripletLongVector[] signs = getSign(saveSortRes);
            result = new TripletLongVector[]{signs[0], signs[1], signs[2], shuffledId, kPai};
            logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime(), "get signs");
        }

        logPhaseInfo(PtoState.PTO_END, "preSort");
        return result;
    }

    /**
     * check whether the input is legal
     */
    protected void checkInput(MpcVector[] leftKeys, MpcVector[] rightKeys,
                            MpcVector leftValidFlag, MpcVector rightValidFlag) {
        Preconditions.checkArgument(leftKeys.length == rightKeys.length);
        Preconditions.checkArgument(leftKeys[0].getNum() == leftValidFlag.getNum());
        Preconditions.checkArgument(rightKeys[0].getNum() == rightValidFlag.getNum());
        aKeyDim = leftKeys.length;
        leftSize = leftValidFlag.getNum();
        rightSize = rightValidFlag.getNum();
    }

    /**
     * 输入是 invFlag，keys，Id
     * 输出的三个sign分别是(E_1, E_upper, E_down)，分别代表上下id不同但key相同，和上面的key相同，和下面的key相同。
     */
    private TripletLongVector[] getSign(TripletZ2Vector[] saveSortRes) throws MpcAbortException {
        int originBitNum = saveSortRes[0].getNum();
        TripletZ2Vector[] tmpRes = new TripletZ2Vector[3];
        z2cParty.noti(saveSortRes[0]);
        IntStream intStream = parallel ? IntStream.range(0, saveSortRes.length).parallel() : IntStream.range(0, saveSortRes.length);
        TripletZ2Vector[] upper = new TripletZ2Vector[saveSortRes.length], below = new TripletZ2Vector[saveSortRes.length];
        intStream.forEach(i -> {
            upper[i] = saveSortRes[i].reduceShiftRight(1);
            below[i] = (TripletZ2Vector) saveSortRes[i].copy();
            below[i].reduce(originBitNum - 1);
        });
        TripletZ2Vector comRes = (TripletZ2Vector) z2IntegerCircuit.eq(Arrays.copyOfRange(upper, 1, upper.length - 1),
            Arrays.copyOfRange(below, 1, below.length - 1));
        z2cParty.andi(comRes, upper[0]);
        z2cParty.andi(comRes, below[0]);
        tmpRes[0] = z2cParty.and(comRes, z2cParty.xor(upper[upper.length - 1], below[below.length - 1]));
        tmpRes[0].extendLength(originBitNum);
        tmpRes[1] = (TripletZ2Vector) comRes.copy();
        tmpRes[1].extendLength(originBitNum);
        tmpRes[2] = comRes.padShiftLeft(1);
        return abb3Party.getConvParty().bit2a(tmpRes);
    }
}
