package edu.alibaba.mpc4j.s2pc.upso.upsu.zlp24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.SparseGf2eDokvs;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.PmPeqtFactory;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.PmPeqtSender;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.upso.upsu.AbstractUpsuSender;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.*;
import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.*;
import static edu.alibaba.mpc4j.s2pc.upso.upsu.zlp24.Zlp24PeqtUpsuPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.upso.upsu.zlp24.Zlp24PeqtUpsuPtoDesc.getInstance;

/**
 * ZLP24 UPSU sender.
 *
 * @author Liqiang Peng
 * @date 2024/3/21
 */
public class Zlp24PeqtUpsuSender extends AbstractUpsuSender {
    /**
     * single - query OPRF receiver
     */
    private final SqOprfReceiver sqOprfReceiver;
    /**
     * permute matrix PEQT sender
     */
    private final PmPeqtSender pmPeqtSender;
    /**
     * batch index PIR client
     */
    private final BatchIndexPirClient batchIndexPirClient;
    /**
     * core COT sender
     */
    private final CoreCotSender coreCotSender;
    /**
     * cuckoo hash type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * Gf2e dokvs type
     */
    private final Gf2eDokvsType gf2eDokvsType;
    /**
     * dokvs
     */
    private SparseGf2eDokvs<ByteBuffer> dokvs;
    /**
     * cuckoo hash bin
     */
    private CuckooHashBin<ByteBuffer> cuckooHashBin;
    /**
     * value bit length
     */
    private int l;
    /**
     * value byte length
     */
    private int byteL;

    public Zlp24PeqtUpsuSender(Rpc senderRpc, Party receiverParty, Zlp24PeqtUpsuConfig config) {
        super(getInstance(), senderRpc, receiverParty, config);
        sqOprfReceiver = SqOprfFactory.createReceiver(senderRpc, receiverParty, config.getSqOprfConfig());
        addSubPto(sqOprfReceiver);
        pmPeqtSender = PmPeqtFactory.createSender(senderRpc, receiverParty, config.getPmPeqtConfig());
        addSubPto(pmPeqtSender);
        batchIndexPirClient = BatchIndexPirFactory.createClient(senderRpc, receiverParty, config.getBatchIndexPirConfig());
        addSubPto(batchIndexPirClient);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        cuckooHashBinType = config.getCuckooHashBinType();
        gf2eDokvsType = config.getGf2eDokvsType();
    }

    @Override
    public void init(int maxSenderElementSize, int receiverElementSize) throws MpcAbortException {
        setInitInput(maxSenderElementSize, receiverElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        DataPacketHeader cuckooHashKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeysPayload = rpc.receive(cuckooHashKeysHeader).getPayload();
        int cuckooHashNum = getHashNum(cuckooHashBinType);
        MpcAbortPreconditions.checkArgument(cuckooHashKeysPayload.size() == cuckooHashNum);
        byte[][] cuckooHashKeys = cuckooHashKeysPayload.toArray(new byte[0][]);

        stopWatch.start();
        // init core COT
        int cuckooHashBinNum = getBinNum(cuckooHashBinType, maxSenderElementSize);
        cuckooHashBin = CuckooHashBinFactory.createNoStashCuckooHashBin(
            envType, cuckooHashBinType, maxSenderElementSize, cuckooHashKeys
        );
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        coreCotSender.init(delta, cuckooHashBinNum);
        // init single query OPRF
        sqOprfReceiver.init(maxSenderElementSize);
        // send dokvs hash keys
        int dokvsHashKeyNum = getHashKeyNum(gf2eDokvsType);
        byte[][] dokvsHashKeys = CommonUtils.generateRandomKeys(dokvsHashKeyNum, secureRandom);
        DataPacketHeader dokvsHashKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_DOKVS_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(dokvsHashKeysHeader, Arrays.stream(dokvsHashKeys).collect(Collectors.toList())));
        // init batch index PIR
        int maxBinSize = MaxBinSizeUtils.approxMaxBinSize(receiverElementSize, cuckooHashBinNum);
        l = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(maxBinSize);
        byteL = CommonUtils.getByteLength(l);
        dokvs = createSparseInstance(envType, gf2eDokvsType, receiverElementSize * cuckooHashNum, l, dokvsHashKeys);
        batchIndexPirClient.init(dokvs.sparsePositionRange(), l, dokvsHashKeyNum * maxSenderElementSize);
        // init permute matrix PEQT
        pmPeqtSender.init(1, cuckooHashBinNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psu(Set<ByteBuffer> senderElementSet, int elementByteLength) throws MpcAbortException {
        setPtoInput(senderElementSet, elementByteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive dokvs dense part
        DataPacketHeader denseOkvsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_DENSE_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> denseOkvsPayload = rpc.receive(denseOkvsHeader).getPayload();
        MpcAbortPreconditions.checkArgument(dokvs.densePositionRange() == denseOkvsPayload.size());

        stopWatch.start();
        generateCuckooHashBin();
        stopWatch.stop();
        long cuckooHashBinTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 6, cuckooHashBinTime, "sender cuckoo hash bin");

        // batch OPRF
        stopWatch.start();
        byte[][] oprfInput = IntStream.range(0, senderElementSize)
            .mapToObj(i -> senderElementList.get(i).array())
            .toArray(byte[][]::new);
        SqOprfReceiverOutput oprfReceiverOutput = sqOprfReceiver.oprf(oprfInput);
        Map<ByteBuffer, byte[]> itemPrfMap = handleReceiverOprfOutput(oprfReceiverOutput);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 6, oprfTime, "sender executes OPRF");

        stopWatch.start();
        List<Integer> retrievalIndexList = generateRetrievalIndexList();
        Map<Integer, byte[]> okvsSparsePayload = batchIndexPirClient.pir(retrievalIndexList);
        MpcAbortPreconditions.checkArgument(retrievalIndexList.size() == okvsSparsePayload.size());
        stopWatch.stop();
        long batchIndexPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 6, batchIndexPirTime, "sender executes PIR");

        stopWatch.start();
        // recover okvs storage
        byte[][] dokvsStorage = generateOkvsStorage(denseOkvsPayload, okvsSparsePayload, retrievalIndexList);
        stopWatch.stop();
        long generateOkvsStorageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 6, generateOkvsStorageTime, "sender recovers OKVS storage");

        stopWatch.start();
        byte[][][] pmPeqtInput = generatePeqtInput(itemPrfMap, dokvsStorage);
        List<Integer> shufflePermutationMap = IntStream.range(0, cuckooHashBin.binNum()).boxed().collect(Collectors.toList());
        Collections.shuffle(shufflePermutationMap, secureRandom);
        int[] permutationMap = shufflePermutationMap.stream().mapToInt(permutation -> permutation).toArray();
        pmPeqtSender.pmPeqt(pmPeqtInput, new int[]{0}, permutationMap, byteL);
        stopWatch.stop();
        long pmPeqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 6, pmPeqtTime, "sender executes pm-PEQT");

        stopWatch.start();
        CotSenderOutput cotSenderOutput = coreCotSender.send(cuckooHashBin.binNum());
        Prg encPrg = PrgFactory.createInstance(envType, elementByteLength);
        IntStream encIntStream = IntStream.range(0, cuckooHashBin.binNum());
        encIntStream = parallel ? encIntStream.parallel() : encIntStream;
        List<byte[]> encPayload = encIntStream
            .mapToObj(index -> {
                // do not need CRHF since we call prg
                byte[] ciphertext = encPrg.extendToBytes(cotSenderOutput.getR0(index));
                BytesUtils.xori(ciphertext, cuckooHashBin.getHashBinEntry(permutationMap[index]).getItemByteArray());
                return ciphertext;
            })
            .collect(Collectors.toList());
        DataPacketHeader encHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_ENC_ELEMENTS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(encHeader, encPayload));
        stopWatch.stop();
        long encTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 6, 6, encTime, "sender executes COT");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * sender handles OPRF output.
     *
     * @param sqOprfReceiverOutput single - query OPRF receiver output.
     * @return item prf map.
     */
    private Map<ByteBuffer, byte[]> handleReceiverOprfOutput(SqOprfReceiverOutput sqOprfReceiverOutput) {
        Prg prg = PrgFactory.createInstance(envType, byteL);
        IntStream intStream = IntStream.range(0, senderElementSize);
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> oprfOutput = intStream
            .mapToObj(sqOprfReceiverOutput::getPrf)
            .map(prg::extendToBytes)
            .peek(bytes -> BytesUtils.reduceByteArray(bytes, l))
            .collect(Collectors.toList());
        return IntStream.range(0, senderElementSize)
            .boxed()
            .collect(Collectors.toMap(i -> senderElementList.get(i), oprfOutput::get, (a, b) -> b));
    }

    /**
     * generate sparse okvs retrieval index list.
     *
     * @return sparse okvs retrieval index list.
     */
    private List<Integer> generateRetrievalIndexList() {
        return IntStream.range(0, cuckooHashBin.binNum())
            .mapToObj(i -> cuckooHashBin.getHashBinEntry(i))
            .filter(entry -> entry.getHashIndex() >= 0)
            .map(entry ->
                ByteBuffer.allocate(elementByteLength + Integer.BYTES)
                    .put(entry.getItemByteArray())
                    .putInt(entry.getHashIndex())
            )
            .map(key -> dokvs.sparsePositions(key))
            .flatMapToInt(Arrays::stream)
            .distinct()
            .boxed()
            .collect(Collectors.toList());
    }

    /**
     * generate dokvs storage.
     *
     * @param okvsDensePayload   dokvs dense payload.
     * @param okvsSparsePayload  dokvs sparse payload.
     * @param retrievalIndexList retrieval index list.
     * @return dokvs storage.
     */
    private byte[][] generateOkvsStorage(List<byte[]> okvsDensePayload, Map<Integer, byte[]> okvsSparsePayload,
                                         List<Integer> retrievalIndexList) {
        byte[][] dokvsStorage = new byte[dokvs.getM()][];
        IntStream.range(0, retrievalIndexList.size()).forEach(i ->
            dokvsStorage[retrievalIndexList.get(i)] = BytesUtils.clone(okvsSparsePayload.get(retrievalIndexList.get(i)))
        );
        IntStream.range(0, dokvs.densePositionRange()).forEach(i ->
            dokvsStorage[i + dokvs.sparsePositionRange()] = BytesUtils.clone(okvsDensePayload.get(i))
        );
        okvsDensePayload.clear();
        okvsSparsePayload.clear();
        return dokvsStorage;
    }

    /**
     * generate permute matrix PEQT input.
     *
     * @param itemPrfMap   item prf map.
     * @param dokvsStorage dokvs storage.
     * @return permute matrix PEQT input.
     */
    private byte[][][] generatePeqtInput(Map<ByteBuffer, byte[]> itemPrfMap, byte[][] dokvsStorage) {
        IntStream intStream = IntStream.range(0, cuckooHashBin.binNum());
        intStream = parallel ? intStream.parallel() : intStream;
        return new byte[][][]{intStream
            .mapToObj(i -> {
                HashBinEntry<ByteBuffer> entry = cuckooHashBin.getHashBinEntry(i);
                if (entry.getHashIndex() >= 0) {
                    ByteBuffer key = ByteBuffer.allocate(elementByteLength + Integer.BYTES)
                        .put(entry.getItemByteArray())
                        .putInt(entry.getHashIndex());
                    return BytesUtils.xor(dokvs.decode(dokvsStorage, key), itemPrfMap.get(entry.getItem()));
                } else {
                    byte[] random = new byte[byteL];
                    secureRandom.nextBytes(random);
                    return random;
                }
            }).toArray(byte[][]::new)};
    }

    /**
     * generate cuckoo hash bin.
     */
    private void generateCuckooHashBin() {
        cuckooHashBin.insertItems(senderElementList);
        cuckooHashBin.insertPaddingItems(botElementByteBuffer);
    }
}
