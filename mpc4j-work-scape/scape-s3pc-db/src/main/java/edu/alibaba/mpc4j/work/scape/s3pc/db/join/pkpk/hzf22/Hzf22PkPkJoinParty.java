package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.hzf22;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.Z2CircuitConfig;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory;
import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleOperations.ShuffleOp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.AbstractPkPkJoinParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.PkPkJoinFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.PkPkJoinParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.SortUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.RandomEncodingFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.RandomEncodingFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.RandomEncodingParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.mrr20.Mrr20RandomEncodingConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.MergeFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.MergeFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.MergeParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteParty;
import gnu.trove.list.linked.TLongLinkedList;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * HZF22 PkPk join protocol
 *
 * @author Feng Han
 * @date 2025/2/21
 */
public class Hzf22PkPkJoinParty extends AbstractPkPkJoinParty implements PkPkJoinParty {
    /**
     * adder type
     */
    public final ComparatorType comparatorType;
    /**
     * z2 circuit
     */
    protected final Z2IntegerCircuit z2IntegerCircuit;
    /**
     * permute party
     */
    protected final PermuteParty permuteParty;
    /**
     * random encoding party
     */
    protected final RandomEncodingParty encodingParty;
    /**
     * soprp party
     */
    protected final MergeParty mergeParty;

    public Hzf22PkPkJoinParty(Abb3Party abb3Party, Hzf22PkPkJoinConfig config) {
        super(Hzf22PkPkJoinPtoDesc.getInstance(), abb3Party, config);
        comparatorType = config.getComparatorTypes();
        z2IntegerCircuit = new Z2IntegerCircuit(z2cParty, new Z2CircuitConfig.Builder().setComparatorType(comparatorType).build());
        permuteParty = PermuteFactory.createParty(abb3Party, config.getPermuteConfig());
        encodingParty = RandomEncodingFactory.createParty(abb3Party, config.getEncodingConfig());
        mergeParty = MergeFactory.createParty(abb3Party, config.getMergeConfig());
        addMultiSubPto(permuteParty, encodingParty, mergeParty);
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        permuteParty.init();
        encodingParty.init();
        mergeParty.init();
        abb3Party.init();
        initState();
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public long[] setUsage(PkPkJoinFnParam... params) {
        long[] tuples = new long[]{0, 0};
        if (isMalicious) {
            for (PkPkJoinFnParam param : params) {
                int totalNum = param.leftDataNum + param.rightDataNum;
                int keyDim = param.isInputSorted ? param.keyDim : Mrr20RandomEncodingConfig.THRESHOLD_REDUCE;
                TLongLinkedList notSet = new TLongLinkedList();
                // circuit
                if (withDummy) {
                    notSet.add(2L * totalNum);
                }
                long eq = (long) keyDim * totalNum;
                notSet.add(eq);
                if (!param.isInputSorted) {
                    // switch network
                    long permuteLeft = abb3Party.getShuffleParty().getTupleNum(ShuffleOp.B_PERMUTE_NETWORK, param.leftDataNum, param.leftDataNum,
                        Mrr20RandomEncodingConfig.THRESHOLD_REDUCE + keyDim + param.leftValueDim + 1);
                    long permuteRight = abb3Party.getShuffleParty().getTupleNum(ShuffleOp.B_PERMUTE_NETWORK, param.rightDataNum, param.rightDataNum,
                        Mrr20RandomEncodingConfig.THRESHOLD_REDUCE + keyDim + param.leftValueDim + 1);
                    long shuffleRight = abb3Party.getShuffleParty().getTupleNum(ShuffleOp.B_SHUFFLE_COLUMN,
                        param.rightDataNum, param.rightDataNum,
                        param.keyDim + param.leftValueDim + param.leftValueDim + 1);
                    long verifyCompare = 2L * ComparatorFactory.getAndGateNum(comparatorType, Mrr20RandomEncodingConfig.THRESHOLD_REDUCE) * totalNum;
                    notSet.add(permuteLeft + permuteRight + shuffleRight + verifyCompare);
                }
                long noSetAll = notSet.sum();
                abb3Party.updateNum(noSetAll, 0);
                tuples[0] += noSetAll;

                // functions
                int indexDim = LongUtils.ceilLog2(totalNum);
                long[] mergeTuple = mergeParty.setUsage(new MergeFnParam(param.leftDataNum, param.rightDataNum, keyDim + indexDim + 2));
                long[] composeTuple = permuteParty.setUsage(new PermuteFnParam(PermuteOp.COMPOSE_B_B, totalNum, param.leftValueDim, indexDim));
                long[] permuteTuple = permuteParty.setUsage(new PermuteFnParam(PermuteOp.APPLY_INV_B_B, totalNum, param.leftValueDim + 1, indexDim));
                tuples[0] += mergeTuple[0] + composeTuple[0] + permuteTuple[0];
                tuples[1] += mergeTuple[1] + composeTuple[1] + permuteTuple[1];
                if(!param.isInputSorted){
                    long[] encodingTuple = encodingParty.setUsage(new RandomEncodingFnParam(param.keyDim, param.leftDataNum, param.rightDataNum, true));
                    tuples[0] += encodingTuple[0];
                    tuples[1] += encodingTuple[1];
                }
            }
        }
        return tuples;
    }

    @Override
    public TripletZ2Vector[] primaryKeyInnerJoin(TripletZ2Vector[] left, TripletZ2Vector[] right, int[] leftKeyIndex,
                                                 int[] rightKeyIndex, boolean withDummy, boolean inputIsSorted) throws MpcAbortException {
        inputProcess(left, right, leftKeyIndex, rightKeyIndex, withDummy, inputIsSorted);
        logPhaseInfo(PtoState.PTO_BEGIN, "primaryKeyInnerJoin");

        stopWatch.start();
        // 1. 得到需要Merge的input
        TripletZ2Vector[][] mergeKey = getSortInput();
        logStepInfo(PtoState.PTO_STEP, 1, 4, resetAndGetTime(), "get merge input");

        stopWatch.start();
        // 2. merge
        TripletZ2Vector[] mergeRes = mergeTables(mergeKey[0], mergeKey[1]);
        logStepInfo(PtoState.PTO_STEP, 2, 4, resetAndGetTime(), "merge");

        stopWatch.start();
        // 3. get the equal flag and permute the payload of left table
        TripletZ2Vector[] payloadAndEqFlag = getEqFlagAndLeftPayload4RightTab(mergeRes, mergeKey[0].length);
        logStepInfo(PtoState.PTO_STEP, 3, 4, resetAndGetTime(), "permute the payload of the left table");

        stopWatch.start();
        // 4. shuffle the result right table
        TripletZ2Vector[] finalRes = new TripletZ2Vector[newRight.length - 1 + payloadAndEqFlag.length];
        System.arraycopy(newRight, 0, finalRes, 0, keyDim);
        System.arraycopy(payloadAndEqFlag, 0, finalRes, keyDim, payloadAndEqFlag.length - 1);
        System.arraycopy(newRight, keyDim, finalRes, payloadAndEqFlag.length - 1 + keyDim, newRight.length - 1 - keyDim);
        finalRes[finalRes.length - 1] = payloadAndEqFlag[payloadAndEqFlag.length - 1];
        if (!inputIsSorted) {
            // if the input is not sorted, shuffle the output
            finalRes = (TripletZ2Vector[]) abb3Party.getShuffleParty().shuffleColumn(finalRes);
        }
        logStepInfo(PtoState.PTO_STEP, 4, 4, resetAndGetTime(), "concat and shuffle");

        logPhaseInfo(PtoState.PTO_END, "primaryKeyInnerJoin");
        return finalRes;
    }

    /**
     * if inputIsSorted: sort the input keys directly
     * else: randomized encoding the input keys, and use them as the sorting input
     *
     * @return leftSortInput, rightSortInput
     */
    protected TripletZ2Vector[][] getSortInput() throws MpcAbortException {
        if (inputIsSorted) {
            return new TripletZ2Vector[][]{Arrays.copyOf(newLeft, keyDim), Arrays.copyOf(newRight, keyDim)};
        } else {
            TripletZ2Vector[][] encoding = encodingParty.getEncodingForTwoKeys(
                Arrays.copyOf(newLeft, keyDim), newLeft[newLeft.length - 1],
                Arrays.copyOf(newRight, keyDim), newRight[newRight.length - 1], withDummy);
            // two party generate permutations
            Party leftPermuteParty = rpc.getParty(0);
            Party rigthPermuteParty = rpc.getParty(1);
            Party aiderParty = rpc.getParty(2);
            int[] perm4LeftKey = null;
            int[] perm4RightKey = null;
            if (abb3Party.ownParty().equals(leftPermuteParty)) {
                BitVector[] leftEncPlain = z2cParty.revealOwn(encoding[0]);
                z2cParty.revealOther(encoding[1], rigthPermuteParty);
                perm4LeftKey = getPermutation(leftEncPlain);
            } else if (abb3Party.ownParty().equals(rigthPermuteParty)) {
                z2cParty.revealOther(encoding[0], leftPermuteParty);
                BitVector[] rightEncPlain = z2cParty.revealOwn(encoding[1]);
                perm4RightKey = getPermutation(rightEncPlain);
            } else {
                z2cParty.revealOther(encoding[0], leftPermuteParty);
                z2cParty.revealOther(encoding[1], rigthPermuteParty);
            }
            // permute the input table
            TripletZ2Vector[] permInputLeft = new TripletZ2Vector[newLeft.length + encoding[0].length];
            System.arraycopy(newLeft, 0, permInputLeft, 0, newLeft.length);
            System.arraycopy(encoding[0], 0, permInputLeft, newLeft.length, encoding[0].length);
            TripletZ2Vector[] permInputRight = new TripletZ2Vector[newRight.length + encoding[1].length];
            System.arraycopy(newRight, 0, permInputRight, 0, newRight.length);
            System.arraycopy(encoding[1], 0, permInputRight, newRight.length, encoding[1].length);
            permInputLeft = (TripletZ2Vector[]) abb3Party.getShuffleParty().permuteNetwork(
                permInputLeft, perm4LeftKey, permInputLeft[0].bitNum(), leftPermuteParty, rigthPermuteParty, aiderParty);
            permInputRight = (TripletZ2Vector[]) abb3Party.getShuffleParty().permuteNetwork(
                permInputRight, perm4RightKey, permInputRight[0].bitNum(), rigthPermuteParty, leftPermuteParty, aiderParty);
            TripletZ2Vector[] sortedLeftEnc = Arrays.copyOfRange(permInputLeft, newLeft.length, permInputLeft.length);
            TripletZ2Vector[] sortedRightEnc = Arrays.copyOfRange(permInputRight, newRight.length, permInputRight.length);
            if (isMalicious()) {
                // verify two input permutations is correct by comparing the sorted encoding
                TripletZ2Vector[] sortLeftEncUpper = new TripletZ2Vector[encoding[0].length];
                TripletZ2Vector[] sortLeftEncBelow = new TripletZ2Vector[encoding[0].length];
                TripletZ2Vector[] sortRightEncUpper = new TripletZ2Vector[encoding[1].length];
                TripletZ2Vector[] sortRightEncBelow = new TripletZ2Vector[encoding[1].length];
                for (int i = 0; i < encoding[0].length; i++) {
                    sortLeftEncUpper[i] = sortedLeftEnc[i].reduceShiftRight(1);
                    sortLeftEncBelow[i] = (TripletZ2Vector) sortedLeftEnc[i].copy();
                    sortLeftEncBelow[i].reduce(sortLeftEncBelow[i].bitNum() - 1);
                    sortRightEncUpper[i] = sortedRightEnc[i].reduceShiftRight(1);
                    sortRightEncBelow[i] = (TripletZ2Vector) sortedRightEnc[i].copy();
                    sortRightEncBelow[i].reduce(sortRightEncBelow[i].bitNum() - 1);
                }
                TripletZ2Vector leftCompRes = (TripletZ2Vector) z2IntegerCircuit.leq(sortLeftEncBelow, sortLeftEncUpper);
                TripletZ2Vector rightCompRes = (TripletZ2Vector) z2IntegerCircuit.leq(sortRightEncBelow, sortRightEncUpper);
                z2cParty.compareView4Zero(leftCompRes, rightCompRes);
            }
            newLeft = Arrays.copyOf(permInputLeft, newLeft.length);
            newRight = Arrays.copyOf(permInputRight, newRight.length);
            return new TripletZ2Vector[][]{sortedLeftEnc, sortedRightEnc};
        }
    }

    private int[] getPermutation(BitVector[] encoding) {
        BigInteger[] bigOut = ZlDatabase.create(envType, parallel, encoding).getBigIntegerData();
        HashSet<BigInteger> h = new HashSet<>(bigOut.length);
        for (BigInteger x : bigOut) {
            Preconditions.checkArgument(!h.contains(x));
            h.add(x);
        }
        return SortUtils.getPermutation(bigOut);
    }

    /**
     * generate the input for merge, and merge them
     *
     * @return merged [key, valid_flag, table_id, indexes], where valid_flag is not in the output if withDummy is false
     */
    private TripletZ2Vector[] mergeTables(TripletZ2Vector[] leftKey, TripletZ2Vector[] rightKey) throws MpcAbortException {
        MathPreconditions.checkEqual("leftKey.length", "rightKey.length", leftKey.length, rightKey.length);
        // input of merge alg should contain [key, valid_flag, table_id, indexes]
        TripletZ2Vector[] indexes = (TripletZ2Vector[]) z2cParty.setPublicValues(
            Z2VectorUtils.getBinaryIndex(leftNum + rightNum));
        List<TripletZ2Vector> leftInputList = new LinkedList<>();
        List<TripletZ2Vector> rightInputList = new LinkedList<>();
        for (int i = 0; i < leftKey.length; i++) {
            leftInputList.add(leftKey[i]);
            rightInputList.add(rightKey[i]);
        }
        if (withDummy) {
            leftInputList.add(newLeft[newLeft.length - 1]);
            rightInputList.add(newRight[newRight.length - 1]);
        }
        leftInputList.add((TripletZ2Vector) z2cParty.setPublicValues(new BitVector[]{BitVectorFactory.createZeros(leftNum)})[0]);
        rightInputList.add((TripletZ2Vector) z2cParty.setPublicValues(new BitVector[]{BitVectorFactory.createOnes(rightNum)})[0]);
        IntStream.range(0, indexes.length).forEach(i -> {
            leftInputList.add(indexes[i].reduceShiftRight(rightNum));
            indexes[i].reduce(rightNum);
            rightInputList.add(indexes[i]);
        });
        return mergeParty.merge(leftInputList.toArray(TripletZ2Vector[]::new), rightInputList.toArray(TripletZ2Vector[]::new));
    }

    /**
     * permute the payload of the left table and get the equal flag
     */
    private TripletZ2Vector[] getEqFlagAndLeftPayload4RightTab(TripletZ2Vector[] mergeRes, int mergeKeyLen) throws MpcAbortException {
        // get the equal flag
        TripletZ2Vector[] upperInput = IntStream.range(0, mergeKeyLen)
            .mapToObj(i -> mergeRes[i].reduceShiftRight(1))
            .toArray(TripletZ2Vector[]::new);
        TripletZ2Vector[] belowInput = IntStream.range(0, mergeKeyLen)
            .mapToObj(i -> {
                TripletZ2Vector tmp = (TripletZ2Vector) mergeRes[i].copy();
                tmp.reduce(tmp.bitNum() - 1);
                return tmp;
            })
            .toArray(TripletZ2Vector[]::new);
        TripletZ2Vector eqFlag = (TripletZ2Vector) z2IntegerCircuit.eq(upperInput, belowInput);
        if (withDummy) {
            z2cParty.andi(eqFlag, mergeRes[mergeKeyLen].reduceShiftRight(1));
            mergeRes[mergeKeyLen].reduce(mergeRes[mergeKeyLen].bitNum() - 1);
            z2cParty.andi(eqFlag, mergeRes[mergeKeyLen]);
        }
        eqFlag.extendLength(leftNum + rightNum);

        // permute the payload of the left table
        int indexStartPos = mergeKeyLen + (withDummy ? 2 : 1);
        TripletZ2Vector[] perm = Arrays.copyOfRange(mergeRes, indexStartPos, mergeRes.length);
        TripletZ2Vector[] composeRes = new TripletZ2Vector[0];
        if (newLeft.length > keyDim + 1) {
            TripletZ2Vector[] composeInput = IntStream.range(keyDim, newLeft.length - 1)
                .mapToObj(i -> newLeft[i].padShiftLeft(rightNum))
                .toArray(TripletZ2Vector[]::new);
            composeRes = permuteParty.composePermutation(perm, composeInput);
            // leave the required left payload
            Arrays.stream(composeRes).forEach(p -> p.fixShiftRighti(1));
            TripletZ2Vector[] extendEqFlag = IntStream.range(0, composeRes.length)
                .mapToObj(i -> eqFlag).toArray(TripletZ2Vector[]::new);
            z2cParty.andi(composeRes, extendEqFlag);
        }

        // permute
        TripletZ2Vector[] permInput = new TripletZ2Vector[composeRes.length + 1];
        System.arraycopy(composeRes, 0, permInput, 0, composeRes.length);
        permInput[composeRes.length] = eqFlag;
        TripletZ2Vector[] permRes = permuteParty.applyInvPermutation(perm, permInput);
        Arrays.stream(permRes).forEach(p -> p.reduce(rightNum));
        return permRes;
    }
}
