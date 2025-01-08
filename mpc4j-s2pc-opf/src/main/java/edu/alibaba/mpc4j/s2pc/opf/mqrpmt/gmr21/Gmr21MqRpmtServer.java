package edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnPartyOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnReceiverOutput;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.AbstractMqRpmtServer;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.oprf.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GMR21-mqRPMT server.
 *
 * @author Weiran Liu
 * @date 2022/09/10
 */
public class Gmr21MqRpmtServer extends AbstractMqRpmtServer {
    /**
     * OPRF used in cuckoo hash
     */
    private final OprfReceiver cuckooHashOprfReceiver;
    /**
     * OSN
     */
    private final DosnReceiver dosnReceiver;
    /**
     * OSN
     */
    private final RosnReceiver rosnReceiver;
    /**
     * OPRF used in PEQT
     */
    private final OprfSender peqtOprfSender;
    /**
     * OKVS type
     */
    private final Gf2eDokvsType okvsType;
    /**
     * cuckoo hash type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * cuckoo hash num
     */
    private final int cuckooHashNum;
    /**
     * hash for finite field
     */
    private Hash finiteFieldHash;
    /**
     * DOKVS hash keys
     */
    private byte[][] okvsHashKeys;
    /**
     * no-stash cuckoo hash
     */
    private CuckooHashBin<ByteBuffer> cuckooHashBin;
    /**
     * bin num
     */
    private int binNum;
    /**
     * m for DOKVS
     */
    private int okvsM;
    /**
     * permutation π
     */
    private int[] pi;
    /**
     * extend entry bytes
     */
    private byte[][] extendEntryBytes;
    /**
     * f_1, ..., f_m
     */
    private byte[][] fArray;
    /**
     * (t_1, ..., t_m)
     */
    private byte[][] tVector;
    /**
     * a'_1, ..., a'_m
     */
    private byte[][] aPrimeArray;
    /**
     * save the precomputed rosn result
     */
    private RosnReceiverOutput rosnReceiverOutput;

    public Gmr21MqRpmtServer(Rpc serverRpc, Party clientParty, Gmr21MqRpmtConfig config) {
        super(Gmr21MqRpmtPtoDesc.getInstance(), serverRpc, clientParty, config);
        cuckooHashOprfReceiver = OprfFactory.createOprfReceiver(serverRpc, clientParty, config.getCuckooHashOprfConfig());
        addSubPto(cuckooHashOprfReceiver);
        dosnReceiver = DosnFactory.createReceiver(serverRpc, clientParty, config.getOsnConfig());
        addSubPto(dosnReceiver);
        rosnReceiver = RosnFactory.createReceiver(serverRpc, clientParty, config.getRosnConfig());
        addSubPto(rosnReceiver);
        peqtOprfSender = OprfFactory.createOprfSender(serverRpc, clientParty, config.getPeqtOprfConfig());
        addSubPto(peqtOprfSender);
        okvsType = config.getOkvsType();
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxServerElementSize);
        int maxPrfNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType) * maxClientElementSize;
        cuckooHashOprfReceiver.init(maxBinNum, maxPrfNum);
        dosnReceiver.init();
        rosnReceiver.init();
        peqtOprfSender.init(maxBinNum);
        finiteFieldHash = HashFactory.createInstance(envType, Gmr21MqRpmtPtoDesc.FINITE_FIELD_BYTE_LENGTH);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime);

        stopWatch.start();
        int okvsHashKeyNum = Gf2eDokvsFactory.getHashKeyNum(okvsType);
        okvsHashKeys = CommonUtils.generateRandomKeys(okvsHashKeyNum, secureRandom);
        List<byte[]> keysPayload = Arrays.stream(okvsHashKeys).collect(Collectors.toList());
        sendOtherPartyPayload(PtoStep.SERVER_SEND_KEYS.ordinal(), keysPayload);
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, keyTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    /**
     * Do pre-computation.
     *
     * @param serverSetSize server set size.
     */
    public void preCompute(int serverSetSize) throws MpcAbortException {
        checkInitialized();
        MathPreconditions.checkGreater("serverSetSize", serverSetSize, 1);
        MathPreconditions.checkLessOrEqual("serverSetSize", serverSetSize, maxServerElementSize);

        logPhaseInfo(PtoState.PTO_BEGIN, "pre-compute OSN");
        int precomputeBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, serverSetSize);
        int[] precomputePi = PermutationNetworkUtils.randomPermutation(precomputeBinNum, secureRandom);
        rosnReceiverOutput = rosnReceiver.rosn(precomputePi, Gmr21MqRpmtPtoDesc.FINITE_FIELD_BYTE_LENGTH);
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public ByteBuffer[] mqRpmt(Set<ByteBuffer> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> cuckooHashKeyPayload = generateCuckooHashKeyPayload();
        sendOtherPartyPayload(PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), cuckooHashKeyPayload);
        binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, serverElementSize);
        if (validPrecomputation()) {
            pi = IntUtils.clone(rosnReceiverOutput.getPi());
        } else {
            pi = PermutationNetworkUtils.randomPermutation(binNum, secureRandom);
        }
        okvsM = Gf2eDokvsFactory.getM(envType, okvsType, clientElementSize * cuckooHashNum);
        Hash peqtHash = HashFactory.createInstance(envType, Gmr21MqRpmtPtoDesc.getPeqtByteLength(binNum));

        stopWatch.stop();
        long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, cuckooHashTime, "Server generates cuckoo hash keys and data structure");

        stopWatch.start();
        generateCuckooHashOprfInput();
        OprfReceiverOutput cuckooHashOprfReceiverOutput = cuckooHashOprfReceiver.oprf(extendEntryBytes);
        IntStream oprfIntStream = parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        fArray = oprfIntStream
            .mapToObj(cuckooHashOprfReceiverOutput::getPrf)
            .map(finiteFieldHash::digestToBytes)
            .toArray(byte[][]::new);
        stopWatch.stop();
        long cuckooHashOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, cuckooHashOprfTime, "Server runs OPRF for cuckoo hash bins");

        List<byte[]> okvsPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_OKVS.ordinal());

        stopWatch.start();
        handleOkvsPayload(okvsPayload);
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, okvsTime, "Server handles OKVS");

        stopWatch.start();
        DosnPartyOutput osnReceiverOutput;
        if (validPrecomputation()) {
            osnReceiverOutput = dosnReceiver.dosn(pi, Gmr21MqRpmtPtoDesc.FINITE_FIELD_BYTE_LENGTH, rosnReceiverOutput);
            rosnReceiverOutput = null;
        } else {
            osnReceiverOutput = dosnReceiver.dosn(pi, Gmr21MqRpmtPtoDesc.FINITE_FIELD_BYTE_LENGTH);
        }
        handleOsnReceiverOutput(osnReceiverOutput);
        stopWatch.stop();
        long osnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 5, osnTime, "Server runs OSN");

        stopWatch.start();
        OprfSenderOutput peqtOprfSenderOutput = peqtOprfSender.oprf(binNum);
        IntStream aPrimeOprfIntStream = parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        List<byte[]> aPrimeOprfPayload = aPrimeOprfIntStream
            .mapToObj(aPrimeIndex -> peqtOprfSenderOutput.getPrf(aPrimeIndex, aPrimeArray[aPrimeIndex]))
            .map(peqtHash::digestToBytes)
            .collect(Collectors.toList());
        sendOtherPartyPayload(PtoStep.SERVER_SEND_A_PRIME_OPRFS.ordinal(), aPrimeOprfPayload);
        ByteBuffer[] serverVector = generateServerVector();
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, peqtTime, "Server runs OPRF for PEQT");

        logPhaseInfo(PtoState.PTO_END);
        return serverVector;
    }

    private boolean validPrecomputation() {
        return rosnReceiverOutput != null && binNum == rosnReceiverOutput.getNum();
    }

    private List<byte[]> generateCuckooHashKeyPayload() {
        cuckooHashBin = CuckooHashBinFactory.createEnforceNoStashCuckooHashBin(
            envType, cuckooHashBinType, serverElementSize, serverElementArrayList, secureRandom
        );
        cuckooHashBin.insertPaddingItems(BOT_ELEMENT_BYTE_BUFFER);
        return Arrays.stream(cuckooHashBin.getHashKeys()).collect(Collectors.toList());
    }

    private void generateCuckooHashOprfInput() {
        extendEntryBytes = IntStream.range(0, binNum)
            .mapToObj(binIndex -> {
                HashBinEntry<ByteBuffer> hashBinEntry = cuckooHashBin.getHashBinEntry(binIndex);
                int hashIndex = hashBinEntry.getHashIndex();
                byte[] entryBytes = hashBinEntry.getItemByteArray();
                ByteBuffer extendEntryByteBuffer = ByteBuffer.allocate(entryBytes.length + Integer.BYTES);
                // x || i
                extendEntryByteBuffer.put(entryBytes);
                extendEntryByteBuffer.putInt(hashIndex);
                return extendEntryByteBuffer.array();
            })
            .toArray(byte[][]::new);
    }

    private void handleOkvsPayload(List<byte[]> okvsPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(okvsPayload.size() == okvsM);
        byte[][] storage = okvsPayload.toArray(new byte[0][]);
        Gf2eDokvs<ByteBuffer> okvs = Gf2eDokvsFactory.createInstance(
            envType, okvsType, clientElementSize * cuckooHashNum,
            Gmr21MqRpmtPtoDesc.FINITE_FIELD_BYTE_LENGTH * Byte.SIZE, okvsHashKeys
        );
        IntStream okvsDecodeIntStream = parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        tVector = okvsDecodeIntStream
            .mapToObj(index -> {
                // extend entries
                ByteBuffer key = ByteBuffer.wrap(extendEntryBytes[index]);
                byte[] pi = okvs.decode(storage, key);
                byte[] fi = fArray[index];
                BytesUtils.xori(pi, fi);
                return pi;
            })
            .toArray(byte[][]::new);
        fArray = null;
        extendEntryBytes = null;
    }

    private void handleOsnReceiverOutput(DosnPartyOutput osnReceiverOutput) {
        byte[][] tPiVector = PermutationNetworkUtils.permutation(pi, tVector);
        tVector = null;
        aPrimeArray = IntStream.range(0, binNum)
            .mapToObj(index -> {
                byte[] ai = osnReceiverOutput.getShare(index);
                byte[] ti = tPiVector[index];
                BytesUtils.xori(ai, ti);
                return ai;
            })
            .toArray(byte[][]::new);
    }

    private ByteBuffer[] generateServerVector() {
        int[] permutedIndexVector = IntStream.range(0, binNum).toArray();
        int[] permutedIndexArray = PermutationNetworkUtils.permutation(pi, permutedIndexVector);
        IntStream binIndexIntStream = parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        ByteBuffer[] serverVector = binIndexIntStream
            .mapToObj(index -> {
                int permuteIndex = permutedIndexArray[index];
                HashBinEntry<ByteBuffer> entry = cuckooHashBin.getHashBinEntry(permuteIndex);
                if (entry.getHashIndex() == HashBinEntry.DUMMY_ITEM_HASH_INDEX) {
                    return null;
                } else {
                    return entry.getItem();
                }
            })
            .toArray(ByteBuffer[]::new);
        pi = null;
        cuckooHashBin = null;
        return serverVector;
    }
}
