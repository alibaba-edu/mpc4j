package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.co15;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBaseNotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BaseNotSenderOutput;

import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * CO15-基础n选1-OT协议发送方。
 *
 * @author Hanwen Feng
 * @date 2022/07/25
 */
public class Co15BaseNotSender extends AbstractBaseNotSender {
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

    public Co15BaseNotSender(Rpc senderRpc, Party receiverParty, Co15BaseNotConfig config) {
        super(Co15BaseNotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        compressEncode = config.getCompressEncode();
        ecc = EccFactory.createInstance(envType);
    }

    @Override
    public void init(int maxChoice) throws MpcAbortException {
        setInitInput(maxChoice);
        logPhaseInfo(PtoState.INIT_BEGIN);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BaseNotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> sPayload = generateSenderPayload();
        DataPacketHeader sDataPacketHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), Co15BaseNotPtoDesc.PtoStep.SENDER_SEND_S.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sDataPacketHeader, sPayload));
        stopWatch.stop();
        long sTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, sTime);

        stopWatch.start();
        DataPacketHeader rHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), Co15BaseNotPtoDesc.PtoStep.RECEIVER_SEND_R.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> rPayload = rpc.receive(rHeader).getPayload();
        BaseNotSenderOutput senderOutput = handleReceiverPayload(rPayload);
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

    private BaseNotSenderOutput handleReceiverPayload(List<byte[]> receiverPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(receiverPayload.size() == num);
        byte[][] rByteArray = receiverPayload.toArray(new byte[0][]);
        // 密钥数组生成流，涉及密码学操作，与输入数量相关，需要并行化处理
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        ECPoint[] rs = indexIntStream
            .mapToObj(index -> {
                // 计算K = yR
                ECPoint capitalR = ecc.decode(rByteArray[index]);
                return ecc.multiply(capitalR, y);
            }).toArray(ECPoint[]::new);
        IntStream choiceIntStream = IntStream.range(0, maxChoice);
        choiceIntStream = parallel ? choiceIntStream.parallel() : choiceIntStream;
        ECPoint[] choiceTs = choiceIntStream
            .mapToObj(choice -> ecc.multiply(capitalT, BigInteger.valueOf(choice)))
            .toArray(ECPoint[]::new);
        indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        byte[][][] rMatrix = indexIntStream
            .mapToObj(index -> {
                byte[][] rnArray = new byte[maxChoice][];
                for (int choice = 0; choice < maxChoice; choice++) {
                    byte[] kByteArray = ecc.encode(rs[index].subtract(choiceTs[choice]), false);
                    rnArray[choice] = ByteBuffer.allocate(Integer.BYTES + kByteArray.length)
                        .putInt(index).put(kByteArray).array();
                    rnArray[choice] = kdf.deriveKey(rnArray[choice]);
                }
                return rnArray;
            })
            .toArray(byte[][][]::new);
        y = null;
        capitalS = null;
        capitalT = null;
        return new BaseNotSenderOutput(maxChoice, rMatrix);
    }
}
