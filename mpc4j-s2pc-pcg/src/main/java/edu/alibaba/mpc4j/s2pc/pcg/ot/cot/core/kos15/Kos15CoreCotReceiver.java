package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.kos15;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
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
import java.util.LinkedList;
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
     * GF(2^128)运算接口
     */
    private final Gf2k gf2k;
    /**
     * KDF-OT协议输出
     */
    private KdfOtSenderOutput kdfOtSenderOutput;
    /**
     * 随机预言机
     */
    private Prf randomOracle;
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

    public Kos15CoreCotReceiver(Rpc receiverRpc, Party senderParty, Kos15CoreCotConfig config) {
        super(Kos15CoreCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        baseOtSender = BaseOtFactory.createSender(receiverRpc, senderParty, config.getBaseOtConfig());
        addSubPtos(baseOtSender);
        gf2k = Gf2kFactory.createInstance(envType);
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
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime);

        stopWatch.start();
        DataPacketHeader randomOracleKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_RANDOM_ORACLE_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> randomOracleKeyPayload = rpc.receive(randomOracleKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(randomOracleKeyPayload.size() == 1);
        // 设置随机预言机
        randomOracle = PrfFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        randomOracle.setKey(randomOracleKeyPayload.remove(0));
        stopWatch.stop();
        long randomOracleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, randomOracleTime);

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

        stopWatch.start();
        List<byte[]> correlateCheckPayload = generateCorrelateCheckPayload();
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
        // l' = l + (κ + s)
        extendNum = num + CommonConstants.BLOCK_BIT_LENGTH + CommonConstants.STATS_BIT_LENGTH;
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

    private List<byte[]> generateCorrelateCheckPayload() {
        // 矩阵转置，得到t
        tTransposeMatrix = tMatrix.transpose();
        tMatrix = null;
        byte[][] chiPolynomials = new byte[extendNum][];
        byte[][] tPolynomials = new byte[extendNum][];
        IntStream extendIndexIntStream = IntStream.range(0, extendNum);
        extendIndexIntStream = parallel ? extendIndexIntStream.parallel() : extendIndexIntStream;
        extendIndexIntStream.forEach(extendIndex -> {
            // 调用随机预言的输入是ExtraInfo || extendIndex
            byte[] indexMessage = ByteBuffer.allocate(Long.BYTES + Integer.BYTES)
                .putLong(extraInfo).putInt(extendIndex).array();
            // Sample (χ_1, ..., χ_{l'}) ← F_{Rand}(F_{2^κ}^{l'}).
            chiPolynomials[extendIndex] = randomOracle.getBytes(indexMessage);
            // t_j·χ_j
            tPolynomials[extendIndex] = gf2k.mul(chiPolynomials[extendIndex], tTransposeMatrix.getColumn(extendIndex));
        });
        byte[] xPolynomial = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        // x = Σ_{j = 1}^{l'} (x_j · χ_j)
        for (int extendIndex = 0; extendIndex < extendNum; extendIndex++) {
            // 如果x_j = 1，则x = x + χ_j
            if (extendChoices[extendIndex]) {
                gf2k.addi(xPolynomial, chiPolynomials[extendIndex]);
            }
        }
        // t = Σ_{j = 1}^{l'} (t_j · χ_j)
        byte[] tPolynomial = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        for (int extendIndex = 0; extendIndex < extendNum; extendIndex++) {
            gf2k.addi(tPolynomial, tPolynomials[extendIndex]);
        }
        List<byte[]> correlateCheckPayload = new LinkedList<>();
        correlateCheckPayload.add(xPolynomial);
        correlateCheckPayload.add(tPolynomial);

        return correlateCheckPayload;
    }

    private CotReceiverOutput generateReceiverOutput() {
        byte[][] rbArray = IntStream.range(0, num)
            .mapToObj(tTransposeMatrix::getColumn)
            .toArray(byte[][]::new);
        return CotReceiverOutput.create(choices, rbArray);
    }
}
