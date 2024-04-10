package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr17;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
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
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;
import org.bouncycastle.crypto.Commitment;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * RR17 Encode-Commit malicious PSI server.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/10/05
 */
public class Rr17EcPsiServer<T> extends AbstractPsiServer<T> {
    /**
     * lcot sender
     */
    private final LcotSender lcotSender;
    /**
     * CoinToss sender
     */
    private final CoinTossParty coinTossSender;
    /**
     * the number of phaseHash bin
     */
    public int binNum;
    /**
     * the maximum size of phaseHash bin
     */
    public int binSize;
    /**
     * OPRF发送方输出
     */
    private LcotSenderOutput lcotSenderOutput;

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
     * Tag PRF byte length
     */
    public int tagPrfByteLength;
    /**
     * This parameter decide the number of PhaseHash
     */
    private final int divParam4PhaseHash;

    public Rr17EcPsiServer(Rpc serverRpc, Party clientParty, Rr17EcPsiConfig config) {
        super(Rr17EcPsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        lcotSender = LcotFactory.createSender(serverRpc, clientParty, config.getLcotConfig());
        coinTossSender = CoinTossFactory.createSender(serverRpc, clientParty, config.getCoinTossConfig());
        divParam4PhaseHash = config.getDivParam4PhaseHash();
        addSubPto(lcotSender);
        addSubPto(coinTossSender);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxItemSize = Math.max(maxClientElementSize, maxServerElementSize);
        binNum = Math.max(maxItemSize / divParam4PhaseHash, 1);
        binSize = MaxBinSizeUtils.expectMaxBinSize(maxItemSize, binNum);
        coinTossSender.init();
        byte[][] hashKeys = coinTossSender.coinToss(3, CommonConstants.BLOCK_BIT_LENGTH);

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

        lcotSender.init(encodeInputByteLength * Byte.SIZE, binNum * binSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime, "Key exchange and OT init");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        phaseHashBin.insertItems(serverElementArrayList.stream().map(arr ->
            BigIntegerUtils.byteArrayToNonNegBigInteger(h1.digestToBytes(ObjectUtils.objectToByteArray(arr)))).collect(Collectors.toList()));
        phaseHashBin.insertPaddingItems(BigInteger.ZERO);
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, hashTime, "Server Hash Insertion");

        stopWatch.start();
        lcotSenderOutput = lcotSender.send(binSize * binNum);
        stopWatch.stop();
        long lcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, lcotTime, "Server LOT");

        stopWatch.start();
        // server sends the filter of PRFs
        List<byte[]> serverPrfPayload = generatePrfPayload();
        DataPacketHeader serverPrfHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr17EcPsiPtoDesc.PtoStep.SERVER_SEND_TUPLES.ordinal(), extraInfo++,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPrfHeader, serverPrfPayload));
        lcotSenderOutput = null;
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverPrfTime, "Server computes Tuples");

        logPhaseInfo(PtoState.PTO_END);
    }


    private List<byte[]> generatePrfPayload() {
        IntStream serverElementStream = parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        return serverElementStream.mapToObj(binIndex -> {
            Commit commit = CommitFactory.createInstance(envType);
            return IntStream.range(0, binSize).mapToObj(entryIndex -> {
                HashBinEntry<BigInteger> hashBinEntry = phaseHashBin.getBin(binIndex).get(entryIndex);
                if (hashBinEntry.getHashIndex() == 0) {
                    byte[] rx = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
                    byte[] elementByteArray = BigIntegerUtils.nonNegBigIntegerToByteArray(phaseHashBin.dephaseItem(binIndex, hashBinEntry.getItem()), h1.getOutputByteLength());
                    byte[] phasedElementByteArray = BigIntegerUtils.nonNegBigIntegerToByteArray(hashBinEntry.getItem(), encodeInputByteLength);
                    Commitment commitment = commit.commit(ByteBuffer.allocate(
                        rx.length + elementByteArray.length).put(elementByteArray).put(rx).array());
                    ByteBuffer tupleArray = ByteBuffer.allocate(commitment.getSecret().length + commitment.getCommitment().length
                            + binSize * (tagPrfByteLength + CommonConstants.BLOCK_BYTE_LENGTH))
                        .put(commitment.getSecret()).put(commitment.getCommitment());
                    for (int i = 0; i < binSize; i++) {
                        byte[] encode = peqtHash.digestToBytes(ByteBuffer.allocate(
                                lcotSenderOutput.getOutputByteLength() + phasedElementByteArray.length)
                            .put(phasedElementByteArray).put(lcotSenderOutput.getRb(binIndex * binSize + i, phasedElementByteArray)).array());
                        byte[] tag = prfTag.getBytes(encode);
                        byte[] enc = prfEnc.getBytes(encode);
                        tupleArray.put(tag).put(BytesUtils.xor(rx, enc));
                    }
                    return tupleArray.array();
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }).flatMap(Collection::stream).collect(Collectors.toList());
    }
}
