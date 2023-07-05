package edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.okvs;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.okvs.Okvs;
import edu.alibaba.mpc4j.crypto.matrix.okve.okvs.OkvsFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.okvs.OkvsUbopprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.AbstractUbopprfSender;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfSender;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * OKVS unbalanced batch OPPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
public class OkvsUbopprfSender extends AbstractUbopprfSender {
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
     * okvs storage
     */
    private byte[][] okvsStorage;
    /**
     * sent OKVS
     */
    private boolean sent;

    public OkvsUbopprfSender(Rpc senderRpc, Party receiverParty, OkvsUbopprfConfig config) {
        super(OkvsUbopprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        sqOprfSender = SqOprfFactory.createSender(senderRpc, receiverParty, config.getSqOprfConfig());
        addSubPtos(sqOprfSender);
        okvsType = config.getOkvsType();
        sent = false;
    }

    @Override
    public void init(int l, byte[][][] inputArrays, byte[][][] targetArrays) throws MpcAbortException {
        setInitInput(l, inputArrays, targetArrays);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        sent = false;
        sqOprfKey = sqOprfSender.keyGen();
        generateOkvs();
        // init oprf
        sqOprfSender.init(batchSize, sqOprfKey);
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, okvsTime, "Sender runs OKVS + OPRF");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void opprf() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        if (!sent) {
            stopWatch.start();
            // send OKVS keys
            List<byte[]> okvsKeysPayload = Arrays.stream(okvsKeys).collect(Collectors.toList());
            DataPacketHeader okvsKeysHeader = new DataPacketHeader(
                encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_OKVS_KEYS.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(okvsKeysHeader, okvsKeysPayload));
            okvsKeys = null;
            // send OKVS storage
            List<byte[]> okvsPayload = Arrays.stream(okvsStorage).collect(Collectors.toList());
            DataPacketHeader okvsHeader = new DataPacketHeader(
                encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_OKVS.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(okvsHeader, okvsPayload));
            okvsStorage = null;
            sent = true;
            stopWatch.stop();
            long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 0, 1, okvsTime, "Sender sends OKVS");
        }

        stopWatch.start();
        sqOprfSender.oprf(batchSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, oprfTime, "Sender runs OPRF");

        logPhaseInfo(PtoState.PTO_END);
    }

    private void generateOkvs() {
        okvsKeys = CommonUtils.generateRandomKeys(OkvsFactory.getHashNum(okvsType), secureRandom);
        Okvs<ByteBuffer> okvs = OkvsFactory.createInstance(envType, okvsType, pointNum, l, okvsKeys);
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
        okvsStorage = okvs.encode(keyValueMap);
    }
}
