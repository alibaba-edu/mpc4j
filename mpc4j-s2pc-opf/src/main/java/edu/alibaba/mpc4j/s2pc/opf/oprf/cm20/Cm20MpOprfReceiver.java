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
import edu.alibaba.mpc4j.s2pc.opf.oprf.cm20.Cm20MpOprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractMpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * CM20-MP-OPRF receiver.
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
public class Cm20MpOprfReceiver extends AbstractMpOprfReceiver {
    /**
     * core COT sender
     */
    private final CoreCotSender coreCotSender;
    /**
     * H_1: {0,1}^* → {0,1}^{2λ}
     */
    private final Hash h1;
    /**
     * n = max(2, batchSize)
     */
    private int n;
    /**
     * n in byte
     */
    private int nByteLength;
    /**
     * n offset
     */
    private int nOffset;
    /**
     * PRF output bit length (w)
     */
    private int w;
    /**
     * w in byte
     */
    private int wByteLength;
    /**
     * w offset
     */
    private int wOffset;
    /**
     * F: {0,1}^λ × {0,1}^* → [1,m]^w
     */
    private Prf f;
    /**
     * COT sender output
     */
    private CotSenderOutput cotSenderOutput;
    /**
     * matrix A, organized by columns
     */
    private byte[][] matrixA;
    /**
     * input encodes
     */
    private int[][] encodes;

    public Cm20MpOprfReceiver(Rpc receiverRpc, Party senderParty, Cm20MpOprfConfig config) {
        super(Cm20MpOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotSender = CoreCotFactory.createSender(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        h1 = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH * 2);
    }

    @Override
    public void init(int maxBatchSize, int maxPrfNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPrfNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxW = Cm20MpOprfPtoDesc.getW(Math.max(maxBatchSize, maxPrfNum));
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
        w = Cm20MpOprfPtoDesc.getW(n);
        wByteLength = CommonUtils.getByteLength(w);
        wOffset = wByteLength * Byte.SIZE - w;
        cotSenderOutput = coreCotSender.send(w);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, cotTime, "COT");

        stopWatch.start();
        byte[] prfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        // we send the key first so that the sender can compute something ahead of time
        List<byte[]> prfKeyPayload = Collections.singletonList(prfKey);
        DataPacketHeader prfKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_KEY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(prfKeyHeader, prfKeyPayload));
        f = PrfFactory.createInstance(envType, w * Integer.BYTES);
        f.setKey(prfKey);
        stopWatch.stop();
        long prfKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, prfKeyTime, "Receiver sends PRF Key");

        stopWatch.start();
        // generate B = A ⊕ D
        List<byte[]> deltaPayload = generateDeltaPayload();
        cotSenderOutput = null;
        DataPacketHeader deltaHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_DELTA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(deltaHeader, deltaPayload));
        stopWatch.stop();
        long deltaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, deltaTime, "Receiver generates Δ");

        stopWatch.start();
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
        // n = max(2, batchSize)
        n = batchSize == 1 ? 2 : batchSize;
    }

    private List<byte[]> generateDeltaPayload() {
        // For each y ∈ Y, compute v = F_k(H_1(y)).
        Stream<byte[]> inputStream = Arrays.stream(inputs);
        inputStream = parallel ? inputStream.parallel() : inputStream;
        encodes = inputStream
            .map(input -> {
                byte[] extendPrf = f.getBytes(h1.digestToBytes(input));
                // F: {0, 1}^λ × {0, 1}^{2λ} → [m]^w
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
        // generate Δ
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
                byte[] prf = new byte[wByteLength];
                IntStream.range(0, w).forEach(wIndex -> BinaryUtils.setBoolean(
                    prf, wIndex + wOffset, BinaryUtils.getBoolean(matrixA[wIndex], encodes[index][wIndex])
                ));
                return prf;
            })
            .toArray(byte[][]::new);
        return new MpOprfReceiverOutput(wByteLength, inputs, prfs);
    }
}
