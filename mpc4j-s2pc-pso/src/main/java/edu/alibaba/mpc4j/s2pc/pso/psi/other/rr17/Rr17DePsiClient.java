package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr17;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.structure.filter.Filter;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.PhaseHashBin;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.*;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * RR17 Dual Execution malicious PSI client.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/10/05
 */
public class Rr17DePsiClient<T> extends AbstractPsiClient<T> {
    /**
     * Lcot receiver instance
     */
    private final LcotReceiver lcotReceiver;
    /**
     * Lcot receiverOutput
     */
    private LcotReceiverOutput lcotReceiverOutput;
    /**
     * Inverse Lcot sender (in dual execution)
     */
    private final LcotSender lcotInvSender;
    /**
     * Inverse Lcot senderOutput (in dual execution)
     */
    private LcotSenderOutput lcotInvSenderOutput;
    /**
     * CoinToss Receiver
     */
    private final CoinTossParty coinTossReceiver;
    /**
     * PEQT hash function
     */
    private Hash peqtHash;
    /**
     * h1: {0, 1}^* → {0, 1}^l
     */
    private Hash h1;
    /**
     * LOT input Length
     */
    private int encodeInputByteLength;
    /**
     * the number of hash functions
     */
    private int binNum;
    /**
     * the maximum size of each hash bin
     */
    private int binSize;
    /**
     * hash
     */
    private PhaseHashBin phaseHashBin;
    /**
     * The filter constructed with the PRFs of server's elements
     */
    Filter<byte[]> serverPrfFilter;
    /**
     * The map from the processed results to original values
     */
    Map<BigInteger, T> elementMap;
    /**
     * This parameter decide the number of PhaseHash
     */
    private final int divParam4PhaseHash;
    /**
     * The data in hash table
     */
    private byte[][] clientByteArrays;
    /**
     * Whether the data in hash table is valid
     */
    private boolean[] ind4ValidElement;

    public Rr17DePsiClient(Rpc clientRpc, Party serverParty, Rr17DePsiConfig config) {
        super(Rr17DePsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        lcotReceiver = LcotFactory.createReceiver(clientRpc, serverParty, config.getLcotConfig());
        lcotInvSender = LcotFactory.createSender(clientRpc, serverParty, config.getLcotConfig());
        coinTossReceiver = CoinTossFactory.createReceiver(clientRpc, serverParty, config.getCoinTossConfig());
        divParam4PhaseHash = config.getDivParam4PhaseHash();
        addSubPto(lcotReceiver);
        addSubPto(lcotInvSender);
        addSubPto(coinTossReceiver);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coinTossReceiver.init();
        byte[][] hashKey = coinTossReceiver.coinToss(1, CommonConstants.BLOCK_BIT_LENGTH);
        int maxItemSize = Math.max(maxClientElementSize, maxServerElementSize);
        this.binNum = Math.max(maxItemSize / divParam4PhaseHash, 1);
        this.binSize = MaxBinSizeUtils.expectMaxBinSize(maxItemSize, binNum);
        phaseHashBin = new PhaseHashBin(envType, binNum, maxItemSize, hashKey[0]);

        int l = PsiUtils.getMaliciousPeqtByteLength(maxServerElementSize, maxClientElementSize);
        h1 = HashFactory.createInstance(envType, l);
        encodeInputByteLength = CommonUtils.getByteLength(l * Byte.SIZE - (int) Math.round(Math.floor(DoubleUtils.log2(binNum))));
        lcotReceiver.init(encodeInputByteLength * Byte.SIZE, binNum * binSize);
        lcotInvSender.init(encodeInputByteLength * Byte.SIZE, binNum * binSize);

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
        int peqtByteLength = CommonConstants.STATS_BYTE_LENGTH +
            CommonUtils.getByteLength(2 * (LongUtils.ceilLog2(Math.max(2, (long) binSize * clientElementSize))));
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        elementMap = parallel ? new ConcurrentHashMap<>(clientElementSize) : new HashMap<>(clientElementSize);
        // insert the elements of client into HashBin
        Stream<T> elementStream = parallel ? clientElementArrayList.stream().parallel() : clientElementArrayList.stream();
        phaseHashBin.insertItems(elementStream.map(arr -> {
            BigInteger intArr = BigIntegerUtils.byteArrayToNonNegBigInteger(h1.digestToBytes(ObjectUtils.objectToByteArray(arr)));
            elementMap.put(intArr, arr);
            return intArr;
        }).collect(Collectors.toList()));
        phaseHashBin.insertPaddingItems(BigInteger.ZERO);
        clientByteArrays = generateElementByteArrays();
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, hashTime, "Client Hash Insertion");

        stopWatch.start();
        this.lcotReceiverOutput = lcotReceiver.receive(clientByteArrays);
        this.lcotInvSenderOutput = lcotInvSender.send(binSize * binNum);
        stopWatch.stop();
        long lcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, lcotTime, "Client LOT");

        stopWatch.start();
        // receives PRFs from server
        DataPacketHeader serverPrfHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr17DePsiPtoDesc.PtoStep.SERVER_SEND_PRFS.ordinal(), extraInfo++,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverPrfPayload = rpc.receive(serverPrfHeader).getPayload();
        // 求交集
        Set<T> intersection = handleServerPrf(serverPrfPayload);
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverPrfTime, "Client computes PRFs");
        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }

    private byte[][] generateElementByteArrays() {
        ind4ValidElement = new boolean[binNum * binSize];
        IntStream intStream = parallel ? IntStream.range(0, binNum * binSize).parallel() : IntStream.range(0, binNum * binSize);
        return intStream.mapToObj(elementIndex -> {
            HashBinEntry<BigInteger> hashBinEntry = phaseHashBin.getBin(elementIndex / binSize).get(elementIndex % binSize);
            ind4ValidElement[elementIndex] = hashBinEntry.getHashIndex() == 0;
            return BigIntegerUtils.nonNegBigIntegerToByteArray(hashBinEntry.getItem(), encodeInputByteLength);
        }).toArray(byte[][]::new);
    }

    private Set<T> handleServerPrf(List<byte[]> serverPrfPayload) {
        int peqtHashInputLength = lcotReceiverOutput.getOutputByteLength() + encodeInputByteLength;
        serverPrfFilter = FilterFactory.load(envType, serverPrfPayload);
        // Iterating over hash buckets in hash table
        IntStream intStream = parallel ? IntStream.range(0, binNum * binSize).parallel() : IntStream.range(0, binNum * binSize);
        Set<T> intersection = intStream.mapToObj(elementIndex -> {
            int binIndex = elementIndex / binSize;
            if (ind4ValidElement[elementIndex]) {
                byte[] elementByteArray = clientByteArrays[elementIndex];
                byte[] halfElementPrf = lcotReceiverOutput.getRb(elementIndex);
                for (int i = 0; i < binSize; i++) {
                    byte[] elementPrf = BytesUtils.xor(halfElementPrf, lcotInvSenderOutput.getRb(binIndex * binSize + i, elementByteArray));
                    byte[] clientPrf = peqtHash.digestToBytes(ByteBuffer.allocate(peqtHashInputLength).put(elementByteArray).put(elementPrf).array());
                    if (serverPrfFilter.mightContain(clientPrf)) {
                        return elementMap.get(phaseHashBin.dephaseItem(binIndex, BigIntegerUtils.byteArrayToNonNegBigInteger(elementByteArray)));
                    }
                }
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toSet());
        serverPrfFilter = null;
        phaseHashBin = null;
        return intersection;
    }
}

