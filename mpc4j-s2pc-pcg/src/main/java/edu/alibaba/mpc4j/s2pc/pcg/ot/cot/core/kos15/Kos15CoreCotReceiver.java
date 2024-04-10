package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.kos15;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf64.Gf64;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf64.Gf64Factory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.KdfOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.AbstractCoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.kos15.Kos15CoreCotPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * KOS15-核COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/6/7
 */
public class Kos15CoreCotReceiver extends AbstractCoreCotReceiver {
    /**
     * 基础OT协议发送方
     */
    private final BaseOtSender baseOtSender;
    /**
     * GF(2^64)运算接口
     */
    private final Gf64 gf64;
    /**
     * KDF-OT协议输出
     */
    private KdfOtSenderOutput kdfOtSenderOutput;
    /**
     * 扩展数量
     */
    private int extendNum;
    /**
     * 扩展选择比特数组
     */
    private boolean[] extendChoices;
    /**
     * 矩阵T
     */
    private TransBitMatrix tMatrix;
    /**
     * 转置矩阵T
     */
    private TransBitMatrix tTransposeMatrix;
    /**
     * partition num
     */
    private int m;
    /**
     * bit length
     */
    private final int s;

    public Kos15CoreCotReceiver(Rpc receiverRpc, Party senderParty, Kos15CoreCotConfig config) {
        super(Kos15CoreCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        baseOtSender = BaseOtFactory.createSender(receiverRpc, senderParty, config.getBaseOtConfig());
        addSubPto(baseOtSender);
        gf64 = Gf64Factory.createInstance(envType, config.getGf64Type());
        s = gf64.getL();
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        baseOtSender.init();
        kdfOtSenderOutput = new KdfOtSenderOutput(envType, baseOtSender.send(CommonConstants.BLOCK_BIT_LENGTH));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public CotReceiverOutput receive(boolean[] choices) throws MpcAbortException {
        setPtoInput(choices);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> matrixPayload = generateMatrixPayload();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(matrixHeader, matrixPayload));
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, matrixTime);

        DataPacketHeader chiPolynomialHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CHI_POLYNOMIAL.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> chiPolynomialPayload = rpc.receive(chiPolynomialHeader).getPayload();
        MpcAbortPreconditions.checkArgument(chiPolynomialPayload.size() == m);

        stopWatch.start();
        List<byte[]> correlateCheckPayload = generateCorrelateCheckPayload(chiPolynomialPayload);
        DataPacketHeader correlateCheckHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CHECK.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(correlateCheckHeader, correlateCheckPayload));
        CotReceiverOutput receiverOutput = generateReceiverOutput();
        tTransposeMatrix = null;
        stopWatch.stop();
        long checkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, checkTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private List<byte[]> generateMatrixPayload() {
        // l' = l + s
        m = CommonUtils.getUnitNum(num, s);
        extendNum = (m + 1) * s;
        int extendByteNum = CommonUtils.getByteLength(extendNum);
        // 扩展选择比特向量
        extendChoices = new boolean[extendNum];
        // x_j = x_j for j \in [l]
        System.arraycopy(choices, 0, extendChoices, 0, num);
        // x_j is random for j \in [l + 1, l']
        for (int extendIndex = num; extendIndex < extendNum; extendIndex++) {
            extendChoices[extendIndex] = secureRandom.nextBoolean();
        }
        // 将选择比特组合成byte[]，方便在矩阵中执行xor运算
        byte[] rExtendBytes = BinaryUtils.binaryToRoundByteArray(extendChoices);
        // 初始化伪随机数生成器
        Prg prg = PrgFactory.createInstance(envType, extendByteNum);
        // 构建矩阵tMatrix，共有l'行，λ列
        tMatrix = TransBitMatrixFactory.createInstance(envType, extendNum, CommonConstants.BLOCK_BIT_LENGTH, parallel);
        // 用密钥扩展得到矩阵T
        IntStream columnIndexIntStream = IntStream.range(0, CommonConstants.BLOCK_BIT_LENGTH);
        columnIndexIntStream = parallel ? columnIndexIntStream.parallel() : columnIndexIntStream;
        return columnIndexIntStream
            .mapToObj(columnIndex -> {
                // R computes t^i = G(k^0_i)
                byte[] tExtendBytes = prg.extendToBytes(kdfOtSenderOutput.getK0(columnIndex, extraInfo));
                BytesUtils.reduceByteArray(tExtendBytes, extendNum);
                tMatrix.setColumn(columnIndex, tExtendBytes);
                // and u^i = t^i ⊕ G(k_i^1) ⊕ r
                byte[] uExtendBytes = prg.extendToBytes(kdfOtSenderOutput.getK1(columnIndex, extraInfo));
                BytesUtils.reduceByteArray(uExtendBytes, extendNum);
                BytesUtils.xori(uExtendBytes, tExtendBytes);
                BytesUtils.xori(uExtendBytes, rExtendBytes);

                return uExtendBytes;
            })
            .collect(Collectors.toList());
    }

    private List<byte[]> generateCorrelateCheckPayload(List<byte[]> chiPolynomial) {
        List<byte[]> correlateCheckPayload = new ArrayList<>();
        byte[][] xBlock = new byte[m + 1][];
        for (int i = 0; i < m + 1; i++) {
            boolean[] block = new boolean[s];
            System.arraycopy(extendChoices, i * s, block, 0, s);
            xBlock[i] = BinaryUtils.binaryToByteArray(block);
        }
        byte[] xPolynomial = gf64.createZero();
        for (int i = 0; i < m; i++) {
            gf64.muli(xBlock[i], chiPolynomial.get(i));
            gf64.addi(xPolynomial, xBlock[i]);
        }
        gf64.addi(xPolynomial, xBlock[m]);
        correlateCheckPayload.add(xPolynomial);
        IntStream tMatrixIntStream = IntStream.range(0, CommonConstants.BLOCK_BIT_LENGTH);
        tMatrixIntStream = parallel ? tMatrixIntStream.parallel() : tMatrixIntStream;
        correlateCheckPayload.addAll(tMatrixIntStream.mapToObj(i -> {
            byte[][] tBlock = new byte[m + 1][CommonUtils.getByteLength(s)];
            ByteBuffer flattenColumn = ByteBuffer.wrap(tMatrix.getColumn(i));
            for (int j = 0; j < m + 1; j++) {
                flattenColumn.get(tBlock[j]);
            }
            byte[] ti = gf64.createZero();
            for (int j = 0; j < m; j++) {
                gf64.muli(tBlock[j], chiPolynomial.get(j));
                gf64.addi(ti, tBlock[j]);
            }
            gf64.addi(ti, tBlock[m]);
            return ti;
        }).collect(Collectors.toList()));
        // 矩阵转置，得到t
        tTransposeMatrix = tMatrix.transpose();
        tMatrix = null;
        return correlateCheckPayload;
    }

    private CotReceiverOutput generateReceiverOutput() {
        byte[][] rbArray = IntStream.range(0, num)
            .mapToObj(tTransposeMatrix::getColumn)
            .toArray(byte[][]::new);
        return CotReceiverOutput.create(choices, rbArray);
    }
}
