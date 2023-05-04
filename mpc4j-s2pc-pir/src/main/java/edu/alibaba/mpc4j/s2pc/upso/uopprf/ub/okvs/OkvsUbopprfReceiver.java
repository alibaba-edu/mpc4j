package edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.okvs;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.okve.okvs.Okvs;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.okvs.OkvsUbopprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.AbstractUbopprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiverOutput;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * OKVS unbalanced batched OPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
public class OkvsUbopprfReceiver extends AbstractUbopprfReceiver {
    /**
     * single-point OPRF receiver
     */
    private final SqOprfReceiver sqOprfReceiver;
    /**
     * the OKVS type
     */
    private final OkvsFactory.OkvsType okvsType;
    /**
     * sent OKVS
     */
    private boolean sent;
    /**
     * OKVS keys
     */
    private byte[][] okvsKeys;
    /**
     * OKVS storage
     */
    private byte[][] okvsStorage;

    public OkvsUbopprfReceiver(Rpc receiverRpc, Party senderParty, OkvsUbopprfConfig config) {
        super(OkvsUbopprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        sqOprfReceiver = SqOprfFactory.createReceiver(receiverRpc, senderParty, config.getSqOprfConfig());
        addSubPtos(sqOprfReceiver);
        okvsType = config.getOkvsType();
        sent = false;
    }

    @Override
    public void init(int l, int batchSize, int pointNum) throws MpcAbortException {
        setInitInput(l, batchSize, pointNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        sent = false;
        // init oprf
        sqOprfReceiver.init(batchSize);
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

        if (!sent) {
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
            int keyNum = OkvsFactory.getHashNum(okvsType);
            MpcAbortPreconditions.checkArgument(okvsKeysPayload.size() == keyNum);
            okvsKeys = okvsKeysPayload.toArray(new byte[0][]);
            MpcAbortPreconditions.checkArgument(okvsPayload.size() == OkvsFactory.getM(okvsType, pointNum));
            okvsStorage = okvsPayload.toArray(new byte[0][]);
            sent = true;
            stopWatch.stop();
            long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 0, 2, okvsTime, "Receiver receives OKVS");
        }

        stopWatch.start();
        // OPRF
        SqOprfReceiverOutput sqOprfReceiverOutput = sqOprfReceiver.oprf(inputArray);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprfTime, "Receiver runs OPRF");

        stopWatch.start();
        byte[][] outputArray = handleOprfOutput(sqOprfReceiverOutput);
        stopWatch.stop();
        long programTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, programTime, "Receiver handles OPRF");

        logPhaseInfo(PtoState.PTO_END);
        return outputArray;
    }

    private byte[][] handleOprfOutput(SqOprfReceiverOutput oprfReceiverOutput) {
        // The PRF maps (random) inputs to {0, 1}^l, we only need to set an empty key
        Prf prf = PrfFactory.createInstance(envType, byteL);
        prf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        // compute PRF output
        Okvs<ByteBuffer> okvs = OkvsFactory.createInstance(envType, okvsType, pointNum, l, okvsKeys);
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
