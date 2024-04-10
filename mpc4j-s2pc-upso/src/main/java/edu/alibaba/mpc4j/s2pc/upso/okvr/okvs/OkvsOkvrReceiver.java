package edu.alibaba.mpc4j.s2pc.upso.okvr.okvs;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.upso.okvr.AbstractOkvrReceiver;
import edu.alibaba.mpc4j.s2pc.upso.okvr.okvs.OkvsOkvrPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * OKVS unbalanced batched OPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
public class OkvsOkvrReceiver extends AbstractOkvrReceiver {
    /**
     * single-point OPRF receiver
     */
    private final SqOprfReceiver sqOprfReceiver;
    /**
     * the OKVS type
     */
    private final Gf2eDokvsType okvsType;
    /**
     * OKVS storage
     */
    private byte[][] okvsStorage;
    /**
     * OKVS
     */
    private Gf2eDokvs<ByteBuffer> okvs;

    public OkvsOkvrReceiver(Rpc receiverRpc, Party senderParty, OkvsOkvrConfig config) {
        super(OkvsOkvrPtoDesc.getInstance(), receiverRpc, senderParty, config);
        sqOprfReceiver = SqOprfFactory.createReceiver(receiverRpc, senderParty, config.getSqOprfConfig());
        addSubPto(sqOprfReceiver);
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
        logStepInfo(PtoState.INIT_STEP, 1, 2, oprfTime, "Receiver inits OPRF");

        // receive OKVS keys
        DataPacketHeader okvsKeysHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_OKVS_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> okvsKeysPayload = rpc.receive(okvsKeysHeader).getPayload();

        stopWatch.start();
        // init OKVS
        int keyNum = Gf2eDokvsFactory.getHashKeyNum(okvsType);
        MpcAbortPreconditions.checkArgument(okvsKeysPayload.size() == keyNum);
        byte[][] okvsKeys = okvsKeysPayload.toArray(new byte[0][]);
        okvs = Gf2eDokvsFactory.createInstance(envType, okvsType, num, l, okvsKeys);
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, okvsTime, "Receiver inits OKVS");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Map<ByteBuffer, byte[]> okvr(Set<ByteBuffer> keys) throws MpcAbortException {
        setPtoInput(keys);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive OKVS
        DataPacketHeader okvsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> okvsPayload = rpc.receive(okvsHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(okvsPayload.size() == Gf2eDokvsFactory.getM(envType, okvsType, num));
        okvsStorage = okvsPayload.toArray(new byte[0][]);
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 0, 2, okvsTime, "Receiver receives OKVS");

        stopWatch.start();
        // OPRF
        byte[][] byteKeys = Arrays.stream(keyArray).map(ByteBuffer::array).toArray(byte[][]::new);
        SqOprfReceiverOutput sqOprfReceiverOutput = sqOprfReceiver.oprf(byteKeys);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprfTime, "Receiver runs OPRF");

        stopWatch.start();
        Map<ByteBuffer, byte[]> outputArray = handleOprfOutput(sqOprfReceiverOutput);
        stopWatch.stop();
        long programTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, programTime, "Receiver handles OPRF");

        logPhaseInfo(PtoState.PTO_END);
        return outputArray;
    }

    private Map<ByteBuffer, byte[]> handleOprfOutput(SqOprfReceiverOutput oprfReceiverOutput) {
        // The PRF maps (random) inputs to {0, 1}^l, we only need to set an empty key
        Prf prf = PrfFactory.createInstance(envType, byteL);
        prf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        // compute PRF output
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
                    byte[] okvsOutput = okvs.decode(okvsStorage, ByteBuffer.wrap(input));
                    BytesUtils.xori(programOutput, okvsOutput);
                    return programOutput;
                }
            ));
    }
}
