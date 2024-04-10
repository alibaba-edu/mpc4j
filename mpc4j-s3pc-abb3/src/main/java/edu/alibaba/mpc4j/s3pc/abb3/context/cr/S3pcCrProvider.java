package edu.alibaba.mpc4j.s3pc.abb3.context.cr;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.AbstractAbbThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.PrpUtils;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProviderPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProviderPtoDesc.RandomType;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

/**
 * Correlated randomness provider for 3PC
 *
 * @author Feng Han
 * @date 2023/12/25
 */
public class S3pcCrProvider extends AbstractAbbThreePartyPto {
    /**
     * 0,1,2; indicating the index of the current party
     */
    protected final int selfId;
    /**
     * parallel or not
     */
    protected boolean parallel;
    /**
     * the size of bytes in buffer
     */
    private final int bufferByteSize;
    /**
     * shared randomness among three party
     */
    private final ByteBuffer[] rand3p;
    /**
     * shared randomness with left party
     */
    private ByteBuffer randWithLeft;
    /**
     * shared randomness with right party
     */
    private ByteBuffer randWithRight;
    /**
     * index for generating shares among three parties
     */
    private long randIndex;
    /**
     * index for generating shares with left party
     */
    private long randIndexWithLeft;
    /**
     * index for generating shares with right party
     */
    private long randIndexWithRight;
    /**
     * prp for generating shares among three parties
     */
    private Prp[][] prp3p;
    /**
     * prp for generating shares with left party
     */
    private Prp[] prpWithLeft;
    /**
     * prp for generating shares with right party
     */
    private Prp[] prpWithRight;
    /**
     * hash for msg
     */
    private Hash[] hash;

    public S3pcCrProvider(Rpc rpc, S3pcCrProviderConfig tripletProviderConfig) {
        super(S3pcCrProviderPtoDesc.getInstance(), rpc,
            rpc.getParty((rpc.ownParty().getPartyId() + 2) % 3),
            rpc.getParty((rpc.ownParty().getPartyId() + 1) % 3),
            tripletProviderConfig);
        selfId = rpc.ownParty().getPartyId();
        bufferByteSize = tripletProviderConfig.getBufferByteSize();
        MathPreconditions.checkNonNegative("bufferByteSize", bufferByteSize);
        MathPreconditions.checkEqual("bufferByteSize & 15", "0", bufferByteSize & 15, 0);
        rand3p = new ByteBuffer[2];
        randIndex = 0;
        randIndexWithLeft = 0;
        randIndexWithRight = 0;
    }

    public void init() {
        if (partyState.equals(PartyState.INITIALIZED)) {
            // if provider has been initialized
            return;
        }
        initState();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // send
        int selfParallelNum = parallel ? ForkJoinPool.getCommonPoolParallelism() : 1;
        List<byte[]> sendData = Collections.singletonList(IntUtils.intToByteArray(selfParallelNum));
        send(PtoStep.COMM_PARALLEL.ordinal(), leftParty(), sendData);
        send(PtoStep.COMM_PARALLEL.ordinal(), rightParty(), sendData);
        byte[][] selfKey = new byte[3][CommonConstants.BLOCK_BYTE_LENGTH];
        Arrays.stream(selfKey).forEach(array -> secureRandom.nextBytes(array));
        List<byte[]> toLeftKeys = Arrays.asList(selfKey[0], selfKey[1]);
        List<byte[]> toRightKey = Collections.singletonList(selfKey[2]);
        send(PtoStep.KEY_SHARE.ordinal(), leftParty(), toLeftKeys);
        send(PtoStep.KEY_SHARE.ordinal(), rightParty(), toRightKey);

        // receive
        int left = IntUtils.byteArrayToInt(receive(PtoStep.COMM_PARALLEL.ordinal(), leftParty()).get(0));
        int right = IntUtils.byteArrayToInt(receive(PtoStep.COMM_PARALLEL.ordinal(), rightParty()).get(0));
        int hashNum = Math.min(selfParallelNum, Math.min(left, right));
        hash = IntStream.range(0, hashNum).mapToObj(i ->
                HashFactory.createInstance(getEnvType(), CommonConstants.BLOCK_BYTE_LENGTH))
            .toArray(Hash[]::new);
        List<byte[]> dataFromLeft = receive(PtoStep.KEY_SHARE.ordinal(), leftParty());
        List<byte[]> dataFromRight = receive(PtoStep.KEY_SHARE.ordinal(), rightParty());

        int prpSize = parallel ? ForkJoinPool.getCommonPoolParallelism() : 1;
        byte[][] prp3pKey = new byte[][]{selfKey[0], dataFromRight.get(0)};
        prp3p = IntStream.range(0, 2).mapToObj(i ->
            IntStream.range(0, prpSize).mapToObj(j -> {
                Prp tmp = PrpFactory.createInstance(getEnvType());
                tmp.setKey(prp3pKey[i]);
                return tmp;
            }).toArray(Prp[]::new)).toArray(Prp[][]::new);
        byte[][] prp2pKeys = new byte[][]{BytesUtils.xor(dataFromLeft.get(0), selfKey[1]), BytesUtils.xor(dataFromRight.get(1), selfKey[2])};
        prpWithLeft = IntStream.range(0, prpSize).mapToObj(j -> {
            Prp tmp = PrpFactory.createInstance(getEnvType());
            tmp.setKey(prp2pKeys[0]);
            return tmp;
        }).toArray(Prp[]::new);
        prpWithRight = IntStream.range(0, prpSize).mapToObj(j -> {
            Prp tmp = PrpFactory.createInstance(getEnvType());
            tmp.setKey(prp2pKeys[1]);
            return tmp;
        }).toArray(Prp[]::new);

        fillBuffer(RandomType.SHARE);
        fillBuffer(RandomType.WITH_LEFT);
        fillBuffer(RandomType.WITH_RIGHT);
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    /**
     * fill the buffer for randomness
     *
     * @param randomType which buffer should be filled
     */
    private void fillBuffer(RandomType randomType) {
        switch (randomType) {
            case SHARE: {
                rand3p[0] = ByteBuffer.wrap(PrpUtils.generateRandBytes(prp3p[0], randIndex, bufferByteSize));
                rand3p[1] = ByteBuffer.wrap(PrpUtils.generateRandBytes(prp3p[1], randIndex, bufferByteSize));
                randIndex += bufferByteSize >> 4;
                break;
            }
            case WITH_LEFT: {
                randWithLeft = ByteBuffer.wrap(PrpUtils.generateRandBytes(prpWithLeft, randIndexWithLeft, bufferByteSize));
                randIndexWithLeft += bufferByteSize >> 4;
                break;
            }
            case WITH_RIGHT: {
                randWithRight = ByteBuffer.wrap(PrpUtils.generateRandBytes(prpWithRight, randIndexWithRight, bufferByteSize));
                randIndexWithRight += bufferByteSize >> 4;
                break;
            }
            default:
                throw new IllegalArgumentException("Invalid RandomType: " + randomType.name());
        }
    }

    /**
     * get the correlated random bytes among three parties
     *
     * @param byteLen the number of byte
     */
    private byte[][] getRandByteArray(int byteLen) {
        MathPreconditions.checkNonNegative("byteLen", byteLen);
        byte[][] res = new byte[2][byteLen];
        int currentPos = 0;
        while (currentPos < byteLen) {
            int bufferLastNum = rand3p[0].limit() - rand3p[0].position();
            int needNum = Math.min(bufferLastNum, byteLen - currentPos);
            for (int i = 0; i < 2; i++) {
                rand3p[i].get(res[i], currentPos, needNum);
            }
            currentPos += needNum;
            if (rand3p[0].limit() == rand3p[0].position()) {
                fillBuffer(RandomType.SHARE);
            }
        }
        return res;
    }

    /**
     * get the correlated random bytes between two parties
     *
     * @param byteLen the number of bytes
     * @param party   which party shares the randomness
     */
    private byte[] getRandByteArray(int byteLen, Party party) {
        assert party.equals(leftParty()) || party.equals(rightParty());
        MathPreconditions.checkNonNegative("byteLen", byteLen);
        byte[] res = new byte[byteLen];
        int currentPos = 0;
        ByteBuffer buffer = party.equals(leftParty()) ? randWithLeft : randWithRight;

        while (currentPos < byteLen) {
            int bufferLastNum = buffer.limit() - buffer.position();
            int needNum = Math.min(bufferLastNum, byteLen - currentPos);
            buffer.get(res, currentPos, needNum);
            currentPos += needNum;
            if (buffer.limit() == buffer.position()) {
                if (party.equals(leftParty())) {
                    fillBuffer(RandomType.WITH_LEFT);
                } else {
                    fillBuffer(RandomType.WITH_RIGHT);
                }
                buffer = party.equals(leftParty()) ? randWithLeft : randWithRight;
            }
        }
        return res;
    }

    /**
     * get the correlated random long elements among three parties
     *
     * @param arrayLen the number of elements
     */
    public long[][] getRandLongArray(int arrayLen) {
        byte[][] res = getRandByteArray(arrayLen << 3);
        return Arrays.stream(res).map(LongUtils::byteArrayToLongArray).toArray(long[][]::new);
    }

    /**
     * get the correlated random int elements among three parties
     *
     * @param arrayLen the number of elements
     */
    public int[][] getRandIntArray(int arrayLen) {
        byte[][] res = getRandByteArray(arrayLen << 2);
        return Arrays.stream(res).map(IntUtils::byteArrayToIntArray).toArray(int[][]::new);
    }

    /**
     * get the correlated elements among three parties, the results of xor on all elements are 0
     *
     * @param bitNum the number of bits
     */
    public BitVector randZeroBitVector(int bitNum) {
        byte[][] r = getRandByteArray(CommonUtils.getByteLength(bitNum));
        BytesUtils.xori(r[0], r[1]);
        if ((bitNum & 7) > 0) {
            r[0][0] &= (byte) ((1 << (bitNum & 7)) - 1);
        }
        return BitVectorFactory.create(bitNum, r[0]);
    }

    /**
     * get the correlated elements among three parties, the results of xor on all elements are 0
     *
     * @param bitNum the number of bits
     */
    public BitVector[] randZeroBitVector(int[] bitNum) {
        int[] byteNums = Arrays.stream(bitNum).map(CommonUtils::getByteLength).toArray();
        int totalByte = Arrays.stream(byteNums).sum();
        byte[][] r = getRandByteArray(totalByte);
        BytesUtils.xori(r[0], r[1]);
        return BitVectorFactory.create(totalByte << 3, r[0]).uncheckSplitWithPadding(bitNum);
    }

    /**
     * get the correlated elements among three parties, the results of summing all elements are 0
     *
     * @param arrayNum the number of elements
     */
    public LongVector randZeroZl64Vector(int arrayNum) {
        long[][] r = getRandLongArray(arrayNum);
        IntStream.range(0, r[0].length).forEach(i -> r[0][i] -= r[1][i]);
        return LongVector.create(r[0]);
    }

    /**
     * get the correlated elements among three parties, the results of summing all elements are 0
     *
     * @param arrayNum the number of elements
     */
    public LongVector[] randZeroZl64Vector(int[] arrayNum) {
        return randZeroZl64Vector(Arrays.stream(arrayNum).sum()).split(arrayNum);
    }

    /**
     * get the shared correlated random elements among three parties
     *
     * @param bitNum the number of bits
     */
    public TripletRpZ2Vector[] randRpShareZ2Vector(int[] bitNum) {
        int totalByteLen = Arrays.stream(bitNum).map(CommonUtils::getByteLength).sum();
        byte[][] r = getRandByteArray(totalByteLen);
        TripletRpZ2Vector randVec = TripletRpZ2Vector.create(r, totalByteLen << 3);
        return randVec.splitWithPadding(bitNum);
    }

    /**
     * get the shared correlated random elements among three parties
     *
     * @param dataNum the number of elements
     */
    public TripletRpLongVector[] randRpShareZl64Vector(int[] dataNum) {
        int totalNum = Arrays.stream(dataNum).sum();
        long[][] r = getRandLongArray(totalNum);
        TripletRpLongVector randVec = TripletRpLongVector.create(r);
        return randVec.split(dataNum);
    }

    /**
     * get the correlated random long elements between two parties
     *
     * @param arrayLen the number of elements
     * @param party    which party shares the randomness
     */
    public long[] getRandLongArray(int arrayLen, Party party) {
        byte[] res = getRandByteArray(arrayLen << 3, party);
        return LongUtils.byteArrayToLongArray(res);
    }

    /**
     * get the correlated random long elements between two parties
     *
     * @param arrayLen the number of elements
     * @param party    which party shares the randomness
     */
    public long[][] getRandLongArrays(int[] arrayLen, Party party) {
        byte[] res = getRandByteArray(Arrays.stream(arrayLen).sum() << 3, party);
        long[][] lR = new long[arrayLen.length][];
        for (int i = 0, pos = 0; i < arrayLen.length; i++) {
            int endPos = pos + (arrayLen[i] << 3);
            lR[i] = LongUtils.byteArrayToLongArray(Arrays.copyOfRange(res, pos, endPos));
            pos = endPos;
        }
        return lR;
    }

    /**
     * get the correlated random int elements between two parties
     *
     * @param arrayLen the number of elements
     * @param party    which party shares the randomness
     */
    public int[] getRandIntArray(int arrayLen, Party party) {
        byte[] res = getRandByteArray(arrayLen << 2, party);
        return IntUtils.byteArrayToIntArray(res);
    }

    /**
     * get the correlated random elements between two parties
     *
     * @param dataNum the number of elements
     * @param party   which party shares the randomness
     */
    public LongVector[] randZl64Vector(int[] dataNum, Party party) {
        int totalNum = Arrays.stream(dataNum).sum();
        LongVector r = LongVector.create(getRandLongArray(totalNum, party));
        return r.split(dataNum);
    }

    /**
     * get the correlated random binary elements between two parties
     *
     * @param bitNum the number of bits
     * @param party  which party shares the randomness
     */
    public BitVector[] randBitVector(int[] bitNum, Party party) {
        int[] byteNums = Arrays.stream(bitNum).map(CommonUtils::getByteLength).toArray();
        int totalByte = Arrays.stream(byteNums).sum();
        byte[] r = getRandByteArray(totalByte, party);
        return BitVectorFactory.create(totalByte << 3, r).uncheckSplitWithPadding(bitNum);
    }

    /**
     * generate hash for the data
     *
     * @param data data in the form of BitVector
     */
    public byte[] genHash(BitVector[] data) {
        assert data != null;
        if (!parallel || (data.length <= hash.length && data[0].byteNum() <= hash[0].getOutputByteLength())) {
            BitVector merge = BitVectorFactory.mergeWithPadding(data);
            return hash[0].digestToBytes(merge.getBytes());
        } else {
            byte[][] hashRes;
            if (data.length <= hash.length) {
                hashRes = IntStream.range(0, data.length).parallel().mapToObj(i ->
                    hash[i].digestToBytes(data[i].getBytes())).toArray(byte[][]::new);
            } else {
                int eachGroup = (int) Math.round(((double) data.length) / hash.length);
                hashRes = IntStream.range(0, hash.length).parallel().mapToObj(i -> {
                    int endIndex = i == hash.length - 1 ? data.length : eachGroup * (i + 1);
                    BitVector merge = BitVectorFactory.mergeWithPadding(Arrays.copyOfRange(data, eachGroup * i, endIndex));
                    return hash[i].digestToBytes(merge.getBytes());
                }).toArray(byte[][]::new);
            }
            byte[] input = new byte[hashRes[0].length * hashRes.length];
            for (int i = 0, pos = 0; i < hashRes.length; i++, pos += hashRes[0].length) {
                System.arraycopy(hashRes[i], 0, input, pos, hashRes[i].length);
            }
            return hash[0].digestToBytes(input);
        }
    }

    /**
     * generate hash for the data. The input data should be formatted
     *
     * @param data data in the form of LongVector
     */
    public byte[] genHash(LongVector[] data) {
        assert data != null;
        if (!parallel || (data.length <= hash.length && data[0].getNum() <= hash[0].getOutputByteLength() >> 3)) {
            LongVector merge = LongVector.merge(data);
            return hash[0].digestToBytes(LongUtils.longArrayToByteArray(merge.getElements()));
        } else {
            byte[][] hashRes;
            if (data.length <= hash.length) {
                hashRes = IntStream.range(0, data.length).parallel().mapToObj(i ->
                    hash[i].digestToBytes(LongUtils.longArrayToByteArray(data[i].getElements()))).toArray(byte[][]::new);
            } else {
                int eachGroup = (int) Math.round(((double) data.length) / hash.length);
                hashRes = IntStream.range(0, hash.length).parallel().mapToObj(i -> {
                    int endIndex = i == hash.length - 1 ? data.length : eachGroup * (i + 1);
                    LongVector merge = LongVector.merge(Arrays.copyOfRange(data, eachGroup * i, endIndex));
                    return hash[i].digestToBytes(LongUtils.longArrayToByteArray(merge.getElements()));
                }).toArray(byte[][]::new);
            }
            byte[] input = new byte[hashRes[0].length * hashRes.length];
            for (int i = 0, pos = 0; i < hashRes.length; i++, pos += hashRes[0].length) {
                System.arraycopy(hashRes[i], 0, input, pos, hashRes[i].length);
            }
            return hash[0].digestToBytes(input);
        }
    }
}
