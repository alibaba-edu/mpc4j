package edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.replicate;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleOperations.ShuffleOp;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.replicate.Aby3ShufflePtoDesc.PtoStep;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.ShuffleUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;
import org.apache.commons.lang3.time.StopWatch;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * The semi-honest version of Replicated-sharing shuffling party
 *
 * @author Feng Han
 * @date 2024/01/18
 */
public class Aby3ShShuffleParty extends AbstractAby3ShuffleParty implements Aby3ShuffleParty {
    public Aby3ShShuffleParty(TripletZ2cParty z2cParty, TripletLongParty zl64cParty, Aby3ShuffleConfig config) {
        super(z2cParty, zl64cParty, config);
    }

    @Override
    public long getTupleNum(ShuffleOp op, int inputDataNum, int outputDataNum, int dataDim){
        return 0;
    }

    @Override
    protected LongVector[] trans3To2Sharing(TripletRpLongVector[] input, Party p0, Party p1, int targetLen) {
        if (rpc.ownParty().equals(p0) || rpc.ownParty().equals(p1)) {
            Party withWho = rpc.ownParty().equals(p0) ? p1 : p0;
            int[] dataNums = Arrays.stream(input).mapToInt(TripletRpLongVector::getNum).toArray();

            LongVector[] randWithWho = crProvider.randZl64Vector(dataNums, withWho);
            IntStream intStream = parallel ? IntStream.range(0, input.length).parallel() : IntStream.range(0, input.length);
            if (withWho.equals(leftParty())) {
                intStream.forEach(i -> randWithWho[i] = input[i].getVectors()[1].sub(randWithWho[i]));
            } else {
                intStream.forEach(i -> {
                    randWithWho[i].addi(input[i].getVectors()[0]);
                    randWithWho[i].addi(input[i].getVectors()[1]);
                });
            }
            if (randWithWho[0].getNum() < targetLen) {
                intStream = parallel ? IntStream.range(0, input.length).parallel() : IntStream.range(0, input.length);
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
    protected TripletRpLongVector[] trans2To3Sharing(LongVector[] input, Party p0, Party p1, int[] dataNums) {
        if (rpc.ownParty().equals(p0) || rpc.ownParty().equals(p1)) {
            Party withWho = rpc.ownParty().equals(p0) ? p1 : p0;
            Party toWho = withWho.equals(leftParty()) ? rightParty() : leftParty();

            LongVector[] rand = crProvider.randZl64Vector(dataNums, toWho);
            LongVector[] subRes = IntStream.range(0, input.length).mapToObj(i -> input[i].sub(rand[i])).toArray(LongVector[]::new);
            sendLongVectors(PtoStep.TWO_SHARE_INTO_THREE_SHARE.ordinal(), withWho, subRes);

            LongVector[] others = receiveLongVectors(PtoStep.TWO_SHARE_INTO_THREE_SHARE.ordinal(), withWho);
            IntStream.range(0, input.length).forEach(i -> subRes[i].addi(others[i]));
            if (withWho.equals(leftParty())) {
                return IntStream.range(0, input.length).mapToObj(i ->
                    TripletRpLongVector.create(subRes[i], rand[i])).toArray(TripletRpLongVector[]::new);
            } else {
                return IntStream.range(0, input.length).mapToObj(i ->
                    TripletRpLongVector.create(rand[i], subRes[i])).toArray(TripletRpLongVector[]::new);
            }
        } else {
            LongVector[] rWithLeft = crProvider.randZl64Vector(dataNums, leftParty());
            LongVector[] rWithRight = crProvider.randZl64Vector(dataNums, rightParty());
            return IntStream.range(0, dataNums.length).mapToObj(i ->
                TripletRpLongVector.create(rWithLeft[i], rWithRight[i])).toArray(TripletRpLongVector[]::new);
        }
    }

    @Override
    public TripletRpZ2Vector[] shuffleRow(int[][] pai, MpcZ2Vector[] data) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);
        MathPreconditions.checkEqual("pai.length", "2", pai.length, 2);
        MathPreconditions.checkEqual("pai[0].length", "pai[1].length", pai[0].length, pai[1].length);
        int[] bitNums = Arrays.stream(data).mapToInt(MpcZ2Vector::bitNum).toArray();

        stopWatch.start();
        TripletRpZ2Vector[] tmp = Arrays.stream(data).map(ea -> (TripletRpZ2Vector) ea).toArray(TripletRpZ2Vector[]::new);
        BitVector[] d2p = trans3To2Sharing(tmp, rpc.getParty(0), rpc.getParty(2));
        logStepInfo(PtoState.PTO_STEP, "shuffleRow", 1, 3, resetAndGetTime(), "trans 3-sharing to 2-sharing");

        stopWatch.start();
        if (selfId == 0) {
            d2p = ShuffleUtils.applyPermutation(d2p, pai[0]);
            reRand2pShare(d2p, rightParty());
            d2p = ShuffleUtils.applyPermutation(d2p, pai[1]);
            sendBitVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), leftParty(), d2p);
        } else if (selfId == 1) {
            int[] mu = ShuffleUtils.applyPermutation(pai[0], pai[1]);
            d2p = receiveBitVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), rightParty(), bitNums);
            reRand2pShare(d2p, leftParty());
            d2p = ShuffleUtils.applyPermutation(d2p, mu);
        } else {
            d2p = ShuffleUtils.applyPermutation(d2p, pai[1]);
            sendBitVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), leftParty(), d2p);
            d2p = receiveBitVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), rightParty(), bitNums);
            d2p = ShuffleUtils.applyPermutation(d2p, pai[0]);
        }
        logStepInfo(PtoState.PTO_STEP, "shuffleRow", 2, 3, resetAndGetTime(), "2p shuffling");

        stopWatch.start();
        TripletRpZ2Vector[] res = trans2To3Sharing(d2p, rpc.getParty(1), rpc.getParty(2), bitNums);
        logStepInfo(PtoState.PTO_STEP, "shuffleRow", 3, 3, resetAndGetTime(), "trans 2-sharing to 3-sharing");

        logPhaseInfo(PtoState.PTO_END);
        return res;
    }

    @Override
    public TripletRpZ2Vector[] shuffleColumn(int[][] pai, MpcZ2Vector... data) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);
        MathPreconditions.checkEqual("pai.length", "2", pai.length, 2);
        MathPreconditions.checkEqual("pai[0].length", "pai[1].length", pai[0].length, pai[1].length);
        int[] transBit = new int[pai[0].length];
        Arrays.fill(transBit, data.length);

        stopWatch.start();
        TripletRpZ2Vector[] tmp = Arrays.stream(data).map(ea -> (TripletRpZ2Vector) ea).toArray(TripletRpZ2Vector[]::new);
        BitVector[] d2p = trans3To2SharingTranspose(tmp, pai[0].length, rpc.getParty(0), rpc.getParty(2));
        logStepInfo(PtoState.PTO_STEP, "shuffleColumn", 1, 3, resetAndGetTime(), "trans 3-sharing to 2-sharing");

        stopWatch.start();
        if (selfId == 0) {
            d2p = ShuffleUtils.applyPermutation(d2p, pai[0]);
            reRand2pShare(d2p, rightParty());
            d2p = ShuffleUtils.applyPermutation(d2p, pai[1]);
            sendBitVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), leftParty(), d2p);
        } else if (selfId == 1) {
            int[] mu = ShuffleUtils.applyPermutation(pai[0], pai[1]);
            d2p = receiveBitVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), rightParty(), transBit);
            reRand2pShare(d2p, leftParty());
            d2p = ShuffleUtils.applyPermutation(d2p, mu);
        } else {
            d2p = ShuffleUtils.applyPermutation(d2p, pai[1]);
            sendBitVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), leftParty(), d2p);
            d2p = receiveBitVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), rightParty(), transBit);
            d2p = ShuffleUtils.applyPermutation(d2p, pai[0]);
        }
        logStepInfo(PtoState.PTO_STEP, "shuffleColumn", 2, 3, resetAndGetTime(), "shuffling data");

        stopWatch.start();
        TripletRpZ2Vector[] res = trans2To3SharingTranspose(d2p, rpc.getParty(2), rpc.getParty(1), pai[0].length, data.length);
        logStepInfo(PtoState.PTO_STEP, "shuffleColumn", 3, 3, resetAndGetTime(), "trans 2-sharing to 3-sharing");

        logPhaseInfo(PtoState.PTO_END);
        return res;
    }

    @Override
    public TripletRpLongVector[] shuffle(int[][] pai, MpcLongVector... data) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);
        MathPreconditions.checkEqual("pai.length", "2", pai.length, 2);
        MathPreconditions.checkEqual("pai[0].length", "pai[1].length", pai[0].length, pai[1].length);

        stopWatch.start();
        TripletRpLongVector[] tmp = Arrays.stream(data).map(ea -> (TripletRpLongVector) ea).toArray(TripletRpLongVector[]::new);
        LongVector[] d2p = trans3To2Sharing(tmp, rpc.getParty(0), rpc.getParty(2), pai[0].length);
        logStepInfo(PtoState.PTO_STEP, "shuffle", 1, 3, resetAndGetTime(), "trans 3-sharing to 2-sharing");

        stopWatch.start();
        if (selfId == 0) {
            d2p = ShuffleUtils.applyPermutationToRows(d2p, pai[0]);
            reRand2pShare(d2p, rightParty());
            d2p = ShuffleUtils.applyPermutationToRows(d2p, pai[1]);
            sendLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), leftParty(), d2p);
        } else if (selfId == 1) {
            int[] mu = ShuffleUtils.applyPermutation(pai[0], pai[1]);
            d2p = receiveLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), rightParty());
            reRand2pShare(d2p, leftParty());
            d2p = ShuffleUtils.applyPermutationToRows(d2p, mu);
        } else {
            d2p = ShuffleUtils.applyPermutationToRows(d2p, pai[1]);
            sendLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), leftParty(), d2p);
            d2p = receiveLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), rightParty());
            d2p = ShuffleUtils.applyPermutationToRows(d2p, pai[0]);
        }
        logStepInfo(PtoState.PTO_STEP, "shuffle", 2, 3, resetAndGetTime(), "shuffling data");

        stopWatch.start();
        int[] targetDataNums = Arrays.stream(data).mapToInt(MpcLongVector::getNum).toArray();
        TripletRpLongVector[] res = trans2To3Sharing(d2p, rpc.getParty(1), rpc.getParty(2), targetDataNums);
        logStepInfo(PtoState.PTO_STEP, "shuffle", 3, 3, resetAndGetTime(), "trans 2-sharing to 3-sharing");

        logPhaseInfo(PtoState.PTO_END);
        return res;
    }

//    @Override
//    public TripletRpZl64Vector[] shuffle(int[][] pai, MpcLongVector... data) throws MpcAbortException {
//        logPhaseInfo(PtoState.PTO_BEGIN);
//        MathPreconditions.checkEqual("pai.length", "2", pai.length, 2);
//        MathPreconditions.checkEqual("pai[0].length", "pai[1].length", pai[0].length, pai[1].length);
//
//        stopWatch.start();
//        LongVector[] d2p;
//        TripletRpZl64Vector[] rand = crProvider.randRpShareZl64Vector(Arrays.stream(data).mapToInt(MpcLongVector::getNum).toArray());
//        logStepInfo(PtoState.PTO_STEP, "shuffle", 1, 3, resetAndGetTime(), "generate randomness");
//
//        stopWatch.start();
//        if (selfId == 0) {
//            d2p = Arrays.stream(data).map(each -> each.getVectors()[0].add(each.getVectors()[1])).toArray(LongVector[]::new);
//            d2p = ShuffleUtils.applyPermutationToRows(d2p, pai[0]);
//            for(int i = 0; i < d2p.length; i++){
//                d2p[i].subi(rand[i].getVectors()[0]);
//            }
//            d2p = ShuffleUtils.applyPermutationToRows(d2p, pai[1]);
//            for(int i = 0; i < d2p.length; i++){
//                d2p[i].subi(rand[i].getVectors()[1]);
//            }
//            sendLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), leftParty(), d2p);
//        } else if (selfId == 1) {
//            d2p = receiveLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), rightParty());
//            d2p = ShuffleUtils.applyPermutationToRows(d2p, pai[0]);
//            for(int i = 0; i < d2p.length; i++){
//                d2p[i].addi(rand[i].getVectors()[0]);
//            }
//            d2p = ShuffleUtils.applyPermutationToRows(d2p, pai[1]);
//        } else {
//            d2p = Arrays.stream(data).map(each -> each.getVectors()[0]).toArray(LongVector[]::new);
//            d2p = ShuffleUtils.applyPermutationToRows(d2p, pai[1]);
//            for(int i = 0; i < d2p.length; i++){
//                d2p[i].addi(rand[i].getVectors()[1]);
//            }
//            sendLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), leftParty(), d2p);
//            d2p = receiveLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), rightParty());
//            d2p = ShuffleUtils.applyPermutationToRows(d2p, pai[0]);
//        }
//        logStepInfo(PtoState.PTO_STEP, "shuffle", 2, 3, resetAndGetTime(), "shuffling data");
//
//        stopWatch.start();
//        int[] targetDataNums = Arrays.stream(data).mapToInt(MpcLongVector::getNum).toArray();
//        TripletRpZl64Vector[] res = trans2To3Sharing(d2p, rpc.getParty(1), rpc.getParty(2), targetDataNums);
//        logStepInfo(PtoState.PTO_STEP, "shuffle", 3, 3, resetAndGetTime(), "trans 2-sharing to 3-sharing");
//
//        logPhaseInfo(PtoState.PTO_END);
//        return res;
//    }

    @Override
    public LongVector[] shuffleOpen(int[][] pai, MpcLongVector... data) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);
        MathPreconditions.checkEqual("pai.length", "2", pai.length, 2);
        MathPreconditions.checkEqual("pai[0].length", "pai[1].length", pai[0].length, pai[1].length);
        LongVector[] d2p;

        int[] nums = new int[data.length];
        Arrays.fill(nums, data[0].getNum());
        if (selfId == 0) {
            LongVector[] random = crProvider.randZl64Vector(nums, rpc.getParty(2));
            d2p = Arrays.stream(data).map(each -> each.getVectors()[0].add(each.getVectors()[1])).toArray(LongVector[]::new);
            d2p = ShuffleUtils.applyPermutationToRows(d2p, pai[0]);
            for(int i = 0; i < d2p.length; i++){
                d2p[i].subi(random[i]);
            }
            sendLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), rightParty(), d2p);
            d2p = receiveLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), rightParty());
        } else if (selfId == 1) {
            int[] mu = ShuffleUtils.applyPermutation(pai[0], pai[1]);
            d2p = receiveLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), rightParty());
            LongVector[] d2pN = receiveLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), leftParty());
            for(int i = 0; i < d2p.length; i++){
                d2p[i].addi(d2pN[i]);
            }
            d2p = ShuffleUtils.applyPermutationToRows(d2p, mu);
            sendLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), rightParty(), d2p);
            sendLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), leftParty(), d2p);
        } else {
            LongVector[] random = crProvider.randZl64Vector(nums, rpc.getParty(0));
            d2p = Arrays.stream(data).map(each -> each.getVectors()[0]).toArray(LongVector[]::new);
            d2p = ShuffleUtils.applyPermutationToRows(d2p, pai[1]);
            for(int i = 0; i < d2p.length; i++){
                d2p[i].addi(random[i]);
            }
            sendLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), leftParty(), d2p);
            d2p = receiveLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), leftParty());
        }

        return d2p;
    }

    @Override
    public TripletRpLongVector[] invShuffle(int[][] pai, MpcLongVector... data) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);
        MathPreconditions.checkEqual("pai.length", "2", pai.length, 2);
        MathPreconditions.checkEqual("pai[0].length", "pai[1].length", pai[0].length, pai[1].length);

        stopWatch.start();
        TripletRpLongVector[] tmp = Arrays.stream(data).map(ea -> (TripletRpLongVector) ea).toArray(TripletRpLongVector[]::new);
        LongVector[] d2p = trans3To2Sharing(tmp, rpc.getParty(1), rpc.getParty(2), pai[0].length);
        logStepInfo(PtoState.PTO_STEP, "invShuffle", 1, 3, resetAndGetTime(), "trans 3-sharing to 2-sharing");

        stopWatch.start();
        if (selfId == 0) {
            int[] mu = ShuffleUtils.invOfPermutation(ShuffleUtils.applyPermutation(pai[0], pai[1]));
            d2p = receiveLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), leftParty());
            reRand2pShare(d2p, rightParty());
            d2p = ShuffleUtils.applyPermutationToRows(d2p, mu);
        } else if (selfId == 1) {
            d2p = ShuffleUtils.applyPermutationToRows(d2p, ShuffleUtils.invOfPermutation(pai[1]));
            reRand2pShare(d2p, leftParty());
            d2p = ShuffleUtils.applyPermutationToRows(d2p, ShuffleUtils.invOfPermutation(pai[0]));
            sendLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), rightParty(), d2p);
        } else {
            d2p = ShuffleUtils.applyPermutationToRows(d2p, ShuffleUtils.invOfPermutation(pai[0]));
            sendLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), rightParty(), d2p);
            d2p = receiveLongVectors(PtoStep.COMM_WITH_SPECIFIC_PARTY.ordinal(), leftParty());
            d2p = ShuffleUtils.applyPermutationToRows(d2p, ShuffleUtils.invOfPermutation(pai[1]));
        }
        logStepInfo(PtoState.PTO_STEP, "invShuffle", 2, 3, resetAndGetTime(), "shuffling data");

        stopWatch.start();
        int[] targetDataNums = Arrays.stream(data).mapToInt(MpcLongVector::getNum).toArray();
        TripletRpLongVector[] res = trans2To3Sharing(d2p, rpc.getParty(0), rpc.getParty(2), targetDataNums);
        logStepInfo(PtoState.PTO_STEP, "invShuffle", 3, 3, resetAndGetTime(), "trans 2-sharing to 3-sharing");

        logPhaseInfo(PtoState.PTO_END);
        return res;
    }

    @Override
    public TripletRpZ2Vector[] switchNetwork(MpcZ2Vector[] input, int[] fun, int targetLen,
                                             Party programmer, Party sender, Party receiver) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);
        int maxLen = Math.max(input[0].bitNum(), targetLen);
        int[][] twoPer = new int[2][];
        boolean[] flag = null;

        StopWatch stopWatch1 = new StopWatch();
        stopWatch1.start();
        BitVector[] d2p = trans3To2SharingTranspose((TripletRpZ2Vector[]) input, targetLen, programmer, sender);
        if (rpc.ownParty().equals(programmer)) {
            MathPreconditions.checkEqual("targetLen", "fun.length", targetLen, fun.length);
            // 1. 生成一个置换，将有效的输入通过shuffle置换到对应的位置
            // int[] 分为三维，第一维标识出现的次数，第二维标识在第一次置换中被置换到的位置，第三维标识第二次置换中的变量，用于生成第二个置换
            flag = new boolean[targetLen];
            Arrays.fill(flag, true);
            twoPer = ShuffleUtils.get2PerForSwitch(fun, input[0].bitNum(), flag);
        }
        stopWatch1.stop();
        logStepInfo(PtoState.PTO_STEP, "switchNetwork", 1, 5, stopWatch1.getTime(TimeUnit.MILLISECONDS), "trans3To2SharingTranspose");
        stopWatch1.reset();

        // shuffle
        stopWatch1.start();
        d2p = this.permuteNetworkImplWithData(d2p, twoPer[0], maxLen, targetLen, input.length, programmer, sender, receiver);
        stopWatch1.stop();
        logStepInfo(PtoState.PTO_STEP, "switchNetwork", 2, 5, stopWatch1.getTime(TimeUnit.MILLISECONDS), "first permute");
        stopWatch1.reset();
        // 2. 通过dup network
        stopWatch1.start();
        d2p = this.duplicateNetworkImplWithData(d2p, flag, targetLen, input.length, programmer, receiver, sender);
        stopWatch1.stop();
        logStepInfo(PtoState.PTO_STEP, "switchNetwork", 3, 5, stopWatch1.getTime(TimeUnit.MILLISECONDS), "duplicate network");
        stopWatch1.reset();
        // 3. 置换expansion之后的结果，得到最终的输入
        stopWatch1.start();
        d2p = this.permuteNetworkImplWithData(d2p, twoPer[1], targetLen, targetLen, input.length, programmer, sender, receiver);
        stopWatch1.stop();
        logStepInfo(PtoState.PTO_STEP, "switchNetwork", 4, 5, stopWatch1.getTime(TimeUnit.MILLISECONDS), "second permute");
        stopWatch1.reset();

        stopWatch1.start();
        TripletRpZ2Vector[] res = trans2To3SharingTranspose(d2p, programmer, receiver, targetLen, input.length);
        stopWatch1.stop();
        logStepInfo(PtoState.PTO_STEP, "switchNetwork", 5, 5, stopWatch1.getTime(TimeUnit.MILLISECONDS), "trans2To3SharingTranspose");
        stopWatch1.reset();

        logPhaseInfo(PtoState.PTO_END);
        return res;
    }

    @Override
    public TripletRpZ2Vector[] permuteNetwork(MpcZ2Vector[] input, int[] fun, int targetLen,
                                              Party programmer, Party sender, Party receiver) throws MpcAbortException {
        if (rpc.ownParty().equals(programmer)) {
            MathPreconditions.checkEqual("targetLen", "fun.length", targetLen, fun.length);
        }
        TripletRpZ2Vector[] tInput = Arrays.stream(input).map(ea -> (TripletRpZ2Vector) ea).toArray(TripletRpZ2Vector[]::new);
        BitVector[] d2p = trans3To2SharingTranspose(tInput, targetLen, programmer, sender);
        int maxLength = Math.max(input[0].bitNum(), targetLen);
        d2p = this.permuteNetworkImplWithData(d2p, fun, maxLength, targetLen, input.length, programmer, sender, receiver);
        return trans2To3SharingTranspose(d2p, programmer, receiver, targetLen, input.length);
    }

    @Override
    public TripletRpZ2Vector[] duplicateNetwork(MpcZ2Vector[] input, boolean[] flag, Party programmer) {
        Party sender = rpc.getParty((programmer.getPartyId() + 1) % 3);
        Party receiver = rpc.getParty((programmer.getPartyId() + 2) % 3);
        BitVector[] d2p = trans3To2SharingTranspose((TripletRpZ2Vector[]) input, input[0].bitNum(), programmer, sender);
        d2p = duplicateNetworkImplWithData(d2p, flag, input[0].bitNum(), input.length, programmer, sender, receiver);
        return trans2To3SharingTranspose(d2p, programmer, receiver, input[0].bitNum(), input.length);
    }

    public BitVector[] duplicateNetworkImplWithData(BitVector[] x, boolean[] flag, int matrixRowLen, int eachBitNum,
                                                    Party programmer, Party sender, Party receiver) {
        // 输入bit矩阵的维度是(matrixRowLen, eachBitNum)
        int[] randLenWithEqualSize = IntStream.range(0, matrixRowLen).map(i -> eachBitNum).toArray();
        int[] matrixBitLenArray = IntStream.range(0, (matrixRowLen - 1) << 1).map(i -> eachBitNum).toArray();
        if (rpc.ownParty().equals(sender)) {
            // 1. 先采样phi，因为后面要发送给role=0的用户，故直接关联随机数生成
            BitVector phiVec = crProvider.randBitVector(new int[]{matrixRowLen}, programmer)[0];
            boolean[] phi = BinaryUtils.uncheckByteArrayToBinary(phiVec.getBytes(), matrixRowLen - 1);
            // 2. 再采样W^0, W^1，维度是(2 * dataDim, dataLen - 1)，发送给role=1的用户
            BitVector[] wMatrix = crProvider.randBitVector(matrixBitLenArray, receiver);
            // 3. 再采样<B>_1和<B_1>_0,   <B>_1维度是(dataDim, dataLen)，因为后面要发送给role=1的用户，故直接关联随机数生成
            BitVector[] b1Matrix = crProvider.randBitVector(randLenWithEqualSize, receiver);
            BitVector b0 = x[0].xor(b1Matrix[0]);
            sendBitVectors(PtoStep.DUPLICATE_MSG.ordinal(), programmer, b0);
            // 4. 两个matrix分别是 M^0 = <A_i>_1 - <B_i>_1 + W_i^{phi_1}, M^1 = <B_{i-1}>_1 - <B_i>_1 + W_i^{1 - phi_1}
            BitVector[] mMatrix = new BitVector[2 * (matrixRowLen - 1)];
            IntStream intStream = parallel ? IntStream.range(0, matrixRowLen - 1).parallel() : IntStream.range(0, matrixRowLen - 1);
            intStream.forEach(i -> {
                int wPhiDim = i + matrixRowLen - 1;
                mMatrix[i] = x[i + 1].xor(b1Matrix[i + 1]).xor(wMatrix[phi[i] ? i + matrixRowLen - 1 : i]);
                mMatrix[wPhiDim] = b1Matrix[i].xor(b1Matrix[i + 1]).xor(wMatrix[phi[i] ? i : i + matrixRowLen - 1]);
            });
            // 5. 将mMatrix，<B_1>_0发送给role=0的用户
            sendBitVectors(PtoStep.DUPLICATE_MSG.ordinal(), programmer, mMatrix);
            return null;
        } else if (rpc.ownParty().equals(programmer)) {
            // 1. 关联随机数得到phi，得到rho= phi ^ flag, 并发送给role=1的用户
            BitVector phiVec = crProvider.randBitVector(new int[]{matrixRowLen}, sender)[0];
            byte[] rhoByte = BytesUtils.xor(phiVec.getBytes(), BinaryUtils.binaryToRoundByteArray(flag));
            send(PtoStep.DUPLICATE_MSG.ordinal(), receiver, Collections.singletonList(rhoByte));
            // 2. 先预计算和role=1在最后一步随机化输出的randomness
            BitVector[] randomLast = crProvider.randBitVector(randLenWithEqualSize, receiver);
            // 3. 从role=2的用户接收 mMatrix，<B_1>_0; 从role=1的用户接收 wChoiceMatrix
            BitVector[] wChoiceMatrix = receiveBitVectors(PtoStep.DUPLICATE_MSG.ordinal(), receiver, Arrays.copyOf(randLenWithEqualSize, matrixRowLen - 1));
            BitVector b0 = receiveBitVectors(PtoStep.DUPLICATE_MSG.ordinal(), sender, new int[]{eachBitNum})[0];
            BitVector[] finalRes = new BitVector[matrixRowLen];
            finalRes[0] = b0.xor(x[0]);
            BitVector[] mMatrix = receiveBitVectors(PtoStep.DUPLICATE_MSG.ordinal(), sender,
                IntStream.range(0, (matrixRowLen - 1) << 1).map(i -> eachBitNum).toArray());
            // 4.得到最终的结果，计算
            IntStream.range(1, matrixRowLen).forEach(i -> {
                int verIndex = i - 1;
                if (flag[i]) {
                    finalRes[i] = mMatrix[matrixRowLen - 2 + i].xor(finalRes[verIndex].xor(wChoiceMatrix[verIndex]));
                } else {
                    finalRes[i] = mMatrix[verIndex].xor(x[i].xor(wChoiceMatrix[verIndex]));
                }
            });
            IntStream intStream = parallel ? IntStream.range(0, matrixRowLen).parallel() : IntStream.range(0, matrixRowLen);
            intStream.forEach(i -> finalRes[i].xori(randomLast[i]));
            return finalRes;
        } else {
            // 1. 关联随机数生成W^0, W^1，维度是(2 * dataDim, dataLen - 1)
            BitVector[] wMatrix = crProvider.randBitVector(matrixBitLenArray, sender);
            // 2. 接收role=0用户的rho, 并根据rho发送 W_i^{rho_i}
            byte[] rhoByte = receive(PtoStep.DUPLICATE_MSG.ordinal(), programmer).get(0);
            boolean[] rho = BinaryUtils.uncheckByteArrayToBinary(rhoByte, matrixRowLen - 1);
            BitVector[] wChoiceMatrix = new BitVector[matrixRowLen - 1];
            IntStream intStream = parallel ? IntStream.range(0, matrixRowLen - 1).parallel() : IntStream.range(0, matrixRowLen - 1);
            intStream.forEach(i -> wChoiceMatrix[i] = wMatrix[rho[i] ? i + matrixRowLen - 1 : i]);
            sendBitVectors(PtoStep.DUPLICATE_MSG.ordinal(), programmer, wChoiceMatrix);
            // 3. 生成和role=0的用户在最后一步随机化输出的randomness，并与关联随机数得到的<B>_1相加，得到自己的输出。
            BitVector[] randomLast = crProvider.randBitVector(randLenWithEqualSize, programmer);
            BitVector[] b1Matrix = crProvider.randBitVector(randLenWithEqualSize, sender);
            intStream = parallel ? IntStream.range(0, matrixRowLen).parallel() : IntStream.range(0, matrixRowLen);
            intStream.forEach(i -> randomLast[i].xori(b1Matrix[i]));
            return randomLast;
        }
    }

    @Override
    public MpcLongVector[] switchNetwork(MpcLongVector[] input, int[] fun, int targetLen,
                                         Party programmer, Party sender, Party receiver) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);
        int maxLen = Math.max(input[0].getNum(), targetLen);
        boolean[] flag = null;
        int[][] twoPer = new int[2][];

        StopWatch stopWatch1 = new StopWatch();
        stopWatch1.start();
        if (rpc.ownParty().equals(programmer)) {
            flag = new boolean[targetLen];
            Arrays.fill(flag, true);
            twoPer = ShuffleUtils.get2PerForSwitch(fun, input[0].getNum(), flag);
        }
        LongVector[] d2p = trans3To2Sharing((TripletRpLongVector[]) input, programmer, sender, maxLen);
        stopWatch1.stop();
        logStepInfo(PtoState.PTO_STEP, "switchNetwork", 1, 5, stopWatch1.getTime(TimeUnit.MILLISECONDS), "trans3To2Sharing");
        stopWatch1.reset();

        // shuffle
        stopWatch1.start();
        d2p = permuteNetworkImplWithData(d2p, twoPer[0], maxLen, programmer, sender, receiver);
        stopWatch1.stop();
        logStepInfo(PtoState.PTO_STEP, "switchNetwork", 2, 5, stopWatch1.getTime(TimeUnit.MILLISECONDS), "first permute");
        stopWatch1.reset();
        // dup network
        stopWatch1.start();
        long[][] d2pData = d2p == null ? null : Arrays.stream(d2p).map(LongVector::getElements).toArray(long[][]::new);
        d2pData = duplicateNetworkImplWithData(d2pData, flag, input.length, targetLen, programmer, receiver, sender);
        d2p = d2pData == null ? null : Arrays.stream(d2pData).map(LongVector::create).toArray(LongVector[]::new);
        stopWatch1.stop();
        logStepInfo(PtoState.PTO_STEP, "switchNetwork", 3, 5, stopWatch1.getTime(TimeUnit.MILLISECONDS), "duplicate network");
        stopWatch1.reset();
        // shuffle
        stopWatch1.start();
        d2p = this.permuteNetworkImplWithData(d2p, twoPer[1], targetLen, programmer, sender, receiver);
        stopWatch1.stop();
        logStepInfo(PtoState.PTO_STEP, "switchNetwork", 4, 5, stopWatch1.getTime(TimeUnit.MILLISECONDS), "second permute");
        stopWatch1.reset();
        // reconstruct
        stopWatch1.start();
        MpcLongVector[] res = trans2To3Sharing(d2p, programmer, receiver, IntStream.range(0, input.length).map(i -> targetLen).toArray());
        stopWatch1.stop();
        logStepInfo(PtoState.PTO_STEP, "switchNetwork", 5, 5, stopWatch1.getTime(TimeUnit.MILLISECONDS), "trans2To3Sharing");
        stopWatch1.reset();

        logPhaseInfo(PtoState.PTO_END);
        return res;
    }

    @Override
    public MpcLongVector[] permuteNetwork(MpcLongVector[] input, int[] fun, int targetLen,
                                          Party programmer, Party sender, Party receiver) throws MpcAbortException {
        LongVector[] d2p = trans3To2Sharing((TripletRpLongVector[]) input, programmer, sender, targetLen);
        int maxLength = Math.max(input[0].getNum(), targetLen);
        d2p = this.permuteNetworkImplWithData(d2p, fun, maxLength, programmer, sender, receiver);
        return trans2To3Sharing(d2p, programmer, receiver, IntStream.range(0, input.length).map(i -> targetLen).toArray());
    }

    @Override
    public TripletRpLongVector[] duplicateNetwork(MpcLongVector[] input, boolean[] flag, Party programmer) {
        Party sender = rpc.getParty((programmer.getPartyId() + 1) % 3);
        Party receiver = rpc.getParty((programmer.getPartyId() + 2) % 3);
        LongVector[] d2p = trans3To2Sharing((TripletRpLongVector[]) input, programmer, sender, input[0].getNum());
        long[][] d2pLong = d2p == null ? null : Arrays.stream(d2p).map(LongVector::getElements).toArray(long[][]::new);
        d2pLong = duplicateNetworkImplWithData(d2pLong, flag, input.length, input[0].getNum(), programmer, sender, receiver);
        return trans2To3Sharing(d2pLong == null ? null : Arrays.stream(d2pLong).map(LongVector::create).toArray(LongVector[]::new),
            programmer, receiver, Arrays.stream(input).mapToInt(MpcVector::getNum).toArray());
    }

    public long[][] duplicateNetworkImplWithData(long[][] x, boolean[] flag, int dataDim, int dataLen,
                                                 Party programmer, Party sender, Party receiver) {
        int[] maskMatrixDataNum = IntStream.range(0, dataDim << 1).map(i -> dataLen - 1).toArray();
        int[] bMatrixDataNum = IntStream.range(0, dataDim).map(i -> dataLen).toArray();
        if (rpc.ownParty().equals(sender)) {
            // 1. 先采样phi，因为后面要发送给role=0的用户，故直接关联随机数生成
            BitVector phiVec = crProvider.randBitVector(new int[]{dataLen}, programmer)[0];
            boolean[] phi = BinaryUtils.uncheckByteArrayToBinary(phiVec.getBytes(), dataLen - 1);
            // 2. 再采样W^0, W^1，维度是(2 * dataDim, dataLen - 1)，因为后面要发送给role=1的用户，故直接关联随机数生成
            long[][] wMatrix = crProvider.getRandLongArrays(maskMatrixDataNum, receiver);
            // 3. 再采样<B>_1和<B_1>_0,   <B>_1维度是(dataDim, dataLen)，因为后面要发送给role=1的用户，故直接关联随机数生成
            long[][] b1Matrix = crProvider.getRandLongArrays(bMatrixDataNum, receiver);
            long[] b0 = IntStream.range(0, dataDim).mapToLong(i -> x[i][0] - b1Matrix[i][0]).toArray();
            sendLong(PtoStep.DUPLICATE_MSG.ordinal(), programmer, b0);
            // 4. 两个matrix分别是 M^0 = <A_i>_1 - <B_i>_1 + W_i^{phi_1}, M^1 = <B_{i-1}>_1 - <B_i>_1 + W_i^{1 - phi_1}
            long[][] mMatrix = new long[2 * dataDim][dataLen - 1];
            IntStream intStream = parallel ? IntStream.range(0, dataDim).parallel() : IntStream.range(0, dataDim);
            intStream.forEach(i -> {
                int wPhiDim = i + dataDim;
                IntStream.range(0, dataLen - 1).forEach(j -> {
                    mMatrix[i][j] = x[i][j + 1] - b1Matrix[i][j + 1] + wMatrix[phi[j] ? wPhiDim : i][j];
                    mMatrix[wPhiDim][j] = b1Matrix[i][j] - b1Matrix[i][j + 1] + wMatrix[phi[j] ? i : wPhiDim][j];
                });
            });
            // 5. 将mMatrix，<B_1>_0发送给role=0的用户
            sendLong(PtoStep.DUPLICATE_MSG.ordinal(), programmer, mMatrix);
            return null;
        } else if (rpc.ownParty().equals(programmer)) {
            // 1. 关联随机数得到phi，得到rho= phi ^ flag, 并发送给role=1的用户
            BitVector phiVec = crProvider.randBitVector(new int[]{dataLen}, sender)[0];
            byte[] rhoByte = BytesUtils.xor(phiVec.getBytes(), BinaryUtils.binaryToRoundByteArray(flag));
            send(PtoStep.DUPLICATE_MSG.ordinal(), receiver, Collections.singletonList(rhoByte));
            // 2. 先预计算和role=1在最后一步随机化输出的randomness
            long[][] randomLast = crProvider.getRandLongArrays(bMatrixDataNum, receiver);
            // 3. 从role=2的用户接收 mMatrix，<B_1>_0; 从role=1的用户接收 wChoiceMatrix
            long[][] wChoiceMatrix = receiveLong(PtoStep.DUPLICATE_MSG.ordinal(), receiver);
            long[] b0 = receiveLong(PtoStep.DUPLICATE_MSG.ordinal(), sender)[0];
            long[][] mMatrix = receiveLong(PtoStep.DUPLICATE_MSG.ordinal(), sender);
            // 4.得到最终的结果，计算
            long[][] finalRes = new long[dataDim][dataLen];
            IntStream intStream = parallel ? IntStream.range(0, dataDim).parallel() : IntStream.range(0, dataDim);
            intStream.forEach(i -> {
                int copyIndex = i + dataDim;
                finalRes[i][0] = b0[i] + x[i][0];
                IntStream.range(1, dataLen).forEach(j -> {
                    int verIndex = j - 1;
                    if (flag[j]) {
                        finalRes[i][j] = mMatrix[copyIndex][verIndex] + finalRes[i][verIndex] - wChoiceMatrix[i][verIndex];
                    } else {
                        finalRes[i][j] = mMatrix[i][verIndex] + x[i][j] - wChoiceMatrix[i][verIndex];
                    }
                });
                IntStream.range(0, finalRes[i].length).forEach(j -> finalRes[i][j] -= randomLast[i][j]);
            });
            return finalRes;
        } else {
            // 1. 关联随机数生成W^0, W^1，维度是(2 * dataDim, dataLen - 1)
            long[][] wMatrix = crProvider.getRandLongArrays(maskMatrixDataNum, sender);
            // 2. 接收role=0用户的rho, 并根据rho发送 W_i^{rho_i}
            byte[] rhoByte = receive(PtoStep.DUPLICATE_MSG.ordinal(), programmer).get(0);
            boolean[] rho = BinaryUtils.uncheckByteArrayToBinary(rhoByte, dataLen - 1);
            long[][] wChoiceMatrix = new long[dataDim][dataLen - 1];
            IntStream intStream = parallel ? IntStream.range(0, dataDim).parallel() : IntStream.range(0, dataDim);
            intStream.forEach(i -> {
                int targetDim = i + dataDim;
                IntStream.range(0, dataLen - 1).forEach(j -> wChoiceMatrix[i][j] = wMatrix[rho[j] ? targetDim : i][j]);
            });
            sendLong(PtoStep.DUPLICATE_MSG.ordinal(), programmer, wChoiceMatrix);
            // 3. 生成和role=0的用户在最后一步随机化输出的randomness，并与关联随机数得到的<B>_1相加，得到自己的输出
            long[][] b1Matrix = crProvider.getRandLongArrays(bMatrixDataNum, sender);
            long[][] randomLast = crProvider.getRandLongArrays(bMatrixDataNum, programmer);
            intStream = parallel ? IntStream.range(0, dataDim).parallel() : IntStream.range(0, dataDim);
            intStream.forEach(i -> {
                for (int j = 0; j < dataLen; j++) {
                    b1Matrix[i][j] += randomLast[i][j];
                }
            });
            return b1Matrix;
        }
    }
}
