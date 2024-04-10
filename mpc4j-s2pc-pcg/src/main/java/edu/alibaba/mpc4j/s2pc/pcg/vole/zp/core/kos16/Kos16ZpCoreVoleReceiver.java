package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.kos16;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpGadget;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.AbstractZpCoreVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.kos16.Kos16ZpCoreVolePtoDesc.PtoStep;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * KOS16-Zp-core VOLE receiver.
 *
 * @author Hanwen Feng, Weiran Liu
 * @date 2022/06/09
 */
public class Kos16ZpCoreVoleReceiver extends AbstractZpCoreVoleReceiver {
    /**
     * base OT receiver
     */
    private final BaseOtReceiver baseOtReceiver;
    /**
     * Zp gadget
     */
    private ZpGadget zpGadget;
    /**
     * base OT receiver output
     */
    private BaseOtReceiverOutput baseOtReceiverOutput;
    /**
     * Δ in binary format
     */
    boolean[] deltaBinary;

    public Kos16ZpCoreVoleReceiver(Rpc receiverRpc, Party senderParty, Kos16ZpCoreVoleConfig config) {
        super(Kos16ZpCoreVolePtoDesc.getInstance(), receiverRpc, senderParty, config);
        baseOtReceiver = BaseOtFactory.createReceiver(receiverRpc, senderParty, config.getBaseOtConfig());
        addSubPto(baseOtReceiver);
    }

    @Override
    public void init(Zp zp, BigInteger delta, int maxNum) throws MpcAbortException {
        setInitInput(zp, delta, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        zpGadget = new ZpGadget(zp);
        baseOtReceiver.init();
        deltaBinary = zpGadget.decomposition(delta);
        baseOtReceiverOutput = baseOtReceiver.receive(deltaBinary);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public ZpVoleReceiverOutput receive(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> matrixPayload = rpc.receive(matrixHeader).getPayload();
        ZpVoleReceiverOutput receiverOutput = handleMatrixPayload(matrixPayload);
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, matrixTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;

    }

    private ZpVoleReceiverOutput handleMatrixPayload(List<byte[]> matrixPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(matrixPayload.size() == num * l);
        byte[][] matrixPayloadArray = matrixPayload.toArray(new byte[0][]);
        // 创建q矩阵
        BigInteger[][] qMatrix = new BigInteger[num][l];
        IntStream matrixStream = IntStream.range(0, num * l);
        matrixStream = parallel ? matrixStream.parallel() : matrixStream;
        matrixStream.forEach(index -> {
            // 计算当前处理q矩阵的位置(i,j)
            int rowIndex = index / l;
            int columnIndex = index % l;
            // 从payload中读取Zp元素u
            BigInteger u = BigIntegerUtils.byteArrayToNonNegBigInteger(matrixPayloadArray[index]);
            // 计算t_b = PRF(kb, i), q(i,j) = u + Δ_j * t_b,
            byte[] tbSeed = ByteBuffer
                .allocate(Long.BYTES + Integer.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
                .putLong(extraInfo).putInt(rowIndex).put(baseOtReceiverOutput.getRb(columnIndex))
                .array();
            BigInteger tb = zp.createRandom(tbSeed);
            qMatrix[rowIndex][columnIndex] = deltaBinary[columnIndex] ? zp.add(u, tb) : tb;
        });
        // 将矩阵q的每一行按照gadget组合为一个Zp元素，得到Zp数组q。
        Stream<BigInteger[]> qMatrixStream = Arrays.stream(qMatrix);
        qMatrixStream = parallel ? qMatrixStream.parallel() : qMatrixStream;
        BigInteger[] q = qMatrixStream
            .map(row -> zpGadget.innerProduct(row))
            .toArray(BigInteger[]::new);
        return ZpVoleReceiverOutput.create(zp, delta, q);
    }
}
