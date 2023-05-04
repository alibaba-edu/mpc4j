package edu.alibaba.mpc4j.s2pc.pcg.ot.base.co15;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.AbstractBaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.co15.Co15BaseOtPtoDesc.PtoStep;
import org.bouncycastle.math.ec.ECPoint;

/**
 * CO15-基础OT协议发送方。
 *
 * @author Weiran Liu, Hanwen Feng
 * @date 2020/06/04
 */
public class Co15BaseOtSender extends AbstractBaseOtSender {
    /**
     * 是否压缩表示
     */
    private final boolean compressEncode;
    /**
     * 椭圆曲线参数
     */
    private final Ecc ecc;
    /**
     * 私钥y
     */
    private BigInteger y;
    /**
     * 椭圆曲线点S
     */
    private ECPoint capitalS;
    /**
     * 椭圆曲线点S
     */
    private ECPoint capitalT;

    public Co15BaseOtSender(Rpc senderRpc, Party receiverParty, Co15BaseOtConfig config) {
        super(Co15BaseOtPtoDesc.getInstance(), senderRpc, receiverParty, config);
        compressEncode = config.getCompressEncode();
        ecc = EccFactory.createInstance(envType);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);
        // empty init step
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BaseOtSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> sPayload = generateSenderPayload();
        DataPacketHeader sDataPacketHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_S.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sDataPacketHeader, sPayload));
        stopWatch.stop();
        long sTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, sTime);

        stopWatch.start();
        DataPacketHeader rHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_R.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> rPayload = rpc.receive(rHeader).getPayload();
        BaseOtSenderOutput senderOutput = handleReceiverPayload(rPayload);
        stopWatch.stop();
        long rTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, rTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private List<byte[]> generateSenderPayload() {
        // 随机生成y
        y = ecc.randomZn(secureRandom);
        // 计算S = yB
        capitalS = ecc.multiply(ecc.getG(), y);
        // 计算T = yS
        capitalT = ecc.multiply(capitalS, y);
        List<byte[]> senderPayload = new LinkedList<>();
        senderPayload.add(ecc.encode(capitalS, compressEncode));

        return senderPayload;
    }

    private BaseOtSenderOutput handleReceiverPayload(List<byte[]> receiverPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(receiverPayload.size() == num);
        byte[][] rByteArray = receiverPayload.toArray(new byte[0][]);
        byte[][] r0Array = new byte[num][];
        byte[][] r1Array = new byte[num][];
        // 密钥数组生成流，涉及密码学操作，与输入数量相关，需要并行化处理
        IntStream keyPairArrayStream = IntStream.range(0, num);
        keyPairArrayStream = parallel ? keyPairArrayStream.parallel() : keyPairArrayStream;
        keyPairArrayStream.forEach(index -> {
            // 计算K = yR
            ECPoint capitalR = ecc.decode(rByteArray[index]);
            ECPoint capitalK = ecc.multiply(capitalR, y);
            // 计算k0 = H(index,K)
            byte[] k0InputByteArray = ecc.encode(capitalK, false);
            r0Array[index] = kdf.deriveKey(ByteBuffer
                .allocate(Integer.BYTES + k0InputByteArray.length)
                .putInt(index).put(k0InputByteArray)
                .array());
            // 计算k1 = H(index, K - T)
            byte[] k1InputByteArray = ecc.encode(capitalK.subtract(capitalT), false);
            r1Array[index] = kdf.deriveKey(ByteBuffer
                .allocate(Integer.BYTES + k1InputByteArray.length)
                .putInt(index).put(k1InputByteArray)
                .array());
        });
        y = null;
        capitalS = null;
        capitalT = null;
        return new BaseOtSenderOutput(r0Array, r1Array);
    }
}
