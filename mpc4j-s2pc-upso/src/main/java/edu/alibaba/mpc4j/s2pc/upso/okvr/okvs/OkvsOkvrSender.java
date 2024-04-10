package edu.alibaba.mpc4j.s2pc.upso.okvr.okvs;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfSender;
import edu.alibaba.mpc4j.s2pc.upso.okvr.AbstractOkvrSender;
import edu.alibaba.mpc4j.s2pc.upso.okvr.okvs.OkvsOkvrPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * OKVS unbalanced batch OPPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
public class OkvsOkvrSender extends AbstractOkvrSender {
    /**
     * single-query OPRF sender
     */
    private final SqOprfSender sqOprfSender;
    /**
     * the OKVS type
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
     * OKVS storage
     */
    private byte[][] okvsStorage;

    public OkvsOkvrSender(Rpc senderRpc, Party receiverParty, OkvsOkvrConfig config) {
        super(OkvsOkvrPtoDesc.getInstance(), senderRpc, receiverParty, config);
        sqOprfSender = SqOprfFactory.createSender(senderRpc, receiverParty, config.getSqOprfConfig());
        addSubPto(sqOprfSender);
        okvsType = config.getOkvsType();
    }

    @Override
    public void init(Map<ByteBuffer, byte[]> keyValueMap, int l, int retrievalSize) throws MpcAbortException {
        setInitInput(keyValueMap, l, retrievalSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init oprf
        sqOprfKey = sqOprfSender.keyGen();
        sqOprfSender.init(retrievalSize, sqOprfKey);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, oprfTime, "Sender inits OPRF");

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
        long okvsKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.INIT_STEP, 2, 2, okvsKeyTime,
            String.format("Sender inits OKVS, l = %sB, OKVS length = %s", byteL, okvsStorage.length)
        );
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void okvr() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // send OKVS storage
        List<byte[]> okvsPayload = Arrays.stream(okvsStorage).collect(Collectors.toList());
        DataPacketHeader okvsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(okvsHeader, okvsPayload));
        okvsStorage = null;
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 0, 1, okvsTime, "Sender sends OKVS");

        stopWatch.start();
        sqOprfSender.oprf(retrievalSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, oprfTime, "Sender runs OPRF");

        logPhaseInfo(PtoState.PTO_END);
    }

    private void generateOkvs() {
        okvsKeys = CommonUtils.generateRandomKeys(Gf2eDokvsFactory.getHashKeyNum(okvsType), secureRandom);
        Gf2eDokvs<ByteBuffer> okvs = Gf2eDokvsFactory.createInstance(envType, okvsType, num, l, okvsKeys);
        okvs.setParallelEncode(parallel);
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
        okvsStorage = okvs.encode(oprfKeyValueMap, false);
    }
}
