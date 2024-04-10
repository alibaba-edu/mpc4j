package edu.alibaba.mpc4j.s2pc.opf.oprf.rs21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k.Gf2kDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k.Gf2kDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k.Gf2kDokvsFactory.Gf2kDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractMpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.opf.oprf.rs21.Rs21MpOprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleSender;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * RS21-MP-OPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/27
 */
public class Rs21MpOprfReceiver extends AbstractMpOprfReceiver {
    /**
     * GF2K-NC-VOLE sender
     */
    private final Gf2kNcVoleSender gf2kNcVoleSender;
    /**
     * OKVS type
     */
    private final Gf2kDokvsType okvsType;
    /**
     * OKVS key num
     */
    private final int okvsKeyNum;
    /**
     * GF2K instance
     */
    private final Gf2k gf2k;
    /**
     * H^F: {0,1}^* → {0,1}^λ
     */
    private final Prf hf;

    public Rs21MpOprfReceiver(Rpc receiverRpc, Party senderParty, Rs21MpOprfConfig config) {
        super(Rs21MpOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        gf2kNcVoleSender = Gf2kNcVoleFactory.createSender(receiverRpc, senderParty, config.getNcVoleConfig());
        addSubPto(gf2kNcVoleSender);
        okvsType = config.getOkvsType();
        okvsKeyNum = Gf2kDokvsFactory.getHashKeyNum(okvsType);
        gf2k = Gf2kFactory.createInstance(envType);
        hf = PrfFactory.createInstance(envType, gf2k.getByteL());
        hf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
    }

    @Override
    public void init(int maxBatchSize, int maxPrfNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPrfNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxM = Gf2kDokvsFactory.getM(envType, okvsType, maxBatchSize);
        gf2kNcVoleSender.init(maxM);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public MpOprfReceiverOutput oprf(byte[][] inputs) throws MpcAbortException {
        setPtoInput(inputs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // The Receiver samples r ← {0,1}^κ, w^r ← F and solves the systems (OKVS)
        byte[] wr = gf2k.createRandom(secureRandom);
        Stream<byte[]> inputStream = Arrays.stream(inputs);
        inputStream = parallel ? inputStream.parallel() : inputStream;
        Map<ByteBuffer, byte[]> keyValueMap = inputStream.collect(Collectors.toMap(
            ByteBuffer::wrap,
            hf::getBytes
        ));
        byte[][] okvsKeys = CommonUtils.generateRandomKeys(okvsKeyNum, secureRandom);
        Gf2kDokvs<ByteBuffer> gf2kOkvs = Gf2kDokvsFactory.createInstance(
            envType, okvsType, batchSize, okvsKeys
        );
        gf2kOkvs.setParallelEncode(parallel);
        byte[][] vectorA = gf2kOkvs.encode(keyValueMap, true);
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, okvsTime, "Receiver computes OKVS");

        // The Sender sends c^s := H^F(w^s) to the Receiver.
        DataPacketHeader csHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> csPayload = rpc.receive(csHeader).getPayload();

        stopWatch.start();
        // the Receiver sends (receiver, sid) to F_{vole} with dimension m and |F| ≈ 2^κ, where m is the size of OKVS
        int m = Gf2kDokvsFactory.getM(envType, okvsType, batchSize);
        Gf2kVoleSenderOutput gf2kVoleSenderOutput = gf2kNcVoleSender.send();
        gf2kVoleSenderOutput.reduce(m);
        stopWatch.stop();
        long voleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, voleTime, "Receiver executes VOLE");

        stopWatch.start();
        // The Receiver sends r, w^r, A := P + A' to the Sender
        IntStream.range(0, m).forEach(mIndex -> gf2k.addi(vectorA[mIndex], gf2kVoleSenderOutput.getX(mIndex)));
        List<byte[]> okvsWrPayload = new LinkedList<>();
        okvsWrPayload.add(wr);
        IntStream.range(0, okvsKeyNum).forEach(okvsKeyIndex -> okvsWrPayload.add(okvsKeys[okvsKeyIndex]));
        IntStream.range(0, m).forEach(mIndex -> okvsWrPayload.add(vectorA[mIndex]));
        DataPacketHeader okvsWrHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_OKVS_WR.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(okvsWrHeader, okvsWrPayload));
        stopWatch.stop();
        long vectorTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, vectorTime, "Receiver computes A");

        // The Sender sends w^s to the Receiver
        DataPacketHeader wsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_WS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> wsPayload = rpc.receive(wsHeader).getPayload();

        stopWatch.start();
        // the Receiver who aborts if c^s != H^F(w^s). Both parties define w := w^r + w^s.
        MpcAbortPreconditions.checkArgument(csPayload.size() == 1);
        byte[] cs = csPayload.get(0);
        MpcAbortPreconditions.checkArgument(wsPayload.size() == 1);
        byte[] ws = wsPayload.get(0);
        MpcAbortPreconditions.checkArgument(BytesUtils.equals(cs, hf.getBytes(ws)));
        final byte[] w = gf2k.add(wr, ws);
        // The Receiver outputs X' := {H(Decode(C, x) + w, x) | x ∈ X}
        byte[][] vectorC = gf2kVoleSenderOutput.getT();
        inputStream = Arrays.stream(inputs);
        inputStream = parallel ? inputStream.parallel() : inputStream;
        byte[][] prfs = inputStream
            .map(x -> {
                byte[] x1 = gf2kOkvs.decode(vectorC, ByteBuffer.wrap(x));
                gf2k.addi(x1, w);
                byte[] x1x = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH + x.length)
                    .put(x1)
                    .put(x)
                    .array();
                return hf.getBytes(x1x);
            })
            .toArray(byte[][]::new);
        MpOprfReceiverOutput receiverOutput = new MpOprfReceiverOutput(CommonConstants.BLOCK_BYTE_LENGTH, inputs, prfs);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, oprfTime, "Receiver generates OPRF");

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
