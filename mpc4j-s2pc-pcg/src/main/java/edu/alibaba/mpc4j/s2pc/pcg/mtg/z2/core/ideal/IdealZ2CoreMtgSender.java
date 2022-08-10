package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.ideal;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.AbstractZ2CoreMtgParty;

/**
 * 理想核布尔三元组生成协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/02/08
 */
public class IdealZ2CoreMtgSender extends AbstractZ2CoreMtgParty {
    /**
     * 密钥派生函数
     */
    private final Kdf kdf;
    /**
     * 根密钥
     */
    private byte[] rootKey;

    public IdealZ2CoreMtgSender(Rpc senderRpc, Party receiverParty, IdealZ2CoreMtgConfig config) {
        super(IdealZ2CoreMtgPtoDesc.getInstance(), senderRpc, receiverParty, config);
        kdf = KdfFactory.createInstance(getEnvType());
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        rootKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(rootKey);
        List<byte[]> rootKeyPayload = new LinkedList<>();
        rootKeyPayload.add(rootKey);
        DataPacketHeader rootKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), IdealZ2CoreMtgPtoDesc.PtoStep.SERVER_SEND_ROOT_KEY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(rootKeyHeader, rootKeyPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public Z2Triple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        Z2Triple senderOutput = generateBooleanTriple();
        stopWatch.stop();
        long generateTripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), generateTripleTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private Z2Triple generateBooleanTriple() {
        Prg prg = PrgFactory.createInstance(envType, byteNum);
        // 更新密钥
        byte[] a0Key = kdf.deriveKey(rootKey);
        byte[] a1Key = kdf.deriveKey(a0Key);
        byte[] b0Key = kdf.deriveKey(a1Key);
        byte[] b1Key = kdf.deriveKey(b0Key);
        byte[] c0Key = kdf.deriveKey(b1Key);
        rootKey = kdf.deriveKey(c0Key);
        // 生成a0、b0、c0
        byte[] a0 = prg.extendToBytes(a0Key);
        BytesUtils.reduceByteArray(a0, num);
        byte[] b0 = prg.extendToBytes(b0Key);
        BytesUtils.reduceByteArray(b0, num);
        byte[] c0 = prg.extendToBytes(c0Key);
        BytesUtils.reduceByteArray(c0, num);

        return Z2Triple.create(num, a0, b0, c0);
    }
}
