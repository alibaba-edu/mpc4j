package edu.alibaba.mpc4j.s2pc.opf.oprf.cm20;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractMpOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * CM20-MPOPRF发送方。
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
public class Cm20MpOprfSender extends AbstractMpOprfSender {
    /**
     * 核COT协议接收方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * 规约批处理数量
     */
    private int n;
    /**
     * 批处理数量字节长度
     */
    private int nByteLength;
    /**
     * 编码比特长度（w）
     */
    private int w;
    /**
     * 选择比特
     */
    private boolean[] s;
    /**
     * COT接收方输出
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * 矩阵C
     */
    private byte[][] matrixC;

    public Cm20MpOprfSender(Rpc senderRpc, Party receiverParty, Cm20MpOprfConfig config) {
        super(Cm20MpOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPtos(coreCotReceiver);
    }

    @Override
    public void init(int maxBatchSize, int maxPrfNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPrfNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 计算maxW，初始化COT协议
        int maxW = Cm20MpOprfUtils.getW(Math.max(maxBatchSize, maxPrfNum));
        coreCotReceiver.init(maxW);
        stopWatch.stop();
        long initCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initCotTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public MpOprfSenderOutput oprf(int batchSize) throws MpcAbortException {
        setPtoInput(batchSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // 设置n
        nByteLength = CommonUtils.getByteLength(n);
        // 设置w
        w = Cm20MpOprfUtils.getW(n);
        s = new boolean[w];
        IntStream.range(0, w).forEach(index -> s[index] = secureRandom.nextBoolean());
        // 执行COT协议
        cotReceiverOutput = coreCotReceiver.receive(s);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, cotTime, "COT");

        // 发送方接收PRF密钥
        DataPacketHeader prfKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Cm20MpOprfPtoDesc.PtoStep.RECEIVER_SEND_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> prfKeyPayload = rpc.receive(prfKeyHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(prfKeyPayload.size() == 1);
        byte[] prfKey = prfKeyPayload.remove(0);
        stopWatch.stop();
        long prfKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, prfKeyTime, "Sender receives PRF Key");

        DataPacketHeader deltaHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), Cm20MpOprfPtoDesc.PtoStep.RECEIVER_SEND_DELTA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> deltaPayload = rpc.receive(deltaHeader).getPayload();

        stopWatch.start();
        // 读取矩阵
        handleDeltaPayload(deltaPayload);
        Cm20MpOprfSenderOutput senderOutput = new Cm20MpOprfSenderOutput(envType, batchSize, w, prfKey, matrixC);
        matrixC = null;
        stopWatch.stop();
        long deltaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, deltaTime, "Sender generates OPRF");

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    @Override
    protected void setPtoInput(int batchSize) {
        super.setPtoInput(batchSize);
        // 如果batchSize = 1，则实际执行时要设置成大于1，否则无法成功编码
        n = batchSize == 1 ? 2 : batchSize;
    }

    private void handleDeltaPayload(List<byte[]> deltaPayload) {
        byte[][] deltaArray = deltaPayload.toArray(new byte[0][]);
        Prg prg = PrgFactory.createInstance(envType, nByteLength);
        // 扩展矩阵C涉及密码学操作，需要并行化
        IntStream wIntStream = IntStream.range(0, w);
        wIntStream = parallel ? wIntStream.parallel() : wIntStream;
        matrixC = wIntStream.mapToObj(index -> {
            // We do not need to use CRHF since we need to call PRG.
            byte[] cColumn = prg.extendToBytes(cotReceiverOutput.getRb(index));
            BytesUtils.reduceByteArray(cColumn, n);
            if (s[index]) {
                BytesUtils.xori(cColumn, deltaArray[index]);
            }
            return cColumn;
        }).toArray(byte[][]::new);
        cotReceiverOutput = null;
    }
}
