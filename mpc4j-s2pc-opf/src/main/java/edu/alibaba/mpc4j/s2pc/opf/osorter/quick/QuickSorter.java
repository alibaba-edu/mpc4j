package edu.alibaba.mpc4j.s2pc.opf.osorter.quick;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.opf.osorter.AbstractObSorter;
import edu.alibaba.mpc4j.s2pc.opf.osorter.quick.QuickSorterPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleParty;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Quick sorter.
 *
 * @author Feng Han
 * @date 2024/9/26
 */
public class QuickSorter extends AbstractObSorter {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuickSorter.class);
    /**
     * threshold to determine the small set
     */
    private final int SMALL_THRESHOLD = 7;
    /**
     * how many pivots should we choose
     */
    private static final int[] PIVOT_NUM = new int[]{3, 5, 7};
    /**
     * Z2c party
     */
    private final Z2cParty z2cParty;
    /**
     * Z2 integer circuit.
     */
    private final Z2IntegerCircuit circuit;
    /**
     * shuffle party
     */
    private final ShuffleParty shuffleParty;
    /**
     * Prp
     */
    protected Prp[] prps;
    /**
     * Prg seed
     */
    protected byte[] seed;
    /**
     * index to generate the randomness
     */
    private int randomFrom;
    /**
     * the dim number for comparison
     */
    private int compareDim;
    /**
     * record the sort of shuffled data
     */
    private int[] permAfterShuffle;
    /**
     * record the sort of shuffled data
     */
    private BitVector[] compareInput;
    /**
     * the original index
     */
    private byte[][] originalIndex;
    /**
     * the original payload
     */
    private byte[][] originalPayload;

    private long compareCount;
    private long compareTime;

    public QuickSorter(Rpc ownRpc, Party otherParty, QuickSorterConfig config) {
        super(QuickSorterPtoDesc.getInstance(), ownRpc, otherParty, config);
        shuffleParty = ownRpc.ownParty().getPartyId() == 0
            ? ShuffleFactory.createSender(ownRpc, otherParty, config.getShuffleConfig())
            : ShuffleFactory.createReceiver(ownRpc, otherParty, config.getShuffleConfig());
        z2cParty = ownRpc.ownParty().getPartyId() == 0
            ? Z2cFactory.createSender(ownRpc, otherParty, config.getZ2cConfig())
            : Z2cFactory.createReceiver(ownRpc, otherParty, config.getZ2cConfig());
        circuit = new Z2IntegerCircuit(z2cParty, config.getZ2CircuitConfig());
        addSubPto(shuffleParty);
        addSubPto(z2cParty);
        randomFrom = 0;
    }

    @Override
    public void init() throws MpcAbortException {
        initState();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        shuffleParty.init();
        z2cParty.init();

        seed = BlockUtils.randomBlock(secureRandom);
        sendOtherPartyPayload(PtoStep.SHARE_SEED.ordinal(), Collections.singletonList(seed));
        byte[] otherSeed = receiveOtherPartyPayload(PtoStep.SHARE_SEED.ordinal()).get(0);
        BlockUtils.xori(seed, otherSeed);
        prps = IntStream.range(0, parallel ? ForkJoinPool.getCommonPoolParallelism() : 1)
            .mapToObj(i -> {
                Prp prp = PrpFactory.createInstance(envType);
                prp.setKey(seed);
                return prp;
            })
            .toArray(Prp[]::new);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector[] unSignSort(SquareZ2Vector[] xiArray, boolean needPermutation, boolean needStable) throws MpcAbortException {
        setPtoInput(xiArray, needPermutation, needStable);
        sort();
        System.arraycopy(data, 0, xiArray, 0, xiArray.length);
        return resultPermutation;
    }

    @Override
    public SquareZ2Vector[] unSignSort(SquareZ2Vector[] xiArray, SquareZ2Vector[] payloads, boolean needPermutation, boolean needStable) throws MpcAbortException {
        setPtoInputWithPayload(xiArray, payloads, needPermutation, needStable);
        sort();
        System.arraycopy(data, 0, xiArray, 0, xiArray.length);
        System.arraycopy(flatPayload, 0, payloads, 0, payloads.length);
        return resultPermutation;
    }

    private void sort() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        processing();
        stopWatch.stop();
        long shuffleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, shuffleTime);

        stopWatch.start();
        sortAll(null, true);
        stopWatch.stop();
        long sortTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, sortTime);

        postProcessing();
        logPhaseInfo(PtoState.PTO_END);
    }

    private void processing() throws MpcAbortException {
        if (needPermutation || needStable) {
            MpcZ2Vector[] indexes = z2cParty.setPublicValues(Z2VectorUtils.getBinaryIndex(inputNum));
            SquareZ2Vector[] all = new SquareZ2Vector[inputDim + indexes.length + (flatPayload != null ? flatPayload.length : 0)];
            System.arraycopy(data, 0, all, 0, data.length);
            IntStream.range(0, indexes.length).forEach(i -> all[i + inputDim] = (SquareZ2Vector) indexes[i]);
            if(flatPayload != null) {
                System.arraycopy(flatPayload, 0, all, inputDim + indexes.length, flatPayload.length);
            }
            data = all;
        }else if (flatPayload != null) {
            SquareZ2Vector[] all = new SquareZ2Vector[inputDim + flatPayload.length];
            System.arraycopy(data, 0, all, 0, data.length);
            System.arraycopy(flatPayload, 0, all, inputDim, flatPayload.length);
            data = all;
        }
        data = shuffleParty.shuffle(data, inputNum, data.length);
        // change payload into row form
        if(flatPayload != null){
            originalPayload = ZlDatabase.create(envType, parallel,
                    Arrays.stream(Arrays.copyOfRange(data, data.length - flatPayload.length, data.length)).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new))
                .getBytesData();
            data = Arrays.copyOf(data, data.length - flatPayload.length);
        }
        permAfterShuffle = IntStream.range(0, inputNum).toArray();
        // matrix transpose
        if (needStable) {
            compareDim = data.length;
            compareInput = Arrays.stream(ZlDatabase.create(envType, parallel,
                        Arrays.stream(data).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new))
                    .getBytesData())
                .map(ea -> BitVectorFactory.create(compareDim, ea))
                .toArray(BitVector[]::new);
            originalIndex = null;
        } else {
            compareDim = inputDim;
            compareInput = Arrays.stream(ZlDatabase.create(envType, parallel,
                        Arrays.stream(Arrays.copyOf(data, compareDim)).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new))
                    .getBytesData())
                .map(ea -> BitVectorFactory.create(compareDim, ea))
                .toArray(BitVector[]::new);
            if (needPermutation) {
                originalIndex = ZlDatabase.create(envType, parallel,
                        Arrays.stream(Arrays.copyOfRange(data, compareDim, data.length)).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new))
                    .getBytesData();
            } else {
                originalIndex = null;
            }
        }
        compareCount = 0;
        compareTime = 0;
    }

    private void postProcessing() {
        data = Arrays.stream(ZlDatabase.create(envType, parallel, compareInput).getBytesData())
            .map(ea -> SquareZ2Vector.create(compareInput.length, ea, false))
            .toArray(SquareZ2Vector[]::new);
        if (needPermutation) {
            if (needStable) {
                // 需要稳定排序，需要置换
                resultPermutation = Arrays.copyOfRange(data, inputDim, data.length);
                data = Arrays.copyOf(data, inputDim);
            } else {
                // 需要置换，不需要稳定排序
                originalIndex = PermutationNetworkUtils.permutation(permAfterShuffle, originalIndex);
                ZlDatabase zlDatabase = ZlDatabase.create(LongUtils.ceilLog2(compareInput.length), originalIndex);
                resultPermutation = Arrays.stream(zlDatabase.bitPartition(envType, parallel))
                    .map(ea -> SquareZ2Vector.create(ea, false))
                    .toArray(SquareZ2Vector[]::new);
            }
        } else {
            resultPermutation = null;
            if (needStable) {
                // 需要稳定排序，不需要置换
                data = Arrays.copyOf(data, inputDim);
            }
        }
        if(flatPayload != null){
            originalPayload = PermutationNetworkUtils.permutation(permAfterShuffle, originalPayload);
            ZlDatabase tmp = ZlDatabase.create(flatPayload.length, originalPayload);
            flatPayload = Arrays.stream(tmp.bitPartition(envType, parallel)).map(ea -> SquareZ2Vector.create(ea, false)).toArray(SquareZ2Vector[]::new);
        }
        LOGGER.info("compare time: {} ms, compare count:{}", compareTime, compareCount);
    }

    private void sortAll(int[] targetRange, boolean stillSorted) throws MpcAbortException {
        List<int[]> currentRanges = new LinkedList<>(), smallsets = new LinkedList<>();
        if (inputNum < SMALL_THRESHOLD) {
            smallsets.add(IntStream.range(0, inputNum).toArray());
        } else {
            currentRanges.add(new int[]{0, inputNum - 1});
        }
        while (!currentRanges.isEmpty()) {
            int[][] map = new int[currentRanges.size()][];
            List<int[]> subsets = getPivotPos(currentRanges, smallsets, map);
            List<int[]>[] res = this.permuteInput( currentRanges, subsets, map);
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
                LOGGER.info("现在还有{}个大分组， {}个小分组待处理", currentRanges.size(), smallsets.size());
            }
        }
        this.dealLastSmallSets(smallsets);
    }

    /**
     * quickSort的每一个subset的处理
     *
     * @param ranges      将要把数据排到 range[0] to range[1] 的范围内
     * @param subsets     要把哪些index的数据放入到 range[0] to range[1] 的范围内，其中第一个index是pivot
     * @param prePivotPos 从得到 pivot 时的map
     * @return 下一轮排序的 ranges， 有哪些smallSet可以直接处理
     */
    private List<int[]>[] permuteInput(List<int[]> ranges, List<int[]> subsets, int[][] prePivotPos) throws MpcAbortException {
        // 1. 得到比较对象，并且将比较的结果公开出来
        SquareZ2Vector[][] compInput = getCompInputs4subset(compareInput, subsets);

        StopWatch stopWatch1 = new StopWatch();
        stopWatch1.start();

        compareCount += compInput[0][0].bitNum();
        SquareZ2Vector compRes = (SquareZ2Vector) circuit.leq(compInput[0], compInput[1]);

        stopWatch1.stop();
        compareTime += stopWatch1.getTime(TimeUnit.MILLISECONDS);

        z2cParty.noti(compRes);
        boolean[] plainRes = BinaryUtils.byteArrayToBinary(z2cParty.open(new SquareZ2Vector[]{compRes})[0].getBytes(), compRes.getNum());

        int[] countIndex = new int[subsets.size()];
        countIndex[0] = 0;
        IntStream.range(1, subsets.size()).forEach(i -> countIndex[i] = countIndex[i - 1] + subsets.get(i - 1).length - 1);

        IntStream intStream = parallel ? IntStream.range(0, ranges.size()).parallel() : IntStream.range(0, ranges.size());
        int[][] nextCompRanges = new int[ranges.size() * 2][];
        int[][] smallRanges = new int[ranges.size() * 2][];

        intStream.forEach(i -> {
            // 1. get map
            int partStart = ranges.get(i)[0], partEnd = ranges.get(i)[1];
            int[] currentSubset = subsets.get(i);
            int num4Compare = currentSubset.length;
            int predeterminedNum = prePivotPos[i].length - num4Compare;
            int tmpStart = predeterminedNum / 2, tmpEnd = prePivotPos[i].length - 1 - predeterminedNum / 2;

            for (int setIndex = 0; setIndex < currentSubset.length - 1; setIndex++) {
                if (plainRes[countIndex[i] + setIndex]) {
                    prePivotPos[i][tmpEnd--] = currentSubset[setIndex + 1];
                } else {
                    prePivotPos[i][tmpStart++] = currentSubset[setIndex + 1];
                }
            }
            Preconditions.checkArgument(tmpEnd == tmpStart);
            prePivotPos[i][tmpEnd] = currentSubset[0];
            // 2. switch data
            switchDataAndIndex(compareInput, prePivotPos[i], IntStream.range(partStart, partEnd + 1).toArray());
            // 3. 形成新的subsets
            if (tmpEnd > SMALL_THRESHOLD) {
                nextCompRanges[2 * i] = new int[]{partStart, partStart + tmpEnd - 1};
            } else {
                smallRanges[2 * i] = IntStream.range(partStart, partStart + tmpEnd).toArray();
            }
            if (prePivotPos[i].length - tmpEnd > SMALL_THRESHOLD) {
                nextCompRanges[2 * i + 1] = new int[]{partStart + tmpEnd + 1, partEnd};
            } else {
                smallRanges[2 * i + 1] = IntStream.range(partStart + tmpEnd + 1, partEnd + 1).toArray();
            }
        });

        return new List[]{
            Arrays.stream(nextCompRanges).filter(Objects::nonNull).toList(),
            Arrays.stream(smallRanges).filter(Objects::nonNull).toList()};
    }

    /**
     * quickSort中为每一个subset得到pivot信息，并且处理smallSets
     *
     * @param ranges       from range[0] to range[1] 的数据还没有排好序
     * @param smallSets    哪些小集合是可以直接处理的
     * @param map          得到 pivot 时的map
     * @return 待处理的各个subset
     */
    private List<int[]> getPivotPos(List<int[]> ranges, List<int[]> smallSets, int[][] map) throws MpcAbortException {
        assert map.length == ranges.size();
        // 1. 根据数据量选出几个需要比较出中位数的点
        List<int[]> pivotPos = getPossiblePivotPos(ranges);
        // 2. 将这些点和smallRanges中的index组一起进行比较，得到对应的rank
        List<int[]> all = new LinkedList<>();
        all.addAll(pivotPos);
        all.addAll(smallSets);
        int[][] ranks = this.getRankByPairwiseComparison(all);
        // 3.1 处理结果，先处理smallRange的
        IntStream intStream = parallel ? IntStream.range(0, smallSets.size()).parallel() : IntStream.range(0, smallSets.size());
        intStream.forEach(i -> switchDataAndIndex(compareInput, ranks[i + ranges.size()], smallSets.get(i)));
        // 3.2 处理pivot的部分
        intStream = parallel ? IntStream.range(0, ranges.size()).parallel() : IntStream.range(0, ranges.size());
        return intStream.mapToObj(i -> {
            int[][] tmpRes = QuickSortUtils.moveIndex(ranges.get(i), ranks[i]);
            map[i] = tmpRes[0];
            return tmpRes[1];
        }).toList();
    }

    /**
     * 根据 ranges 信息随机得到几个可能作为pivot的点
     */
    private List<int[]> getPossiblePivotPos(List<int[]> ranges) {
        int[] rangeLen = new int[ranges.size()];
        int[] pivotNum = new int[ranges.size()];
        int[] sumPivotNum = new int[ranges.size()];
        sumPivotNum[0] = 0;
        IntStream intStream = parallel ? IntStream.range(0, ranges.size()).parallel() : IntStream.range(0, ranges.size());
        intStream.forEach(i -> {
            int[] range = ranges.get(i);
            rangeLen[i] = range[1] - range[0] + 1;
            pivotNum[i] = choosePivotFromMany(rangeLen[i]);
        });
        IntStream.range(0, ranges.size() - 1).forEach(i -> sumPivotNum[i + 1] = sumPivotNum[i] + pivotNum[i]);
        int allNum = sumPivotNum[ranges.size() - 1] + pivotNum[ranges.size() - 1];
        byte[] tmpRandom = PrpUtils.generateRandBytes(prps, randomFrom, allNum << 2);
        randomFrom += allNum;
        int[] randIndex = IntUtils.byteArrayToIntArray(tmpRandom);

        intStream = parallel ? IntStream.range(0, ranges.size()).parallel() : IntStream.range(0, ranges.size());
        return intStream.mapToObj(index -> {
            int startPos = ranges.get(index)[0];
            HashSet<Integer> set = new HashSet<>();
            int[] randoms = Arrays.copyOfRange(randIndex, sumPivotNum[index], sumPivotNum[index] + pivotNum[index]);
            for (int i = 0; i < randoms.length; i++) {
                randoms[i] = Math.floorMod(randoms[i], rangeLen[index]);
                while (set.contains(randoms[i])) {
                    randoms[i] = (randoms[i] + 1) % rangeLen[index];
                }
                set.add(randoms[i]);
                randoms[i] += startPos;
            }
            return randoms;
        }).toList();
    }

    /**
     * 如果待排序区长度为 rangeLen，那么应该找多少个潜在的pivot
     */
    private static int choosePivotFromMany(int rangeLen) {
        int shouldNum = LongUtils.ceilLog2(rangeLen);
        // 因为如果range范围 <=7 的时候会直接用smallRange解决，所以至少选出 3 个
        for (int j = PIVOT_NUM.length - 1; j >= 0; j--) {
            if (shouldNum >= PIVOT_NUM[j]) {
                return PIVOT_NUM[j];
            }
        }
        return 1;
    }

    /**
     * 处理最后一组smallSets
     */
    private void dealLastSmallSets(List<int[]> smallSets) throws MpcAbortException {
        if (!smallSets.isEmpty()) {
            int[][] ranks = this.getRankByPairwiseComparison(smallSets);
            IntStream intStream = parallel ? IntStream.range(0, ranks.length).parallel() : IntStream.range(0, ranks.length);
            intStream.forEach(i -> switchDataAndIndex(compareInput, ranks[i], smallSets.get(i)));
        }
    }

    private void switchDataAndIndex(BitVector[] input, int[] replaceIndexes, int[] sourceIndexes) {
        // switch data
        BitVector[] origin = Arrays.stream(replaceIndexes).mapToObj(index -> input[index]).toArray(BitVector[]::new);
        IntStream.range(0, sourceIndexes.length).forEach(j -> input[sourceIndexes[j]] = origin[j]);
        // switch index
        int[] originIndex = Arrays.stream(replaceIndexes).map(index -> permAfterShuffle[index]).toArray();
        IntStream.range(0, sourceIndexes.length).forEach(j -> permAfterShuffle[sourceIndexes[j]] = originIndex[j]);
    }

    /**
     * 根据小范围内的index，得到这几个index的排序信息
     *
     * @param smallSets 需要得知排序的index组
     */
    private int[][] getRankByPairwiseComparison(List<int[]> smallSets) throws MpcAbortException {
        // 每一组smallRanges中包含 N 个数据，则需要有 N(N-1)/2 次比较
        Stream<int[]> stream = parallel ? smallSets.stream().parallel() : smallSets.stream();
        List<int[]> indexes = stream.map(nums -> {
                List<int[]> tmp = new LinkedList<>();
                for (int i = 0; i < nums.length - 1; i++) {
                    for (int j = i + 1; j < nums.length; j++) {
                        tmp.add(new int[]{nums[i], nums[j]});
                    }
                }
                return tmp;
            })
            .flatMap(List::stream).toList();
        SquareZ2Vector[][] compInput = getInputs4Comp(indexes);


        StopWatch stopWatch1 = new StopWatch();
        stopWatch1.start();

        compareCount += compInput[0][0].bitNum();
        SquareZ2Vector compRes = (SquareZ2Vector) circuit.leq(compInput[0], compInput[1]);

        stopWatch1.stop();
        compareTime += stopWatch1.getTime(TimeUnit.MILLISECONDS);

        z2cParty.noti(compRes);

        boolean[] plainRes = BinaryUtils.uncheckByteArrayToBinary(z2cParty.open(new SquareZ2Vector[]{compRes})[0].getBytes(), compRes.getNum());
        int[][] res = new int[smallSets.size()][];
        int[] startIndex = new int[smallSets.size()];
        startIndex[0] = 0;
        for (int i = 1; i < smallSets.size(); i++) {
            int num = smallSets.get(i - 1).length;
            startIndex[i] = startIndex[i - 1] + num * (num - 1) / 2;
        }
        IntStream intStream = parallel ? IntStream.range(0, smallSets.size()).parallel() : IntStream.range(0, smallSets.size());
        intStream.forEach(which -> {
            int currentIndex = startIndex[which];
            int[] nums = smallSets.get(which);
            int[] bigCount = new int[nums.length];
            for (int i = 0; i < nums.length - 1; i++) {
                for (int j = i + 1; j < nums.length; j++) {
                    bigCount[plainRes[currentIndex++] ? i : j]++;
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
     * 根据小范围内的index，得到这几个index的排序信息
     *
     * @param input  输入的数据
     * @param ranges 需要排序的一组组数据对应的index
     */
    private SquareZ2Vector[][] getCompInputs4subset(BitVector[] input, List<int[]> ranges) {
        Stream<int[]> stream = parallel ? ranges.stream().parallel() : ranges.stream();
        List<int[]> indexes = stream
            .map(all -> IntStream.range(1, all.length).mapToObj(i -> new int[]{all[i], all[0]}).toArray(int[][]::new))
            .flatMap(Arrays::stream)
            .toList();
        return getInputs4Comp(indexes);
    }

    /**
     * 根据小范围内的index，得到这几个index的排序信息
     *
     * @param indexes 需要比较的index对
     */
    private SquareZ2Vector[][] getInputs4Comp(List<int[]> indexes) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        BitVector[][] transInput = IntStream.range(0, 2).mapToObj(i ->
            indexes.stream().map(index ->
                compareInput[index[i]]).toArray(BitVector[]::new)).toArray(BitVector[][]::new);
        return Arrays.stream(transInput)
            .map(ea -> {
                ZlDatabase zlDatabase = ZlDatabase.create(envType, parallel, ea);
                return Arrays.stream(zlDatabase.getBytesData()).map(oneByteArray -> SquareZ2Vector.create(ea.length, oneByteArray, false)).toArray(SquareZ2Vector[]::new);
            })
            .toArray(SquareZ2Vector[][]::new);
    }

    private void openAndPrint() throws MpcAbortException {
        LOGGER.info("len of compareInput:{}", compareInput.length);
        BitVector[] res = z2cParty.open(Arrays.stream(compareInput).map(ea -> SquareZ2Vector.create(ea, false)).toArray(SquareZ2Vector[]::new));
        if (getRpc().ownParty().getPartyId() == 0) {
            int shiftLen = res[0].bitNum() - inputDim;
            BigInteger[] sortRes = Arrays.stream(res).map(ea -> ea.getBigInteger().shiftRight(shiftLen)).toArray(BigInteger[]::new);
            LOGGER.info("currentSortRes: {}", Arrays.toString(sortRes));
        }
    }

}
