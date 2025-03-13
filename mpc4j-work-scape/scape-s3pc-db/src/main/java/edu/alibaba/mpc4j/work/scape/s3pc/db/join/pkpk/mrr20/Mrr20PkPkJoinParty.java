package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.mrr20;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.NoStashCuckooHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleOperations.ShuffleOp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.AbstractPkPkJoinParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.PkPkJoinFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.PkPkJoinParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.mrr20.Mrr20PkPkJoinPtoDesc.PtoStep;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.CuckooHashWithPos;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.RandomEncodingFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.RandomEncodingFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.RandomEncodingParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.mrr20.Mrr20RandomEncodingConfig;
import gnu.trove.list.linked.TLongLinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.IntStream;

/**
 * Mrr20 PkPk Join Party
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public class Mrr20PkPkJoinParty extends AbstractPkPkJoinParty implements PkPkJoinParty {
    private static final Logger LOGGER = LoggerFactory.getLogger(Mrr20PkPkJoinParty.class);
    /**
     * how many hash functions should be used in join
     */
    public final int hashNum;
    /**
     * z2 circuit
     */
    protected final Z2IntegerCircuit z2IntegerCircuit;
    /**
     * random encoding party
     */
    protected final RandomEncodingParty encodingParty;
    /**
     * cuckoo hash type
     */
    protected final CuckooHashBinType hashBinType;
    /**
     * the number of hash bin
     */
    protected int hashBinNum;
    /**
     * prf for hash
     */
    protected Prf[] prf4Hash;

    public Mrr20PkPkJoinParty(Abb3Party abb3Party, Mrr20PkPkJoinConfig config) {
        super(Mrr20PkPkJoinPtoDesc.getInstance(), abb3Party, config);
        hashNum = config.getHashNum();
        z2IntegerCircuit = new Z2IntegerCircuit(z2cParty);
        encodingParty = RandomEncodingFactory.createParty(abb3Party, config.getEncodingConfig());
        hashBinType = config.getHashBinType();
        addMultiSubPto(encodingParty);
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        encodingParty.init();
        abb3Party.init();
        initState();
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public long[] setUsage(PkPkJoinFnParam... params) {
        long[] tuples = new long[]{0, 0};
        if (isMalicious) {
            for (PkPkJoinFnParam param : params) {
                TLongLinkedList notSet = new TLongLinkedList();
                // circuit
                int payloadLen = Mrr20RandomEncodingConfig.THRESHOLD_REDUCE + param.keyDim + param.leftValueDim;
                long leqAndEq = 2L * hashNum * payloadLen * param.rightDataNum;
                notSet.add(leqAndEq);
                // switch network
                int targetHashNum = CuckooHashBinFactory.getBinNum(hashBinType, param.leftDataNum);
                long permuteLeft = abb3Party.getShuffleParty().getTupleNum(ShuffleOp.B_PERMUTE_NETWORK, param.leftDataNum, targetHashNum, payloadLen);
                long switchLeft = 3L * abb3Party.getShuffleParty().getTupleNum(ShuffleOp.B_SWITCH_NETWORK, targetHashNum, param.rightDataNum, payloadLen);
                notSet.add(permuteLeft + switchLeft);
                long noSetAll = notSet.sum();
                abb3Party.updateNum(noSetAll, 0);

                long[] encodingTuple = encodingParty.setUsage(new RandomEncodingFnParam(param.keyDim, param.leftDataNum, param.rightDataNum, true));
                tuples[0] += encodingTuple[0] + noSetAll;
                tuples[1] += encodingTuple[1];
            }
        }
        return tuples;
    }

    @Override
    public TripletZ2Vector[] primaryKeyInnerJoin(TripletZ2Vector[] left, TripletZ2Vector[] right, int[] leftKeyIndex,
                                                 int[] rightKeyIndex, boolean withDummy, boolean inputIsSorted) throws MpcAbortException {
        inputProcess(left, right, leftKeyIndex, rightKeyIndex, withDummy, inputIsSorted);
        logPhaseInfo(PtoState.PTO_BEGIN, "primaryKeyInnerJoin");

        stopWatch.start();
        TripletZ2Vector[][] twoEnc = encodingParty.getEncodingForTwoKeys(Arrays.copyOf(newLeft, keyDim), newLeft[newLeft.length - 1],
            Arrays.copyOf(newRight, keyDim), newRight[newRight.length - 1], withDummy);
        TripletZ2Vector[] leftLowMcRes = twoEnc[0], rightLowMcRes = twoEnc[1];
        logStepInfo(PtoState.PTO_STEP, 1, 5, resetAndGetTime(), "lowmc computation");

        stopWatch.start();
        int[] permutation4Left;
        Party leftCuckooParty = rpc.getParty(0), rigthHashParty = rpc.getParty(1);
        TripletZ2Vector[] encPart = null;
        if (isMalicious()) {
            encPart = new TripletZ2Vector[CuckooHashWithPos.getHashParam(left[0].bitNum())[0] * hashNum];
            permutation4Left = this.maliciousPermutation4Left(leftLowMcRes, encPart, leftCuckooParty);
        } else {
            permutation4Left = this.semiHonestPermutation4Left(leftLowMcRes, leftCuckooParty, rigthHashParty);
        }
        logStepInfo(PtoState.PTO_STEP, 2, 5, resetAndGetTime(), "generate permutation");

        stopWatch.start();
        TripletZ2Vector[] leftPerRes = permuteExtendLeft(newLeft, permutation4Left, encPart, leftCuckooParty);
        logStepInfo(PtoState.PTO_STEP, 3, 5, resetAndGetTime(), "permute left table into cuckoo hash position");

        stopWatch.start();
        TripletZ2Vector[][] leftSwitchRes = switchLeftInput(leftPerRes, rightLowMcRes, newRight[0].bitNum(), rigthHashParty);
        logStepInfo(PtoState.PTO_STEP, 4, 5, resetAndGetTime(), "switch left table according to the hash of the right table");

        stopWatch.start();
        TripletZ2Vector[] finalRes = this.compareAndConcat4SmallPayload(leftSwitchRes, newRight);
        logStepInfo(PtoState.PTO_STEP, 5, 5, resetAndGetTime(), "concat tables together");

        logPhaseInfo(PtoState.PTO_END, "primaryKeyInnerJoin");
        return finalRes;
    }

    public void checkUnique(BigInteger[] items) {
        HashSet<BigInteger> h = new HashSet<>(items.length);
        for (BigInteger x : items) {
            Preconditions.checkArgument(!h.contains(x));
            h.add(x);
        }
    }

    /**
     * get the permutation of left table, which is generated with cuckoo hash and soprp encoding values
     * for malicious version, directly use the soprp encoding values as the result of hash functions
     *
     * @param prpRes          soprp of left keys
     * @param encPart         the part of prp to verify the correctness of permutation
     * @param leftCuckooParty which party generates the permutation
     */
    protected int[] maliciousPermutation4Left(TripletZ2Vector[] prpRes, TripletZ2Vector[] encPart, Party leftCuckooParty) throws MpcAbortException {
        int leftLen = prpRes[0].bitNum();
        int[] hashParam = CuckooHashWithPos.getHashParam(leftLen);
        int hashBitLen = hashParam[0];
        Preconditions.checkArgument(hashBitLen * hashNum <= prpRes.length);
        hashBinNum = hashParam[1];
        int modNum = hashBinNum - 1;
        boolean success;
        int[][] hashPos = new int[hashNum][leftLen];
        int[] pai = new int[hashBinNum];
        System.arraycopy(prpRes, prpRes.length - hashNum * hashBitLen, encPart, 0, hashNum * hashBitLen);
        if (rpc.ownParty().equals(leftCuckooParty)) {
            BitVector[] plainEncRes = z2cParty.revealOwn(prpRes);
            BigInteger[] transKeyPlain = ZlDatabase.create(envType, parallel, plainEncRes).getBigIntegerData();
            checkUnique(transKeyPlain);
            IntStream transStream = parallel ? IntStream.range(0, leftLen).parallel() : IntStream.range(0, leftLen);
            int[] shiftLen = new int[]{2 * hashBitLen, hashBitLen, 0};
            transStream.forEach(i -> IntStream.range(0, hashPos.length).forEach(j -> hashPos[j][i] =
                transKeyPlain[i].shiftRight(shiftLen[j]).intValue() & modNum));
            CuckooHashWithPos cuckooHashWithPos = new CuckooHashWithPos(hashNum, hashPos);
            success = cuckooHashWithPos.insertAllItems();
            send(PtoStep.HASH_SUCCESS_SIGN.ordinal(), leftParty(), Collections.singletonList(new byte[]{(byte) (success ? 1 : 0)}));
            send(PtoStep.HASH_SUCCESS_SIGN.ordinal(), rightParty(), Collections.singletonList(new byte[]{(byte) (success ? 1 : 0)}));
            if (success) {
                pai = cuckooHashWithPos.getHashPermutation();
            }
        } else {
            z2cParty.revealOther(prpRes, leftCuckooParty);
            success = receive(PtoStep.HASH_SUCCESS_SIGN.ordinal(), leftCuckooParty).get(0)[0] == (byte) 1;
        }
        if (!success) {
            throw new MpcAbortException("fail to insert data into cuckoo hash table, check code or data");
        } else {
            LOGGER.info("successfully insert data into cuckoo hash table");
            return pai;
        }
    }

    /**
     * 根据lowMc得到left table的permutation
     *
     * @param leftCuckooParty 得到左表lowMc的是哪个
     * @param rightHashParty  得到右表lowMc的是哪个
     */
    protected int[] semiHonestPermutation4Left(TripletZ2Vector[] prpRes, Party leftCuckooParty, Party rightHashParty) throws MpcAbortException {
        int leftLen = prpRes[0].bitNum();
        if (rpc.ownParty().equals(leftCuckooParty)) {
            NoStashCuckooHashBin<byte[]> cuckooHash;
            BitVector[] plainEncRes = z2cParty.revealOwn(prpRes);
            byte[][] transKeyByte = ZlDatabase.create(envType, parallel, plainEncRes).getBytesData();
            BigInteger[] transKeyPlain = Arrays.stream(transKeyByte).map(BigIntegerUtils::byteArrayToNonNegBigInteger).toArray(BigInteger[]::new);

            LOGGER.info("P{} construct cuckoo hash", leftCuckooParty.getPartyId());
            BitVector[] keyVec = z2cParty.getTripletProvider().getCrProvider().randBitVector(
                IntStream.range(0, hashNum).map(i -> CommonConstants.BLOCK_BIT_LENGTH).toArray(), rightHashParty);
            byte[][] keys = Arrays.stream(keyVec).map(BitVector::getBytes).toArray(byte[][]::new);
            cuckooHash = CuckooHashBinFactory.createNoStashCuckooHashBin(envType, hashBinType, leftLen, keys);

            cuckooHash.insertItems(Arrays.asList(transKeyByte));
            hashBinNum = cuckooHash.binNum();
            byte[] hashBinNumBytes = cuckooHash.insertedItems() ? IntUtils.intToByteArray(hashBinNum) : new byte[4];
            send(PtoStep.HASH_SUCCESS_SIGN.ordinal(), leftParty(), Collections.singletonList(hashBinNumBytes));
            send(PtoStep.HASH_SUCCESS_SIGN.ordinal(), rightParty(), Collections.singletonList(hashBinNumBytes));

            if (cuckooHash.insertedItems()) {
                int[] pai = new int[hashBinNum];
                HashMap<BigInteger, Integer> map = new HashMap<>();
                IntStream.range(0, leftLen).forEach(i -> map.put(transKeyPlain[i], i));
                int startPos = leftLen;
                for (int i = 0; i < hashBinNum; i++) {
                    HashBinEntry<byte[]> existBinHashEntry = cuckooHash.getHashBinEntry(i);
                    if (existBinHashEntry == null) {
                        pai[i] = startPos++;
                    } else {
                        BigInteger tmp = BigIntegerUtils.byteArrayToNonNegBigInteger(existBinHashEntry.getItem());
                        pai[i] = map.get(tmp);
                    }
                }
                MathPreconditions.checkEqual("startPos", "hashBinNum", startPos, hashBinNum);
                return pai;
            } else {
                throw new MpcAbortException("cuckoo hash intersection fails");
            }
        } else {
            z2cParty.revealOther(prpRes, leftCuckooParty);
            if (rpc.ownParty().equals(rightHashParty)) {
                BitVector[] keyVec = z2cParty.getTripletProvider().getCrProvider().randBitVector(
                    IntStream.range(0, hashNum).map(i -> CommonConstants.BLOCK_BIT_LENGTH).toArray(), leftCuckooParty);
                prf4Hash = Arrays.stream(keyVec).map(key -> {
                    Prf prf = PrfFactory.createInstance(envType, Integer.BYTES);
                    prf.setKey(key.getBytes());
                    return prf;
                }).toArray(Prf[]::new);
            }
            hashBinNum = IntUtils.byteArrayToInt(receive(PtoStep.HASH_SUCCESS_SIGN.ordinal(), leftCuckooParty).get(0));
            if (hashBinNum == 0) {
                throw new MpcAbortException("cuckoo hash intersection fails");
            }
            return null;
        }
    }

    /**
     * 当payload数量较少的时候，得到key，payload，F，的置换
     *
     * @param left            输入的数据
     * @param pai             需要的置换
     * @param encPart         用于验证正确性的值，如果不存在就说明是semi-honest模式
     * @param leftCuckooParty 由哪一方permute左表
     */
    protected TripletZ2Vector[] permuteExtendLeft(TripletZ2Vector[] left, int[] pai, TripletZ2Vector[] encPart, Party leftCuckooParty) throws MpcAbortException {
        if (rpc.ownParty().equals(leftCuckooParty)) {
            MathPreconditions.checkEqual("pai.length", "hashBinNum", pai.length, hashBinNum);
        }
        int hashBitLen = LongUtils.ceilLog2(hashBinNum);
        TripletZ2Vector[] permuteInput;
        if (encPart != null) {
            permuteInput = new TripletZ2Vector[left.length + encPart.length];
            System.arraycopy(left, 0, permuteInput, 0, left.length);
            System.arraycopy(encPart, 0, permuteInput, left.length, encPart.length);
        } else {
            permuteInput = left;
        }
        int leftCuckooPartyId = leftCuckooParty.getPartyId();
        TripletZ2Vector[] permuteRes = (TripletZ2Vector[]) abb3Party.getShuffleParty().permuteNetwork(permuteInput, pai, hashBinNum,
            leftCuckooParty, rpc.getParty((leftCuckooPartyId + 1) % 3), rpc.getParty((leftCuckooPartyId + 2) % 3));
        if (encPart != null) {
            LOGGER.info("verify correctness of permutation");
            int copyStart = permuteInput.length - hashBitLen * hashNum;
            TripletZ2Vector[][] encPer = IntStream.range(0, hashNum).mapToObj(i ->
                Arrays.copyOfRange(permuteRes, copyStart + i * hashBitLen, copyStart + (i + 1) * hashBitLen)).toArray(TripletZ2Vector[][]::new);
            BitVector[] index = Z2VectorUtils.getBinaryIndex(hashBinNum);
            TripletZ2Vector[] shareIndex = (TripletZ2Vector[]) z2cParty.setPublicValues(index);
            TripletZ2Vector[][] extendShareIndex = IntStream.range(0, hashNum).mapToObj(i -> shareIndex).toArray(TripletZ2Vector[][]::new);
            MpcZ2Vector[] eqSign = batchEq(encPer, extendShareIndex);
            TripletZ2Vector flag = permuteRes[copyStart - 1];
            // 至少有1bit为1，得到的是没有一个equal的sign
            TripletZ2Vector reverseEqual = (TripletZ2Vector) z2cParty.not(eqSign[0]);
            for (int i = 1; i < hashNum; i++) {
                reverseEqual = z2cParty.and(reverseEqual, z2cParty.not(eqSign[i]));
            }
            TripletZ2Vector cheatFlag = z2cParty.and(reverseEqual, flag);
            z2cParty.compareView4Zero(cheatFlag);
            return Arrays.copyOf(permuteRes, copyStart);
        } else {
            return permuteRes;
        }
    }

    /**
     * multiple arrays x == y.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, where z = (x == y).
     * @throws MpcAbortException the protocol failure aborts.
     */
    private MpcZ2Vector[] batchEq(MpcZ2Vector[][] xiArray, MpcZ2Vector[][] yiArray) throws MpcAbortException {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        int[] bitNums = Arrays.stream(xiArray).mapToInt(ea -> ea[0].bitNum()).toArray();
        MpcZ2Vector[] left = IntStream.range(0, xiArray[0].length).mapToObj(i ->
            z2cParty.mergeWithPadding(Arrays.stream(xiArray).map(ea -> ea[i]).toArray(MpcZ2Vector[]::new))
        ).toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] right = IntStream.range(0, yiArray[0].length).mapToObj(i ->
            z2cParty.mergeWithPadding(Arrays.stream(yiArray).map(ea -> ea[i]).toArray(MpcZ2Vector[]::new))
        ).toArray(MpcZ2Vector[]::new);
        return z2IntegerCircuit.eq(left, right).splitWithPadding(bitNums);
    }

    /**
     * 根据右表的key，得到switch network的输入
     *
     * @param rightPrpRes    右表key的lowMc结果
     * @param rightHashParty 由哪一个party来switch
     */
    private int[][] getFun4Right(TripletZ2Vector[] rightPrpRes, Party rightHashParty) throws MpcAbortException {
        if (rpc.ownParty().equals(rightHashParty)) {
            int[][] fun = new int[hashNum][rightPrpRes[0].bitNum()];
            BitVector[] rightPrpResPlain = z2cParty.revealOwn(rightPrpRes);
            if (!isMalicious()) {
                IntStream intStream = parallel ? IntStream.range(0, hashNum).parallel() : IntStream.range(0, hashNum);
                byte[][] prpInput = ZlDatabase.create(envType, parallel, rightPrpResPlain).getBytesData();
                intStream.forEach(i -> IntStream.range(0, prpInput.length).forEach(k ->
                    fun[i][k] = prf4Hash[i].getInteger(prpInput[k], hashBinNum)));
            } else {
                BigInteger[] rightPrpPlain = ZlDatabase.create(envType, parallel, rightPrpResPlain).getBigIntegerData();
                int hashBitLen = LongUtils.ceilLog2(hashBinNum);
                int modNum = (1 << hashBitLen) - 1;
                IntStream transStream = parallel ? IntStream.range(0, rightPrpPlain.length).parallel() : IntStream.range(0, rightPrpPlain.length);
                int[] shiftLen = new int[]{2 * hashBitLen, hashBitLen, 0};
                transStream.forEach(i -> IntStream.range(0, hashNum).forEach(j ->
                    fun[j][i] = rightPrpPlain[i].shiftRight(shiftLen[j]).intValue() & modNum));
            }
            return fun;
        } else {
            z2cParty.revealOther(rightPrpRes, rightHashParty);
            return new int[hashNum][];
        }
    }

    /**
     * switch the permuted left table, if malicious, compare the result prp with index to verify the correctness of rightHashParty's input fun
     *
     * @param extendLeftRes  permuted left table
     * @param rightPrp       the prp of right keys
     * @param targetLen      the length of right table
     * @param rightHashParty the party who compute the function of switch network
     */
    protected TripletZ2Vector[][] switchLeftInput(TripletZ2Vector[] extendLeftRes, TripletZ2Vector[] rightPrp, int targetLen, Party rightHashParty) throws MpcAbortException {
        int[][] fun = this.getFun4Right(rightPrp, rightHashParty);
        Party[] parties = IntStream.range(0, 3).mapToObj(i ->
            rpc.getParty((rightHashParty.getPartyId() + i) % 3)).toArray(Party[]::new);
        TripletZ2Vector[][] res = new TripletZ2Vector[hashNum][];
        if (isMalicious()) {
            BitVector[] plainIndex = Z2VectorUtils.getBinaryIndex(extendLeftRes[0].bitNum());
            TripletZ2Vector[] shareIndex = (TripletZ2Vector[]) z2cParty.setPublicValues(plainIndex);
            TripletZ2Vector[] switchInput = new TripletZ2Vector[extendLeftRes.length + shareIndex.length];
            System.arraycopy(extendLeftRes, 0, switchInput, 0, extendLeftRes.length);
            System.arraycopy(shareIndex, 0, switchInput, extendLeftRes.length, shareIndex.length);
            for (int i = 0; i < hashNum; i++) {
                res[i] = (TripletZ2Vector[]) abb3Party.getShuffleParty().switchNetwork(switchInput, fun[i], targetLen,
                    parties[0], parties[1], parties[2]);
            }
            // verify
            int hashBitLen = LongUtils.ceilLog2(extendLeftRes[0].bitNum());
            LOGGER.info("verify the correctness of right permutation");
            TripletZ2Vector[][] left4Eq = Arrays.stream(res).map(x ->
                Arrays.copyOfRange(x, extendLeftRes.length, x.length)).toArray(TripletZ2Vector[][]::new);
            TripletZ2Vector[][] right4Eq = IntStream.range(0, hashNum).mapToObj(i -> Arrays.copyOfRange(rightPrp,
                rightPrp.length - (hashNum - i) * hashBitLen, rightPrp.length - (hashNum - i - 1) * hashBitLen)).toArray(TripletZ2Vector[][]::new);

            MpcZ2Vector[] eq = batchEq(left4Eq, right4Eq);
            z2cParty.compareView4Zero(Arrays.stream(eq).map(e -> (TripletZ2Vector) z2cParty.not(e)).toArray(TripletZ2Vector[]::new));
            IntStream.range(0, hashNum).forEach(i -> {
                z2cParty.noti(eq[i]);
                res[i] = Arrays.copyOf(res[i], extendLeftRes.length);
            });
        } else {
            for (int i = 0; i < hashNum; i++) {
                res[i] = (TripletZ2Vector[]) abb3Party.getShuffleParty().switchNetwork(extendLeftRes, fun[i], targetLen,
                    parties[0], parties[1], parties[2]);
            }
        }
        return res;
    }

    /**
     * compare the join keys between switched left table and right table, return {hashNum} equal sign vectors
     *
     * @param leftSwitch switched left table
     * @param right      right table
     * @param keyLen     the bit length of join key
     */
    private TripletZ2Vector[] getEqualSignWithDupLeft(TripletZ2Vector[][] leftSwitch, TripletZ2Vector[] right, int keyLen) throws MpcAbortException {
        TripletZ2Vector[][] leftKeys = Arrays.stream(leftSwitch).map(x -> Arrays.copyOf(x, keyLen)).toArray(TripletZ2Vector[][]::new);
        TripletZ2Vector[][] rightKeys = IntStream.range(0, hashNum).mapToObj(i -> Arrays.copyOf(right, keyLen)).toArray(TripletZ2Vector[][]::new);
        MpcZ2Vector[] compRes = batchEq(leftKeys, rightKeys);
        return z2cParty.and(z2cParty.and(
            Arrays.stream(leftSwitch).map(x -> x[x.length - 1]).toArray(TripletZ2Vector[]::new),
            IntStream.range(0, hashNum).mapToObj(i -> right[right.length - 1]).toArray(TripletZ2Vector[]::new)), compRes);
    }

    /**
     * compare and concat the left table and right table
     *
     * @param leftSwitchRes switched left table
     * @param right         right table
     */
    protected TripletZ2Vector[] compareAndConcat4SmallPayload(TripletZ2Vector[][] leftSwitchRes, TripletZ2Vector[] right) throws MpcAbortException {
        int leftDim = leftSwitchRes[0].length, rightDim = right.length;
        // 2. 分别比较三个table对应位置的key，计算出是否真的存在匹配记录
        TripletZ2Vector[] equalTestRes = this.getEqualSignWithDupLeft(leftSwitchRes, right, keyDim);
        TripletZ2Vector[] finalRes = new TripletZ2Vector[rightDim + leftDim - keyDim - 1];
        // 先将已有的右表数据copy过去
        System.arraycopy(right, 0, finalRes, 0, keyDim);
        System.arraycopy(right, keyDim, finalRes, leftDim - 1, rightDim - keyDim - 1);
        // 如果匹配，payload的取值直接复制，否则置为0
        if (leftDim - keyDim - 1 > 0) {
            TripletZ2Vector[] leftPayload = IntStream.range(0, leftDim - keyDim - 1).mapToObj(i ->
                z2cParty.createShareZeros(leftSwitchRes[0][0].bitNum())).toArray(TripletZ2Vector[]::new);
            for (int i = 0; i < hashNum; i++) {
                TripletZ2Vector[] tmpFlag = new TripletZ2Vector[leftPayload.length];
                Arrays.fill(tmpFlag, equalTestRes[i]);
                TripletZ2Vector[] xorRes = z2cParty.xor(leftPayload,
                    Arrays.copyOfRange(leftSwitchRes[i], keyDim, leftSwitchRes[i].length - 1));
                TripletZ2Vector[] andRes = z2cParty.and(tmpFlag, xorRes);
                z2cParty.xori(leftPayload, andRes);
            }
            IntStream.range(0, leftPayload.length).forEach(i -> finalRes[i + keyDim] = leftPayload[i]);
        }
        // 2.1 因为可能同一个记录不止有一个equalTest的结果为1，因为可能存在两个hash值相同的情况，所以先得到 是否没有相等的这一标识
        TripletZ2Vector invEqualFlag = (TripletZ2Vector) z2cParty.not(equalTestRes[0]);
        for (int i = 1; i < hashNum; i++) {
            z2cParty.andi(invEqualFlag, z2cParty.not(equalTestRes[i]));
        }
        finalRes[finalRes.length - 1] = (TripletZ2Vector) z2cParty.not(invEqualFlag);
        return finalRes;
    }
}
