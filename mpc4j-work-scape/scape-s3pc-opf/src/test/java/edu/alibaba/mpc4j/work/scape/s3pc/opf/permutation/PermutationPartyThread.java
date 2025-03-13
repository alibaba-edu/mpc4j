package edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation;

import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.ShuffleUtils;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.AcPermuteRes;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.BcPermuteRes;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteOp;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 3p permutation party thread
 *
 * @author Feng Han
 * @date 2024/03/01
 */
public class PermutationPartyThread extends Thread{
    private static final Logger LOGGER = LoggerFactory.getLogger(PermutationPartyThread.class);
    /**
     * permute party
     */
    private final PermuteParty permuteParty;
    /**
     * test data size
     */
    private final int dataNum;
    /**
     * dimension length in bit
     */
    private final int bitDim;
    /**
     * dimension length of input long element
     */
    private final int longDim;
    /**
     * operations to be tested
     */
    private final PermuteOp[] ops;
    /**
     * result of arithmetic permute
     */
    private final HashMap<PermuteOp, AcPermuteRes> aopMap;
    /**
     * result of binary permute
     */
    private final HashMap<PermuteOp, BcPermuteRes> bopMap;

    public PermutationPartyThread(PermuteParty permuteParty, int dataNum, int dataDim, PermuteOp[] ops) {
        this.permuteParty = permuteParty;
        this.dataNum = dataNum;
        longDim = dataDim;
        bitDim = longDim << 6;
        this.ops = ops;
        aopMap = new HashMap<>();
        bopMap = new HashMap<>();
    }

    public BcPermuteRes getBcPermuteRes(PermuteOp op) {
        Assert.assertTrue(bopMap.containsKey(op));
        return bopMap.get(op);
    }

    public AcPermuteRes getAcPermuteRes(PermuteOp op) {
        Assert.assertTrue(aopMap.containsKey(op));
        return aopMap.get(op);
    }

    public void testAcPermute(PermuteOp op) throws MpcAbortException {
        LOGGER.info("testing {}", op.toString());
        LongVector[] plainData = null;
        int[] pai = null;
        TripletLongVector paiShare;
        TripletLongVector[] input;
        MpcLongVector[] output;
        SecureRandom secureRandom = new SecureRandom();
        if (permuteParty.getRpc().ownParty().getPartyId() == 0) {
            plainData = IntStream.range(0, longDim).parallel().mapToObj(i ->
                LongVector.createRandom(dataNum, secureRandom)).toArray(LongVector[]::new);
            int[] r = IntStream.range(0, dataNum).map(i -> secureRandom.nextInt()).toArray();
            pai = ShuffleUtils.permutationGeneration(r);
            LongVector plainPai = LongVector.create(Arrays.stream(pai).mapToLong(x -> (long) x).toArray());
            input = (TripletLongVector[]) permuteParty.getAbb3Party().getLongParty().shareOwn(plainData);
            paiShare = (TripletLongVector) permuteParty.getAbb3Party().getLongParty().shareOwn(plainPai);
        } else {
            input = (TripletLongVector[]) permuteParty.getAbb3Party().getLongParty().shareOther(
                IntStream.range(0, longDim).map(i -> dataNum).toArray(), permuteParty.getRpc().getParty(0));
            paiShare = (TripletLongVector) permuteParty.getAbb3Party().getLongParty().shareOther(
                dataNum, permuteParty.getRpc().getParty(0));
        }
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        switch (op) {
            case COMPOSE_A_A: {
                output = permuteParty.composePermutation(paiShare, input);
                break;
            }
            case APPLY_INV_A_A: {
                output = permuteParty.applyInvPermutation(paiShare, input);
                break;
            }
            default:
                throw new IllegalArgumentException(op + " is not an arithmetic permute operation");
        }
        permuteParty.getAbb3Party().checkUnverified();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        LOGGER.info("P{} op:[{}] process time: {}ms", permuteParty.getRpc().ownParty().getPartyId(), op.name(), time);
        if(permuteParty.getRpc().ownParty().getPartyId() == 0){
            LongVector[] outputPlain = permuteParty.getAbb3Party().getLongParty().open(output);
            aopMap.put(op, new AcPermuteRes(pai, plainData, outputPlain));
        }else{
            permuteParty.getAbb3Party().getLongParty().open(output);
        }
    }

    public void testBcPermute(PermuteOp op) throws MpcAbortException {
        LOGGER.info("testing {}", op.toString());
        int paiDim = LongUtils.ceilLog2(dataNum);
        BitVector[] plainData = null;
        TripletZ2Vector[] input;
        TripletZ2Vector[] output;
        int[] pai = null;
        TripletLongVector paiShareA;
        TripletZ2Vector[] paiShareB;
        SecureRandom secureRandom = new SecureRandom();
        if (permuteParty.getRpc().ownParty().getPartyId() == 0) {
            plainData = IntStream.range(0, bitDim).parallel().mapToObj(i ->
                BitVectorFactory.createRandom(dataNum, secureRandom)).toArray(BitVector[]::new);
            int[] r = IntStream.range(0, dataNum).map(i -> secureRandom.nextInt()).toArray();
            pai = ShuffleUtils.permutationGeneration(r);
            LongVector plainPai = LongVector.create(Arrays.stream(pai).mapToLong(x -> (long) x).toArray());
            input = permuteParty.getAbb3Party().getZ2cParty().shareOwn(plainData);
            paiShareA = (TripletLongVector) permuteParty.getAbb3Party().getLongParty().shareOwn(plainPai);
            BitVector[] tmp = ZlDatabase.create(32, Arrays.stream(pai).mapToObj(IntUtils::intToByteArray).toArray(byte[][]::new))
                .bitPartition(EnvType.STANDARD, true);
            paiShareB = permuteParty.getAbb3Party().getZ2cParty().shareOwn(Arrays.copyOfRange(tmp, 32 - paiDim, 32));
        } else {
            input = permuteParty.getAbb3Party().getZ2cParty().shareOther(
                IntStream.range(0, bitDim).map(i -> dataNum).toArray(), permuteParty.getRpc().getParty(0));
            paiShareA = (TripletLongVector) permuteParty.getAbb3Party().getLongParty().shareOther(
                dataNum, permuteParty.getRpc().getParty(0));
            paiShareB = permuteParty.getAbb3Party().getZ2cParty().shareOther(
                IntStream.range(0, paiDim).map(i -> dataNum).toArray(), permuteParty.getRpc().getParty(0));
        }
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        switch (op) {
            case COMPOSE_B_B:{
                output = permuteParty.composePermutation(paiShareB, input);
                break;
            }
            case APPLY_INV_B_B: {
                output = permuteParty.applyInvPermutation(paiShareB, input);
                break;
            }
            case APPLY_INV_A_B: {
                output = permuteParty.applyInvPermutation(paiShareA, input);
                break;
            }
            default:
                throw new IllegalArgumentException(op + " is not an binary permute operation");
        }
        permuteParty.getAbb3Party().checkUnverified();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        LOGGER.info("P{} op:[{}] process time: {}ms", permuteParty.getRpc().ownParty().getPartyId(), op.name(), time);
        if(permuteParty.getRpc().ownParty().getPartyId() == 0){
            BitVector[] outputPlain = permuteParty.getAbb3Party().getZ2cParty().open(output);
            bopMap.put(op, new BcPermuteRes(pai, plainData, outputPlain));
        }else{
            permuteParty.getAbb3Party().getZ2cParty().open(output);
        }
    }

    @Override
    public void run() {
        int bitPaiLen = LongUtils.ceilLog2(dataNum);
        PermuteFnParam[] params = new PermuteFnParam[ops.length];
        IntStream.range(0, ops.length).forEach(i -> params[i] = new PermuteFnParam(ops[i], dataNum, bitDim, bitPaiLen));

        try {
            long computedBitTupleNum = permuteParty.setUsage(params)[0];
            permuteParty.init();

            for (PermuteOp op : ops) {
                switch (op) {
                    case COMPOSE_A_A:
                    case APPLY_INV_A_A:
                        testAcPermute(op);
                        break;
                    case COMPOSE_B_B:
                    case APPLY_INV_A_B:
                    case APPLY_INV_B_B:
                        testBcPermute(op);
                        break;
                    default:
                        throw new IllegalArgumentException(op + " is not a permute operation");
                }
            }
            long usedBitTuple = computedBitTupleNum == 0 ? 0 : permuteParty.getAbb3Party().getTripletProvider().getZ2MtProvider().getAllTupleNum();
            LOGGER.info("computed bitTupleNum:{}, actually used bitTupleNum:{}", computedBitTupleNum, usedBitTuple);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
