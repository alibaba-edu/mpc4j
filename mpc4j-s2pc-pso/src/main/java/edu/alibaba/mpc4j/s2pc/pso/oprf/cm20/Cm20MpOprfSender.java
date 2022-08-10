package edu.alibaba.mpc4j.s2pc.pso.oprf.cm20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pso.oprf.AbstractMpOprfSender;
import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pso.oprf.cm20.Cm20MpOprfPtoDesc.PtoStep;

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
     * 抗关联哈希函数
     */
    private final Crhf crhf;
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
        coreCotReceiver.addLogLevel();
        crhf = CrhfFactory.createInstance(envType, CrhfType.MMO);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        coreCotReceiver.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        coreCotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        coreCotReceiver.addLogLevel();
    }

    @Override
    public void init(int maxBatchSize) throws MpcAbortException {
        setInitInput(maxBatchSize);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 计算maxW，初始化COT协议
        int maxW = Cm20MpOprfUtils.getW(maxBatchSize);
        coreCotReceiver.init(maxW);
        stopWatch.stop();
        long initCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initCotTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public MpOprfSenderOutput oprf(int batchSize) throws MpcAbortException {
        setPtoInput(batchSize);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

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
        info("{}{} Send. Step 1/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cotTime);

        stopWatch.start();
        // 发送方接收PRF密钥
        DataPacketHeader prfKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> prfKeyPayload = rpc.receive(prfKeyHeader).getPayload();
        // 读取密钥
        MpcAbortPreconditions.checkArgument(prfKeyPayload.size() == 1);
        byte[] prfKey = prfKeyPayload.remove(0);
        stopWatch.stop();
        long prfKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), prfKeyTime);

        stopWatch.start();
        DataPacketHeader deltaHeader = new DataPacketHeader(
            taskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_DELTA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> deltaPayload = rpc.receive(deltaHeader).getPayload();
        // 读取矩阵
        handleDeltaPayload(deltaPayload);
        Cm20MpOprfSenderOutput senderOutput = new Cm20MpOprfSenderOutput(envType, batchSize, w, prfKey, matrixC);
        matrixC = null;
        stopWatch.stop();
        long deltaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 3/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), deltaTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
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
            byte[] choiceSeed = cotReceiverOutput.getRb(index);
            choiceSeed = crhf.hash(choiceSeed);
            byte[] cColumn = prg.extendToBytes(choiceSeed);
            BytesUtils.reduceByteArray(cColumn, n);
            if (s[index]) {
                BytesUtils.xori(cColumn, deltaArray[index]);
            }
            return cColumn;
        }).toArray(byte[][]::new);
        cotReceiverOutput = null;
    }
}
