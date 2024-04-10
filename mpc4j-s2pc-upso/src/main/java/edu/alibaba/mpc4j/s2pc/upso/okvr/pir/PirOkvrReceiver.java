package edu.alibaba.mpc4j.s2pc.upso.okvr.pir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.SparseGf2eDokvs;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.upso.okvr.AbstractOkvrReceiver;
import edu.alibaba.mpc4j.s2pc.upso.okvr.pir.PirOkvrPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * PIR OKVS receiver.
 *
 * @author Liqiang Peng
 * @date 2023/4/20
 */
public class PirOkvrReceiver extends AbstractOkvrReceiver {
    /**
     * single-point OPRF receiver
     */
    private final SqOprfReceiver sqOprfReceiver;
    /**
     * OKVS type
     */
    private final Gf2eDokvsType okvsType;
    /**
     * OKVS keys
     */
    private byte[][] okvsKeys;
    /**
     * OKVS storage
     */
    private byte[][] okvsStorage;
    /**
     * batch index pir
     */
    private final BatchIndexPirClient batchIndexPirClient;
    /**
     * sparse OKVS
     */
    private SparseGf2eDokvs<ByteBuffer> sparseOkvs;

    public PirOkvrReceiver(Rpc receiverRpc, Party senderParty, PirOkvrConfig config) {
        super(PirOkvrPtoDesc.getInstance(), receiverRpc, senderParty, config);
        sqOprfReceiver = SqOprfFactory.createReceiver(receiverRpc, senderParty, config.getSqOprfConfig());
        addSubPto(sqOprfReceiver);
        batchIndexPirClient = BatchIndexPirFactory.createClient(receiverRpc, senderParty, config.getBatchIndexPirConfig());
        addSubPto(batchIndexPirClient);
        okvsType = config.getOkvsType();
    }

    @Override
    public void init(int num, int l, int retrievalSize) throws MpcAbortException {
        setInitInput(num, l, retrievalSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init oprf
        sqOprfReceiver.init(retrievalSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, oprfTime, "Receiver inits OPRF");

        // receive OKVS keys
        DataPacketHeader okvsKeysHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_OKVS_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> okvsKeysPayload = rpc.receive(okvsKeysHeader).getPayload();

        stopWatch.start();
        // init okvs
        int keyNum = Gf2eDokvsFactory.getHashKeyNum(okvsType);
        MpcAbortPreconditions.checkArgument(okvsKeysPayload.size() == keyNum);
        okvsKeys = okvsKeysPayload.toArray(new byte[0][]);
        sparseOkvs = Gf2eDokvsFactory.createSparseInstance(envType, okvsType, num, l, okvsKeys);
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, okvsTime, "Receiver inits OKVS");

        stopWatch.start();
        // init batch PIR
        batchIndexPirClient.init(sparseOkvs.sparsePositionRange(), l, retrievalSize * sparseOkvs.sparsePositionNum());
        stopWatch.stop();
        long pirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, pirTime, "Receiver inits PIR");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Map<ByteBuffer, byte[]> okvr(Set<ByteBuffer> keys) throws MpcAbortException {
        setPtoInput(keys);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive OKVS dense part
        DataPacketHeader denseOkvsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_DENSE_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> denseOkvsPayload = rpc.receive(denseOkvsHeader).getPayload();

        // receiver run batch index PIR
        stopWatch.start();
        List<Integer> retrievalIndexList = generateRetrievalIndexList();
        Map<Integer, byte[]> okvsSparsePayload = batchIndexPirClient.pir(retrievalIndexList);
        // recover okvs storage
        generateOkvsStorage(denseOkvsPayload, okvsSparsePayload, retrievalIndexList);
        stopWatch.stop();
        long batchPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, batchPirTime, "Receiver runs batch PIR");

        stopWatch.start();
        // OPRF
        byte[][] byteKeys = Arrays.stream(keyArray).map(ByteBuffer::array).toArray(byte[][]::new);
        SqOprfReceiverOutput sqOprfReceiverOutput = sqOprfReceiver.oprf(byteKeys);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, oprfTime, "Receiver runs OPRF");

        stopWatch.start();
        Map<ByteBuffer, byte[]> outputArray = handleOprfOutput(sqOprfReceiverOutput);
        stopWatch.stop();
        long programTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, programTime, "Receiver handles OPRF");

        logPhaseInfo(PtoState.PTO_END);
        return outputArray;
    }

    /**
     * recover okvs storage.
     *
     * @param okvsDensePayload   okvs dense payload.
     * @param okvsSparsePayload  okvs sparse payload.
     * @param retrievalIndexList retrieval index list.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void generateOkvsStorage(List<byte[]> okvsDensePayload, Map<Integer, byte[]> okvsSparsePayload,
                                     List<Integer> retrievalIndexList) throws MpcAbortException {
        int sparsePositionNum = sparseOkvs.sparsePositionRange();
        int densePositionNum = sparseOkvs.densePositionRange();
        MpcAbortPreconditions.checkArgument(densePositionNum == okvsDensePayload.size());
        MpcAbortPreconditions.checkArgument(retrievalIndexList.size() == okvsSparsePayload.size());
        okvsStorage = new byte[sparsePositionNum + densePositionNum][];
        for (int i = 0; i < okvsSparsePayload.size(); i++) {
            okvsStorage[retrievalIndexList.get(i)] = okvsSparsePayload.get(retrievalIndexList.get(i));
        }
        byte[][] denseOkvsStorage = okvsDensePayload.toArray(new byte[0][]);
        System.arraycopy(denseOkvsStorage, 0, okvsStorage, sparsePositionNum, denseOkvsStorage.length);
        MpcAbortPreconditions.checkArgument(okvsStorage.length == Gf2eDokvsFactory.getM(envType, okvsType, num));
    }

    /**
     * generate sparse okvs retrieval index list.
     *
     * @return sparse okvs retrieval index list.
     */
    private List<Integer> generateRetrievalIndexList() {
        SparseGf2eDokvs<ByteBuffer> sparseOkvs = Gf2eDokvsFactory.createSparseInstance(envType, okvsType, num, l, okvsKeys);
        return Arrays.stream(keyArray)
            .map(sparseOkvs::sparsePositions)
            .flatMapToInt(Arrays::stream)
            .distinct()
            .boxed()
            .collect(Collectors.toList());
    }

    /**
     * handle OPRF output.
     *
     * @param oprfReceiverOutput OPRF receiver output.
     * @return PRF output.
     */
    private Map<ByteBuffer, byte[]> handleOprfOutput(SqOprfReceiverOutput oprfReceiverOutput) {
        // The PRF maps (random) inputs to {0, 1}^l, we only need to set an empty key
        Prf prf = PrfFactory.createInstance(envType, byteL);
        prf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        // compute PRF output
        SparseGf2eDokvs<ByteBuffer> sparseOkvs = Gf2eDokvsFactory.createSparseInstance(envType, okvsType, num, l, okvsKeys);
        IntStream indexIntStream = IntStream.range(0, retrievalSize);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        return indexIntStream
            .boxed()
            .collect(Collectors.toMap(
                index -> keyArray[index],
                index -> {
                    byte[] input = keyArray[index].array();
                    byte[] programOutput = oprfReceiverOutput.getPrf(index);
                    programOutput = prf.getBytes(programOutput);
                    BytesUtils.reduceByteArray(programOutput, l);
                    byte[] okvsOutput = sparseOkvs.decode(okvsStorage, ByteBuffer.wrap(input));
                    BytesUtils.xori(programOutput, okvsOutput);
                    return programOutput;
                }
            ));
    }
}
