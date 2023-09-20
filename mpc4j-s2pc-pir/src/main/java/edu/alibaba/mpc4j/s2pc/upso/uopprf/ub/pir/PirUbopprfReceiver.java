package edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.pir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.SparseGf2eDokvs;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.AbstractUbopprfReceiver;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.pir.PirUbopprfPtoDesc.*;

/**
 * sparse OKVS unbalanced batched OPPRF receiver.
 *
 * @author Liqiang Peng
 * @date 2023/4/20
 */
public class PirUbopprfReceiver extends AbstractUbopprfReceiver {
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

    public PirUbopprfReceiver(Rpc receiverRpc, Party senderParty, PirUbopprfConfig config) {
        super(getInstance(), receiverRpc, senderParty, config);
        sqOprfReceiver = SqOprfFactory.createReceiver(receiverRpc, senderParty, config.getSqOprfConfig());
        addSubPtos(sqOprfReceiver);
        batchIndexPirClient = BatchIndexPirFactory.createClient(receiverRpc, senderParty, config.getBatchIndexPirConfig());
        addSubPtos(batchIndexPirClient);
        okvsType = config.getOkvsType();
    }

    @Override
    public void init(int l, int batchSize, int pointNum) throws MpcAbortException {
        setInitInput(l, batchSize, pointNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

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
        sparseOkvs = Gf2eDokvsFactory.createSparseInstance(envType, okvsType, pointNum, l, okvsKeys);
        // init oprf
        sqOprfReceiver.init(batchSize);
        // init batch PIR
        batchIndexPirClient.init(sparseOkvs.sparsePositionRange(), l, batchSize * sparseOkvs.maxSparsePositionNum());
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] opprf(byte[][] inputArray) throws MpcAbortException {
        setPtoInput(inputArray);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive OKVS dense part
        DataPacketHeader okvsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> okvsDensePayload = rpc.receive(okvsHeader).getPayload();

        // receiver run batch index PIR
        stopWatch.start();
        List<Integer> retrievalIndexList = generateRetrievalIndexList();
        Map<Integer, byte[]> okvsSparsePayload = batchIndexPirClient.pir(retrievalIndexList);
        // recover okvs storage
        generateOkvsStorage(okvsDensePayload, okvsSparsePayload, retrievalIndexList);
        stopWatch.stop();
        long batchPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, batchPirTime, "Receiver runs batch PIR");

        stopWatch.start();
        // OPRF
        SqOprfReceiverOutput sqOprfReceiverOutput = sqOprfReceiver.oprf(inputArray);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, oprfTime, "Receiver runs OPRF");

        stopWatch.start();
        byte[][] outputArray = handleOprfOutput(sqOprfReceiverOutput);
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
        MpcAbortPreconditions.checkArgument(
            okvsStorage.length == Gf2eDokvsFactory.getM(envType, okvsType, pointNum)
        );
    }

    /**
     * generate sparse okvs retrieval index list.
     *
     * @return sparse okvs retrieval index list.
     */
    private List<Integer> generateRetrievalIndexList() {
        SparseGf2eDokvs<ByteBuffer> sparseOkvs = Gf2eDokvsFactory.createSparseInstance(envType, okvsType, pointNum, l, okvsKeys);
        return Arrays.stream(inputArray)
            .map(bytes -> sparseOkvs.sparsePositions(ByteBuffer.wrap(bytes)))
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
    private byte[][] handleOprfOutput(SqOprfReceiverOutput oprfReceiverOutput) {
        // The PRF maps (random) inputs to {0, 1}^l, we only need to set an empty key
        Prf prf = PrfFactory.createInstance(envType, byteL);
        prf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        // compute PRF output
        SparseGf2eDokvs<ByteBuffer> sparseOkvs = Gf2eDokvsFactory.createSparseInstance(envType, okvsType, pointNum, l, okvsKeys);
        IntStream batchIntStream = IntStream.range(0, batchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        return batchIntStream
            .mapToObj(batchIndex -> {
                byte[] input = inputArray[batchIndex];
                byte[] programOutput = oprfReceiverOutput.getPrf(batchIndex);
                programOutput = prf.getBytes(programOutput);
                BytesUtils.reduceByteArray(programOutput, l);
                byte[] okvsOutput = sparseOkvs.decode(okvsStorage, ByteBuffer.wrap(input));
                BytesUtils.xori(programOutput, okvsOutput);
                return programOutput;
            })
            .toArray(byte[][]::new);
    }
}
