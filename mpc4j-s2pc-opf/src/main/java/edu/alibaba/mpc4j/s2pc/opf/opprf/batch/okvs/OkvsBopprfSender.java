package edu.alibaba.mpc4j.s2pc.opf.opprf.batch.okvs;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.AbstractBopprfSender;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.okvs.OkvsBopprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSenderOutput;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * OKVS Batch OPPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
public class OkvsBopprfSender extends AbstractBopprfSender {
    /**
     * OPRF sender
     */
    private final OprfSender oprfSender;
    /**
     * OKVS type
     */
    private final Gf2eDokvsType okvsType;

    public OkvsBopprfSender(Rpc senderRpc, Party receiverParty, OkvsBopprfConfig config) {
        super(OkvsBopprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        oprfSender = OprfFactory.createOprfSender(senderRpc, receiverParty, config.getOprfConfig());
        addSubPto(oprfSender);
        okvsType = config.getOkvsType();
    }

    @Override
    public void init(int maxBatchSize, int maxPointNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPointNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        oprfSender.init(maxBatchSize, maxPointNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void opprf(int l, byte[][][] inputArrays, byte[][][] targetArrays) throws MpcAbortException {
        setPtoInput(l, inputArrays, targetArrays);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // OPRF
        OprfSenderOutput oprfSenderOutput = oprfSender.oprf(batchSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, oprfTime, "Sender runs OPRF");

        stopWatch.start();
        // generate OKVS keys
        byte[][] okvsKeys = CommonUtils.generateRandomKeys(Gf2eDokvsFactory.getHashKeyNum(okvsType), secureRandom);
        List<byte[]> okvsKeysPayload = Arrays.stream(okvsKeys).collect(Collectors.toList());
        DataPacketHeader okvsKeysHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_OKVS_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(okvsKeysHeader, okvsKeysPayload));
        stopWatch.stop();
        long okvsKeysTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, okvsKeysTime, "Sender sends OKVS keys");

        stopWatch.start();
        List<byte[]> okvsPayload = generateOkvsPayload(oprfSenderOutput, okvsKeys);
        DataPacketHeader okvsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(okvsHeader, okvsPayload));
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, okvsTime, "Sender sends OKVS");

        logPhaseInfo(PtoState.PTO_END);
    }

    private List<byte[]> generateOkvsPayload(OprfSenderOutput oprfSenderOutput, byte[][] okvsKeys) {
        Gf2eDokvs<ByteBuffer> okvs = Gf2eDokvsFactory.createInstance(envType, okvsType, pointNum, l, okvsKeys);
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
                byte[] programOutput = oprfSenderOutput.getPrf(batchIndex, input);
                programOutput = prf.getBytes(programOutput);
                BytesUtils.reduceByteArray(programOutput, l);
                BytesUtils.xori(programOutput, target);
                keyValueMap.put(ByteBuffer.wrap(input), programOutput);
            }
        });
        byte[][] okvsStorage = okvs.encode(keyValueMap, false);
        return Arrays.stream(okvsStorage).collect(Collectors.toList());
    }
}
