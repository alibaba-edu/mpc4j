package edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.ahi22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleOperations.ShuffleOp;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.ShuffleUtils;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.AbstractPermuteParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteParty;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Ahi22 oblivious permutation party
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class Ahi22PermuteParty extends AbstractPermuteParty implements PermuteParty {
    /**
     * shuffle Party
     */
    protected final ShuffleParty shuffleParty;
    /**
     * correlated randomness provider
     */
    protected final S3pcCrProvider crProvider;

    public Ahi22PermuteParty(Abb3Party abb3Party, Ahi22PermuteConfig config){
        super(Ahi22PermutePtoDesc.getInstance(), abb3Party, config);
        shuffleParty = abb3Party.getShuffleParty();
        crProvider = provider.getCrProvider();
    }

    /**
     * initialize the party
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

    @Override
    public long[] setUsage(PermuteFnParam... params){
        if(isMalicious){
            long bitTupleNum = 0;
            for(PermuteFnParam param : params){
                if(param.op.equals(PermuteOp.COMPOSE_B_B)){
                    bitTupleNum += shuffleParty.getTupleNum(ShuffleOp.B_INV_SHUFFLE_COLUMN,
                        param.dataNum, param.dataNum, param.dataDim + param.paiDim);
                }
                if(param.op.equals(PermuteOp.APPLY_INV_A_B)){
                    bitTupleNum += shuffleParty.getTupleNum(ShuffleOp.B_SHUFFLE_COLUMN,
                        param.dataNum, param.dataNum, param.dataDim);
                }
                if(param.op.equals(PermuteOp.APPLY_INV_B_B)){
                    bitTupleNum += shuffleParty.getTupleNum(ShuffleOp.B_SHUFFLE_COLUMN,
                        param.dataNum, param.dataNum, param.dataDim + param.paiDim);
                }
            }
            abb3Party.updateNum(bitTupleNum, 0);
            return new long[]{bitTupleNum, 0};
        }else{
            return new long[]{0, 0};
        }
    }

    @Override
    public TripletLongVector[] composePermutation(TripletLongVector pai, TripletLongVector... sigma) throws MpcAbortException {
        checkInput(pai, sigma);
        logPhaseInfo(PtoState.PTO_BEGIN, "COMPOSE_A_A");

        stopWatch.start();
        int len = pai.getNum();
        // 1. 生成一个随机的share permutation vector \mu
        int[][] randWires = crProvider.getRandIntArray(len);
        int[][] mu = new int[2][];
        mu[0] = ShuffleUtils.permutationGeneration(randWires[0]);
        mu[1] = ShuffleUtils.permutationGeneration(randWires[1]);
        // 2. paiAfter = shuffle(\mu, pai)
        long[] plainPaiAfter = shuffleParty.shuffleOpen(mu, pai)[0].getElements();
        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime());

        stopWatch.start();
        // 4. 用paiAfter置换sigma得到 sigmaAfter
        int[] plainPaiAfterInt = Arrays.stream(plainPaiAfter).mapToInt(Math::toIntExact).toArray();
        TripletLongVector[] sigmaAfter = ShuffleUtils.applyPermutationToRows(sigma, plainPaiAfterInt);
        // 5. shuffle(\mu^-1, sigmaAfter)
        TripletLongVector[] res = (TripletLongVector[]) shuffleParty.invShuffle(mu, sigmaAfter);
        logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END, "COMPOSE_A_A");
        return res;
    }

    @Override
    public TripletZ2Vector[] composePermutation(TripletZ2Vector[] pai, TripletZ2Vector[] sigma) throws MpcAbortException {
        checkInput(pai, sigma);
        logPhaseInfo(PtoState.PTO_BEGIN, "COMPOSE_B_B");

        stopWatch.start();
        int len = pai[0].getNum();
        // 1. 生成一个随机的share permutation vector \mu
        int[][] randWires = crProvider.getRandIntArray(len);
        int[][] mu = new int[2][];
        mu[0] = ShuffleUtils.permutationGeneration(randWires[0]);
        mu[1] = ShuffleUtils.permutationGeneration(randWires[1]);
        // 2. paiAfter = shuffle(\mu, pai)
        TripletZ2Vector[] shuffleRes = (TripletZ2Vector[]) shuffleParty.shuffleColumn(mu, pai);
        BitVector[] openPaiBinary = z2cParty.open(shuffleRes);
        int[] plainPaiAfter = Arrays.stream(ZlDatabase.create(envType, parallel, openPaiBinary).getBigIntegerData())
            .mapToInt(BigInteger::intValue).toArray();
        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime());

        stopWatch.start();
        // 4. 用paiAfter置换sigma得到 sigmaAfter
        TripletZ2Vector[] sigmaAfter = ShuffleUtils.applyPermutationToRows(sigma, plainPaiAfter);
        // 5. shuffle(\mu^-1, sigmaAfter)
        TripletZ2Vector[] res = (TripletZ2Vector[]) shuffleParty.invShuffleColumn(mu, sigmaAfter);
        logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END, "COMPOSE_B_B");
        return res;
    }

    @Override
    public TripletZ2Vector[] applyInvPermutation(TripletZ2Vector[] pai, TripletZ2Vector[] x) throws MpcAbortException {
        checkInput(pai, x);

        logPhaseInfo(PtoState.PTO_BEGIN, "APPLY_INV_B_B");
        stopWatch.start();
        int len = pai[0].getNum();
        // 1. 生成一个随机的share permutation vector \sigma
        int[][] randWires = crProvider.getRandIntArray(len);
        int[][] mu = new int[2][];
        mu[0] = ShuffleUtils.permutationGeneration(randWires[0]);
        mu[1] = ShuffleUtils.permutationGeneration(randWires[1]);
        // 2. 用sigma置换pai和x得到paiAfter和xAfter
        TripletZ2Vector[] input = new TripletZ2Vector[pai.length + x.length];
        System.arraycopy(pai, 0, input, 0, pai.length);
        System.arraycopy(x, 0, input, pai.length, x.length);
        TripletZ2Vector[] shuffleRes = (TripletZ2Vector[]) shuffleParty.shuffleColumn(mu, input);
        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime());

        stopWatch.start();
        // 3. open paiAfter
        BitVector[] openPaiBinary = z2cParty.open(Arrays.copyOf(shuffleRes, pai.length));
        int[] plainPaiAfter = Arrays.stream(ZlDatabase.create(envType, parallel, openPaiBinary).getBigIntegerData())
            .mapToInt(BigInteger::intValue).toArray();
        // 4. 用 inverse of paiAfter 置换x
        int[] invPaiAfter = ShuffleUtils.invOfPermutation(plainPaiAfter);
        TripletZ2Vector[] res = ShuffleUtils.applyPermutationToRows(
            Arrays.copyOfRange(shuffleRes, pai.length, shuffleRes.length), invPaiAfter);
        logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END, "APPLY_INV_B_B");
        return res;
    }

    @Override
    public TripletLongVector[] applyInvPermutation(TripletLongVector pai, TripletLongVector... x) throws MpcAbortException {
        checkInput(pai, x);

        logPhaseInfo(PtoState.PTO_BEGIN, "APPLY_INV_A_A");
        stopWatch.start();
        int len = pai.getNum();
        // 1. 生成一个随机的share permutation vector \sigma
        int[][] randWires = crProvider.getRandIntArray(len);
        int[][] mu = new int[2][];
        mu[0] = ShuffleUtils.permutationGeneration(randWires[0]);
        mu[1] = ShuffleUtils.permutationGeneration(randWires[1]);
        // 2. 用sigma置换pai和x得到paiAfter和xAfter
        TripletLongVector[] shuffleInput = new TripletLongVector[x.length + 1];
        System.arraycopy(x, 0, shuffleInput, 0, x.length);
        shuffleInput[x.length] = pai;
        TripletLongVector[] result = (TripletLongVector[]) shuffleParty.shuffle(mu, shuffleInput);
        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime());

        stopWatch.start();
        // 3. open paiAfter
        long[] plainPaiAfter = zl64cParty.open(result[x.length])[0].getElements();
        // 4. 用 inverse of paiAfter 置换x
        int[] plainPaiAfterInt = new int[len];
        IntStream.range(0, len).forEach(index ->
            plainPaiAfterInt[index] = Math.toIntExact(plainPaiAfter[index]));
        // 4.1 得到invPaiAfter
        int[] invPaiAfter = ShuffleUtils.invOfPermutation(plainPaiAfterInt);
        TripletLongVector[] res = ShuffleUtils.applyPermutationToRows(
            Arrays.copyOfRange(result, 0, x.length), invPaiAfter);
        logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END, "APPLY_INV_A_A");
        return res;
    }

    @Override
    public TripletZ2Vector[] applyInvPermutation(TripletLongVector pai, TripletZ2Vector[] x) throws MpcAbortException {
        checkInput(pai, x);
        logPhaseInfo(PtoState.PTO_BEGIN, "APPLY_INV_A_B");

        stopWatch.start();
        int len = pai.getNum();
        // 1. 生成一个随机的share permutation vector \sigma
        int[][] randWires = crProvider.getRandIntArray(len);
        int[][] mu = new int[2][];
        mu[0] = ShuffleUtils.permutationGeneration(randWires[0]);
        mu[1] = ShuffleUtils.permutationGeneration(randWires[1]);
        // 2. 用sigma置换pai和x得到paiAfter和xAfter
        TripletLongVector aRes = (TripletLongVector) shuffleParty.shuffle(mu, pai)[0];
        TripletZ2Vector[] bRes = (TripletZ2Vector[]) shuffleParty.shuffleColumn(mu, x);
        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime());

        stopWatch.start();
        // 3. open paiAfter
        long[] plainPaiAfter = zl64cParty.open(aRes)[0].getElements();
        // 4. 用 inverse of paiAfter 置换x
        int[] plainPaiAfterInt = new int[len];
        IntStream.range(0, len).forEach(index -> plainPaiAfterInt[index] = Math.toIntExact(plainPaiAfter[index]));
        // 4.1 得到invPaiAfter
        int[] invPaiAfter = ShuffleUtils.invOfPermutation(plainPaiAfterInt);
        TripletZ2Vector[] res = ShuffleUtils.applyPermutationToRows(bRes, invPaiAfter);
        logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END, "APPLY_INV_A_B");
        return res;
    }
}
