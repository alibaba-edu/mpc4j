package edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleOperations.AcShuffleRes;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleOperations.BcShuffleRes;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleOperations.ShuffleOp;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.replicate.Aby3ShuffleParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.ShuffleUtils;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * aby3 shuffle computation party thread
 *
 * @author Feng Han
 * @date 2024/02/02
 */
public class Aby3ShufflePartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(Aby3ShufflePartyThread.class);
    private final Aby3ShuffleParty shuffleParty;
    private final int bitNum;
    private final int bitDim;
    private final int longNum;
    private final int longDim;
    private final ShuffleOp[] ops;
    private final HashMap<ShuffleOp, BcShuffleRes[]> bopMap;
    private final HashMap<ShuffleOp, AcShuffleRes[]> aopMap;

    public Aby3ShufflePartyThread(Aby3ShuffleParty shuffleParty, int[] bitParam, int[] longParam, ShuffleOp[] ops) {
        this.shuffleParty = shuffleParty;
        bitNum = bitParam[0];
        bitDim = bitParam[1];
        longNum = longParam[0];
        longDim = longParam[1];
        this.ops = ops;
        bopMap = new HashMap<>();
        aopMap = new HashMap<>();
    }

    public BcShuffleRes[] getBcShuffleRes(ShuffleOp op) {
        Assert.assertTrue(bopMap.containsKey(op));
        return bopMap.get(op);
    }

    public AcShuffleRes[] getAcShuffleRes(ShuffleOp op) {
        Assert.assertTrue(aopMap.containsKey(op));
        return aopMap.get(op);
    }

    public long[] getTupleNum(int[] bitOutputLen, int[] longOutputLen) {
        long bitTupleNum = 0, longTupleNum = 0;
        for (ShuffleOp op : ops) {
            if(op.name().startsWith("A_")){
                if(op.equals(ShuffleOp.A_PERMUTE_NETWORK) || op.equals(ShuffleOp.A_SWITCH_NETWORK)){
                    for (int outputLen : longOutputLen) {
                        longTupleNum += shuffleParty.getTupleNum(op, longNum, outputLen, longDim);
                    }
                }else{
                    longTupleNum += shuffleParty.getTupleNum(op, longNum, longNum, longDim);
                }
            }else{
                if(op.equals(ShuffleOp.B_PERMUTE_NETWORK) || op.equals(ShuffleOp.B_SWITCH_NETWORK)){
                    for (int outputLen : bitOutputLen) {
                        bitTupleNum += shuffleParty.getTupleNum(op, bitNum, outputLen, bitDim);
                    }
                }else{
                    bitTupleNum += shuffleParty.getTupleNum(op, bitNum, bitNum, bitDim);
                }
            }
        }
        return new long[]{bitTupleNum, longTupleNum};
    }

    private int[][] genOutputLen(){
        int[] bitOutputLen = new int[3];
        int[] longOutputLen = new int[3];
        bitOutputLen[0] = bitNum;
        longOutputLen[0] = longNum;
        Rpc rpc = shuffleParty.getRpc();
        if(rpc.ownParty().getPartyId() == 0){
            SecureRandom secureRandom = new SecureRandom();
            bitOutputLen[1] = secureRandom.nextInt(bitNum / 2) + bitNum / 2;
            bitOutputLen[2] = secureRandom.nextInt(bitNum / 2) + bitNum;
            longOutputLen[1] = secureRandom.nextInt(longNum / 2) + longNum / 2;
            longOutputLen[2] = secureRandom.nextInt(longNum / 2) + longNum;
            DataPacketHeader header1 = new DataPacketHeader(0, 0, 0, 0, 1);
            DataPacketHeader header2 = new DataPacketHeader(0, 0, 0, 0, 2);
            byte[] sendData = IntUtils.intArrayToByteArray(new int[]{bitOutputLen[1], bitOutputLen[2], longOutputLen[1], longOutputLen[2]});
            rpc.send(DataPacket.fromByteArrayList(header1, Collections.singletonList(sendData)));
            rpc.send(DataPacket.fromByteArrayList(header2, Collections.singletonList(sendData)));
        }else{
            DataPacketHeader header = new DataPacketHeader(0, 0, 0, 0, rpc.ownParty().getPartyId());
            int[] params = IntUtils.byteArrayToIntArray(rpc.receive(header).getPayload().get(0));
            for(int i = 0; i < 2; i++){
                bitOutputLen[i + 1] = params[i];
                longOutputLen[i + 1] = params[i + 2];
            }
        }
        return new int[][]{bitOutputLen, longOutputLen};
    }

    @Override
    public void run() {
        int[][] params = genOutputLen();

        int[] bitOutputLen = params[0];
        int[] longOutputLen = params[1];

        long[] tupleNum = getTupleNum(bitOutputLen, longOutputLen);

        try {
            TripletProvider provider = shuffleParty.getProvider();
            provider.init(tupleNum[0], tupleNum[1]);
            shuffleParty.init();

            for (ShuffleOp op : ops) {
                switch (op) {
                    case A_SWITCH_NETWORK:
                    case A_INV_SHUFFLE:
                    case A_SHUFFLE:
                    case A_PERMUTE_NETWORK:
                    case A_DUPLICATE_NETWORK:
                        testAcShuffle(op, longOutputLen);
                        break;
                    case A_SHUFFLE_OPEN:
                        testAcShuffleOpen(op);
                        break;
                    case B_SHUFFLE_COLUMN:
                    case B_SHUFFLE_ROW:
                    case B_SWITCH_NETWORK:
                    case B_PERMUTE_NETWORK:
                    case B_DUPLICATE_NETWORK:
                        testBcShuffle(op, bitOutputLen);
                }
            }
            long actualUsedBit = provider.getZ2MtProvider() == null ? 0 : provider.getZ2MtProvider().getAllTupleNum();
            long actualUsedLong = provider.getZl64MtProvider() == null ? 0 : provider.getZl64MtProvider().getAllTupleNum();
            if(shuffleParty.getRpc().ownParty().getPartyId() == 0){
                LOGGER.info("computed bitTupleNum:{}, actually used bitTupleNum:{} " +
                    "| computed longTupleNum:{}, actually used longTupleNum:{} ",
                    tupleNum[0], actualUsedBit, tupleNum[1], actualUsedLong);
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }


    public void testAcShuffle(ShuffleOp op, int[] longOutputLen) throws MpcAbortException {
        LOGGER.info("testing {}", op.toString());
        TripletProvider tripletProvider = shuffleParty.getProvider();
        LongVector[] plainData = null;
        TripletRpLongVector[] input;
        MpcLongVector[][] output;
        int[][] fun = new int[longOutputLen.length][];
        boolean[] flag = null;
        SecureRandom secureRandom = new SecureRandom();
        if (shuffleParty.getRpc().ownParty().getPartyId() == 0) {
            plainData = IntStream.range(0, longDim).parallel().mapToObj(i ->
                LongVector.createRandom(longNum, secureRandom)).toArray(LongVector[]::new);
            input = (TripletRpLongVector[]) shuffleParty.getZl64cParty().shareOwn(plainData);
        } else {
            input = (TripletRpLongVector[]) shuffleParty.getZl64cParty().shareOther(
                IntStream.range(0, longDim).map(i -> longNum).toArray(), shuffleParty.getRpc().getParty(0));
        }
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        switch (op) {
            case A_SHUFFLE: {
                output = new MpcLongVector[][]{shuffleParty.shuffle(input)};
                break;
            }
            case A_INV_SHUFFLE: {
                int[][] rand = tripletProvider.getCrProvider().getRandIntArray(longNum);
                int[][] pai = Arrays.stream(rand).map(ShuffleUtils::permutationGeneration).toArray(int[][]::new);
                output = new MpcLongVector[][]{shuffleParty.invShuffle(pai, input)};
                Rpc rpc = shuffleParty.getRpc();
                DataPacketHeader header = new DataPacketHeader(0, 0, 0, 1, 0);
                if (rpc.ownParty().getPartyId() == 0) {
                    byte[] pai2Byte = rpc.receive(header).getPayload().get(0);
                    int[] pai2 = IntUtils.byteArrayToIntArray(pai2Byte);
                    fun = new int[][]{ShuffleUtils.applyPermutation(ShuffleUtils.applyPermutation(pai[0], pai[1]), pai2)};
                } else if (rpc.ownParty().getPartyId() == 1) {
                    byte[] pai2Byte = IntUtils.intArrayToByteArray(pai[1]);
                    rpc.send(DataPacket.fromByteArrayList(header, Collections.singletonList(pai2Byte)));
                }
                break;
            }
            case A_PERMUTE_NETWORK: {
                output = new MpcLongVector[longOutputLen.length][];
                for (int i = 0; i < longOutputLen.length; i++) {
                    LOGGER.info("A_PERMUTE_NETWORK input size:{}, output size:{}", bitNum, longOutputLen[i]);
                    int outputLen = longOutputLen[i];
                    int maxNum = Math.max(outputLen, longNum);
                    if (shuffleParty.getRpc().ownParty().getPartyId() == 0) {
                        int[] randomInt = IntStream.range(0, maxNum).map(j -> secureRandom.nextInt()).toArray();
                        fun[i] = ShuffleUtils.permutationGeneration(randomInt);
                        fun[i] = Arrays.copyOf(fun[i], outputLen);
                    }
                    output[i] = shuffleParty.permuteNetwork(input, fun[i], outputLen, shuffleParty.getRpc().getParty(0),
                        shuffleParty.getRpc().getParty(1), shuffleParty.getRpc().getParty(2));
                }
                break;
            }
            case A_SWITCH_NETWORK:{
                output = new MpcLongVector[longOutputLen.length][];
                for (int i = 0; i < longOutputLen.length; i++) {
                    LOGGER.info("A_SWITCH_NETWORK input size:{}, output size:{}", bitNum, longOutputLen[i]);
                    int outputLen = longOutputLen[i];
                    if (shuffleParty.getRpc().ownParty().getPartyId() == 0) {
                        fun[i] = IntStream.range(0, outputLen).map(j -> secureRandom.nextInt(longNum)).toArray();
                    }
                    output[i] = shuffleParty.switchNetwork(input, fun[i], outputLen, shuffleParty.getRpc().getParty(0),
                        shuffleParty.getRpc().getParty(1), shuffleParty.getRpc().getParty(2));
                }
                break;
            }
            case A_DUPLICATE_NETWORK:{
                if(shuffleParty.getRpc().ownParty().getPartyId() == 0){
                    flag = new boolean[longNum];
                    for(int i = 1; i < longNum; i++){
                        flag[i] = secureRandom.nextBoolean();
                    }
                }
                output = new MpcLongVector[][]{shuffleParty.duplicateNetwork(input, flag, shuffleParty.getRpc().getParty(0))};
                break;
            }
            default:
                throw new IllegalArgumentException(op + " is not an arithmetic shuffle operation");
        }
        shuffleParty.getZl64cParty().checkUnverified();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        LOGGER.info("P{} op:[{}] process time: {}ms", shuffleParty.getRpc().ownParty().getPartyId(), op.name(), time);
        if(shuffleParty.getRpc().ownParty().getPartyId() == 0){
            AcShuffleRes[] res = new AcShuffleRes[output.length];
            for(int i = 0; i < output.length; i++){
                LongVector[] outputPlain = shuffleParty.getZl64cParty().open(output[i]);
                res[i] = flag == null ? new AcShuffleRes(fun[i], plainData, outputPlain) : new AcShuffleRes(flag, plainData, outputPlain);
            }
            aopMap.put(op, res);
        }else{
            for (MpcLongVector[] mpcLongVectors : output) {
                shuffleParty.getZl64cParty().open(mpcLongVectors);
            }
        }
    }

    public void testAcShuffleOpen(ShuffleOp op) throws MpcAbortException {
        LOGGER.info("testing {}", op.toString());
        TripletProvider tripletProvider = shuffleParty.getProvider();
        LongVector[] plainData = null;
        TripletRpLongVector[] input;
        int[] fun = null;

        int[][] rand = tripletProvider.getCrProvider().getRandIntArray(longNum);
        int[][] pai = Arrays.stream(rand).map(ShuffleUtils::permutationGeneration).toArray(int[][]::new);
        Rpc rpc = shuffleParty.getRpc();
        DataPacketHeader header = new DataPacketHeader(0, 0, 0, 1, 0);
        if (rpc.ownParty().getPartyId() == 0) {
            byte[] pai2Byte = rpc.receive(header).getPayload().get(0);
            int[] pai2 = IntUtils.byteArrayToIntArray(pai2Byte);
            fun = ShuffleUtils.applyPermutation(ShuffleUtils.applyPermutation(pai[0], pai[1]), pai2);
        } else if (rpc.ownParty().getPartyId() == 1) {
            byte[] pai2Byte = IntUtils.intArrayToByteArray(pai[1]);
            rpc.send(DataPacket.fromByteArrayList(header, Collections.singletonList(pai2Byte)));
        }
        SecureRandom secureRandom = new SecureRandom();
        if (shuffleParty.getRpc().ownParty().getPartyId() == 0) {
            plainData = IntStream.range(0, longDim).parallel().mapToObj(i ->
                LongVector.createRandom(longNum, secureRandom)).toArray(LongVector[]::new);
            input = (TripletRpLongVector[]) shuffleParty.getZl64cParty().shareOwn(plainData);
        } else {
            input = (TripletRpLongVector[]) shuffleParty.getZl64cParty().shareOther(
                IntStream.range(0, longDim).map(i -> longNum).toArray(), shuffleParty.getRpc().getParty(0));
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LongVector[] output = shuffleParty.shuffleOpen(pai, input);
        shuffleParty.getZl64cParty().checkUnverified();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        LOGGER.info("P{} op:[{}] process time: {}ms", shuffleParty.getRpc().ownParty().getPartyId(), op.name(), time);

        if(shuffleParty.getRpc().ownParty().getPartyId() == 0){
            AcShuffleRes[] res = new AcShuffleRes[]{new AcShuffleRes(fun, plainData, output)};
            aopMap.put(op, res);
        }
    }


    public void testBcShuffle(ShuffleOp op, int[] bitOutputLen) throws MpcAbortException {
        LOGGER.info("testing {}", op.toString());
        BitVector[] plainData = null;
        TripletRpZ2Vector[] input;
        MpcZ2Vector[][] output;
        int[][] fun = new int[bitOutputLen.length][];
        boolean[] flag = null;
        SecureRandom secureRandom = new SecureRandom();
        if (shuffleParty.getRpc().ownParty().getPartyId() == 0) {
            if(op.equals(ShuffleOp.B_SHUFFLE_ROW)){
                plainData = IntStream.range(0, bitNum).parallel().mapToObj(i ->
                    BitVectorFactory.createRandom(bitDim, secureRandom)).toArray(BitVector[]::new);
            }else{
                plainData = IntStream.range(0, bitDim).mapToObj(i ->
                    BitVectorFactory.createRandom(bitNum, secureRandom)).toArray(BitVector[]::new);
            }
            input = (TripletRpZ2Vector[]) shuffleParty.getZ2cParty().shareOwn(plainData);
        } else {
            int[] bitNums = op.equals(ShuffleOp.B_SHUFFLE_ROW)
                ? IntStream.range(0, bitNum).map(i -> bitDim).toArray()
                : IntStream.range(0, bitDim).map(i -> bitNum).toArray();
            input = (TripletRpZ2Vector[]) shuffleParty.getZ2cParty().shareOther(bitNums, shuffleParty.getRpc().getParty(0));
        }
        LOGGER.info("generated data, start operation");
        boolean isRow = false;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        switch (op) {
            case B_SHUFFLE_ROW: {
                isRow = true;
                output = new MpcZ2Vector[][]{shuffleParty.shuffleRow(input)};
                break;
            }
            case B_SHUFFLE_COLUMN: {
                output = new MpcZ2Vector[][]{shuffleParty.shuffleColumn(input)};
                break;
            }
            case B_PERMUTE_NETWORK: {
                output = new MpcZ2Vector[bitOutputLen.length][];
                for (int i = 0; i < bitOutputLen.length; i++) {
                    LOGGER.info("B_PERMUTE_NETWORK input size:{}, output size:{}", bitNum, bitOutputLen[i]);
                    int outputLen = bitOutputLen[i];
                    int maxNum = Math.max(outputLen, bitNum);
                    if (shuffleParty.getRpc().ownParty().getPartyId() == 0) {
                        int[] randomInt = IntStream.range(0, maxNum).map(j -> secureRandom.nextInt()).toArray();
                        fun[i] = ShuffleUtils.permutationGeneration(randomInt);
                        fun[i] = Arrays.copyOf(fun[i], outputLen);
                    }
                    output[i] = shuffleParty.permuteNetwork(input, fun[i], outputLen, shuffleParty.getRpc().getParty(0),
                        shuffleParty.getRpc().getParty(1), shuffleParty.getRpc().getParty(2));
                }
                break;
            }
            case B_SWITCH_NETWORK:{
                output = new MpcZ2Vector[bitOutputLen.length][];
                for (int i = 0; i < bitOutputLen.length; i++) {
                    LOGGER.info("B_SWITCH_NETWORK input size:{}, output size:{}", bitNum, bitOutputLen[i]);
                    int outputLen = bitOutputLen[i];
                    if (shuffleParty.getRpc().ownParty().getPartyId() == 0) {
                        fun[i] = IntStream.range(0, outputLen).map(j -> secureRandom.nextInt(bitNum)).toArray();
                    }
                    output[i] = shuffleParty.switchNetwork(input, fun[i], outputLen, shuffleParty.getRpc().getParty(0),
                        shuffleParty.getRpc().getParty(1), shuffleParty.getRpc().getParty(2));
                }
                break;
            }
            case B_DUPLICATE_NETWORK:{
                if(shuffleParty.getRpc().ownParty().getPartyId() == 0){
                    flag = new boolean[longNum];
                    for(int i = 1; i < longNum; i++){
                        flag[i] = secureRandom.nextBoolean();
                    }
                }
                output = new MpcZ2Vector[][]{shuffleParty.duplicateNetwork(input, flag, shuffleParty.getRpc().getParty(0))};
                break;
            }
            default:
                throw new IllegalArgumentException(op + " is not an binary shuffle operation");
        }
        shuffleParty.getZl64cParty().checkUnverified();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        LOGGER.info("op:[{}] process time: {}ms", op.name(), time);
        if(shuffleParty.getRpc().ownParty().getPartyId() == 0){
            BcShuffleRes[] res = new BcShuffleRes[output.length];
            for(int i = 0; i < output.length; i++){
                BitVector[] outputPlain = shuffleParty.getZ2cParty().open((TripletZ2Vector[]) output[i]);
                res[i] = flag == null ? new BcShuffleRes(isRow, fun[i], plainData, outputPlain) : new BcShuffleRes(flag, plainData, outputPlain);
            }
            bopMap.put(op, res);
        }else{
            for (MpcZ2Vector[] tmp : output) {
                shuffleParty.getZ2cParty().open((TripletZ2Vector[]) tmp);
            }
        }
    }

}
