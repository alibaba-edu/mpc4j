package edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.pir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.okvs.OkvsFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.okvs.SparseOkvs;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfSender;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.AbstractUbopprfSender;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.pir.PirUbopprfPtoDesc.*;

/**
 * sparse OKVS unbalanced batch OPPRF sender.
 *
 * @author Liqiang Peng
 * @date 2023/4/20
 */
public class PirUbopprfSender extends AbstractUbopprfSender {
    /**
     * single-query OPRF sender
     */
    private final SqOprfSender sqOprfSender;
    /**
     * the OKVS type
     */
    private final OkvsFactory.OkvsType okvsType;
    /**
     * single-query OPRF key
     */
    private SqOprfKey sqOprfKey;
    /**
     * OKVS keys
     */
    private byte[][] okvsKeys;
    /**
     * okvs dense storage
     */
    private byte[][] okvsDenseStorage;
    /**
     * okvs sparse storage
     */
    private byte[][] okvsSparseStorage;
    /**
     * batch index PIR server
     */
    private final BatchIndexPirServer batchIndexPirServer;
    /**
     * sparse okvs
     */
    SparseOkvs<ByteBuffer> okvs;

    public PirUbopprfSender(Rpc senderRpc, Party receiverParty, PirUbopprfConfig config) {
        super(getInstance(), senderRpc, receiverParty, config);
        sqOprfSender = SqOprfFactory.createSender(senderRpc, receiverParty, config.getSqOprfConfig());
        addSubPtos(sqOprfSender);
        batchIndexPirServer = BatchIndexPirFactory.createServer(senderRpc, receiverParty, config.getBatchIndexPirConfig());
        addSubPtos(batchIndexPirServer);
        okvsType = config.getOkvsType();
    }

    @Override
    public void init(int l, byte[][][] inputArrays, byte[][][] targetArrays) throws MpcAbortException {
        setInitInput(l, inputArrays, targetArrays);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        sqOprfKey = sqOprfSender.keyGen();
        okvsKeys = CommonUtils.generateRandomKeys(OkvsFactory.getHashNum(okvsType), secureRandom);
        okvs = OkvsFactory.createSparseInstance(envType, okvsType, pointNum, l, okvsKeys);
        generateOkvs();
        // init oprf
        sqOprfSender.init(batchSize, sqOprfKey);
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, okvsTime, "Sender runs OPRF + OKVS");

        stopWatch.start();
        // init batch index PIR
        batchIndexPirServer.init(NaiveDatabase.create(l, okvsSparseStorage), batchSize * okvs.sparsePositionNum());
        stopWatch.stop();
        long pirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, pirTime, "Sender inits PIR");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void opprf() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // send OKVS keys
        List<byte[]> okvsKeysPayload = Arrays.stream(okvsKeys).collect(Collectors.toList());
        DataPacketHeader okvsKeysHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_OKVS_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(okvsKeysHeader, okvsKeysPayload));
        okvsKeys = null;
        // send OKVS dense storage
        List<byte[]> okvsPayload = Arrays.stream(okvsDenseStorage).collect(Collectors.toList());
        DataPacketHeader okvsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(okvsHeader, okvsPayload));
        okvsDenseStorage = null;
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, okvsTime, "Sender sends OKVS");

        stopWatch.start();
        batchIndexPirServer.pir();
        stopWatch.stop();
        long batchPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, batchPirTime, "Sender runs batch PIR");

        stopWatch.start();
        sqOprfSender.oprf(batchSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, oprfTime, "Sender runs OPRF");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * generate okvs.
     */
    private void generateOkvs() {
        okvs.setParallelEncode(parallel);
        // construct key-value map
        Map<ByteBuffer, byte[]> keyValueMap = new ConcurrentHashMap<>(pointNum);
        // The PRF maps (random) inputs to {0, 1}^l, we only need to set an empty key
        Prf prf = PrfFactory.createInstance(envType, byteL);
        prf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        IntStream batchIntStream = IntStream.range(0, batchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        batchIntStream.forEach(batchIndex -> {
            byte[][] inputArray = inputArrays[batchIndex];
            byte[][] targetArray = targetArrays[batchIndex];
            assert inputArray.length == targetArray.length;
            int num = inputArray.length;
            for (int index = 0; index < num; index++) {
                byte[] input = inputArray[index];
                byte[] target = targetArray[index];
                byte[] programOutput = sqOprfKey.getPrf(input);
                programOutput = prf.getBytes(programOutput);
                BytesUtils.xori(programOutput, target);
                BytesUtils.reduceByteArray(programOutput, l);
                keyValueMap.put(ByteBuffer.wrap(input), programOutput);
            }
        });
        byte[][] okvsStorage = okvs.encode(keyValueMap);
        okvsSparseStorage = IntStream.range(0, okvs.getM() - okvs.maxDensePositionNum())
            .mapToObj(i -> BytesUtils.clone(okvsStorage[i]))
            .toArray(byte[][]::new);
        okvsDenseStorage = IntStream.range(0, okvs.maxDensePositionNum())
            .mapToObj(i -> BytesUtils.clone(okvsStorage[i + okvs.getM() - okvs.maxDensePositionNum()]))
            .toArray(byte[][]::new);
    }
}
