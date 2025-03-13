package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.quick;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.Z2CircuitConfig;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory;
import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvOperations.ConvOp;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.PrpUtils;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.AbstractPgSortParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations.PgSortFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.quick.QuickPgSortPtoDesc.PtoStep;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * oblivious mixed sorting party using somewhat-opt strategy
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class QuickPgSortParty extends AbstractPgSortParty implements PgSortParty {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuickPgSortParty.class);
    /**
     * adder type
     */
    public final ComparatorType comparatorType;
    /**
     * z2 circuit
     */
    public final Z2IntegerCircuit z2IntegerCircuit;
    /**
     * shuffle party
     */
    public final ShuffleParty shuffleParty;
    /**
     * permute party
     */
    public final PermuteParty permuteParty;
    /**
     * the threshold of small group. if the number of elements is smaller than it, perform comparison between each 2 elements
     */
    private static final int SMALL_THRESHOLD = 7;
    /**
     * the number to decide the pivot number for each group, e.g. if 5 <= ceil(num) < 7, use 2 pivot
     */
    private static final int[] PIVOT_NUM = new int[]{3, 5, 7};
    /**
     * the seed to generate randoms for pivot choice
     */
    private byte[] seed;
    /**
     * the prp to generate randoms for pivot choice
     */
    private Prp[] prp;
    /**
     * the prp index to generate randoms for pivot choice
     */
    private int randomFrom;

    /**
     * related timer
     */
    public long transTimer, compareTimer, dealPivotTimer, dealPermuteTimer, totalTimer;

    public QuickPgSortParty(Abb3Party abb3Party, QuickPgSortConfig config) {
        super(QuickPgSortPtoDesc.getInstance(), abb3Party, config);
        comparatorType = config.getComparatorTypes();
        z2IntegerCircuit = new Z2IntegerCircuit(abb3Party.getZ2cParty(),
            new Z2CircuitConfig.Builder().setComparatorType(comparatorType).build());
        shuffleParty = abb3Party.getShuffleParty();
        permuteParty = PermuteFactory.createParty(abb3Party, config.getPermuteConfig());
        addMultiSubPto(permuteParty);
        this.resetTimer();
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        if (rpc.ownParty().getPartyId() == 0) {
            seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            secureRandom.nextBytes(seed);
            send(PtoStep.SEND_SEED.ordinal(), rpc.getParty(1), Collections.singletonList(seed));
            send(PtoStep.SEND_SEED.ordinal(), rpc.getParty(2), Collections.singletonList(seed));
        } else {
            seed = receive(PtoStep.SEND_SEED.ordinal(), rpc.getParty(0)).get(0);
        }
        int parallelNum = parallel ? ForkJoinPool.getCommonPoolParallelism() : 1;
        prp = IntStream.range(0, parallelNum).mapToObj(j -> {
            Prp tmpPrp = PrpFactory.createInstance(getEnvType());
            tmpPrp.setKey(seed);
            return tmpPrp;
        }).toArray(Prp[]::new);
        randomFrom = 0;
        permuteParty.init();
        initState();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public long[] setUsage(PgSortFnParam... params) {
        if (!isMalicious) {
            return new long[]{0, 0};
        }
        long bitTupleNum = 0, longTupleNum = 0;
        for (PgSortFnParam funParam : params) {
            int dataNum = CommonUtils.getByteLength(funParam.dataNum) << 3;
            int sumBitNum = funParam.op.name().endsWith("_A") ? Arrays.stream(funParam.dims).sum() : funParam.dims[0];
            // it is the estimated number of tuples in the sorting process
            int logM = LongUtils.ceilLog2(dataNum);
            long compareNum = 8L * logM * dataNum;
            bitTupleNum += compareNum * (sumBitNum + logM + ComparatorFactory.getAndGateNum(comparatorType, sumBitNum + logM));
            // permute operation
            permuteParty.setUsage(new PermuteFnParam(PermuteOp.APPLY_INV_B_B, dataNum, logM, logM));
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
    public TripletLongVector perGen4MultiDim(TripletLongVector[] input, int[] bitLens) throws MpcAbortException {
        if(input[0].getNum() == 1){
            return (TripletLongVector) zl64cParty.setPublicValue(LongVector.createZeros(1));
        }
        int totalBitNum = Arrays.stream(bitLens).sum();
        TripletZ2Vector[] saveSortRes = new TripletZ2Vector[totalBitNum];
        return perGen4MultiDimWithOrigin(input, bitLens, saveSortRes);
    }

    @Override
    public TripletLongVector perGen4MultiDimWithOrigin(TripletLongVector[] input, int[] bitLens, TripletZ2Vector[] saveSortRes) throws MpcAbortException {
        checkInput(input, bitLens, saveSortRes);
        if(input[0].getNum() == 1){
            for (int i = 0, start = 0; i < input.length; i++) {
                System.arraycopy(abb3Party.getConvParty().a2b(input[i], bitLens[i]), 0, saveSortRes, start, bitLens[i]);
                start += bitLens[i];
            }
            return (TripletLongVector) zl64cParty.setPublicValue(LongVector.createZeros(1));
        }
        logPhaseInfo(PtoState.PTO_BEGIN, "perGen4MultiDimWithOrigin");

        stopWatch.start();
        for (int i = 0, start = 0; i < input.length; i++) {
            System.arraycopy(abb3Party.getConvParty().a2b(input[i], bitLens[i]), 0, saveSortRes, start, bitLens[i]);
            start += bitLens[i];
        }
        logStepInfo(PtoState.PTO_STEP, 1, 3, resetAndGetTime(), "a2b time");

        stopWatch.start();
        TripletZ2Vector[] res = sortAll(saveSortRes, null, true);
        TripletZ2Vector[] invPai = getPerWithIndex(Arrays.copyOfRange(res, saveSortRes.length, res.length));
        System.arraycopy(res, 0, saveSortRes, 0, saveSortRes.length);
        logStepInfo(PtoState.PTO_STEP, 2, 3, resetAndGetTime(), "sort time");

        stopWatch.start();
        TripletLongVector resPerm = abb3Party.getConvParty().b2a(invPai);
        logStepInfo(PtoState.PTO_STEP, 3, 3, resetAndGetTime(), "b2a time");

        logPhaseInfo(PtoState.PTO_END, "perGen4MultiDimWithOrigin");
        return resPerm;
    }

    @Override
    public TripletZ2Vector[] perGen(TripletZ2Vector[] input) throws MpcAbortException {
        if(input[0].bitNum() == 1){
            return (TripletZ2Vector[]) z2cParty.setPublicValues(new BitVector[]{BitVectorFactory.createZeros(1)});
        }
        logPhaseInfo(PtoState.PTO_BEGIN, "perGen");

        stopWatch.start();
        TripletZ2Vector[] res = sortAll(input, null, true);
        stopWatch.stop();
        long sortTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, sortTime, "sort time");

        stopWatch.start();
        res = getPerWithIndex(Arrays.copyOfRange(res, input.length, res.length));
        stopWatch.stop();
        long permTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, permTime, "permute time");

        logPhaseInfo(PtoState.PTO_END, "perGen");
        return res;
    }

    @Override
    public TripletZ2Vector[] perGenAndSortOrigin(TripletZ2Vector[] input) throws MpcAbortException {
        if(input[0].bitNum() == 1){
            return (TripletZ2Vector[]) z2cParty.setPublicValues(new BitVector[]{BitVectorFactory.createZeros(1)});
        }
        logPhaseInfo(PtoState.PTO_BEGIN, "perGenAndSortOrigin");

        stopWatch.start();
        TripletZ2Vector[] res = sortAll(input, null, true);
        stopWatch.stop();
        long sortTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, sortTime, "sort time");

        stopWatch.start();
        TripletZ2Vector[] invPai = getPerWithIndex(Arrays.copyOfRange(res, input.length, res.length));
        System.arraycopy(res, 0, input, 0, input.length);
        stopWatch.stop();
        long invPermTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, invPermTime, "inverse permute time");

        logPhaseInfo(PtoState.PTO_END, "perGenAndSortOrigin");
        return invPai;
    }

    /**
     * return the data whose index after sorting is in [from, to]
     *
     * @param wires       input data
     * @param range       [from, to]
     * @param stillSorted the result data is sorted or not
     */
    public TripletZ2Vector[] getPart(TripletZ2Vector[] wires, int[] range, boolean stillSorted) throws MpcAbortException {
        MathPreconditions.checkEqual("range.length", "2", range.length, 2);
        MathPreconditions.checkNonNegativeInRange("range[0]", range[0], wires[0].bitNum());
        MathPreconditions.checkInRange("range[1]", range[1], range[0], wires[0].bitNum());
        TripletZ2Vector[] res = this.sortAll(wires, range, stillSorted);
        int validBitLen = range[1] - range[0];
        return IntStream.range(0, wires.length).mapToObj(i -> {
            TripletZ2Vector tmp = res[i].reduceShiftRight(wires[0].bitNum() - range[1]);
            tmp.reduce(validBitLen);
            return tmp;
        }).toArray(TripletZ2Vector[]::new);
    }

    private TripletZ2Vector[] sortAll(TripletZ2Vector[] keys, int[] targetRange, boolean stillSorted) throws MpcAbortException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        TripletZ2Vector[] index = (TripletZ2Vector[]) z2cParty.setPublicValues(Z2VectorUtils.getBinaryIndex(keys[0].getNum()));
        TripletZ2Vector[] sortInput = new TripletZ2Vector[keys.length + index.length];
        System.arraycopy(keys, 0, sortInput, 0, keys.length);
        System.arraycopy(index, 0, sortInput, keys.length, index.length);

        sortInput = (TripletZ2Vector[]) shuffleParty.shuffleColumn(sortInput);

        TripletZ2Vector[] transInput = z2cParty.matrixTranspose(sortInput);

        List<int[]> currentRanges = new LinkedList<>(), smallsets = new LinkedList<>();
        if (transInput.length <= SMALL_THRESHOLD) {
            smallsets.add(IntStream.range(0, transInput.length).toArray());
        } else {
            currentRanges.add(new int[]{0, transInput.length - 1});
            while (!currentRanges.isEmpty()) {
                List<int[]> map = new LinkedList<>();
                TIntList extendLen = new TIntLinkedList();
                List<int[]> subsets = getPivotPos(transInput, currentRanges, smallsets, map, extendLen);
                List<int[]>[] res = permuteInput(transInput, currentRanges, subsets, map, extendLen);
                currentRanges = res[0];
                smallsets = res[1];
                if (targetRange != null) {
                    currentRanges.removeIf(range -> range[0] >= targetRange[1] || range[1] < targetRange[0]);
                    smallsets.removeIf(range -> range[0] >= targetRange[1] || range[range.length - 1] < targetRange[0]);
                    if (!stillSorted) {
                        currentRanges.removeIf(range -> range[0] >= targetRange[0] && range[1] < targetRange[1]);
                        smallsets.removeIf(range -> range[0] >= targetRange[0] || range[range.length - 1] < targetRange[1]);
                    }
                }
                if (rpc.ownParty().getPartyId() == 0) {
                    LOGGER.info("current there are {} big group， {} small group to be processed", currentRanges.size(), smallsets.size());
                }
            }
        }

        this.dealLastSmallSets(transInput, smallsets);
        TripletZ2Vector[] allTransBack = z2cParty.matrixTranspose(transInput);
        this.totalTimer += stopWatch.getTime(TimeUnit.MILLISECONDS);
        if (rpc.ownParty().getPartyId() == 0) {
            printTimer();
        }
        return allTransBack;
    }

    private TripletZ2Vector[] getPerWithIndex(TripletZ2Vector[] index) throws MpcAbortException {
        TripletZ2Vector[] rho = (TripletZ2Vector[]) z2cParty.setPublicValues(Z2VectorUtils.getBinaryIndex(index[0].getNum()));
        return permuteParty.applyInvPermutation(index, rho);
    }

    /**
     * process each subset
     *
     * @param input   data to be sorted
     * @param ranges  for each range in ranges, the data from range[0] to range[1] are in the same group need to be sorted
     * @param subsets the index pairs of data should be compared
     * @param map     the map generated from comparison with pivot
     * @return the ranges of big groups for the next round, and the small groups
     */
    private List<int[]>[] permuteInput(TripletZ2Vector[] input, List<int[]> ranges, List<int[]> subsets,
                                       List<int[]> map, TIntList extendLen) throws MpcAbortException {
        List<int[]> nextCompRanges = new LinkedList<>(), smallRanges = new LinkedList<>();
        // 1. 得到比较对象，并且将比较的结果公开出来
        TripletZ2Vector[][] compInput = getCompInputs4subset(input, subsets);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        TripletZ2Vector compRes = (TripletZ2Vector) z2IntegerCircuit.leq(compInput[0], compInput[1]);
        z2cParty.noti(compRes);
        this.compareTimer += stopWatch.getTime(TimeUnit.MILLISECONDS);
        boolean[] plainRes = BinaryUtils.byteArrayToBinary(z2cParty.open(new TripletZ2Vector[]{compRes})[0].getBytes(), compRes.getNum());

        stopWatch.reset();
        stopWatch.start();
        for (int i = 0, compIndex = 0; i < ranges.size(); i++) {
            // 2. 处理结果，左移或者右移
            int partStart = ranges.get(i)[0], partEnd = ranges.get(i)[1];
            int[] currentSubset = subsets.get(i);
            for (int setIndex = 1; setIndex < currentSubset.length; setIndex++) {
                if (plainRes[compIndex++]) {
                    map.add(new int[]{partEnd--, currentSubset[setIndex]});
                } else {
                    map.add(new int[]{partStart++, currentSubset[setIndex]});
                }
            }
            Preconditions.checkArgument(partEnd == partStart);
            map.add(new int[]{partStart, currentSubset[0]});
            // 3. 形成新的subsets
            int[] oldRange = ranges.get(i);
            oldRange[0] -= extendLen.get(i);
            oldRange[1] += extendLen.get(i);
            int leftRange = partStart - oldRange[0], rightRange = oldRange[1] - partStart;
            if (leftRange > SMALL_THRESHOLD) {
                nextCompRanges.add(new int[]{oldRange[0], partStart - 1});
            } else if (leftRange > 1) {
                smallRanges.add(IntStream.range(oldRange[0], partStart).toArray());
            }
            if (rightRange > SMALL_THRESHOLD) {
                nextCompRanges.add(new int[]{partStart + 1, oldRange[1]});
            } else {
                smallRanges.add(IntStream.range(partStart + 1, oldRange[1] + 1).toArray());
            }
        }
        TripletZ2Vector[] origin = Arrays.copyOf(input, input.length);
        map.stream().parallel().forEach(index -> input[index[0]] = origin[index[1]]);
        this.dealPermuteTimer += stopWatch.getTime(TimeUnit.MILLISECONDS);
        return new List[]{nextCompRanges, smallRanges};
    }

    /**
     * get the pivot for each group and deal with the small group
     *
     * @param input     data to be sorted
     * @param ranges    data from range[0] to range[1] are not sorted
     * @param smallSets the small groups
     * @param map       the map in generating pivot
     * @return 待处理的各个subset
     */
    private List<int[]> getPivotPos(TripletZ2Vector[] input, List<int[]> ranges, List<int[]> smallSets,
                                    List<int[]> map, TIntList extendLen) throws MpcAbortException {
        TripletZ2Vector[] tmp = Arrays.copyOf(input, input.length);
        // 1. get pivots based on the amount of elements
        int[][] pivotPos = getPossiblePivotPos(ranges);
        // 2. run the comparison on pivots and small groups to get the rank
        // we need to choose the median of pivots as the true pivot for each group
        int[][] all = new int[pivotPos.length + smallSets.size()][];
        System.arraycopy(pivotPos, 0, all, 0, pivotPos.length);
        System.arraycopy(smallSets.toArray(new int[0][]), 0, all, pivotPos.length, smallSets.size());
        int[][] ranks = this.getRank(input, all);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 3.1 deal with small group
        IntStream.range(0, smallSets.size()).forEach(i -> {
            int[] range = smallSets.get(i);
            int[] currentRank = ranks[i + ranges.size()];
            IntStream.range(0, range.length).forEach(j -> input[range[j]] = tmp[currentRank[j]]);
        });
        // 3.2 deal with pivots to decide the real pivot for each group
        List<int[]> res = new LinkedList<>();
        IntStream.range(0, ranges.size()).forEach(i -> {
            int[] range = ranges.get(i), currentRank = ranks[i];
            int[] fullRange = IntStream.range(range[0], range[1] + 1).toArray();
            int sepIndex = currentRank.length >> 1;
            extendLen.add(sepIndex);
            for (int j = 0; j < sepIndex; j++) {
                map.add(new int[]{range[0]++, currentRank[j]});
                map.add(new int[]{range[1]--, currentRank[currentRank.length - 1 - j]});
            }
            TIntHashSet currentRankSet = new TIntHashSet();
            currentRankSet.addAll(currentRank);
            int[] lastIndexes = new int[fullRange.length - currentRank.length + 1];
            lastIndexes[0] = currentRank[sepIndex];
            for (int origin = 0, current = 1; origin < fullRange.length; origin++) {
                if (!currentRankSet.contains(fullRange[origin])) {
                    lastIndexes[current++] = fullRange[origin];
                }
            }
            res.add(lastIndexes);
        });
        this.dealPivotTimer += stopWatch.getTime(TimeUnit.MILLISECONDS);
        return res;
    }

    /**
     * get the index of possible pivots for each big group
     *
     * @param ranges data from range[0] to range[1] are not sorted
     */
    private int[][] getPossiblePivotPos(List<int[]> ranges) {
        int[][] pivotPos = new int[ranges.size()][];
        int[] size = new int[pivotPos.length], rangeLen = new int[pivotPos.length], startIndex = new int[pivotPos.length];
        rangeLen[0] = ranges.get(0)[1] - ranges.get(0)[0] + 1;
        size[0] = choosePivotFromMany(rangeLen[0]);
        for (int i = 1; i < pivotPos.length; i++) {
            int[] range = ranges.get(i);
            rangeLen[i] = range[1] - range[0] + 1;
            size[i] = choosePivotFromMany(rangeLen[i]);
            startIndex[i] = startIndex[i - 1] + size[i - 1];
        }
        int byteNum = (startIndex[startIndex.length - 1] + size[size.length - 1]) << 2;
        byte[] tmpRandom = PrpUtils.generateRandBytes(prp, randomFrom, byteNum);
        int[] randoms = IntUtils.byteArrayToIntArray(tmpRandom);
        randomFrom += randoms.length;

        IntStream intStream = parallel ? IntStream.range(0, pivotPos.length).parallel() : IntStream.range(0, pivotPos.length);
        intStream.forEach(index -> {
            int[] range = ranges.get(index);
            TIntList c = new TIntLinkedList();
            for (int i = startIndex[index], len = 0; len < size[index]; len++, i++) {
                int tmp = Math.floorMod(randoms[i], rangeLen[index]);
                while (c.contains(tmp)) {
                    tmp = (tmp + 1) % rangeLen[index];
                }
                c.add(tmp);
            }
            pivotPos[index] = c.toArray();
            IntStream.range(0, pivotPos[index].length).forEach(i -> pivotPos[index][i] += range[0]);
        });
        return pivotPos;
    }

    /**
     * get the number of possible pivots based on the number of elements in each group
     */
    private static int choosePivotFromMany(int rangeLen) {
        int shouldNum = LongUtils.ceilLog2(rangeLen);
        for (int j = PIVOT_NUM.length - 1; j >= 0; j--) {
            if (shouldNum >= PIVOT_NUM[j]) {
                return PIVOT_NUM[j];
            }
        }
        return 1;
    }

    /**
     * deal with the small groups in the last round
     *
     * @param input     input data
     * @param smallSets indexes of small groups
     */
    private void dealLastSmallSets(TripletZ2Vector[] input, List<int[]> smallSets) throws MpcAbortException {
        if (!smallSets.isEmpty()) {
            int[][] ranks = this.getRank(input, smallSets.toArray(new int[0][]));
            TripletZ2Vector[] tmp = Arrays.copyOf(input, input.length);
            IntStream.range(0, ranks.length).forEach(i -> {
                int[] range = smallSets.get(i);
                IntStream.range(0, range.length).forEach(j -> input[range[j]] = tmp[ranks[i][j]]);
            });
        }
    }

    /**
     * get the rank for each small group
     *
     * @param input     input data
     * @param smallSets the indexes of elements in each small group
     */
    private int[][] getRank(TripletZ2Vector[] input, int[][] smallSets) throws MpcAbortException {
        // if a small group has N elements，then we need N(N-1)/2 comparison
        MathPreconditions.checkPositive("smallSets.size()", smallSets.length);
        int[] startIndex = new int[smallSets.length], size = new int[smallSets.length];
        size[0] = smallSets[0].length * (smallSets[0].length - 1) / 2;
        for (int i = 1; i < size.length; i++) {
            int[] range = smallSets[i];
            size[i] = range.length * (range.length - 1) / 2;
            startIndex[i] = startIndex[i - 1] + size[i - 1];
        }
        int totalCompareNum = startIndex[startIndex.length - 1] + size[size.length - 1];
        if (totalCompareNum == 0) {
            throw new MpcAbortException("the comparison is empty");
        }
        int[][] indexArray = new int[totalCompareNum][];
        IntStream intStream = parallel ? IntStream.range(0, size.length).parallel() : IntStream.range(0, size.length);
        intStream.forEach(index -> {
            int startPos = startIndex[index];
            int[] nums = smallSets[index];
            for (int i = 0; i < nums.length - 1; i++) {
                for (int j = i + 1; j < nums.length; j++) {
                    indexArray[startPos++] = new int[]{nums[i], nums[j]};
                }
            }
        });

        TripletZ2Vector[][] compInput = getInputs4Comp(input, Arrays.stream(indexArray).collect(Collectors.toList()));
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        TripletZ2Vector compRes = (TripletZ2Vector) z2IntegerCircuit.leq(compInput[0], compInput[1]);
        z2cParty.noti(compRes);
        this.compareTimer += stopWatch.getTime(TimeUnit.MILLISECONDS);

        boolean[] plainRes = BinaryUtils.uncheckByteArrayToBinary(z2cParty.open(new TripletZ2Vector[]{compRes})[0].getBytes(), compRes.getNum());
        int[][] res = new int[smallSets.length][];

        intStream = parallel ? IntStream.range(0, size.length).parallel() : IntStream.range(0, size.length);
        intStream.forEach(which -> {
            int[] nums = smallSets[which];
            int[] bigCount = new int[nums.length];
            int startPos = startIndex[which];
            for (int i = 0; i < nums.length - 1; i++) {
                for (int j = i + 1; j < nums.length; j++) {
                    if (plainRes[startPos++]) {
                        bigCount[i]++;
                    } else {
                        bigCount[j]++;
                    }
                }
            }
            res[which] = new int[nums.length];
            for (int i = 0; i < bigCount.length; i++) {
                res[which][bigCount[i]] = nums[i];
            }
        });
        return res;
    }

    /**
     * get the input data for comparison
     *
     * @param input  input data
     * @param ranges the indexes of each group
     */
    private TripletZ2Vector[][] getCompInputs4subset(TripletZ2Vector[] input, List<int[]> ranges) {
        List<int[]> indexs = new LinkedList<>();
        for (int[] all : ranges) {
            IntStream.range(1, all.length).forEach(i -> indexs.add(new int[]{all[i], all[0]}));
        }
        return getInputs4Comp(input, indexs);
    }

    /**
     * transform data to get the input for comparison
     *
     * @param input   input data
     * @param indexes the index pairs to be compared
     */
    private TripletZ2Vector[][] getInputs4Comp(TripletZ2Vector[] input, List<int[]> indexes) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        IntStream intStream = parallel ? IntStream.range(0, 2).parallel() : IntStream.range(0, 2);
        TripletZ2Vector[][] transInput = intStream.mapToObj(i ->
            z2cParty.matrixTranspose(indexes.stream().map(index -> input[index[i]]).toArray(TripletZ2Vector[]::new))
        ).toArray(TripletZ2Vector[][]::new);
        this.transTimer += stopWatch.getTime(TimeUnit.MILLISECONDS);
        return transInput;
    }

    /**
     * reset timer
     */
    public void resetTimer() {
        transTimer = 0L;
        compareTimer = 0L;
        dealPivotTimer = 0L;
        dealPermuteTimer = 0L;
        totalTimer = 0L;
    }

    public void printTimer() {
        LOGGER.info("transTimer:{}, compareTimer:{}, dealPivotTimer:{}, dealPermuteTimer:{}, totalTimer:{}",
            transTimer, compareTimer, dealPivotTimer, dealPermuteTimer, totalTimer);
        transTimer = 0L;
        compareTimer = 0L;
        dealPivotTimer = 0L;
        dealPermuteTimer = 0L;
        totalTimer = 0L;
    }
}

