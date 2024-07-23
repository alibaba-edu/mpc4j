package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.aprr24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.AbstractGf2kCoreVodeReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.aprr24.Aprr24Gf2kCoreVodePtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * APRR24 GF2K-core-VODE receiver.
 *
 * @author Weiran Liu
 * @date 2024/6/11
 */
public class Aprr24Gf2kCoreVodeReceiver extends AbstractGf2kCoreVodeReceiver {
    /**
     * base OT receiver
     */
    private final BaseOtReceiver baseOtReceiver;
    /**
     * base OT receiver output
     */
    private BaseOtReceiverOutput baseOtReceiverOutput;
    /**
     * Δ in binary format
     */
    private boolean[] deltaBinary;
    /**
     * counter
     */
    private long counter;

    public Aprr24Gf2kCoreVodeReceiver(Rpc receiverRpc, Party senderParty, Aprr24Gf2kCoreVodeConfig config) {
        super(Aprr24Gf2kCoreVodePtoDesc.getInstance(), receiverRpc, senderParty, config);
        baseOtReceiver = BaseOtFactory.createReceiver(receiverRpc, senderParty, config.getBaseOtConfig());
        addSubPto(baseOtReceiver);
        counter = 0L;
    }

    @Override
    public void init(int subfieldL, byte[] delta) throws MpcAbortException {
        setInitInput(subfieldL, delta);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        baseOtReceiver.init();
        deltaBinary = BinaryUtils.byteArrayToBinary(delta);
        baseOtReceiverOutput = baseOtReceiver.receive(deltaBinary);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kVodeReceiverOutput receive(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        List<byte[]> matrixPayload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_MATRIX.ordinal());

        stopWatch.start();
        Gf2kVodeReceiverOutput receiverOutput = handleMatrixPayload(matrixPayload);
        counter++;
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, matrixTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;

    }

    private Gf2kVodeReceiverOutput handleMatrixPayload(List<byte[]> matrixPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(matrixPayload.size() == num * fieldL);
        byte[][] matrixPayloadArray = matrixPayload.toArray(new byte[0][]);
        // create matrix Q
        byte[][][] qMatrix = new byte[num][fieldL][];
        IntStream matrixStream = IntStream.range(0, num * fieldL);
        matrixStream = parallel ? matrixStream.parallel() : matrixStream;
        matrixStream.forEach(index -> {
            // current position for matrix Q
            int rowIndex = index / fieldL;
            int columnIndex = index % fieldL;
            // read τ^i from the payload
            byte[] tau = matrixPayloadArray[index];
            // PB computes w^i_{∆_i} := PRF(K^i_{∆_i} , j).
            byte[] wbSeed = ByteBuffer
                .allocate(Long.BYTES + Integer.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
                .putLong(counter).putInt(index).put(baseOtReceiverOutput.getRb(columnIndex))
                .array();
            byte[] wb = subfield.createRandom(wbSeed);
            // PB computes v^i := w^i_{∆_i} + ∆_i · τ^i = w^i_0 - ∆_i · u ∈ Fp
            qMatrix[rowIndex][columnIndex] = deltaBinary[columnIndex] ? subfield.add(tau, wb) : wb;
        });
        // composite each row in q using gadget
        Stream<byte[][]> qMatrixStream = Arrays.stream(qMatrix);
        qMatrixStream = parallel ? qMatrixStream.parallel() : qMatrixStream;
        byte[][] q = qMatrixStream
            .map(field::mixInnerProduct)
            .toArray(byte[][]::new);
        return Gf2kVodeReceiverOutput.create(field, delta, q);
    }
}
