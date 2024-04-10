package edu.alibaba.mpc4j.s2pc.opf.oprf.rs21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k.Gf2kDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k.Gf2kDokvsFactory.Gf2kDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractMpOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.opf.oprf.rs21.Rs21MpOprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleReceiver;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * RS21-MP-OPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/7/27
 */
public class Rs21MpOprfSender extends AbstractMpOprfSender {
    /**
     * GF2K-NC-VOLE receiver
     */
    private final Gf2kNcVoleReceiver gf2kNcVoleReceiver;
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
    /**
     * Δ
     */
    private byte[] delta;

    public Rs21MpOprfSender(Rpc senderRpc, Party receiverParty, Rs21MpOprfConfig config) {
        super(Rs21MpOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        gf2kNcVoleReceiver = Gf2kNcVoleFactory.createReceiver(senderRpc, receiverParty, config.getNcVoleConfig());
        addSubPto(gf2kNcVoleReceiver);
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
        delta = gf2k.createNonZeroRandom(secureRandom);
        int maxM = Gf2kDokvsFactory.getM(envType, okvsType, maxBatchSize);
        gf2kNcVoleReceiver.init(delta, maxM);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public MpOprfSenderOutput oprf(int batchSize) throws MpcAbortException {
        setPtoInput(batchSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // The Sender samples w^s ← F and sends c^s := H^F(w^s) to the Receiver.
        byte[] ws = gf2k.createRandom(secureRandom);
        byte[] cs = hf.getBytes(ws);
        List<byte[]> csPayload = Collections.singletonList(cs);
        DataPacketHeader csHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(csHeader, csPayload));
        stopWatch.stop();
        long csTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, csTime, "Sender generates c^s");

        stopWatch.start();
        // The Sender sends (sender, sid) to F_{vole} with dimension m and |F| ≈ 2^κ, where m is the size of OKVS
        int m = Gf2kDokvsFactory.getM(envType, okvsType, batchSize);
        Gf2kVoleReceiverOutput gf2kVoleReceiverOutput = gf2kNcVoleReceiver.receive();
        gf2kVoleReceiverOutput.reduce(m);
        stopWatch.stop();
        long voleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, voleTime, "Sender executes VOLE");

        // The Receiver sends r, w^r, A := P + A' to the Sender
        DataPacketHeader okvsWrHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_OKVS_WR.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> okvsWrPayload = rpc.receive(okvsWrHeader).getPayload();

        stopWatch.start();
        // w^r, OKVS keys (r), OKVS payload (A)
        MpcAbortPreconditions.checkArgument(okvsWrPayload.size() == 1 + okvsKeyNum + m);
        byte[] wr = okvsWrPayload.remove(0);
        byte[][] okvsKeys = IntStream.range(0, okvsKeyNum)
            .mapToObj(okvsKeyIndex -> okvsWrPayload.remove(0))
            .toArray(byte[][]::new);
        byte[][] vectorA = okvsWrPayload.toArray(new byte[0][]);
        // the Sender defines K := B + A · ∆
        byte[][] vectorK = gf2kVoleReceiverOutput.getQ();
        IntStream mIndexStream = IntStream.range(0, m);
        mIndexStream = parallel ? mIndexStream.parallel() : mIndexStream;
        mIndexStream.forEach(mIndex -> gf2k.addi(vectorK[mIndex], gf2k.mul(vectorA[mIndex], delta)));
        // The Sender sends w^s to the Receiver
        List<byte[]> wsPayload = Collections.singletonList(ws);
        DataPacketHeader wsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_WS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(wsHeader, wsPayload));
        // set output
        byte[] w = gf2k.add(ws, wr);
        Rs21MpOprfSenderOutput senderOutput = new Rs21MpOprfSenderOutput(
            envType, batchSize, delta, w, okvsType, okvsKeys, vectorK
        );
        stopWatch.stop();
        long wsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, wsTime, "Sender computes K and sends w^s");

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
