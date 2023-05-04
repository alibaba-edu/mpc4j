package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BaseNotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19.Mr19EccBaseNotPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBaseNotReceiver;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * MR19-椭圆曲线-基础n选1-OT协议接收方。
 *
 * @author Hanwen Feng
 * @date 2022/07/26
 */
public class Mr19EccBaseNotReceiver extends AbstractBaseNotReceiver {
    /**
     * 是否压缩表示
     */
    private final boolean compressEncode;
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * OT协议接收方参数
     */
    private BigInteger[] aArray;

    public Mr19EccBaseNotReceiver(Rpc receiverRpc, Party senderParty, Mr19EccBaseNotConfig config) {
        super(Mr19EccBaseNotPtoDesc.getInstance(), receiverRpc, senderParty, config);
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
    public BaseNotReceiverOutput receive(int[] choices) throws MpcAbortException {
        setPtoInput(choices);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> pkPayload = generatePkPayload();
        DataPacketHeader pkHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_PK.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(pkHeader, pkPayload));
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, pkTime);

        stopWatch.start();
        DataPacketHeader betaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_BETA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> betaPayload = rpc.receive(betaHeader).getPayload();
        BaseNotReceiverOutput receiverOutput = handleBetaPayload(betaPayload);
        stopWatch.stop();
        long betaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, betaTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private List<byte[]> generatePkPayload() {
        aArray = new BigInteger[choices.length];
        // 公钥生成流
        IntStream pkIntStream = IntStream.range(0, choices.length);
        pkIntStream = parallel ? pkIntStream.parallel() : pkIntStream;
        return pkIntStream
            .mapToObj(index -> {
                // 生成一个随机的元素a
                aArray[index] = ecc.randomZn(secureRandom);
                // 设置乘积A
                ECPoint upperA = ecc.multiply(ecc.getG(), aArray[index]);
                // 生成n个参数r
                ECPoint[] rArray = new ECPoint[maxChoice];
                byte[][] rByteArrays = new byte[maxChoice][];
                // 计算椭圆曲线点总字节长度
                int pointByteLength = 0;
                for (int i = 0; i < maxChoice; i++) {
                    if (i != choices[index]) {
                        rArray[i] = ecc.randomPoint(secureRandom);
                        rByteArrays[i] = rArray[i].getEncoded(false);
                        pointByteLength += rByteArrays[i].length;
                    }
                }
                ByteBuffer hashBuffer = ByteBuffer.allocate(Integer.BYTES + pointByteLength);
                hashBuffer.putInt(choices[index]);
                for (int i = 0; i < maxChoice; i++) {
                    if (i != choices[index]) {
                        hashBuffer.put(rByteArrays[i]);
                    }
                }
                rArray[choices[index]] = upperA.add(ecc.hashToCurve(hashBuffer.array()).negate());
                return rArray;
            })
            .flatMap(Arrays::stream)
            .map(pk -> ecc.encode(pk, compressEncode))
            .collect(Collectors.toList());
    }

    private BaseNotReceiverOutput handleBetaPayload(List<byte[]> betaPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(betaPayload.size() == 1);
        ECPoint senderBeta = ecc.decode(betaPayload.get(0));
        byte[][] rbArray = new byte[num][];
        IntStream pkIntStream = IntStream.range(0, num);
        pkIntStream = this.parallel ? pkIntStream.parallel() : pkIntStream;
        pkIntStream.forEach(index -> {
                // 计算密钥 k = H(index, aB)。
                byte[] kInputByteArray = ecc.encode(ecc.multiply(senderBeta, aArray[index]), false);
                rbArray[index] = kdf.deriveKey(ByteBuffer
                    .allocate(Integer.BYTES + kInputByteArray.length)
                    .putInt(index)
                    .put(kInputByteArray)
                    .array());
            }
        );
        aArray = null;
        return new BaseNotReceiverOutput(maxChoice, choices, rbArray);
    }
}
