package edu.alibaba.mpc4j.s2pc.pcg.ot.base.csw20;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.AbstractBaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiverOutput;
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
 * CSW20-基础OT协议接收方。
 *
 * @author Hanwen Feng
 * @date 2022/04/26
 */
public class Csw20BaseOtReceiver extends AbstractBaseOtReceiver {
    /**
     * 是否压缩表示
     */
    private final boolean compressEncode;
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * 选择比特数组对应的密钥
     */
    private byte[][] rbArray;
    /**
     * OT协议接收方参数
     */
    private BigInteger[] aArray;

    public Csw20BaseOtReceiver(Rpc receiverRpc, Party senderParty, Csw20BaseOtConfig config) {
        super(Csw20BaseOtPtoDesc.getInstance(), receiverRpc, senderParty, config);
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
    public BaseOtReceiverOutput receive(boolean[] choices) throws MpcAbortException {
        setPtoInput(choices);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        DataPacketHeader rChooseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_C.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> rChoosePayLoad = generateReceiverChoosePayLoad();
        rpc.send(DataPacket.fromByteArrayList(rChooseHeader, rChoosePayLoad));
        stopWatch.stop();
        long cTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, cTime);

        stopWatch.start();
        DataPacketHeader senderHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_S.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> senderPayload = rpc.receive(senderHeader).getPayload();
        stopWatch.stop();
        long sTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, sTime);

        stopWatch.start();
        List<byte[]> receiverPayload = generateReceiverPayload(senderPayload);
        DataPacketHeader receiverHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_R.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(receiverHeader, receiverPayload));
        BaseOtReceiverOutput receiverOutput = new BaseOtReceiverOutput(choices, rbArray);
        rbArray = null;
        stopWatch.stop();
        long rTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, rTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private List<byte[]> generateReceiverChoosePayLoad() {
        // 随机选取种子，用于生成群元素T
        byte[] seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(seed);
        ECPoint upperT = ecc.hashToCurve(ByteBuffer
            .allocate(Long.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
            .putLong(extraInfo).put(seed)
            .array());
        aArray = new BigInteger[choices.length];
        // 涉及密码学操作，与输入数量相关，需要并行化处理
        IntStream aIntStream = IntStream.range(0, choices.length);
        aIntStream = parallel ? aIntStream.parallel() : aIntStream;
        List<byte[]> receiverChoosePayLoad = aIntStream
            .mapToObj(index -> {
                // 采样a_i
                aArray[index] = ecc.randomZn(secureRandom);
                // 计算B_i = a_i*G + b_i*T
                return choices[index] ?
                    ecc.multiply(ecc.getG(), aArray[index]).add(upperT) : ecc.multiply(ecc.getG(), aArray[index]);
            })
            .map(capitalB -> ecc.encode(capitalB, compressEncode))
            .collect(Collectors.toList());
        receiverChoosePayLoad.add(seed);
        return receiverChoosePayLoad;
    }

    private List<byte[]> generateReceiverPayload(List<byte[]> senderPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(senderPayload.size() == choices.length + 2);
        // 由于要先移除最后一个元素，因此先转换为数组
        ArrayList<byte[]> senderPayloadArrayList = new ArrayList<>(senderPayload);
        ECPoint capitalS = ecc.decode(senderPayloadArrayList.remove(senderPayloadArrayList.size() - 1));
        byte[] gammaBytes = senderPayloadArrayList.remove(senderPayloadArrayList.size() - 1);
        rbArray = new byte[choices.length][];
        byte[][] respByteArray = new byte[choices.length][];
        byte[][] sByteArray = senderPayloadArrayList.toArray(new byte[0][]);
        // r_i计算流，涉及密码学操作，与输入数量相关，需要并行化处理
        IntStream rIntStream = IntStream.range(0, choices.length);
        rIntStream = this.parallel ? rIntStream.parallel() : rIntStream;
        rIntStream.forEach(index -> {
            // 计算密钥 k = H(index, a * S)
            byte[] kInputByteArray = ecc.encode(ecc.multiply(capitalS, aArray[index]), false);
            rbArray[index] = kdf.deriveKey(ByteBuffer
                .allocate(Integer.BYTES + kInputByteArray.length)
                .putInt(index).put(kInputByteArray)
                .array());
            // 计算resp = H(k) \xor b* chall
            respByteArray[index] = choices[index] ?
                BytesUtils.xor(kdf.deriveKey(rbArray[index]), sByteArray[index]) :
                kdf.deriveKey(rbArray[index]);
        });
        aArray = null;
        // 计算ans' = H(resp_1, ... , resp_n)
        ByteBuffer ansInputByteBuffer = ByteBuffer.allocate(respByteArray[0].length * choices.length);
        for (int i = 0; i < choices.length; i++) {
            ansInputByteBuffer.put(respByteArray[i]);
        }
        byte[] answerBytes = kdf.deriveKey(ansInputByteBuffer.array());
        // 检验gamma == H(ans') 是否成立
        byte[] gammaReceiverBytes = kdf.deriveKey(answerBytes);
        MpcAbortPreconditions.checkArgument(Arrays.equals(gammaBytes, gammaReceiverBytes));
        // 将ans'发送给Sender
        List<byte[]> receiverPayload = new ArrayList<>();
        receiverPayload.add(answerBytes);
        return receiverPayload;
    }
}