package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.core.kos16;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.core.AbstractZ2CoreVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiverOutput;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * KOS16-Z2-半诚实安全核VOLE协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/6/12
 */
public class Kos16ShZ2CoreVoleReceiver extends AbstractZ2CoreVoleReceiver {
    /**
     * 基础OT协议接收方
     */
    private final BaseOtReceiver baseOtReceiver;
    /**
     * 密钥派生函数
     */
    private final Kdf kdf;
    /**
     * 基础OT协议输出
     */
    private BaseOtReceiverOutput baseOtReceiverOutput;

    public Kos16ShZ2CoreVoleReceiver(Rpc senderRpc, Party receiverParty, Kos16ShZ2CoreVoleConfig config) {
        super(Kos16ShZ2CoreVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
        baseOtReceiver = BaseOtFactory.createReceiver(senderRpc, receiverParty, config.getBaseOtConfig());
        baseOtReceiver.addLogLevel();
        kdf = KdfFactory.createInstance(envType);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        baseOtReceiver.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        baseOtReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        baseOtReceiver.addLogLevel();
    }

    @Override
    public void init(boolean delta, int maxNum) throws MpcAbortException {
        setInitInput(delta, maxNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        baseOtReceiver.init();
        baseOtReceiverOutput = baseOtReceiver.receive(new boolean[]{delta});
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public Z2VoleReceiverOutput receive(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Kos16ShZ2CoreVolePtoDesc.PtoStep.SENDER_SEND_MATRIX.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> matrixPayload = rpc.receive(matrixHeader).getPayload();
        Z2VoleReceiverOutput receiverOutput = handleMatrixPayload(matrixPayload);
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), matrixTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }

    private Z2VoleReceiverOutput handleMatrixPayload(List<byte[]> matrixPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(matrixPayload.size() == 1);
        Prg prg = PrgFactory.createInstance(envType, byteNum);
        byte[] u = matrixPayload.remove(0);
        byte[] rbSeed = ByteBuffer.allocate(Long.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
            .putLong(extraInfo).put(baseOtReceiverOutput.getRb(0))
            .array();
        rbSeed = kdf.deriveKey(rbSeed);
        byte[] q = prg.extendToBytes(rbSeed);
        BytesUtils.reduceByteArray(q, num);
        if (delta) {
            BytesUtils.xori(q, u);
        }
        return Z2VoleReceiverOutput.create(num, delta, q);
    }
}
