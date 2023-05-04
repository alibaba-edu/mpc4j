package edu.alibaba.mpc4j.s2pc.pcg.ot.base.csw20;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.AbstractBaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.csw20.Csw20BaseOtPtoDesc.PtoStep;

import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * CSW20-基础OT协议发送方。
 *
 * @author Hanwen Feng
 * @date 2022/04/26
 */
public class Csw20BaseOtSender extends AbstractBaseOtSender {
    /**
     * 是否压缩表示
     */
    private final boolean compressEncode;
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * 用于校验receiver回复的参数Ans
     */
    private byte[] answerBytes;
    /**
     * 存储k0的数组
     */
    private byte[][] r0Array;
    /**
     * 存储k1的数组
     */
    private byte[][] r1Array;

    public Csw20BaseOtSender(Rpc senderRpc, Party receiverParty, Csw20BaseOtConfig config) {
        super(Csw20BaseOtPtoDesc.getInstance(), senderRpc, receiverParty, config);
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
        DataPacketHeader rChooseHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_C.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> rChoosePayload = rpc.receive(rChooseHeader).getPayload();
        stopWatch.stop();
        long rChooseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        logStepInfo(PtoState.PTO_STEP, 1, 3, rChooseTime);
        stopWatch.reset();

        stopWatch.start();
        DataPacketHeader sHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_S.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> sPayLoad = generateSenderPayload(rChoosePayload);
        rpc.send(DataPacket.fromByteArrayList(sHeader, sPayLoad));
        stopWatch.stop();
        long sTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        logStepInfo(PtoState.PTO_STEP, 2, 3, sTime);
        stopWatch.reset();

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
        logStepInfo(PtoState.PTO_STEP, 3, 3, rTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private List<byte[]> generateSenderPayload(List<byte[]> receiverPayload) throws MpcAbortException {
        // receiverPayload包含种子，因此长度为n + 1
        MpcAbortPreconditions.checkArgument(receiverPayload.size() == num + 1);
        // 由于要先取出最后一个元素，因此要先转换为数组
        ArrayList<byte[]> receiverPayloadArrayList = new ArrayList<>(receiverPayload);
        // 随机生成y
        final BigInteger y = ecc.randomZn(secureRandom);
        // 计算S = yB
        ECPoint capitalS = ecc.multiply(ecc.getG(), y);
        // 读取seed，生成群元素T, 并计算yT
        byte[] seed = receiverPayloadArrayList.remove(receiverPayloadArrayList.size() - 1);
        ECPoint capitalTy = ecc.multiply(ecc.hashToCurve(ByteBuffer
                .allocate(Long.BYTES + seed.length)
                .putLong(extraInfo).put(seed)
                .array()), y);
        // 创建数组
        byte[][] rByteArray = receiverPayloadArrayList.toArray(new byte[0][]);
        r0Array = new byte[num][];
        r1Array = new byte[num][];
        byte[][] answerInputArray = new byte[num][];
        // 密钥数组生成流，涉及密码学操作，与输入数量相关，需要并行化处理
        IntStream keyPairArrayStream = IntStream.range(0, num);
        keyPairArrayStream = parallel ? keyPairArrayStream.parallel() : keyPairArrayStream;
        List<byte[]> senderPayload = keyPairArrayStream
            .mapToObj(index -> {
                // 读取点B，并计算yB
                ECPoint capitalBy = ecc.multiply(ecc.decode(rByteArray[index]), y);
                // 计算k0 = H(index, yB)和k1 = H（index, yB - yT）
                byte[] k0InputArray = ecc.encode(capitalBy, false);
                byte[] k1InputArray = ecc.encode(capitalBy.subtract(capitalTy), false);
                r0Array[index] = kdf.deriveKey(ByteBuffer
                    .allocate(Integer.BYTES + k0InputArray.length)
                    .putInt(index).put(k0InputArray)
                    .array());
                r1Array[index] = kdf.deriveKey(ByteBuffer
                    .allocate(Integer.BYTES + k1InputArray.length)
                    .putInt(index).put(k1InputArray)
                    .array());
                // 计算挑战消息chall = H(k0) \xor H(k1)，并添加到payload
                answerInputArray[index] = kdf.deriveKey(r0Array[index]);
                return BytesUtils.xor(answerInputArray[index], kdf.deriveKey(r1Array[index]));
            })
            .collect(Collectors.toList());
        // 计算ans= H(k0_1, ...., k0_n)
        ByteBuffer answerByteBuffer = ByteBuffer.allocate(r0Array[0].length * num);
        for (int index = 0; index < num; index++) {
            answerByteBuffer.put(answerInputArray[index]);
        }
        answerBytes = kdf.deriveKey(answerByteBuffer.array());
        // 计算gamma = H(ans)
        byte[] gammaBytes = kdf.deriveKey(answerBytes);
        senderPayload.add(gammaBytes);
        // 将S添加到payload
        senderPayload.add(ecc.encode(capitalS, compressEncode));
        return senderPayload;
    }

    private BaseOtSenderOutput handleReceiverPayload(List<byte[]> receiverPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(receiverPayload.size() == 1);
        MpcAbortPreconditions.checkArgument(Arrays.equals(answerBytes, receiverPayload.get(0)));
        answerBytes = null;
        BaseOtSenderOutput senderOutput = new BaseOtSenderOutput(r0Array, r1Array);
        r0Array = null;
        r1Array = null;
        return senderOutput;
    }
}
