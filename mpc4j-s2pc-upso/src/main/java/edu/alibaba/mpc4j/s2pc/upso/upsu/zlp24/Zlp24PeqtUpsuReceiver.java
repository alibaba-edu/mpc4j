package edu.alibaba.mpc4j.s2pc.upso.upsu.zlp24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.SparseGf2eDokvs;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.PmPeqtFactory;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.PmPeqtReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.upso.upsu.AbstractUpsuReceiver;
import edu.alibaba.mpc4j.s2pc.upso.upsu.UpsuReceiverOutput;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.*;
import static edu.alibaba.mpc4j.s2pc.upso.upsu.zlp24.Zlp24PeqtUpsuPtoDesc.*;

/**
 * ZLP24 UPSU receiver.
 *
 * @author Liqiang Peng
 * @date 2024/3/20
 */
public class Zlp24PeqtUpsuReceiver extends AbstractUpsuReceiver {
    /**
     * single - query OPRF sender
     */
    private final SqOprfSender sqOprfSender;
    /**
     * permute matrix PEQT receiver
     */
    private final PmPeqtReceiver pmPeqtReceiver;
    /**
     * batch index PIR server
     */
    private final BatchIndexPirServer batchIndexPirServer;
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * cuckoo hash type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * Gf2e dokvs type
     */
    private final Gf2eDokvsFactory.Gf2eDokvsType gf2eDokvsType;
    /**
     * dokvs dense payload
     */
    private List<byte[]> dokvsDensePayload;
    /**
     * random items
     */
    private byte[][][] r;
    /**
     * cuckoo hash bin num
     */
    private int cuckooHashBinNum;
    /**
     * single query key
     */
    private SqOprfKey sqOprfKey;
    /**
     * value bit length
     */
    private int l;
    /**
     * value byte length
     */
    private int byteL;

    public Zlp24PeqtUpsuReceiver(Rpc receiverRpc, Party senderParty, Zlp24PeqtUpsuConfig config) {
        super(getInstance(), receiverRpc, senderParty, config);
        sqOprfSender = SqOprfFactory.createSender(receiverRpc, senderParty, config.getSqOprfConfig());
        addSubPto(sqOprfSender);
        pmPeqtReceiver = PmPeqtFactory.createReceiver(receiverRpc, senderParty, config.getPmPeqtConfig());
        addSubPto(pmPeqtReceiver);
        batchIndexPirServer = BatchIndexPirFactory.createServer(receiverRpc, senderParty, config.getBatchIndexPirConfig());
        addSubPto(batchIndexPirServer);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        cuckooHashBinType = config.getCuckooHashBinType();
        gf2eDokvsType = config.getGf2eDokvsType();
    }

    @Override
    public void init(Set<ByteBuffer> receiverElementSet, int maxSenderElementSize, int elementByteLength)
        throws MpcAbortException {
        setInitInput(receiverElementSet, maxSenderElementSize, elementByteLength);

        stopWatch.start();
        // send cuckoo hash keys
        int cuckooHashNum = getHashNum(cuckooHashBinType);
        byte[][] cuckooHashKeys = CommonUtils.generateRandomKeys(cuckooHashNum, secureRandom);
        DataPacketHeader cuckooHashKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeysHeader, Arrays.stream(cuckooHashKeys).collect(Collectors.toList())));
        // init core COT
        cuckooHashBinNum = getBinNum(cuckooHashBinType, maxSenderElementSize);
        coreCotReceiver.init(cuckooHashBinNum);
        // init OPRF
        sqOprfKey = sqOprfSender.keyGen();
        sqOprfSender.init(maxSenderElementSize, sqOprfKey);
        int maxBinSize = MaxBinSizeUtils.approxMaxBinSize(receiverElementSize, cuckooHashBinNum);
        l = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(maxBinSize);
        byteL = CommonUtils.getByteLength(l);
        Map<ByteBuffer, byte[]> receiverElementPrfMap = computePrf();
        // simple hash bin
        List<List<HashBinEntry<ByteBuffer>>> completeHashBin = generateCompleteHashBin(cuckooHashKeys);
        // receiver dokvs hash keys
        DataPacketHeader dokvsHashKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_DOKVS_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> dokvsHashKeysPayload = rpc.receive(dokvsHashKeysHeader).getPayload();
        int dokvsHashKeyNum = Gf2eDokvsFactory.getHashKeyNum(gf2eDokvsType);
        MpcAbortPreconditions.checkArgument(dokvsHashKeysPayload.size() == dokvsHashKeyNum);
        byte[][] dokvsHashKeys = dokvsHashKeysPayload.toArray(new byte[0][]);
        // create okvs instance
        NaiveDatabase database = generateDokvsStorage(completeHashBin, receiverElementPrfMap, dokvsHashKeys);
        // init batch Index PIR
        batchIndexPirServer.init(database, dokvsHashKeyNum * maxSenderElementSize);
        // init permute matrix PEQT
        pmPeqtReceiver.init(1, cuckooHashBinNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public UpsuReceiverOutput psu(int senderElementSize) throws MpcAbortException {
        setPtoInput(senderElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // send OKVS dense part
        DataPacketHeader denseOkvsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_DENSE_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(denseOkvsHeader, dokvsDensePayload));

        stopWatch.start();
        // batch OPRF
        sqOprfSender.oprf(senderElementSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, oprfTime, "receiver executes OPRF");

        stopWatch.start();
        batchIndexPirServer.pir();
        stopWatch.stop();
        long batchIndexPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, batchIndexPirTime, "receiver executes PIR");

        stopWatch.start();
        boolean[][] peqtOutput = pmPeqtReceiver.pmPeqt(r, byteL, 1, cuckooHashBinNum);
        int intersectionSetSize = (int) IntStream.range(0, peqtOutput[0].length).filter(i -> peqtOutput[0][i]).count();
        stopWatch.stop();
        long pmPeqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, pmPeqtTime, "receiver executes pm-PEQT");

        stopWatch.start();
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(peqtOutput[0]);
        DataPacketHeader encHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_ENC_ELEMENTS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> encPayload = rpc.receive(encHeader).getPayload();
        MpcAbortPreconditions.checkArgument(encPayload.size() == cuckooHashBinNum);
        ArrayList<byte[]> encArrayList = new ArrayList<>(encPayload);
        Prg encPrg = PrgFactory.createInstance(envType, elementByteLength);
        IntStream decIntStream = IntStream.range(0, cuckooHashBinNum);
        decIntStream = parallel ? decIntStream.parallel() : decIntStream;
        Set<ByteBuffer> union = decIntStream
            .mapToObj(index -> {
                if (peqtOutput[0][index]) {
                    return botElementByteBuffer;
                } else {
                    // do not need CRHF since we call prg
                    byte[] message = encPrg.extendToBytes(cotReceiverOutput.getRb(index));
                    BytesUtils.xori(message, encArrayList.get(index));
                    return ByteBuffer.wrap(message);
                }
            })
            .collect(Collectors.toSet());
        union.remove(botElementByteBuffer);
        union.addAll(receiverElementList);
        stopWatch.stop();
        long unionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, unionTime, "receiver handles union");

        logPhaseInfo(PtoState.PTO_END);
        return new UpsuReceiverOutput(union, intersectionSetSize);
    }

    /**
     * generate complete hash bins.
     *
     * @param hashKeys hash keys.
     * @return complete hash bins.
     */
    private List<List<HashBinEntry<ByteBuffer>>> generateCompleteHashBin(byte[][] hashKeys) {
        RandomPadHashBin<ByteBuffer> completeHash = new RandomPadHashBin<>(
            envType, cuckooHashBinNum, receiverElementSize, hashKeys
        );
        completeHash.insertItems(receiverElementList);
        List<List<HashBinEntry<ByteBuffer>>> completeHashBins = new ArrayList<>();
        for (int i = 0; i < cuckooHashBinNum; i++) {
            List<HashBinEntry<ByteBuffer>> binItems = new ArrayList<>(completeHash.getBin(i));
            completeHashBins.add(new ArrayList<>(binItems));
        }
        return completeHashBins;
    }

    /**
     * compute input prf.
     *
     * @return element prf map.
     */
    private Map<ByteBuffer, byte[]> computePrf() {
        Prg prg = PrgFactory.createInstance(envType, byteL);
        Stream<ByteBuffer> receiverInputStream = receiverElementList.stream();
        receiverInputStream = parallel ? receiverInputStream.parallel() : receiverInputStream;
        List<byte[]> receiverElementPrf = receiverInputStream
            .map(ByteBuffer::array)
            .map(sqOprfKey::getPrf)
            .map(prg::extendToBytes)
            .peek(bytes -> BytesUtils.reduceByteArray(bytes, l))
            .collect(Collectors.toList());
        return IntStream.range(0, receiverElementSize)
            .boxed()
            .collect(Collectors.toMap(i -> receiverElementList.get(i), receiverElementPrf::get, (a, b) -> b));
    }

    /**
     * generate dokvs storage.
     *
     * @param completeHashBin       complete hash bins.
     * @param receiverElementPrfMap element prf map.
     * @param dokvsHashKeys         dokvs hash keys.
     * @return naive database.
     */
    private NaiveDatabase generateDokvsStorage(List<List<HashBinEntry<ByteBuffer>>> completeHashBin,
                                               Map<ByteBuffer, byte[]> receiverElementPrfMap, byte[][] dokvsHashKeys) {
        int n = completeHashBin.stream().mapToInt(List::size).sum();
        SparseGf2eDokvs<ByteBuffer> dokvs = Gf2eDokvsFactory.createSparseInstance(
            envType, gf2eDokvsType, n, l, dokvsHashKeys
        );
        Map<ByteBuffer, byte[]> keyValueMap = new HashMap<>();
        r = new byte[1][cuckooHashBinNum][byteL];
        Arrays.setAll(r[0], i -> BytesUtils.randomByteArray(byteL, l, secureRandom));
        for (int i = 0; i < cuckooHashBinNum; i++) {
            for (int j = 0; j < completeHashBin.get(i).size(); j++) {
                HashBinEntry<ByteBuffer> entry = completeHashBin.get(i).get(j);
                byte[] key = ByteBuffer.allocate(elementByteLength + Integer.BYTES)
                    .put(entry.getItemByteArray())
                    .putInt(entry.getHashIndex())
                    .array();
                byte[] value = BytesUtils.xor(receiverElementPrfMap.get(entry.getItem()), r[0][i]);
                keyValueMap.put(ByteBuffer.wrap(key), value);
            }
        }
        byte[][] dokvsStorage = dokvs.encode(keyValueMap, true);
        int sparsePositionRange = dokvs.sparsePositionRange();
        int densePositionRange = dokvs.densePositionRange();
        dokvsDensePayload = IntStream.range(0, densePositionRange)
            .mapToObj(i -> dokvsStorage[i + sparsePositionRange])
            .collect(Collectors.toList());
        byte[][] database = new byte[sparsePositionRange][];
        IntStream.range(0, sparsePositionRange).forEach(i -> database[i] = BytesUtils.clone(dokvsStorage[i]));
        return NaiveDatabase.create(l, database);
    }
}
