package edu.alibaba.mpc4j.s2pc.upso.okvr.kw;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.SparseGf2eDokvs;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfSender;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirServer;
import edu.alibaba.mpc4j.s2pc.upso.okvr.AbstractOkvrSender;
import edu.alibaba.mpc4j.s2pc.upso.okvr.kw.KwOkvrPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Keyword PIR OKVR sender.
 *
 * @author Liqiang Peng
 * @date 2023/4/20
 */
public class KwOkvrSender extends AbstractOkvrSender {
    /**
     * single-query OPRF sender
     */
    private final SqOprfSender sqOprfSender;
    /**
     * OKVS type
     */
    private final Gf2eDokvsType okvsType;
    /**
     * single-query OPRF key
     */
    private SqOprfKey sqOprfKey;
    /**
     * OKVS keys
     */
    private byte[][] okvsKeys;
    /**
     * OKVS dense storage
     */
    private byte[][] okvsDenseStorage;
    /**
     * OKVS sparse storage
     */
    private byte[][] okvsSparseStorage;
    /**
     * Keyword PIR server
     */
    private final KwPirServer kwPirServer;
    /**
     * sparse OKVS
     */
    private SparseGf2eDokvs<ByteBuffer> sparseOkvs;

    public KwOkvrSender(Rpc senderRpc, Party receiverParty, KwOkvrConfig config) {
        super(KwOkvrPtoDesc.getInstance(), senderRpc, receiverParty, config);
        sqOprfSender = SqOprfFactory.createSender(senderRpc, receiverParty, config.getSqOprfConfig());
        addSubPto(sqOprfSender);
        kwPirServer = KwPirFactory.createServer(senderRpc, receiverParty, config.getKwPirConfig());
        addSubPto(kwPirServer);
        okvsType = config.getOkvsType();
    }

    @Override
    public void init(Map<ByteBuffer, byte[]> keyValueMap, int l, int retrievalSize) throws MpcAbortException {
        setInitInput(keyValueMap, l, retrievalSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        sqOprfKey = sqOprfSender.keyGen();
        // init oprf
        sqOprfSender.init(retrievalSize, sqOprfKey);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, oprfTime, "Sender inits OPRF");

        stopWatch.start();
        generateOkvs();
        // send OKVS keys
        List<byte[]> okvsKeysPayload = Arrays.stream(okvsKeys).collect(Collectors.toList());
        DataPacketHeader okvsKeysHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_OKVS_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(okvsKeysHeader, okvsKeysPayload));
        okvsKeys = null;
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.INIT_STEP, 2, 3, okvsTime,
            String.format("Sender inits OKVS, l = %sB, sparse length = %s, dense length = %s, OKVS length = %s",
                byteL, okvsSparseStorage.length, okvsDenseStorage.length, okvsSparseStorage.length + okvsDenseStorage.length
            )
        );

        stopWatch.start();
        // init Keyword PIR
        Map<ByteBuffer, byte[]> keyLabelMap = IntStream.range(0, okvsSparseStorage.length)
            .boxed()
            .collect(Collectors.toMap(
                index -> ByteBuffer.wrap(IntUtils.intToByteArray(index)),
                index -> okvsSparseStorage[index]
            ));
        kwPirServer.init(keyLabelMap, retrievalSize * sparseOkvs.sparsePositionNum(), byteL);
        stopWatch.stop();
        long pirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, pirTime, "Sender inits PIR");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void okvr() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // send OKVS dense storage
        List<byte[]> denseOkvsPayload = Arrays.stream(okvsDenseStorage).collect(Collectors.toList());
        DataPacketHeader denseOkvsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_DENSE_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(denseOkvsHeader, denseOkvsPayload));
        okvsDenseStorage = null;
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, okvsTime, "Sender sends dense OKVS");

        stopWatch.start();
        kwPirServer.pir();
        stopWatch.stop();
        long kwPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, kwPirTime, "Sender runs Keyword PIR");

        stopWatch.start();
        sqOprfSender.oprf(retrievalSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, oprfTime, "Sender runs OPRF");

        logPhaseInfo(PtoState.PTO_END);
    }

    private void generateOkvs() {
        okvsKeys = CommonUtils.generateRandomKeys(Gf2eDokvsFactory.getHashKeyNum(okvsType), secureRandom);
        sparseOkvs = Gf2eDokvsFactory.createSparseInstance(envType, okvsType, num, l, okvsKeys);
        sparseOkvs.setParallelEncode(parallel);
        // The PRF maps (random) inputs to {0, 1}^l, we only need to set an empty key
        Prf prf = PrfFactory.createInstance(envType, byteL);
        prf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        // oprf key-value map
        Stream<Entry<ByteBuffer, byte[]>> keyValueMapStream = keyValueMap.entrySet().stream();
        keyValueMapStream = parallel ? keyValueMapStream.parallel() : keyValueMapStream;
        Map<ByteBuffer, byte[]> oprfKeyValueMap = keyValueMapStream
            .collect(Collectors.toMap(
                Entry::getKey,
                entry -> {
                    ByteBuffer input = entry.getKey();
                    byte[] target = entry.getValue();
                    byte[] programOutput = sqOprfKey.getPrf(input.array());
                    programOutput = prf.getBytes(programOutput);
                    BytesUtils.xori(programOutput, target);
                    BytesUtils.reduceByteArray(programOutput, l);
                    return programOutput;
                }
            ));
        byte[][] okvsStorage = sparseOkvs.encode(oprfKeyValueMap, false);
        okvsSparseStorage = IntStream.range(0, sparseOkvs.sparsePositionRange())
            .mapToObj(i -> BytesUtils.clone(okvsStorage[i]))
            .toArray(byte[][]::new);
        okvsDenseStorage = IntStream.range(0, sparseOkvs.densePositionRange())
            .mapToObj(i -> BytesUtils.clone(okvsStorage[i + sparseOkvs.sparsePositionRange()]))
            .toArray(byte[][]::new);
    }
}
