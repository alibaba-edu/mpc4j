package edu.alibaba.mpc4j.s2pc.opf.oprf.cm20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractMpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * CM20-MPOPRF接收方。
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
public class Cm20MpOprfReceiver extends AbstractMpOprfReceiver {
    /**
     * 核COT协议发送方
     */
    private final CoreCotSender coreCotSender;
    /**
     * H_1: {0,1}^* → {0,1}^{2λ}
     */
    private final Hash h1;
    /**
     * 规约批处理数量
     */
    private int n;
    /**
     * 批处理数量字节长度
     */
    private int nByteLength;
    /**
     * 批处理数量偏移量
     */
    private int nOffset;
    /**
     * 编码比特长度（w）
     */
    private int w;
    /**
     * PRF输出字节长度
     */
    private int wByteLength;
    /**
     * PRF输出字节长度偏移量
     */
    private int wOffset;
    /**
     * F: {0,1}^λ × {0,1}^* → [1,m]^w
     */
    private Prf f;
    /**
     * ROT发送方输出
     */
    private CotSenderOutput cotSenderOutput;
    /**
     * 矩阵A
     */
    private byte[][] matrixA;
    /**
     * 输入编码结果
     */
    private int[][] encodes;

    public Cm20MpOprfReceiver(Rpc receiverRpc, Party senderParty, Cm20MpOprfConfig config) {
        super(Cm20MpOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotSender = CoreCotFactory.createSender(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPtos(coreCotSender);
        h1 = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH * 2);
    }

    @Override
    public void init(int maxBatchSize, int maxPrfNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPrfNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 计算maxW，初始化COT协议
        int maxW = Cm20MpOprfUtils.getW(Math.max(maxBatchSize, maxPrfNum));
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        coreCotSender.init(delta, maxW);
        stopWatch.stop();
        long initCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initCotTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public MpOprfReceiverOutput oprf(byte[][] inputs) throws MpcAbortException {
        setPtoInput(inputs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        nByteLength = CommonUtils.getByteLength(n);
        nOffset = nByteLength * Byte.SIZE - n;
        // 计算w
        w = Cm20MpOprfUtils.getW(n);
        wByteLength = CommonUtils.getByteLength(w);
        wOffset = wByteLength * Byte.SIZE - w;
        // 执行COT协议
        cotSenderOutput = coreCotSender.send(w);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, cotTime, "COT");

        stopWatch.start();
        // 初始化伪随机函数密钥
        byte[] prfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        // 先发送生成的伪随机函数密钥
        List<byte[]> prfKeyPayload = new LinkedList<>();
        prfKeyPayload.add(prfKey);
        DataPacketHeader prfKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Cm20MpOprfPtoDesc.PtoStep.RECEIVER_SEND_KEY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(prfKeyHeader, prfKeyPayload));
        // 再初始化自己的伪随机函数
        f = PrfFactory.createInstance(envType, w * Integer.BYTES);
        f.setKey(prfKey);
        stopWatch.stop();
        long prfKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, prfKeyTime, "Receiver sends PRF Key");

        stopWatch.start();
        // 生成关联矩阵B = A ⊕ D
        List<byte[]> deltaPayload = generateDeltaPayload();
        cotSenderOutput = null;
        DataPacketHeader deltaHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), Cm20MpOprfPtoDesc.PtoStep.RECEIVER_SEND_DELTA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(deltaHeader, deltaPayload));
        stopWatch.stop();
        long deltaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, deltaTime, "Receiver generates Δ");

        stopWatch.start();
        // 生成OPRF
        MpOprfReceiverOutput receiverOutput = generateOprfOutput();
        matrixA = null;
        encodes = null;
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, oprfTime, "Receiver generates OPRF");

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    @Override
    protected void setPtoInput(byte[][] inputs) {
        super.setPtoInput(inputs);
        // 如果batchSize = 1，则实际执行时的n要设置成大于1，否则无法成功编码
        n = batchSize == 1 ? 2 : batchSize;
    }

    private List<byte[]> generateDeltaPayload() {
        // For each y ∈ Y, compute v = F_k(H_1(y)).
        Stream<byte[]> inputStream = Arrays.stream(inputs);
        inputStream = parallel ? inputStream.parallel() : inputStream;
        encodes = inputStream
            .map(input -> {
                // 计算哈希值
                byte[] extendPrf = f.getBytes(h1.digestToBytes(input));
                // F: {0, 1}^λ × {0, 1}^{2λ} → [m]^w，这里使用不安全转换函数来提高效率。
                int[] encode = IntUtils.byteArrayToIntArray(extendPrf);
                for (int index = 0; index < w; index++) {
                    encode[index] = Math.abs(encode[index] % n) + nOffset;
                }
                return encode;
            })
            .toArray(int[][]::new);
        // Initialize an m × w binary matrix D to all 1’s. Set D_i[v[i]] = 0 for all i ∈ [w].
        IntStream wIntStream = IntStream.range(0, w);
        wIntStream = parallel ? wIntStream.parallel() : wIntStream;
        byte[][] matrixD = wIntStream.mapToObj(wIndex -> {
            byte[] dColumn = new byte[nByteLength];
            Arrays.fill(dColumn, (byte) 0xFF);
            BytesUtils.reduceByteArray(dColumn, n);
            int[] positions = IntStream.range(0, batchSize).map(index -> encodes[index][wIndex]).toArray();
            BinaryUtils.setBoolean(dColumn, positions, false);
            return dColumn;
        }).toArray(byte[][]::new);
        // 生成Δ
        Prg prg = PrgFactory.createInstance(envType, nByteLength);
        matrixA = new byte[w][nByteLength];
        IntStream deltaIntStream = IntStream.range(0, w);
        deltaIntStream = parallel ? deltaIntStream.parallel() : deltaIntStream;
        return deltaIntStream.mapToObj(index -> {
            // We do not need to use CRHF since we need to call PRG.
            matrixA[index] = prg.extendToBytes(cotSenderOutput.getR0(index));
            BytesUtils.reduceByteArray(matrixA[index], n);
            // B_i = A_i ⊕ D_i, Δ_i = B_i ⊕ r_i^1
            byte[] deltaColumn = prg.extendToBytes(cotSenderOutput.getR1(index));
            BytesUtils.reduceByteArray(deltaColumn, n);
            BytesUtils.xori(deltaColumn, matrixA[index]);
            BytesUtils.xori(deltaColumn, matrixD[index]);
            return deltaColumn;
        }).collect(Collectors.toList());
    }

    private MpOprfReceiverOutput generateOprfOutput() {
        IntStream inputIndexStream = IntStream.range(0, batchSize);
        inputIndexStream = parallel ? inputIndexStream.parallel() : inputIndexStream;
        byte[][] prfs = inputIndexStream
            .mapToObj(index -> {
                // 当输出长度比较短的时候，直接用boolean[]会更快一些
                boolean[] binaryPrf = new boolean[wByteLength * Byte.SIZE];
                IntStream.range(0, w).forEach(wIndex -> binaryPrf[wIndex + wOffset] =
                    BinaryUtils.getBoolean(matrixA[wIndex], encodes[index][wIndex]));
                return BinaryUtils.binaryToByteArray(binaryPrf);
            })
            .toArray(byte[][]::new);
        return new MpOprfReceiverOutput(wByteLength, inputs, prfs);
    }
}
