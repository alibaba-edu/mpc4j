package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.radix;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvOperations.ConvOp;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.ShuffleUtils;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.AbstractPgSortParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations.PgSortFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortParty;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * oblivious radix sorting party
 *
 * @author Feng Han
 * @date 2024/02/27
 */
public class RadixPgSortParty extends AbstractPgSortParty implements PgSortParty {
    /**
     * shuffle party
     */
    private final ShuffleParty shuffleParty;
    /**
     * permute party
     */
    private final PermuteParty permuteParty;

    public RadixPgSortParty(Abb3Party abb3Party, RadixPgSortConfig config) {
        super(RadixPgSortPtoDesc.getInstance(), abb3Party, config);
        shuffleParty = abb3Party.getShuffleParty();
        permuteParty = PermuteFactory.createParty(abb3Party, config.getPermuteConfig());
        addMultiSubPto(permuteParty);
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
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
            // 2. sorting process
            int[] param = getBatch(sumBitNum);
            // 2.1 cost of bit2A and init permutation generation
            longTupleNum += abb3Party.getConvParty().getTupleNum(ConvOp.BIT2A, dataNum, sumBitNum, 1)[1];
            longTupleNum += (long) dataNum * getMulNumParam(param[0]);
            // 2.2 cost of permute binary sharing
            for (int i = 1; i < param.length; i++) {
                permuteParty.setUsage(new PermuteFnParam(PermuteOp.APPLY_INV_A_B, dataNum, param[i], 64));
                longTupleNum += (long) dataNum * getMulNumParam(param[i]);
            }
            if (funParam.op.name().endsWith("_A")) {
                // 1. the cost of a2b
                for (int bit : funParam.dims) {
                    bitTupleNum += abb3Party.getConvParty().getTupleNum(ConvOp.A2B, dataNum, 1, bit)[0];
                }
            }
            if (funParam.op.name().startsWith("SORT_PERMUTE")) {
                // 3. the cost of last permutation of  binary sharing
                permuteParty.setUsage(new PermuteFnParam(PermuteOp.APPLY_INV_A_B, dataNum, sumBitNum, 64));
            }
        }
        abb3Party.updateNum(bitTupleNum, longTupleNum);
        return new long[]{bitTupleNum, longTupleNum};
    }

    /**
     * sorting plan
     *
     * @param totalLen the input bit length
     */
    private int[] getBatch(int totalLen) {
        if (totalLen <= 3) {
            return new int[]{totalLen};
        }
        int remainder = totalLen % 3;
        int[] res = new int[totalLen / 3 + (remainder > 0 ? 1 : 0)];
        Arrays.fill(res, 3);
        if (remainder > 0) {
            res[res.length - 1] = 2;
        }
        if (remainder == 1) {
            res[res.length - 2] = 2;
        }
        return res;
    }

    /**
     * get the number of multiplication gate
     *
     * @param dim the dimension of input data
     */
    private int getMulNumParam(int dim) {
        switch (dim) {
            case 1:
                return 1;
            case 2:
                return 5;
            case 3:
                return 12;
            default:
                throw new IllegalArgumentException("dim > 3");
        }
    }

    @Override
    public TripletLongVector perGen4MultiDim(TripletLongVector[] input, int[] bitLens) throws MpcAbortException {
        MathPreconditions.checkEqual("input.length", "bitLens.length", input.length, bitLens.length);
        int maxBitNum = 1;
        for (int bit : bitLens) {
            MathPreconditions.checkInRangeClosed("1 <= bit <= 64", bit, 1, 64);
            maxBitNum = Math.max(maxBitNum, bit);
        }
        int totalBitNum = Arrays.stream(bitLens).sum();
        if (totalBitNum <= 3) {
            // if there is no need for Permutation
            if (maxBitNum == 1) {
                return sort(input);
            } else {
                TripletLongVector[] data = new TripletLongVector[totalBitNum];
                for (int i = 0, start = 0; i < input.length; i++) {
                    TripletZ2Vector[] tmp = abb3Party.getConvParty().a2b(input[i], bitLens[i]);
                    System.arraycopy(abb3Party.getConvParty().bit2a(tmp), 0, data, start, bitLens[i]);
                    start += bitLens[i];
                }
                return sort(data);
            }
        } else {
            // first transform all shared values into binary form, then invoke radix sorting
            TripletZ2Vector[] trans = new TripletZ2Vector[totalBitNum];
            for (int i = 0, start = 0; i < bitLens.length; i++) {
                System.arraycopy(abb3Party.getConvParty().a2b(input[i], bitLens[i]), 0, trans, start, bitLens[i]);
                start += bitLens[i];
            }
            return genPermutation(trans);
        }
    }

    @Override
    public TripletLongVector perGen4MultiDimWithOrigin(TripletLongVector[] input, int[] bitLens, TripletZ2Vector[] saveSortRes) throws MpcAbortException {
        int totalBitNum = Arrays.stream(bitLens).sum();
        MathPreconditions.checkEqual("saveSortRes.length", "totalBitNum", saveSortRes.length, totalBitNum);
        TripletZ2Vector[] trans = new TripletZ2Vector[totalBitNum];
        for (int i = 0, start = 0; i < bitLens.length; i++) {
            System.arraycopy(abb3Party.getConvParty().a2b(input[i], bitLens[i]), 0, trans, start, bitLens[i]);
            start += bitLens[i];
        }
        TripletLongVector pai = totalBitNum <= 3 ? perGen4MultiDim(input, bitLens) : genPermutation(trans);
        System.arraycopy(permuteParty.applyInvPermutation(pai, trans), 0, saveSortRes, 0, totalBitNum);
        return pai;
    }

    @Override
    public TripletZ2Vector[] perGen(TripletZ2Vector[] input) throws MpcAbortException {
        TripletLongVector pai = genPermutation(input);
        return abb3Party.getConvParty().a2b(pai, LongUtils.ceilLog2(pai.getNum()));
    }

    @Override
    public TripletZ2Vector[] perGenAndSortOrigin(TripletZ2Vector[] input) throws MpcAbortException {
        TripletLongVector pai = genPermutation(input);
        System.arraycopy(permuteParty.applyInvPermutation(pai, input), 0, input, 0, input.length);
        return abb3Party.getConvParty().a2b(pai, LongUtils.ceilLog2(pai.getNum()));
    }


    /**
     * the optimized sorting with batch
     *
     * @param data input binary data
     */
    public TripletLongVector genPermutation(TripletZ2Vector[] data) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int currentLastBit = data.length;
        int[] batch = getBatch(currentLastBit);
        TripletZ2Vector[] currentBits = Arrays.copyOfRange(data, currentLastBit - batch[0], currentLastBit);
        TripletLongVector[] aTrans = abb3Party.getConvParty().bit2a(currentBits);
        currentLastBit -= batch[0];
        TripletLongVector pai = sort(aTrans);
        logStepInfo(PtoState.PTO_STEP, "radix sort genPermutation", 0, batch.length, resetAndGetTime(), "batch-0");
        int[][] mu = new int[2][];
        int[] shuffledPai = new int[pai.getNum()];
        for (int i = 1; i < batch.length; i++) {
            stopWatch.start();
            // 1. apply inv permutation
            currentBits = Arrays.copyOfRange(data, currentLastBit - batch[i], currentLastBit);
            TripletZ2Vector[] nextBits = optApplyInvPerm(pai, currentBits, mu, shuffledPai);
            currentLastBit -= batch[i];
            // 2. get the new permutation
            aTrans = abb3Party.getConvParty().bit2a(nextBits);
            TripletLongVector rho = sort(aTrans);
            // 3. compose two permutation
            pai = optCompose(rho, shuffledPai, mu);
            logStepInfo(PtoState.PTO_STEP, "radix sort genPermutation", i, batch.length, resetAndGetTime(), "batch-" + i);
        }

        logPhaseInfo(PtoState.PTO_END);
        return pai;
    }

    /**
     * optimized apply inv permutation
     *
     * @param pai         the input shared permutation
     * @param x           the input shared values to be permuted in binary form
     * @param mu          the random permutation used in shuffle
     * @param shuffledPai the value of shuffled pai using mu
     */
    public TripletZ2Vector[] optApplyInvPerm(TripletLongVector pai, TripletZ2Vector[] x, int[][] mu, int[] shuffledPai) throws MpcAbortException {
        Preconditions.checkArgument(pai != null && x != null);
        MathPreconditions.checkEqual("x[0].getNum()", "pai.getNum()", pai.getNum(), x[0].getNum());
        MathPreconditions.checkEqual("mu.length", "2", mu.length, 2);
        MathPreconditions.checkEqual("shuffledPai.length", "pai.getNum()", shuffledPai.length, pai.getNum());
        int len = pai.getNum();
        // 1. generate a random share permutation vector \sigma
        int[][] randWires = abb3Party.getTripletProvider().getCrProvider().getRandIntArray(len);
        IntStream.range(0, 2).forEach(i -> mu[i] = ShuffleUtils.permutationGeneration(randWires[i]));
        // 2. permute pai, x with \sigma and get paiAfter, xAfter
        TripletZ2Vector[] bRes = (TripletZ2Vector[]) shuffleParty.shuffleColumn(mu, x);
        LongVector openARes;
        if (isMalicious) {
            TripletLongVector aRes = (TripletLongVector) shuffleParty.shuffle(mu, pai)[0];
            // 3. open paiAfter
            openARes = zl64cParty.open(aRes)[0];
        } else {
            openARes = shuffleParty.shuffleOpen(mu, pai)[0];
        }
        long[] plainPaiAfter = openARes.getElements();
        // 4. permute x with the inverse of paiAfter
        IntStream.range(0, len).forEach(index -> shuffledPai[index] = Math.toIntExact(plainPaiAfter[index]));
        // 4.1 get invPaiAfter
        int[] invPaiAfter = ShuffleUtils.invOfPermutation(shuffledPai);
        return ShuffleUtils.applyPermutationToRows(bRes, invPaiAfter);
    }

    /**
     * optimized compose permutation, return pai·sigma
     *
     * @param sigma       the input shared permutation
     * @param shuffledPai the value of shuffled pai using mu
     * @param mu          the random permutation used in shuffle
     */
    public TripletLongVector optCompose(TripletLongVector sigma, int[] shuffledPai, int[][] mu) throws MpcAbortException {
        TripletLongVector sigmaAfter = ShuffleUtils.applyPermutationToRows(sigma, shuffledPai);
        return (TripletLongVector) shuffleParty.invShuffle(mu, sigmaAfter)[0];
    }

    /**
     * get the permutation representing the stable sorting on input
     *
     * @param xiArray the input data, each element is 0 or 1
     */
    public TripletLongVector sort(TripletLongVector[] xiArray) {
        MathPreconditions.checkGreaterOrEqual("Number of input bits <= 3", 3, xiArray.length);

        TripletLongVector result;
        if (xiArray.length == 1) {
            result = execute(xiArray[0]);
        } else if (xiArray.length == 2) {
            result = execute2(xiArray[0], xiArray[1]);
        } else {
            result = execute3(xiArray[0], xiArray[1], xiArray[2]);
        }
        return result;
    }

    /**
     * get the permutation representing the stable sorting on one-bit input
     *
     * @param xi the input data, each element is 0 or 1
     */
    private TripletLongVector execute(TripletLongVector xi) {
        TripletLongVector ones = (TripletLongVector) zl64cParty.setPublicValue(LongVector.createOnes(xi.getNum()));
        TripletLongVector[] signs = new TripletLongVector[2];
        signs[1] = xi;
        signs[0] = zl64cParty.sub(ones, signs[1]);

        // prefix sum
        TripletLongVector[] indexes = computeIndex(signs);
        TripletLongVector res = zl64cParty.add(indexes[0], zl64cParty.mul(signs[1], zl64cParty.sub(indexes[1], indexes[0])));
        TripletLongVector plainOne = (TripletLongVector) zl64cParty.setPublicValue(LongVector.createOnes(signs[0].getNum()));
        return zl64cParty.sub(res, plainOne);
    }

    /**
     * get the permutation representing the stable sorting on two-bit input
     *
     * @param a the first input data, each element is 0 or 1
     * @param b the second input data, each element is 0 or 1
     */
    private TripletLongVector execute2(TripletLongVector a, TripletLongVector b) {
        // compute sign for 00， 01， 10， 11
        TripletLongVector arithmeticOnes = (TripletLongVector) zl64cParty.setPublicValue(LongVector.createOnes(a.getNum()));
        TripletLongVector[] signs = new TripletLongVector[4];
        signs[3] = zl64cParty.mul(a, b);
        signs[2] = zl64cParty.sub(a, signs[3]);
        signs[1] = zl64cParty.sub(b, signs[3]);
        signs[0] = zl64cParty.sub(zl64cParty.sub(arithmeticOnes, a), signs[1]);

        // prefix sum
        TripletLongVector[] indexes = computeIndex(signs);

        return mulWithAdd(signs, indexes);
    }

    /**
     * get the permutation representing the stable sorting on three-bit input
     *
     * @param a the first input data, each element is 0 or 1
     * @param b the second input data, each element is 0 or 1
     * @param c the third input data, each element is 0 or 1
     */
    private TripletLongVector execute3(TripletLongVector a, TripletLongVector b, TripletLongVector c) {
        // compute 8 signs, corresponding to 000 ~ 111
        TripletLongVector[] signs = new TripletLongVector[8];
        TripletLongVector[] firstMulRes = zl64cParty.mul(new TripletLongVector[]{a, b, c}, new TripletLongVector[]{b, c, a});
        signs[7] = zl64cParty.mul(firstMulRes[0], c);
        signs[6] = zl64cParty.sub(firstMulRes[0], signs[7]);
        signs[5] = zl64cParty.sub(firstMulRes[2], signs[7]);
        signs[4] = zl64cParty.sub(a, zl64cParty.add(firstMulRes[0], signs[5]));
        signs[3] = zl64cParty.sub(firstMulRes[1], signs[7]);
        signs[2] = zl64cParty.sub(b, zl64cParty.add(firstMulRes[0], signs[3]));
        signs[1] = zl64cParty.sub(c, zl64cParty.add(firstMulRes[1], signs[5]));
        TripletLongVector invAInvB = zl64cParty.add(zl64cParty.sub(firstMulRes[0], zl64cParty.add(a, b)), zl64cParty.setPublicValue(LongVector.createOnes(a.getNum())));
        signs[0] = zl64cParty.sub(invAInvB, signs[1]);

        // prefix sum
        TripletLongVector[] indexes = computeIndex(signs);

        return mulWithAdd(signs, indexes);
    }

    private TripletLongVector[] computeIndex(TripletLongVector[] signs) {
        // prefix sum
        TripletLongVector zeroPlain = zl64cParty.createZeros(1);
        TripletLongVector[] indexes = new TripletLongVector[signs.length];
        for (int i = 0; i < signs.length; i++) {
            indexes[i] = zl64cParty.rowAdderWithPrefix(signs[i], zeroPlain, false);
        }
        return indexes;
    }

    private TripletLongVector mulWithAdd(TripletLongVector[] a, TripletLongVector[] b) {
        TripletLongVector[] mulRes = zl64cParty.mul(a, b);
        for (int i = 1; i < mulRes.length; i++) {
            zl64cParty.addi(mulRes[0], mulRes[i]);
        }
        TripletLongVector plainOne = (TripletLongVector) zl64cParty.setPublicValue(LongVector.createOnes(mulRes[0].getNum()));
        return zl64cParty.sub(mulRes[0], plainOne);
    }
}

