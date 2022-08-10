package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.core.kos16;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.core.AbstractZ2CoreVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSenderOutput;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * KOS16-Z2-半诚实安全核VOLE协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/6/12
 */
public class Kos16ShZ2CoreVoleSender extends AbstractZ2CoreVoleSender {
    /**
     * 基础OT协议接收方
     */
    private final BaseOtSender baseOtSender;
    /**
     * 密钥派生函数
     */
    private final Kdf kdf;
    /**
     * 基础OT协议输出
     */
    private BaseOtSenderOutput baseOtSenderOutput;
    /**
     * t
     */
    private byte[] t;

    public Kos16ShZ2CoreVoleSender(Rpc senderRpc, Party receiverParty, Kos16ShZ2CoreVoleConfig config) {
        super(Kos16ShZ2CoreVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
        baseOtSender = BaseOtFactory.createSender(senderRpc, receiverParty, config.getBaseOtConfig());
        baseOtSender.addLogLevel();
        kdf = KdfFactory.createInstance(envType);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        baseOtSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        baseOtSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        baseOtSender.addLogLevel();
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        baseOtSender.init();
        // k = 1
        baseOtSenderOutput = baseOtSender.send(1);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public Z2VoleSenderOutput send(byte[] x, int num) throws MpcAbortException {
        setPtoInput(x, num);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        List<byte[]> matrixPayload = generateMatrixPayload();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Kos16ShZ2CoreVolePtoDesc.PtoStep.SENDER_SEND_MATRIX.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(matrixHeader, matrixPayload));
        Z2VoleSenderOutput senderOutput = Z2VoleSenderOutput.create(num, x, t);
        t = null;
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), matrixTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private List<byte[]> generateMatrixPayload() {
        // 初始化伪随机数生成器
        Prg prg = PrgFactory.createInstance(envType, byteNum);
        // R computes t_0 = G(k^0)
        byte[] r0Seed = ByteBuffer.allocate(Long.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
            .putLong(extraInfo).put(baseOtSenderOutput.getR0(0))
            .array();
        r0Seed = kdf.deriveKey(r0Seed);
        t = prg.extendToBytes(r0Seed);
        BytesUtils.reduceByteArray(t, num);
        // and u = t_0 ⊕ G(k^1) ⊕ x
        byte[] r1Seed = ByteBuffer.allocate(Long.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
            .putLong(extraInfo).put(baseOtSenderOutput.getR1(0))
            .array();
        r1Seed = kdf.deriveKey(r1Seed);
        byte[] u = prg.extendToBytes(r1Seed);
        BytesUtils.reduceByteArray(u, num);
        BytesUtils.xori(u, t);
        BytesUtils.xori(u, x);
        List<byte[]> matrixPayload = new LinkedList<>();
        matrixPayload.add(u);
        return matrixPayload;
    }
}
