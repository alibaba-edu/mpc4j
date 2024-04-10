package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr17;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.commit.Commit;
import edu.alibaba.mpc4j.common.tool.crypto.commit.CommitFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.PhaseHashBin;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;
import org.bouncycastle.crypto.Commitment;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * RR17 Encode-Commit malicious PSI client.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/10/05
 */
public class Rr17EcPsiClient<T> extends AbstractPsiClient<T> {
    /**
     * Lcot receiver
     */
    private final LcotReceiver lcotReceiver;
    /**
     * Lcot receiverOutput
     */
    private LcotReceiverOutput lcotReceiverOutput;
    /**
     * CoinToss Receiver
     */
    private final CoinTossParty coinTossReceiver;
    /**
     * PEQT hash function
     */
    public Hash peqtHash;
    /**
     * h1: {0, 1}^* → {0, 1}^l
     */
    public Hash h1;
    /**
     * hash
     */
    public PhaseHashBin phaseHashBin;
    /**
     * the number of phaseHash bin
     */
    public int binNum;
    /**
     * the maximum size of phaseHash bin
     */
    public int binSize;
    /**
     * LOT input Length
     */
    public int encodeInputByteLength;
    /**
     * Enc PRF
     */
    public Prf prfEnc;
    /**
     * Tag PRF
     */
    public Prf prfTag;
    /**
     * Tag PRF输出的byte长度
     */
    public int tagPrfByteLength;
    /**
     * Element HashMap
     */
    Map<BigInteger, T> elementMap;
    /**
     * Tuple HashMap
     */
    Map<BigInteger, byte[][]> tuplesMap;
    /**
     * This parameter decide the number of PhaseHash
     */
    private final int divParam4PhaseHash;

    public Rr17EcPsiClient(Rpc clientRpc, Party serverParty, Rr17EcPsiConfig config) {
        super(Rr17EcPsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        lcotReceiver = LcotFactory.createReceiver(clientRpc, serverParty, config.getLcotConfig());
        coinTossReceiver = CoinTossFactory.createReceiver(clientRpc, serverParty, config.getCoinTossConfig());
        divParam4PhaseHash = config.getDivParam4PhaseHash();
        addSubPto(lcotReceiver);
        addSubPto(coinTossReceiver);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coinTossReceiver.init();
        byte[][] hashKeys = coinTossReceiver.coinToss(3, CommonConstants.BLOCK_BIT_LENGTH);
        int maxItemSize = Math.max(maxClientElementSize, maxServerElementSize);
        this.binNum = Math.max(maxItemSize / divParam4PhaseHash, 1);
        this.binSize = MaxBinSizeUtils.expectMaxBinSize(maxItemSize, binNum);

        int l = PsiUtils.getMaliciousPeqtByteLength(maxServerElementSize, maxClientElementSize);
        int peqtByteLength = CommonConstants.STATS_BYTE_LENGTH +
            CommonUtils.getByteLength(2 * LongUtils.ceilLog2(Math.max(2, binSize * clientElementSize)));
        tagPrfByteLength = CommonConstants.STATS_BYTE_LENGTH + CommonUtils.getByteLength(64 + LongUtils.ceilLog2(maxClientElementSize));
        encodeInputByteLength = CommonUtils.getByteLength(l * Byte.SIZE - (int) Math.round(Math.floor(DoubleUtils.log2(binNum))));

        h1 = HashFactory.createInstance(envType, l);
        prfEnc = PrfFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        prfEnc.setKey(hashKeys[1]);
        prfTag = PrfFactory.createInstance(envType, tagPrfByteLength);
        prfTag.setKey(hashKeys[2]);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        phaseHashBin = new PhaseHashBin(envType, binNum, maxItemSize, hashKeys[0]);
        lcotReceiver.init(encodeInputByteLength * Byte.SIZE, binNum * binSize);

        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime, "Key exchange and OT init");
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet, int serverSetSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverSetSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        elementMap = new HashMap<>(clientElementSize);
        phaseHashBin.insertItems(clientElementArrayList.stream().map(arr -> {
            BigInteger intArr = BigIntegerUtils.byteArrayToNonNegBigInteger(h1.digestToBytes(ObjectUtils.objectToByteArray(arr)));
            elementMap.put(intArr, arr);
            return intArr;
        }).collect(Collectors.toList()));
        phaseHashBin.insertPaddingItems(BigInteger.ZERO);

        // 桶中的元素，后面的是贮存区中的元素
        byte[][] clientByteArrays = IntStream.range(0, binNum).mapToObj(binIndex ->
                IntStream.range(0, binSize).mapToObj(entryIndex -> {
                    HashBinEntry<BigInteger> hashBinEntry = phaseHashBin.getBin(binIndex).get(entryIndex);
                    return BigIntegerUtils.nonNegBigIntegerToByteArray(hashBinEntry.getItem(), encodeInputByteLength);
                }).collect(Collectors.toList()))
            .flatMap(Collection::stream).toArray(byte[][]::new);
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, hashTime, "Client Hash Insertion");

        stopWatch.start();
        this.lcotReceiverOutput = lcotReceiver.receive(clientByteArrays);
        tuplesMap = generateTupleHashMap();
        stopWatch.stop();
        long lcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, lcotTime, "Client LOT");

        stopWatch.start();
        // 接收服务端Tuples
        DataPacketHeader serverTuplesHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr17EcPsiPtoDesc.PtoStep.SERVER_SEND_TUPLES.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverTuplesPayload = rpc.receive(serverTuplesHeader).getPayload();
        extraInfo++;
        // 求交集
        Set<T> intersection = handleServerTuples(serverTuplesPayload);
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverPrfTime, "Client handles Tuples");

        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }

    private Map<BigInteger, byte[][]> generateTupleHashMap() {
        int encodeInputLength = lcotReceiverOutput.getOutputByteLength() + encodeInputByteLength;
        Map<BigInteger, byte[][]> map = parallel ? new ConcurrentHashMap<>(binNum) : new HashMap<>(binNum);
        IntStream intStream = parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        intStream.forEach(binIndex ->
            IntStream.range(0, binSize).forEach(entryIndex -> {
                HashBinEntry<BigInteger> hashBinEntry = phaseHashBin.getBin(binIndex).get(entryIndex);
                if (hashBinEntry.getHashIndex() == 0) {
                    byte[] elementByteArray = BigIntegerUtils.nonNegBigIntegerToByteArray(phaseHashBin.dephaseItem(binIndex, hashBinEntry.getItem()), h1.getOutputByteLength());
                    byte[] phasedElementByteArray = BigIntegerUtils.nonNegBigIntegerToByteArray(hashBinEntry.getItem(), encodeInputByteLength);
                    byte[] encode = peqtHash.digestToBytes(ByteBuffer.allocate(encodeInputLength)
                        .put(phasedElementByteArray).put(lcotReceiverOutput.getRb(binIndex * binSize + entryIndex)).array());
                    BigInteger tag = BigIntegerUtils.byteArrayToBigInteger(prfTag.getBytes(encode));
                    byte[] enc = prfEnc.getBytes(encode);
                    byte[][] value = {enc, elementByteArray};
                    map.put(tag, value);
                }
            }));
        return map;
    }

    private Set<T> handleServerTuples(List<byte[]> serverTuplesPayload) {
        Commit commitTmp = CommitFactory.createInstance(envType);
        int commitLength = commitTmp.commit(new byte[0]).getCommitment().length;
        int eachMesLength = tagPrfByteLength + CommonConstants.BLOCK_BYTE_LENGTH;
        Stream<byte[]> tupleStream = parallel ? serverTuplesPayload.stream().parallel() : serverTuplesPayload.stream();
        return tupleStream.map(tuple -> {
            Commit commit = CommitFactory.createInstance(envType);
            int secretLength = tuple.length - binSize * eachMesLength - commitLength;
            byte[] serverSecret = Arrays.copyOf(tuple, secretLength);
            byte[] serverCommitment = Arrays.copyOfRange(tuple, secretLength, secretLength + commitLength);
            for (int i = 0; i < binSize; i++) {
                int copyStartIndex = secretLength + commitLength + i * eachMesLength;
                byte[] tagArray = Arrays.copyOfRange(tuple, copyStartIndex, copyStartIndex + tagPrfByteLength);
                BigInteger tag = BigIntegerUtils.byteArrayToBigInteger(tagArray);
                if (tuplesMap.containsKey(tag)) {
                    byte[][] clientTuple = tuplesMap.get(tag);
                    byte[] ry = Arrays.copyOfRange(tuple, copyStartIndex + tagPrfByteLength, copyStartIndex + eachMesLength);
                    BytesUtils.xori(ry, clientTuple[0]);
                    byte[] clientMessage = ByteBuffer.allocate(h1.getOutputByteLength() + ry.length).put(clientTuple[1]).put(ry).array();
                    if (commit.isRevealed(clientMessage, new Commitment(serverSecret, serverCommitment))) {
                        return elementMap.get(BigIntegerUtils.byteArrayToNonNegBigInteger(clientTuple[1]));
                    }
                }
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toSet());
    }
}

