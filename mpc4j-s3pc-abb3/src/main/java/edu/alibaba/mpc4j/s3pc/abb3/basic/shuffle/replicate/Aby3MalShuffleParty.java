package edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.replicate;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleOperations.ShuffleOp;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.replicate.Aby3ShufflePtoDesc.PtoStep;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.ShuffleUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.mac.Cgh18RpLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.MatrixUtils;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongMacVector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * The malicious version of Replicated-sharing shuffling party
 *
 * @author Feng Han
 * @date 2024/01/18
 */
public class Aby3MalShuffleParty extends AbstractAby3ShuffleParty implements Aby3ShuffleParty {
    private static final Logger LOGGER = LoggerFactory.getLogger(Aby3MalShuffleParty.class);
    private final Cgh18RpLongParty macParty;

    public Aby3MalShuffleParty(TripletZ2cParty z2cParty, TripletLongParty zl64cParty, Cgh18RpLongParty macParty, Aby3ShuffleConfig config) {
        super(z2cParty, zl64cParty, config);
        this.macParty = macParty;
    }

    @Override
    public void init() {
        super.init();
        macParty.init();
    }

    @Override
    public long getTupleNum(ShuffleOp op, int inputDataNum, int outputDataNum, int dataDim) {
        if (inputDataNum == 0) {
            return 0;
        }
        inputDataNum = CommonUtils.getByteLength(inputDataNum) << 3;
        switch (op) {
            case B_SHUFFLE_ROW:
            case B_SHUFFLE_COLUMN:
                return 4L * inputDataNum * getBinaryShuffleParam();
            case B_PERMUTE_NETWORK: {
                int maxNum = Math.max(inputDataNum, outputDataNum);
                return ((long) CommonUtils.getByteLength(maxNum) << 3) * getBinaryShuffleParam() * 2L;
            }
            case B_SWITCH_NETWORK: {
                int maxNum = Math.max(inputDataNum, outputDataNum);
                return getTupleNum(ShuffleOp.B_PERMUTE_NETWORK, maxNum, maxNum, dataDim)
                    + getTupleNum(ShuffleOp.B_PERMUTE_NETWORK, outputDataNum, outputDataNum, dataDim)
                    + 2L * ((long) CommonUtils.getByteLength(outputDataNum) << 3) * dataDim;
            }
            case B_DUPLICATE_NETWORK:
                return 2L * ((long) CommonUtils.getByteLength(outputDataNum) << 3) * dataDim;
            case A_SHUFFLE:
            case A_SHUFFLE_OPEN:
            case A_INV_SHUFFLE:
            case A_PERMUTE_NETWORK:
                return 0;
            case A_SWITCH_NETWORK:
            case A_DUPLICATE_NETWORK: {
                if (macParty.equals(zl64cParty)) {
                    return 0;
                } else {
                    return 2L * outputDataNum * dataDim;
                }
            }
            default:
                throw new IllegalArgumentException("Invalid BcShuffleOp in computing the number of required tuples");
        }
    }

    @Override
    LongVector[] trans3To2Sharing(TripletRpLongVector[] input, Party p0, Party p1, int targetLen) {
        macParty.genMac(input);
        if (rpc.ownParty().equals(p0) || rpc.ownParty().equals(p1)) {
            Party withWho = rpc.ownParty().equals(p0) ? p1 : p0;
            int[] dataNums = new int[2 * input.length];
            Arrays.fill(dataNums, input[0].getNum());

            TripletRpLongMacVector[] data = Arrays.stream(input).map(x -> (TripletRpLongMacVector) x).toArray(TripletRpLongMacVector[]::new);
            LongVector[] randWithWho = crProvider.randZl64Vector(dataNums, withWho);
            IntStream intStream = parallel ? IntStream.range(0, data.length).parallel() : IntStream.range(0, data.length);
            if (withWho.equals(leftParty())) {
                intStream.forEach(i -> {
                    randWithWho[i] = data[i].getVectors()[1].sub(randWithWho[i]);
                    randWithWho[i + data.length] = data[i].getMacVec()[1].sub(randWithWho[i + data.length]);
                });
            } else {
                intStream.forEach(i -> {
                    for (int j = 0; j < 2; j++) {
                        randWithWho[i].addi(data[i].getVectors()[j]);
                        randWithWho[i + data.length].addi(data[i].getMacVec()[j]);
                    }
                });
            }
            if (randWithWho[0].getNum() < targetLen) {
                intStream = parallel ? IntStream.range(0, data.length << 1).parallel() : IntStream.range(0, data.length << 1);
                intStream.forEach(i -> {
                    LongVector zero = LongVector.createZeros(targetLen);
                    zero.setValues(randWithWho[i], 0, 0, randWithWho[i].getNum());
                    randWithWho[i] = zero;
                });
            }
            return randWithWho;
        } else {
            return null;
        }
    }

    @Override
    TripletRpLongMacVector[] trans2To3Sharing(LongVector[] input, Party p0, Party p1, int[] dataNums) {
        int[] extendDataNums = new int[dataNums.length << 1];
        System.arraycopy(dataNums, 0, extendDataNums, 0, dataNums.length);
        System.arraycopy(dataNums, 0, extendDataNums, dataNums.length, dataNums.length);

        if (rpc.ownParty().equals(p0) || rpc.ownParty().equals(p1)) {
            MathPreconditions.checkEqual("input.length", "dataNums.length<<1", input.length, dataNums.length << 1);
            Party withWho = rpc.ownParty().equals(p0) ? p1 : p0;
            Party toWho = withWho.equals(leftParty()) ? rightParty() : leftParty();

            LongVector[] rand = crProvider.randZl64Vector(extendDataNums, toWho);
            LongVector[] subRes = IntStream.range(0, input.length).mapToObj(i -> input[i].sub(rand[i])).toArray(LongVector[]::new);
            sendLongVectors(PtoStep.TWO_SHARE_INTO_THREE_SHARE.ordinal(), withWho, subRes);

            LongVector[] others = receiveLongVectors(PtoStep.TWO_SHARE_INTO_THREE_SHARE.ordinal(), withWho);
            IntStream.range(0, input.length).forEach(i -> subRes[i].addi(others[i]));
            if (withWho.equals(leftParty())) {
                return IntStream.range(0, dataNums.length).mapToObj(i ->
                        TripletRpLongMacVector.create(macParty.getCurrentMacIndex(),
                            new LongVector[]{subRes[i], rand[i]},
                            new LongVector[]{subRes[i + dataNums.length], rand[i + dataNums.length]}))
                    .toArray(TripletRpLongMacVector[]::new);
            } else {
                return IntStream.range(0, dataNums.length).mapToObj(i ->
                        TripletRpLongMacVector.create(macParty.getCurrentMacIndex(),
                            new LongVector[]{rand[i], subRes[i]},
                            new LongVector[]{rand[i + dataNums.length], subRes[i + dataNums.length]}))
                    .toArray(TripletRpLongMacVector[]::new);
            }
        } else {
            LOGGER.info("P{} start generate randomness in trans2To3Sharing", rpc.ownParty().getPartyId());
            LongVector[] rWithLeft = crProvider.randZl64Vector(extendDataNums, leftParty());
            LongVector[] rWithRight = crProvider.randZl64Vector(extendDataNums, rightParty());
            LOGGER.info("P{} finish generating randomness in trans2To3Sharing", rpc.ownParty().getPartyId());
            return IntStream.range(0, dataNums.length).mapToObj(i ->
                    TripletRpLongMacVector.create(macParty.getCurrentMacIndex(),
                        new LongVector[]{rWithLeft[i], rWithRight[i]},
                        new LongVector[]{rWithLeft[i + dataNums.length], rWithRight[i + dataNums.length]}))
                .toArray(TripletRpLongMacVector[]::new);
        }
    }

    private int getBinaryShuffleParam() {
        return 80;
    }

    /**
     * from Secure Graph Analysis at Scale -CCS21
     */
    private void verifyShufflePairColumn(TripletRpZ2Vector[] input, TripletRpZ2Vector[] output) throws MpcAbortException {
        int k = getBinaryShuffleParam();
        int verifyBitNum = input.length;

        // generate the random keys, and open them
        int[] keyNums = IntStream.range(0, k).map(i -> verifyBitNum).toArray();
        TripletRpZ2Vector[] keyShare = crProvider.randRpShareZ2Vector(keyNums);
        BitVector[] openKey = z2cParty.open(keyShare);

        IntStream intStream = parallel ? IntStream.range(0, k).parallel() : IntStream.range(0, k);
        TripletRpZ2Vector[][] andInput = new TripletRpZ2Vector[2][k << 1];
        intStream.forEach(i -> {
            andInput[0][i] = input[verifyBitNum - i - 1];
            andInput[0][i + k] = output[verifyBitNum - i - 1];
            andInput[1][i] = TripletRpZ2Vector.createEmpty(input[0].bitNum());
            andInput[1][i + k] = TripletRpZ2Vector.createEmpty(input[0].bitNum());
            boolean[] keyBits = BinaryUtils.byteArrayToBinary(openKey[i].getBytes(), verifyBitNum);
            IntStream.range(0, keyBits.length).forEach(one -> {
                if (keyBits[one]) {
                    z2cParty.xori(andInput[1][i], input[one]);
                    z2cParty.xori(andInput[1][i + k], output[one]);
                }
            });
        });

        TripletRpZ2Vector[] andRes = (TripletRpZ2Vector[]) z2cParty.and(andInput[0], andInput[1]);
        IntStream.range(0, k).forEach(i -> z2cParty.xori(andRes[i], andRes[i + k]));
        boolean[][] count = new boolean[2][k];
        intStream = parallel ? IntStream.range(0, k).parallel() : IntStream.range(0, k);
        intStream.forEach(i -> {
            count[0][i] = andRes[i].getBitVectors()[0].numOf1IsOdd();
            count[1][i] = andRes[i].getBitVectors()[1].numOf1IsOdd();
        });
        z2cParty.compareView4Zero(TripletRpZ2Vector.create(Arrays.stream(count).map(BinaryUtils::binaryToRoundByteArray).toArray(byte[][]::new), k));
    }

    @Override
    public MpcZ2Vector[] shuffleRow(int[][] pai, MpcZ2Vector[] data) throws MpcAbortException {
        MathPreconditions.checkEqual("pai.length", "2", pai.length, 2);
        MathPreconditions.checkEqual("pai[0].length", "pai[1].length", pai[0].length, pai[1].length);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int originBitNum = data[0].bitNum();
        int k = getBinaryShuffleParam();
        int extendBitLen = (data[0].byteNum() << 3) + (CommonUtils.getByteLength(k) << 3);
        int[] maskBitNum = new int[pai[0].length];
        Arrays.fill(maskBitNum, k);
        TripletRpZ2Vector[] mask = crProvider.randRpShareZ2Vector(maskBitNum);
        IntStream intStream = parallel ? IntStream.range(0, pai[0].length).parallel() : IntStream.range(0, pai[0].length);
        TripletRpZ2Vector[] shuffleInput = intStream.mapToObj(i -> {
            TripletRpZ2Vector tmp = TripletRpZ2Vector.createEmpty(extendBitLen);
            tmp.setBytes(mask[i], 0, 0, mask[i].byteNum());
            tmp.setBytes((TripletRpZ2Vector) data[i], 0, mask[i].byteNum(), data[i].byteNum());
            return tmp;
        }).toArray(TripletRpZ2Vector[]::new);
        logStepInfo(PtoState.PTO_STEP, "shuffleRow", 1, 4, resetAndGetTime(), "append mask");

        stopWatch.start();
        BitVector[] d2p = trans3To2Sharing(shuffleInput, rpc.getParty(0), rpc.getParty(2));
        int[] mu = null;
        if (selfId != 1) {
            d2p = ShuffleUtils.applyPermutation(d2p, pai[selfId >> 1]);
        } else {
            mu = ShuffleUtils.applyPermutation(pai[0], pai[1]);
        }
        int[] allBitNums = new int[data.length];
        Arrays.fill(allBitNums, k + data[0].bitNum());
        TripletRpZ2Vector[] midRes = trans2To3Sharing(d2p, rpc.getParty(0), rpc.getParty(2), allBitNums);
        logStepInfo(PtoState.PTO_STEP, "shuffleRow", 2, 4, resetAndGetTime(), "P0 and P2 permute");

        stopWatch.start();
        d2p = trans3To2Sharing(midRes, rpc.getParty(0), rpc.getParty(1));
        if (selfId == 0) {
            d2p = ShuffleUtils.applyPermutation(d2p, pai[1]);
            sendBitVectors(PtoStep.SHUFFLE_MSG.ordinal(), leftParty(), d2p);
        } else if (selfId == 1) {
            d2p = ShuffleUtils.applyPermutation(d2p, mu);
        } else {
            d2p = receiveBitVectors(PtoStep.SHUFFLE_MSG.ordinal(), rightParty(), allBitNums);
            d2p = ShuffleUtils.applyPermutation(d2p, pai[0]);
        }
        TripletRpZ2Vector[] extendRes = trans2To3Sharing(d2p, rpc.getParty(1), rpc.getParty(2), allBitNums);
        logStepInfo(PtoState.PTO_STEP, "shuffleRow", 3, 4, resetAndGetTime(), "P0-P1, P1-P2 shuffle");

        stopWatch.start();

        TripletRpZ2Vector[] t0 = MatrixUtils.bitPartition(shuffleInput, getEnvType(), parallel);
        TripletRpZ2Vector[] t1 = MatrixUtils.bitPartition(midRes, getEnvType(), parallel);
        TripletRpZ2Vector[] t2 = MatrixUtils.bitPartition(extendRes, getEnvType(), parallel);
        verifyShufflePairColumn(t0, t1);
        verifyShufflePairColumn(t1, t2);

        Arrays.stream(extendRes).forEach(each -> each.reduce(originBitNum));
        logStepInfo(PtoState.PTO_STEP, "shuffleRow", 4, 4, resetAndGetTime(), "verify shuffle row");

        logPhaseInfo(PtoState.PTO_END);
        return extendRes;
    }

    @Override
    public MpcZ2Vector[] shuffleColumn(int[][] pai, MpcZ2Vector... data) throws MpcAbortException {
        MathPreconditions.checkEqual("pai.length", "2", pai.length, 2);
        MathPreconditions.checkEqual("pai[0].length", "pai[1].length", pai[0].length, pai[1].length);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int k = getBinaryShuffleParam();
        int bitLen = pai[0].length;
        int[] randomBitNums = IntStream.range(0, k).map(i -> bitLen).toArray();
        int[] allBitNums = IntStream.range(0, k + data.length).map(i -> bitLen).toArray();
        TripletRpZ2Vector[] randoms = crProvider.randRpShareZ2Vector(randomBitNums);
        TripletRpZ2Vector[] shuffleInput = new TripletRpZ2Vector[data.length + k];
        IntStream.range(0, data.length).forEach(i -> shuffleInput[i] = (TripletRpZ2Vector) data[i]);
        System.arraycopy(randoms, 0, shuffleInput, data.length, randoms.length);
        logStepInfo(PtoState.PTO_STEP, "shuffleColumn", 1, 4, resetAndGetTime(), "append mask");

        stopWatch.start();
        BitVector[] d2p = trans3To2Sharing(shuffleInput, rpc.getParty(0), rpc.getParty(2));
        int[] mu = null;
        if (selfId != 1) {
            d2p = ShuffleUtils.applyPermutationToRows(d2p, pai[selfId >> 1]);
        } else {
            mu = ShuffleUtils.applyPermutation(pai[0], pai[1]);
        }
        TripletRpZ2Vector[] midRes = trans2To3Sharing(d2p, rpc.getParty(0), rpc.getParty(2), allBitNums);
        logStepInfo(PtoState.PTO_STEP, "shuffleColumn", 2, 4, resetAndGetTime(), "P0 and P2 shuffle");

        stopWatch.start();
        d2p = trans3To2SharingTranspose(midRes, bitLen, rpc.getParty(0), rpc.getParty(1));
        if (selfId == 0) {
            d2p = ShuffleUtils.applyPermutation(d2p, pai[1]);
            sendBitVectors(PtoStep.SHUFFLE_MSG.ordinal(), leftParty(), d2p);
        } else if (selfId == 1) {
            d2p = ShuffleUtils.applyPermutation(d2p, mu);
        } else {
            int[] receiveBitNums = new int[pai[0].length];
            Arrays.fill(receiveBitNums, k + data.length);
            d2p = receiveBitVectors(PtoStep.SHUFFLE_MSG.ordinal(), rightParty(), receiveBitNums);
            d2p = ShuffleUtils.applyPermutation(d2p, pai[0]);
        }
        TripletRpZ2Vector[] extendRes = trans2To3SharingTranspose(d2p, rpc.getParty(1), rpc.getParty(2), bitLen, shuffleInput.length);
        logStepInfo(PtoState.PTO_STEP, "shuffleColumn", 3, 4, resetAndGetTime(), "P0-P1, P1-P2 shuffle");

        stopWatch.start();
        verifyShufflePairColumn(shuffleInput, midRes);
        verifyShufflePairColumn(midRes, extendRes);
        logStepInfo(PtoState.PTO_STEP, "shuffleColumn", 4, 4, resetAndGetTime(), "verify shuffle column");

        logPhaseInfo(PtoState.PTO_END);
        return Arrays.copyOf(extendRes, data.length);
    }

    /**
     * the difference between two papers:
     * 1. Efficient Secure Three-Party Sorting with Applications to Data Analysis and Heavy Hitters CCS2022
     * 2. Scape
     * In 1, the mac key is not opened in each verification, while in our implementation, we follow CGH+18, which open mac key in each verification;
     * In 2, there are three verification process, according to 1, they can be merged into one process before reveal or open.
     */
    @Override
    public TripletRpLongMacVector[] shuffle(int[][] pai, MpcLongVector... data) throws MpcAbortException {
        int[] dataNum = Arrays.stream(data).mapToInt(MpcVector::getNum).toArray();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        TripletRpLongVector[] tmp = Arrays.stream(data).map(ea -> (TripletRpLongVector) ea).toArray(TripletRpLongVector[]::new);
        LongVector[] d2p = trans3To2Sharing(tmp, rpc.getParty(0), rpc.getParty(2), pai[0].length);
        int[] mu = null;
        if (selfId != 1) {
            d2p = ShuffleUtils.applyPermutationToRows(d2p, pai[selfId >> 1]);
        } else {
            mu = ShuffleUtils.applyPermutation(pai[0], pai[1]);
        }
        TripletRpLongMacVector[] midRes = trans2To3Sharing(d2p, rpc.getParty(0), rpc.getParty(2), dataNum);
        macParty.intoBuffer(midRes);
        logStepInfo(PtoState.PTO_STEP, "shuffle", 1, 2, resetAndGetTime(), "P0-P2 shuffle");

        stopWatch.start();
        d2p = trans3To2Sharing(midRes, rpc.getParty(0), rpc.getParty(1), pai[0].length);
        if (selfId == 0) {
            d2p = ShuffleUtils.applyPermutationToRows(d2p, pai[1]);
            sendLongVectors(PtoStep.SHUFFLE_MSG.ordinal(), leftParty(), d2p);
        } else if (selfId == 1) {
            d2p = ShuffleUtils.applyPermutationToRows(d2p, mu);
        } else {
            d2p = receiveLongVectors(PtoStep.SHUFFLE_MSG.ordinal(), rightParty());
            d2p = ShuffleUtils.applyPermutationToRows(d2p, pai[0]);
        }
        TripletRpLongMacVector[] finalRes = trans2To3Sharing(d2p, rpc.getParty(1), rpc.getParty(2), dataNum);
        macParty.intoBuffer(finalRes);
        logStepInfo(PtoState.PTO_STEP, "shuffle", 1, 2, resetAndGetTime(), "P0-P1, P1-P2 shuffle");

        logPhaseInfo(PtoState.PTO_END);
        return finalRes;
    }

    @Override
    public LongVector[] shuffleOpen(int[][] pai, MpcLongVector... data) throws MpcAbortException {
        TripletRpLongVector[] res = shuffle(pai, data);
        return zl64cParty.open(res);
    }

    @Override
    public MpcLongVector[] invShuffle(int[][] pai, MpcLongVector... data) throws MpcAbortException {
        int[] dataNum = Arrays.stream(data).mapToInt(MpcVector::getNum).toArray();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        TripletRpLongVector[] tmp = Arrays.stream(data).map(ea -> (TripletRpLongVector) ea).toArray(TripletRpLongVector[]::new);
        LongVector[] d2p = trans3To2Sharing(tmp, rpc.getParty(1), rpc.getParty(2), pai[0].length);
        int[] mu = null;
        if (selfId >= 1) {
            d2p = ShuffleUtils.applyPermutationToRows(d2p, ShuffleUtils.invOfPermutation(pai[selfId & 1]));
        } else {
            mu = ShuffleUtils.invOfPermutation(ShuffleUtils.applyPermutation(pai[0], pai[1]));
        }
        TripletRpLongMacVector[] midRes = trans2To3Sharing(d2p, rpc.getParty(1), rpc.getParty(2), dataNum);
        macParty.intoBuffer(midRes);
        logStepInfo(PtoState.PTO_STEP, "invShuffle", 1, 2, resetAndGetTime(), "P1-P2 invShuffle");

        stopWatch.start();
        d2p = trans3To2Sharing(midRes, rpc.getParty(0), rpc.getParty(1), pai[0].length);
        if (selfId == 0) {
            d2p = ShuffleUtils.applyPermutationToRows(d2p, mu);
        } else if (selfId == 1) {
            d2p = ShuffleUtils.applyPermutationToRows(d2p, ShuffleUtils.invOfPermutation(pai[0]));
            sendLongVectors(PtoStep.SHUFFLE_MSG.ordinal(), rightParty(), d2p);
        } else {
            d2p = receiveLongVectors(PtoStep.SHUFFLE_MSG.ordinal(), leftParty());
            d2p = ShuffleUtils.applyPermutationToRows(d2p, ShuffleUtils.invOfPermutation(pai[1]));
        }
        TripletRpLongMacVector[] finalRes = trans2To3Sharing(d2p, rpc.getParty(0), rpc.getParty(2), dataNum);
        macParty.intoBuffer(finalRes);
        logStepInfo(PtoState.PTO_STEP, "invShuffle", 2, 2, resetAndGetTime(), "P0-P2 P0-P1 invShuffle");

        logPhaseInfo(PtoState.PTO_END);
        return finalRes;
    }

    @Override
    public TripletRpZ2Vector[] switchNetwork(MpcZ2Vector[] input, int[] fun, int targetLen,
                                             Party programmer, Party sender, Party receiver) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        StopWatch stopWatch1 = new StopWatch();
        stopWatch1.start();
        boolean[] flag = new boolean[targetLen];
        int[][] twoPer = new int[2][];
        if (rpc.ownParty().equals(programmer)) {
            Arrays.fill(flag, true);
            twoPer = ShuffleUtils.get2PerForSwitch(fun, input[0].bitNum(), flag);
        }
        TripletRpZ2Vector[] tmp = permuteNetwork(input, twoPer[0], targetLen, programmer, sender, receiver);
        stopWatch1.stop();
        logStepInfo(PtoState.PTO_STEP, "switchNetwork", 1, 3, stopWatch1.getTime(TimeUnit.MILLISECONDS), "first permute");
        stopWatch1.reset();

        stopWatch1.start();
        tmp = duplicateNetwork(tmp, flag, programmer);
        stopWatch1.stop();
        logStepInfo(PtoState.PTO_STEP, "switchNetwork", 2, 3, stopWatch1.getTime(TimeUnit.MILLISECONDS), "duplicateNetwork");
        stopWatch1.reset();

        stopWatch1.start();
        TripletRpZ2Vector[] res = permuteNetwork(tmp, twoPer[1], targetLen, programmer, sender, receiver);
        stopWatch1.stop();
        logStepInfo(PtoState.PTO_STEP, "switchNetwork", 3, 3, stopWatch1.getTime(TimeUnit.MILLISECONDS), "second permute");
        stopWatch1.reset();

        logPhaseInfo(PtoState.PTO_END);
        return res;
    }

    /**
     * 1. programmer commit flag
     * 2. w0 = X - B, w1 = B_{i-1} - B_i
     * 3. choice = (1-f)·w0 + f·w1
     * 4. open choice to programmer, programmer compute the value of choice, making C_i +B_i = Y_i
     * 5. verify whether programmer conduct the correct calculation
     *
     * @param x          input data
     * @param programmer the party who input flag
     * @param flag       flag[i] = false: y[i] = x[i]; flag[i] = true: y[i] = y[i-1]
     */
    @Override
    public TripletRpZ2Vector[] duplicateNetwork(MpcZ2Vector[] x, boolean[] flag, Party programmer) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // programmer commit the flag
        TripletRpZ2Vector flagWire;
        if (rpc.ownParty().equals(programmer)) {
            flagWire = (TripletRpZ2Vector) z2cParty.shareOwn(BitVectorFactory.create(x[0].bitNum(), BinaryUtils.binaryToRoundByteArray(flag)));
        } else {
            flagWire = (TripletRpZ2Vector) z2cParty.shareOther(x[0].bitNum(), programmer);
        }
        // mask for origin data
        int[] inputBitNums = Arrays.stream(x).mapToInt(MpcZ2Vector::bitNum).toArray();
        TripletRpZ2Vector[] mask = crProvider.randRpShareZ2Vector(inputBitNums);
        // w0 = X - B, w1MinusW0_i = B_{i-1} - B_i - w0_i
        IntStream intStream = parallel ? IntStream.range(0, x.length).parallel() : IntStream.range(0, x.length);
        TripletRpZ2Vector[] w0 = new TripletRpZ2Vector[x.length], w1MinusW0 = new TripletRpZ2Vector[x.length];
        intStream.forEach(i -> {
            w0[i] = z2cParty.xor(x[i], mask[i]);
            w1MinusW0[i] = mask[i].copy();
            w1MinusW0[i].fixShiftRighti(1);
            z2cParty.xori(w1MinusW0[i], x[i]);
        });
        logStepInfo(PtoState.PTO_STEP, "duplicateNetwork", 1, 4, resetAndGetTime(), "mask the original data");

        stopWatch.start();
        // choice = (1-f)·w0 + f·w1
        TripletRpZ2Vector[] extendFlag = new TripletRpZ2Vector[x.length];
        Arrays.fill(extendFlag, flagWire);
        TripletRpZ2Vector[] choice = (TripletRpZ2Vector[]) z2cParty.and(extendFlag, w1MinusW0);
        z2cParty.xori(choice, w0);
        logStepInfo(PtoState.PTO_STEP, "duplicateNetwork", 2, 4, resetAndGetTime(), "compute choice matrix");

        stopWatch.start();
        TripletRpZ2Vector[] afterChoice;
        if (rpc.ownParty().equals(programmer)) {
            BitVector[] choiceUnTrans = z2cParty.revealOwn(choice);
            BitVector[] choicePlain = Arrays.stream(ZlDatabase.create(envType, parallel, choiceUnTrans).getBytesData())
                .map(each -> BitVectorFactory.create(choice.length, each)).toArray(BitVector[]::new);
            IntStream.range(1, choicePlain.length).forEach(i -> {
                if (flag[i]) {
                    choicePlain[i].xori(choicePlain[i - 1]);
                }
            });
            afterChoice = (TripletRpZ2Vector[]) z2cParty.shareOwn(
                Arrays.stream(ZlDatabase.create(envType, parallel, Arrays.copyOfRange(choicePlain, 1, choicePlain.length)).getBytesData())
                    .map(each -> BitVectorFactory.create(choicePlain.length - 1, each)).toArray(BitVector[]::new));
        } else {
            z2cParty.revealOther(choice, programmer);
            int[] afterChoiceBitNums = IntStream.range(0, choice.length).map(i -> choice[0].bitNum() - 1).toArray();
            afterChoice = (TripletRpZ2Vector[]) z2cParty.shareOther(afterChoiceBitNums, programmer);
        }
        logStepInfo(PtoState.PTO_STEP, "duplicateNetwork", 3, 4, resetAndGetTime(), "programmer update and share choice matrix");

        stopWatch.start();
        // verify whether programmer conduct the correct calculation
        TripletRpZ2Vector[] shouldSub = new TripletRpZ2Vector[x.length], res = new TripletRpZ2Vector[x.length];
        intStream = parallel ? IntStream.range(0, x.length).parallel() : IntStream.range(0, x.length);
        intStream.forEach(i -> {
            shouldSub[i] = afterChoice[i].copy();
            shouldSub[i].fixShiftRighti(1);
            shouldSub[i].setPointsWithFixedSpace(choice[i], 0, 1, 1);
            res[i] = afterChoice[i].copy();
            res[i].extendLength(x[0].bitNum());
            res[i].setPointsWithFixedSpace(choice[i], 0, 1, 1);
            z2cParty.xori(res[i], mask[i]);
        });
        flagWire.reduce(flagWire.bitNum() - 1);

        TripletRpZ2Vector[] extendSubFlag = IntStream.range(0, x.length).mapToObj(i -> flagWire).toArray(TripletRpZ2Vector[]::new);
        TripletRpZ2Vector[] mul4Sub = (TripletRpZ2Vector[]) z2cParty.and(extendSubFlag, shouldSub);

        Arrays.stream(choice).forEach(each -> each.reduce(each.bitNum() - 1));
        z2cParty.xori(mul4Sub, afterChoice);
        z2cParty.xori(mul4Sub, choice);
        z2cParty.compareView4Zero(mul4Sub);
        logStepInfo(PtoState.PTO_STEP, "duplicateNetwork", 4, 4, resetAndGetTime(), "compute and verify the result");

        logPhaseInfo(PtoState.PTO_END);
        return res;
    }

    @Override
    public TripletRpZ2Vector[] permuteNetwork(MpcZ2Vector[] input, int[] fun, int targetLen,
                                              Party programmer, Party sender, Party receiver) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int inputLen = input[0].bitNum();
        int shuffleExtendLen = getBinaryShuffleParam();

        TripletRpZ2Vector[] shuffleInput = new TripletRpZ2Vector[input.length + shuffleExtendLen];
        // padding dummy elements
        if (targetLen > input[0].bitNum()) {
            int extendByteNum = CommonUtils.getByteLength(targetLen - input[0].bitNum());
            inputLen += extendByteNum << 3;
            for (int i = 0; i < input.length; i++) {
                shuffleInput[i] = TripletRpZ2Vector.createEmpty(inputLen);
                shuffleInput[i].setBytes((TripletRpZ2Vector) input[i], 0, 0, input[i].byteNum());
            }
        } else {
            TripletRpZ2Vector[] tripletInput = Arrays.stream(input).map(ea -> (TripletRpZ2Vector) ea).toArray(TripletRpZ2Vector[]::new);
            System.arraycopy(tripletInput, 0, shuffleInput, 0, input.length);
        }
        int[] randBitNums = new int[shuffleExtendLen];
        Arrays.fill(randBitNums, inputLen);
        System.arraycopy(crProvider.randRpShareZ2Vector(randBitNums), 0, shuffleInput, input.length, shuffleExtendLen);
        logStepInfo(PtoState.PTO_STEP, "permuteNetwork", 1, 3, resetAndGetTime(), "appending mask");

        stopWatch.start();
        BitVector[] d2p = trans3To2SharingTranspose(shuffleInput, inputLen, programmer, sender);
        d2p = permuteNetworkImplWithData(d2p, fun, inputLen, targetLen, shuffleInput.length, programmer, sender, receiver);
        TripletRpZ2Vector[] res = trans2To3SharingTranspose(d2p, programmer, receiver, inputLen, shuffleInput.length);
        logStepInfo(PtoState.PTO_STEP, "permuteNetwork", 2, 3, resetAndGetTime(), "permute the data");

        stopWatch.start();
        verifyShufflePairColumn(shuffleInput, res);
        // shift the result to get the true out
        int shiftBit = 0;
        if (targetLen > input[0].bitNum() && inputLen > targetLen) {
            shiftBit = inputLen - targetLen;
        } else if (targetLen < input[0].bitNum()) {
            shiftBit = input[0].bitNum() - targetLen;
        }
        if (shiftBit > 0) {
            for (int i = 0; i < input.length; i++) {
                res[i].reduceShiftRighti(shiftBit);
            }
        }
        logStepInfo(PtoState.PTO_STEP, "permuteNetwork", 3, 3, resetAndGetTime(), "verify and get the output");

        logPhaseInfo(PtoState.PTO_END);
        return Arrays.copyOf(res, input.length);
    }

    @Override
    public TripletRpLongVector[] switchNetwork(MpcLongVector[] input, int[] fun, int targetLen,
                                               Party programmer, Party sender, Party receiver) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        StopWatch stopWatch1 = new StopWatch();
        stopWatch1.start();
        boolean[] flag = new boolean[targetLen];
        int[][] twoPer = new int[2][];
        if (rpc.ownParty().equals(programmer)) {
            Arrays.fill(flag, true);
            twoPer = ShuffleUtils.get2PerForSwitch(fun, input[0].getNum(), flag);
        }
        TripletRpLongVector[] tmp = permuteNetwork(input, twoPer[0], targetLen, programmer, sender, receiver);
        stopWatch1.stop();
        logStepInfo(PtoState.PTO_STEP, "switchNetwork", 1, 3, stopWatch1.getTime(TimeUnit.MILLISECONDS), "first permute");
        stopWatch1.reset();

        stopWatch1.start();
        tmp = duplicateNetwork(tmp, flag, programmer);
        stopWatch1.stop();
        logStepInfo(PtoState.PTO_STEP, "switchNetwork", 2, 3, stopWatch1.getTime(TimeUnit.MILLISECONDS), "duplicateNetwork");
        stopWatch1.reset();

        stopWatch1.start();
        TripletRpLongVector[] res = permuteNetwork(tmp, twoPer[1], targetLen, programmer, sender, receiver);
        stopWatch1.stop();
        logStepInfo(PtoState.PTO_STEP, "switchNetwork", 3, 3, stopWatch1.getTime(TimeUnit.MILLISECONDS), "second permute");
        stopWatch1.reset();

        logPhaseInfo(PtoState.PTO_END);
        return res;
    }

    @Override
    public TripletRpLongMacVector[] permuteNetwork(MpcLongVector[] input, int[] fun, int targetLen,
                                                   Party programmer, Party sender, Party receiver) throws MpcAbortException {
        LOGGER.info("P{} start trans3To2Sharing in A permuteNetwork", rpc.ownParty().getPartyId());
        LongVector[] d2p = trans3To2Sharing((TripletRpLongVector[]) input, programmer, sender, targetLen);
        int columnLength = Math.max(input[0].getNum(), targetLen);
        LOGGER.info("P{} start permuteNetworkImplWithData in A permuteNetwork", rpc.ownParty().getPartyId());
        d2p = this.permuteNetworkImplWithData(d2p, fun, columnLength, programmer, sender, receiver);
        LOGGER.info("P{} start trans2To3Sharing in A permuteNetwork", rpc.ownParty().getPartyId());
        TripletRpLongMacVector[] res = trans2To3Sharing(d2p, programmer, receiver, IntStream.range(0, input.length).map(i -> columnLength).toArray());
        LOGGER.info("P{} finish interaction in A permuteNetwork", rpc.ownParty().getPartyId());
        macParty.intoBuffer(res);
        if (columnLength > targetLen) {
            for (TripletRpLongMacVector each : res) {
                each.reduce(targetLen);
            }
        }
        return res;
    }

    @Override
    public TripletRpLongVector[] duplicateNetwork(MpcLongVector[] x, boolean[] flag, Party programmer) throws MpcAbortException {
        // Pp commit flag
        TripletRpLongVector flagWire;
        if (rpc.ownParty().equals(programmer)) {
            long[] flagLong = IntStream.range(0, flag.length).mapToLong(i -> flag[i] ? 1L : 0L).toArray();
            flagWire = (TripletRpLongVector) zl64cParty.shareOwn(LongVector.create(flagLong));
        } else {
            flagWire = (TripletRpLongVector) zl64cParty.shareOther(x[0].getNum(), programmer);
        }
        TripletRpLongVector[] mask = crProvider.randRpShareZl64Vector(IntStream.range(0, x.length).map(i -> x[0].getNum()).toArray());
        if (zl64cParty instanceof Cgh18RpLongParty) {
            // all using mac
            TripletRpLongVector[] all = new TripletRpLongVector[mask.length + 1];
            System.arraycopy(mask, 0, all, 0, mask.length);
            all[mask.length] = flagWire;
            macParty.genMac(all);
            TripletRpLongVector[] tmpX = Arrays.stream(x).map(each -> (TripletRpLongVector) each).toArray(TripletRpLongVector[]::new);
            macParty.genMac(tmpX);
            flagWire = all[mask.length];
            System.arraycopy(all, 0, mask, 0, mask.length);
        }

        // w0 = X - B, w1MinusW0_i = B_{i-1} - B_i - w0_i
        IntStream intStream = parallel ? IntStream.range(0, x.length).parallel() : IntStream.range(0, x.length);
        TripletRpLongVector[] w0 = new TripletRpLongVector[x.length], w1MinusW0 = new TripletRpLongVector[x.length];
        intStream.forEach(i -> {
            w0[i] = (TripletRpLongVector) zl64cParty.sub(x[i], mask[i]);
            w1MinusW0[i] = (TripletRpLongVector) zl64cParty.sub(mask[i].shiftRight(1, x[i].getNum()), x[i]);
        });

        // choice = (1-f)·w0 + f·w1
        TripletRpLongVector[] extendFlag = new TripletRpLongVector[x.length];
        Arrays.fill(extendFlag, flagWire);
        TripletLongVector[] choice = zl64cParty.mul(extendFlag, w1MinusW0);
        zl64cParty.addi(choice, w0);
        TripletRpLongVector[] afterChoice;

        // open choice to Pp
        if (rpc.ownParty().equals(programmer)) {
            long[][] choicePlain = Arrays.stream(zl64cParty.revealOwn(choice)).map(LongVector::getElements).toArray(long[][]::new);
            long[][] res = new long[choicePlain.length][x[0].getNum() - 1];
            intStream = parallel ? IntStream.range(0, x.length).parallel() : IntStream.range(0, x.length);
            intStream.forEach(i -> {
                res[i][0] = flag[1] ? choicePlain[i][1] + choicePlain[i][0] : choicePlain[i][1];
                IntStream.range(1, res[i].length).forEach(j ->
                    res[i][j] = flag[j + 1] ? (choicePlain[i][j + 1] + res[i][j - 1]) : choicePlain[i][j + 1]);
            });
            afterChoice = (TripletRpLongVector[]) zl64cParty.shareOwn(Arrays.stream(res).map(LongVector::create).toArray(LongVector[]::new));
        } else {
            zl64cParty.revealOther(programmer, choice);
            afterChoice = (TripletRpLongVector[]) zl64cParty.shareOther(IntStream.range(0, choice.length).map(i -> x[0].getNum() - 1).toArray(), programmer);
        }

        // verify
        TripletRpLongVector[] shouldSub = IntStream.range(0, x.length).mapToObj(i -> {
            TripletRpLongVector tmp = TripletRpLongVector.createZeros(x[i].getNum() - 1);
            tmp.setElements(choice[i], 0, 0, 1);
            tmp.setElements(afterChoice[i], 0, 1, afterChoice[i].getNum() - 1);
            return tmp;
        }).toArray(TripletRpLongVector[]::new);
        TripletRpLongVector[] subFlagArray = new TripletRpLongVector[]{flagWire.copyToNew(1, flagWire.getNum())};
        if (zl64cParty instanceof Cgh18RpLongParty) {
            macParty.genMac(subFlagArray);
        }
        TripletRpLongVector[] extendSubFlag = IntStream.range(0, x.length).mapToObj(i -> subFlagArray[0]).toArray(TripletRpLongVector[]::new);
        TripletLongVector[] mul4Sub = zl64cParty.mul(extendSubFlag, shouldSub);
        TripletRpLongVector[] shouldBeZero = IntStream.range(0, x.length).mapToObj(i -> {
            TripletRpLongVector tmp = ((TripletRpLongVector) choice[i]).copyToNew(1, choice[i].getNum());
            return (TripletRpLongVector) zl64cParty.sub(zl64cParty.sub(afterChoice[i], mul4Sub[i]), tmp);
        }).toArray(TripletRpLongVector[]::new);
        zl64cParty.compareView4Zero(64, shouldBeZero);

        // final result
        return IntStream.range(0, x.length).mapToObj(i -> {
            TripletRpLongVector tmp = TripletRpLongVector.createZeros(x[i].getNum());
            tmp.setElements(choice[i], 0, 0, 1);
            tmp.setElements(afterChoice[i], 0, 1, x[i].getNum() - 1);
            zl64cParty.addi(tmp, mask[i]);
            return tmp;
        }).toArray(TripletRpLongVector[]::new);
    }
}
