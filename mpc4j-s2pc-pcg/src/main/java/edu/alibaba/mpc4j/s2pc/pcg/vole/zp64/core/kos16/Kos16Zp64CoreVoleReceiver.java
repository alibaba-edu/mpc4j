package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core.kos16;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Gadget;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core.AbstractZp64CoreVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleReceiverOutput;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * KOS16-Zp64-core VOLE receiver.
 *
 * @author Hanwen Feng
 * @date 2022/06/09
 */
public class Kos16Zp64CoreVoleReceiver extends AbstractZp64CoreVoleReceiver {
    /**
     * base OT receiver
     */
    private final BaseOtReceiver baseOtReceiver;
    /**
     * Zp64 gadget
     */
    private Zp64Gadget zp64Gadget;
    /**
     * base OT receiver output
     */
    private BaseOtReceiverOutput baseOtReceiverOutput;
    /**
     * Δ in binary format
     */
    boolean[] deltaBinary;

    public Kos16Zp64CoreVoleReceiver(Rpc receiverRpc, Party senderParty, Kos16Zp64CoreVoleConfig config) {
        super(Kos16Zp64CoreVolePtoDesc.getInstance(), receiverRpc, senderParty, config);
        baseOtReceiver = BaseOtFactory.createReceiver(receiverRpc, senderParty, config.getBaseOtConfig());
        addSubPto(baseOtReceiver);
    }

    @Override
    public void init(Zp64 zp64, long delta, int maxNum) throws MpcAbortException {
        setInitInput(zp64, delta, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        zp64Gadget = new Zp64Gadget(zp64);
        baseOtReceiver.init();
        deltaBinary = zp64Gadget.bitDecomposition(delta);
        baseOtReceiverOutput = baseOtReceiver.receive(deltaBinary);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Zp64VoleReceiverOutput receive(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Kos16Zp64CoreVolePtoDesc.PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> matrixPayload = rpc.receive(matrixHeader).getPayload();
        Zp64VoleReceiverOutput receiverOutput = handleMatrixPayload(matrixPayload);
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, matrixTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;

    }

    private Zp64VoleReceiverOutput handleMatrixPayload(List<byte[]> matrixPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(matrixPayload.size() == num * l);
        byte[][] matrixPayloadArray = matrixPayload.toArray(new byte[0][]);
        // 创建q矩阵
        long[][] qMatrix = new long[num][l];
        IntStream matrixStream = IntStream.range(0, num * l);
        matrixStream = parallel ? matrixStream.parallel() : matrixStream;
        matrixStream.forEach(index -> {
            // 计算当前处理q矩阵的位置(i,j)
            int rowIndex = index / l;
            int columnIndex = index % l;
            // 从payload中读取Zp元素u
            long u = LongUtils.byteArrayToLong(matrixPayloadArray[index]);
            // 计算tb = PRF(kb, i), q(i,j) = tb + Δ_j * u,
            byte[] tbSeed = ByteBuffer
                .allocate(Long.BYTES + Integer.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
                .putLong(extraInfo).putInt(rowIndex).put(baseOtReceiverOutput.getRb(columnIndex))
                .array();
            long tb = zp64.createRandom(tbSeed);
            qMatrix[rowIndex][columnIndex] = deltaBinary[columnIndex] ? zp64.add(u, tb) : tb;
        });
        // 将矩阵q的每一行按照gadget组合为一个Zp元素，得到Zp数组q。
        Stream<long[]> qMatrixStream = Arrays.stream(qMatrix);
        qMatrixStream = parallel ? qMatrixStream.parallel() : qMatrixStream;
        long[] q = qMatrixStream
            .mapToLong(row -> zp64Gadget.innerProduct(row))
            .toArray();
        return Zp64VoleReceiverOutput.create(zp64, delta, q);
    }
}
