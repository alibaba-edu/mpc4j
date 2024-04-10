package edu.alibaba.mpc4j.s3pc.abb3.basic.conversion;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvOperations.ConvOp;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvOperations.ConvRes;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.replicate.Aby3ConvParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.MatrixUtils;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
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
 * aby3 type conversion party thread
 *
 * @author Feng Han
 * @date 2024/02/06
 */
public class Aby3ConvPartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(Aby3ConvPartyThread.class);
    private final Aby3ConvParty convParty;
    private final int dataNum;
    private final int dataDim;
    private final ConvOp[] ops;
    private final HashMap<ConvOp, ConvRes[]> hashMap;

    public Aby3ConvPartyThread(Aby3ConvParty convParty, int dataNum, int dataDim, ConvOp[] ops) {
        this.convParty = convParty;
        this.dataNum = dataNum;
        this.dataDim = dataDim;
        this.ops = ops;
        hashMap = new HashMap<>();
    }

    public ConvRes[] getConvRes(ConvOp op) {
        Assert.assertTrue(hashMap.containsKey(op));
        return hashMap.get(op);
    }

    public long[] getTupleNum(int[] bitLens) {
        long bitTupleNum = 0, longTupleNum = 0;
        for (ConvOp op : ops) {
            switch (op) {
                case BIT_EXTRACTION:
                    for (int bitLen : bitLens) {
                        bitTupleNum += convParty.getTupleNum(op, dataNum, dataDim, 64 - bitLen)[0];
                    }
                    break;
                case A2B:
                case B2A:
                    for (int bitLen : bitLens) {
                        bitTupleNum += convParty.getTupleNum(op, dataNum, dataDim, bitLen)[0];
                    }
                    break;
                case A_MUL_B:
                case BIT2A:
                    longTupleNum += convParty.getTupleNum(op, dataNum, dataDim, bitLens[0])[1];
                    break;
            }
        }
        return new long[]{bitTupleNum, longTupleNum};
    }

    private int[] genBitLen() {
        // 用于测试 BIT_EXTRACTION 和 A2B 的bit数量
//        int[] bitLens = new int[]{64};

        int[] bitLens = new int[4];
        bitLens[0] = 1;
        bitLens[3] = 64;
        Rpc rpc = convParty.getRpc();
        if (rpc.ownParty().getPartyId() == 0) {
            SecureRandom secureRandom = new SecureRandom();
//            bitLens[1] = 2;
//            bitLens[2] = 16 + 32;
            bitLens[1] = secureRandom.nextInt(31) + 1;
            bitLens[2] = secureRandom.nextInt(32) + 32;
            DataPacketHeader header1 = new DataPacketHeader(0, 0, 0, 0, 1);
            DataPacketHeader header2 = new DataPacketHeader(0, 0, 0, 0, 2);
            byte[] sendData = IntUtils.intArrayToByteArray(Arrays.copyOfRange(bitLens, 1, bitLens.length));
            rpc.send(DataPacket.fromByteArrayList(header1, Collections.singletonList(sendData)));
            rpc.send(DataPacket.fromByteArrayList(header2, Collections.singletonList(sendData)));
        } else {
            DataPacketHeader header = new DataPacketHeader(0, 0, 0, 0, rpc.ownParty().getPartyId());
            int[] params = IntUtils.byteArrayToIntArray(rpc.receive(header).getPayload().get(0));
            System.arraycopy(params, 0, bitLens, 1, params.length);
        }

        return bitLens;
    }

    @Override
    public void run(){
        int[] bitLens = genBitLen();
        long[] tupleNum = getTupleNum(bitLens);
        try {
            TripletProvider provider = convParty.getProvider();
            provider.init(tupleNum[0], tupleNum[1]);
            convParty.init();
            for (ConvOp op : ops) {
                switch (op) {
                    case A2B:
                    case BIT_EXTRACTION:
                        testConvAInput(op, bitLens);
                        break;
                    case B2A:
                    case BIT2A:
                        testConvBInput(op, bitLens);
                        break;
                    case A_MUL_B:
                        testConvBAndAInput(op);
                }
            }
            long actualUsedBit = provider.getZ2MtProvider() == null ? 0 : provider.getZ2MtProvider().getAllTupleNum();
            long actualUsedLong = provider.getZl64MtProvider() == null ? 0 : provider.getZl64MtProvider().getAllTupleNum();
            LOGGER.info("computed bitTupleNum:{}, actually used bitTupleNum:{} | computed longTupleNum:{}, actually used longTupleNum:{} ", tupleNum[0], actualUsedBit, tupleNum[1], actualUsedLong);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }

    private void testConvAInput(ConvOp op, int[] bitLens) throws MpcAbortException {
        LOGGER.info("testing {}", op.toString());
        LongVector[] plainData = null;
        TripletRpLongVector[] input;
        TripletRpZ2Vector[][][] output = new TripletRpZ2Vector[bitLens.length][][];
        SecureRandom secureRandom = new SecureRandom();
        if (convParty.getRpc().ownParty().getPartyId() == 0) {
            plainData = IntStream.range(0, dataDim).parallel().mapToObj(i ->
                LongVector.createRandom(dataNum, secureRandom)).toArray(LongVector[]::new);
            input = (TripletRpLongVector[]) convParty.getZl64cParty().shareOwn(plainData);
        } else {
            input = (TripletRpLongVector[]) convParty.getZl64cParty().shareOther(
                IntStream.range(0, dataDim).map(i -> dataNum).toArray(), convParty.getRpc().getParty(0));
        }
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        switch (op) {
            case A2B: {
                for (int i = 0; i < bitLens.length; i++) {
                    int bitLen = bitLens[i];
                    output[i] = convParty.a2b(input, bitLen);
                }
                break;
            }
            case BIT_EXTRACTION: {
                for (int i = 0; i < bitLens.length; i++) {
                    int bitLen = 64 - bitLens[i];
                    LOGGER.info("bitIndex:{}", bitLen);
                    output[i] = new TripletRpZ2Vector[][]{convParty.bitExtraction(input, bitLen)};
                }
                break;
            }
            default:
                throw new IllegalArgumentException(op.name() + " is not the operation whose input is arithmetic sharing");
        }
        convParty.getZl64cParty().checkUnverified();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        LOGGER.info("op:[{}] process time: {}ms", op.name(), time);
        if (convParty.getRpc().ownParty().getPartyId() == 0) {
            ConvRes[] res = new ConvRes[output.length];
            for (int i = 0; i < output.length; i++) {
                BitVector[] outputPlain = convParty.getZ2cParty().open(MatrixUtils.flat(output[i]));
                BitVector[][] matrixPlain = MatrixUtils.intoMatrix(outputPlain, dataDim);
                res[i] = new ConvRes(op.equals(ConvOp.A2B) ? bitLens[i] : 64 - bitLens[i], matrixPlain, plainData);
            }
            hashMap.put(op, res);
        } else {
            for (TripletRpZ2Vector[][] tripletRpZ2Vectors : output) {
                convParty.getZ2cParty().open(MatrixUtils.flat(tripletRpZ2Vectors));
            }
        }
    }

    private void testConvBInput(ConvOp op, int[] bitLens) throws MpcAbortException {
        LOGGER.info("testing {}", op.toString());
        BitVector[][] plainData = null;
        TripletRpZ2Vector[][] input;
        TripletRpLongVector[][] output;
        SecureRandom secureRandom = new SecureRandom();
        int eachDim = op.equals(ConvOp.BIT2A) ? 1 : 64;
        TripletRpZ2Vector[] tmp;
        if (convParty.getRpc().ownParty().getPartyId() == 0) {
            plainData = IntStream.range(0, dataDim).mapToObj(i ->
                IntStream.range(0, eachDim).mapToObj(j ->
                        BitVectorFactory.createRandom(dataNum, secureRandom))
                    .toArray(BitVector[]::new)).toArray(BitVector[][]::new);
            tmp = (TripletRpZ2Vector[]) convParty.getZ2cParty().shareOwn(MatrixUtils.flat(plainData));
        } else {
            int[] bits = new int[eachDim * dataDim];
            Arrays.fill(bits, dataNum);
            tmp = (TripletRpZ2Vector[]) convParty.getZ2cParty().shareOther(bits, convParty.getRpc().getParty(0));
        }
        input = op.equals(ConvOp.BIT2A) ? new TripletRpZ2Vector[][]{tmp} : MatrixUtils.intoMatrix(tmp, dataDim);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        switch (op) {
            case B2A: {
                output = new TripletRpLongVector[bitLens.length][];
                for(int i = 0; i < bitLens.length; i++){
                    int finalI = i;
                    output[i] = convParty.b2a(Arrays.stream(input).map(each -> Arrays.copyOf(each, bitLens[finalI])).toArray(TripletRpZ2Vector[][]::new));
                }
                break;
            }
            case BIT2A: {
                output = new TripletRpLongVector[][]{convParty.bit2a(input[0])};
                break;
            }
            default:
                throw new IllegalArgumentException(op.name() + " is not the operation whose input is binary sharing");
        }
        convParty.getZl64cParty().checkUnverified();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        LOGGER.info("op:[{}] process time: {}ms", op.name(), time);
        if (convParty.getRpc().ownParty().getPartyId() == 0) {
            ConvRes[] res = new ConvRes[output.length];
            for(int i = 0; i < output.length; i++){
                LongVector[] outputPlain = convParty.getZl64cParty().open(output[i]);
                int finalI = i;
                if (op.equals(ConvOp.BIT2A)) {
                    res[i] = new ConvRes(64, plainData, outputPlain);
                } else {
                    Assert.assertNotNull(plainData);
                    res[i] = new ConvRes(bitLens[i], Arrays.stream(plainData).map(each -> Arrays.copyOf(each, bitLens[finalI])).toArray(BitVector[][]::new), outputPlain);
                }
            }
            hashMap.put(op, res);
        } else {
            for (TripletRpLongVector[] tripletRpLongVectors : output) {
                convParty.getZl64cParty().open(tripletRpLongVectors);
            }
        }
    }

    private void testConvBAndAInput(ConvOp op) throws MpcAbortException {
        Assert.assertEquals(op, ConvOp.A_MUL_B);
        LOGGER.info("testing {}", op);
        BitVector[][] plainBitData = null;
        LongVector[] plainLongData = null;
        TripletRpZ2Vector[] bInput;
        TripletRpLongVector[] aInput;
        TripletRpLongVector[] output;
        SecureRandom secureRandom = new SecureRandom();
        if (convParty.getRpc().ownParty().getPartyId() == 0) {
            plainBitData = IntStream.range(0, dataDim).mapToObj(i ->
                new BitVector[]{BitVectorFactory.createRandom(dataNum, secureRandom)}).toArray(BitVector[][]::new);
            plainLongData = IntStream.range(0, dataDim).mapToObj(i -> LongVector.createRandom(dataNum, secureRandom)).toArray(LongVector[]::new);
            bInput = (TripletRpZ2Vector[]) convParty.getZ2cParty().shareOwn(MatrixUtils.flat(plainBitData));
            aInput = (TripletRpLongVector[]) convParty.getZl64cParty().shareOwn(plainLongData);
        } else {
            int[] bits = new int[dataDim];
            Arrays.fill(bits, dataNum);
            bInput = (TripletRpZ2Vector[]) convParty.getZ2cParty().shareOther(bits, convParty.getRpc().getParty(0));
            aInput = (TripletRpLongVector[]) convParty.getZl64cParty().shareOther(bits, convParty.getRpc().getParty(0));
        }
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        output = convParty.aMulB(aInput, bInput);
        convParty.getZl64cParty().checkUnverified();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        LOGGER.info("op:[{}] process time: {}ms", op.name(), time);
        if (convParty.getRpc().ownParty().getPartyId() == 0) {
            LongVector[] outputPlain = convParty.getZl64cParty().open(output);
            ConvRes[] res = new ConvRes[]{new ConvRes(64, plainBitData, plainLongData, outputPlain)};
            hashMap.put(op, res);
        } else {
            convParty.getZl64cParty().open(output);
        }
    }

}
