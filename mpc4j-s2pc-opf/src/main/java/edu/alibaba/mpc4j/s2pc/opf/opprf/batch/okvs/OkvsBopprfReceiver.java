package edu.alibaba.mpc4j.s2pc.opf.opprf.batch.okvs;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.AbstractBopprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.okvs.OkvsBopprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * OKVS Batch OPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
public class OkvsBopprfReceiver extends AbstractBopprfReceiver {
    /**
     * the OPRF receiver
     */
    private final OprfReceiver oprfReceiver;
    /**
     * OKVS type
     */
    private final Gf2eDokvsType okvsType;

    public OkvsBopprfReceiver(Rpc receiverRpc, Party senderParty, OkvsBopprfConfig config) {
        super(OkvsBopprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        oprfReceiver = OprfFactory.createOprfReceiver(receiverRpc, senderParty, config.getOprfConfig());
        addSubPto(oprfReceiver);
        okvsType = config.getOkvsType();
    }

    @Override
    public void init(int maxBatchSize, int maxPointNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPointNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        oprfReceiver.init(maxBatchSize, maxPointNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] opprf(int l, byte[][] inputArray, int pointNum) throws MpcAbortException {
        setPtoInput(l, inputArray, pointNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // OPRF
        OprfReceiverOutput oprfReceiverOutput = oprfReceiver.oprf(inputArray);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprfTime, "Receiver runs OPRF");

        // receive OKVS keys
        DataPacketHeader okvsKeysHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_OKVS_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> okvsKeysPayload = rpc.receive(okvsKeysHeader).getPayload();
        // receive OKVS
        DataPacketHeader okvsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> okvsPayload = rpc.receive(okvsHeader).getPayload();

        stopWatch.start();
        byte[][] outputArray = handleOkvsPayload(oprfReceiverOutput, okvsKeysPayload, okvsPayload);
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, okvsTime, "Receiver handles OKVS");

        logPhaseInfo(PtoState.PTO_END);
        return outputArray;
    }

    private byte[][] handleOkvsPayload(OprfReceiverOutput oprfReceiverOutput,
                                       List<byte[]> okvsKeysPayload, List<byte[]> okvsPayload) throws MpcAbortException {
        // parse keys
        MpcAbortPreconditions.checkArgument(okvsKeysPayload.size() == Gf2eDokvsFactory.getHashKeyNum(okvsType));
        byte[][] okvsKeys = okvsKeysPayload.toArray(new byte[0][]);
        // The PRF maps (random) inputs to {0, 1}^l, we only need to set an empty key
        Prf prf = PrfFactory.createInstance(envType, byteL);
        prf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        // parse OKVS storage
        MpcAbortPreconditions.checkArgument(okvsPayload.size() == Gf2eDokvsFactory.getM(envType, okvsType, pointNum));
        byte[][] okvsStorage = okvsPayload.toArray(new byte[0][]);
        // compute PRF output
        Gf2eDokvs<ByteBuffer> okvs = Gf2eDokvsFactory.createInstance(envType, okvsType, pointNum, l, okvsKeys);
        IntStream batchIntStream = IntStream.range(0, batchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        return batchIntStream
            .mapToObj(batchIndex -> {
                byte[] input = inputArray[batchIndex];
                byte[] programOutput = oprfReceiverOutput.getPrf(batchIndex);
                programOutput = prf.getBytes(programOutput);
                BytesUtils.reduceByteArray(programOutput, l);
                byte[] okvsOutput = okvs.decode(okvsStorage, ByteBuffer.wrap(input));
                BytesUtils.xori(programOutput, okvsOutput);
                return programOutput;
            })
            .toArray(byte[][]::new);
    }
}
