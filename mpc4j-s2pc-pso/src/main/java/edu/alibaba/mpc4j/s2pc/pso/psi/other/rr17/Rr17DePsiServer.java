package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr17;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
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
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RR17 Dual Execution malicious PSI server.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/10/05
 */
public class Rr17DePsiServer<T> extends AbstractPsiServer<T> {
    /**
     * Lcot sender
     */
    private final LcotSender lcotSender;
    /**
     * Lcot senderOutput
     */
    private LcotSenderOutput lcotSenderOutput;
    /**
     * Lcot receiver in the second time
     */
    private final LcotReceiver lcotInvReceiver;
    /**
     * Lcot receiverOutput in the second time
     */
    private LcotReceiverOutput lcotInvReceiverOutput;
    /**
     * CoinToss sender
     */
    private final CoinTossParty coinTossSender;
    /**
     * filter type
     */
    private final FilterFactory.FilterType filterType;
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
     * hash
     */
    private PhaseHashBin phaseHashBin;
    /**
     * The number of hash bin
     */
    private int binNum;
    /**
     * The maximum size of each hash bin
     */
    private int binSize;
    /**
     * This parameter decide the number of PhaseHash
     */
    private final int divParam4PhaseHash;
    /**
     * The data in hash table
     */
    private byte[][] serverByteArrays;
    /**
     * Whether the data in hash table is valid
     */
    private boolean[] ind4ValidElement;

    public Rr17DePsiServer(Rpc serverRpc, Party clientParty, Rr17DePsiConfig config) {
        super(Rr17DePsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        lcotSender = LcotFactory.createSender(serverRpc, clientParty, config.getLcotConfig());
        lcotInvReceiver = LcotFactory.createReceiver(serverRpc, clientParty, config.getLcotConfig());
        coinTossSender = CoinTossFactory.createSender(serverRpc, clientParty, config.getCoinTossConfig());
        divParam4PhaseHash = config.getDivParam4PhaseHash();
        addSubPto(lcotSender);
        addSubPto(lcotInvReceiver);
        addSubPto(coinTossSender);
        filterType = config.getFilterType();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coinTossSender.init();
        byte[][] hashKeys = coinTossSender.coinToss(1, CommonConstants.BLOCK_BIT_LENGTH);
        int maxItemSize = Math.max(maxClientElementSize, maxServerElementSize);
        binNum = Math.max(maxItemSize / divParam4PhaseHash, 1);
        binSize = MaxBinSizeUtils.expectMaxBinSize(maxItemSize, binNum);
        phaseHashBin = new PhaseHashBin(envType, binNum, maxItemSize, hashKeys[0]);
        int l = PsiUtils.getMaliciousPeqtByteLength(maxServerElementSize, maxClientElementSize);
        h1 = HashFactory.createInstance(envType, l);
        encodeInputByteLength = CommonUtils.getByteLength(l * Byte.SIZE - (int) Math.round(Math.floor(DoubleUtils.log2(binNum))));

        // init OT
        lcotSender.init(encodeInputByteLength * Byte.SIZE, binNum * binSize);
        lcotInvReceiver.init(encodeInputByteLength * Byte.SIZE, binNum * binSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime, "Server exchange key and init OT");
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int peqtByteLength = CommonConstants.STATS_BYTE_LENGTH +
            CommonUtils.getByteLength(2 * (LongUtils.ceilLog2(Math.max(2, (long) binSize * clientElementSize))));
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);

        phaseHashBin.insertItems(serverElementArrayList.stream().map(arr ->
                BigIntegerUtils.byteArrayToNonNegBigInteger(h1.digestToBytes(ObjectUtils.objectToByteArray(arr))))
            .collect(Collectors.toList()));
        phaseHashBin.insertPaddingItems(BigInteger.ZERO);
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, hashTime, "Server hash insertion");

        stopWatch.start();
        lcotSenderOutput = lcotSender.send(binSize * binNum);
        serverByteArrays = generateElementByteArrays();
        lcotInvReceiverOutput = lcotInvReceiver.receive(serverByteArrays);
        stopWatch.stop();
        long lcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, lcotTime, "Server LOT");

        stopWatch.start();
        List<byte[]> serverPrfPayload = generatePrfPayload();
        DataPacketHeader serverPrfHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr17DePsiPtoDesc.PtoStep.SERVER_SEND_PRFS.ordinal(), extraInfo++,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPrfHeader, serverPrfPayload));
        lcotSenderOutput = null;
        lcotInvReceiverOutput = null;
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverPrfTime, "Server computes PRFs");
        logPhaseInfo(PtoState.PTO_END);
    }

    private byte[][] generateElementByteArrays() {
        ind4ValidElement = new boolean[binNum * binSize];
        return IntStream.range(0, ind4ValidElement.length).mapToObj(elementIndex -> {
            HashBinEntry<BigInteger> hashBinEntry = phaseHashBin.getBin(elementIndex / binSize).get(elementIndex % binSize);
            ind4ValidElement[elementIndex] = hashBinEntry.getHashIndex() == 0;
            return BigIntegerUtils.nonNegBigIntegerToByteArray(hashBinEntry.getItem(), encodeInputByteLength);
        }).toArray(byte[][]::new);
    }

    private List<byte[]> generatePrfPayload() {
        int peqtHashInputLength = lcotSenderOutput.getOutputByteLength() + encodeInputByteLength;
        IntStream serverElementStream = parallel ? IntStream.range(0, binNum * binSize).parallel() : IntStream.range(0, binNum * binSize);
        List<byte[]> prfList = Collections.synchronizedList(new LinkedList<>());
        serverElementStream.forEach(index -> {
            if (ind4ValidElement[index]) {
                int binIndex = index / binSize;
                byte[] halfElementPrf = lcotInvReceiverOutput.getRb(index);
                byte[] elementByteArray = serverByteArrays[index];
                for (int i = 0; i < binSize; i++) {
                    byte[] elementPrf = BytesUtils.xor(halfElementPrf, lcotSenderOutput.getRb(
                        binIndex * binSize + i, BytesUtils.paddingByteArray(elementByteArray, encodeInputByteLength)));
                    prfList.add(peqtHash.digestToBytes(ByteBuffer.allocate(peqtHashInputLength)
                        .put(elementByteArray).put(elementPrf).array()));
                }
            }
        });

//        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime, "Server exchange key and init OT");

        Collections.shuffle(prfList, secureRandom);
        // constructing filter
        Filter<byte[]> prfFilter = FilterFactory.load(envType, filterType, serverElementSize * binSize, secureRandom);
        prfList.forEach(prfFilter::put);
        return prfFilter.save();
    }

}
