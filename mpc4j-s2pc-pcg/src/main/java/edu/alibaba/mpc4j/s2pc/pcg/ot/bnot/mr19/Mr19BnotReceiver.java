package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BnotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19.Mr19BnotPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBnotReceiver;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * MR19-基础N选1-OT协议接收方。
 *
 * @author Hanwen Feng
 * @date 2022/07/26
 */
public class Mr19BnotReceiver extends AbstractBnotReceiver {
    /**
     * 是否压缩表示
     */
    private final boolean compressEncode;
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * 椭圆曲线点长度
     */
    private final int ecPointByteLength;
    /**
     * OT协议接收方参数
     */
    private BigInteger[] aArray;

    public Mr19BnotReceiver(Rpc receiverRpc, Party senderParty, Mr19BnotConfig config) {
        super(Mr19BnotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        compressEncode = config.getCompressEncode();
        ecc = EccFactory.createInstance(envType);
        ecPointByteLength = ecc.getG().getEncoded(false).length;
    }

    @Override
    public void init(int n) throws MpcAbortException {
        setInitInput(n);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BnotReceiverOutput receive(int[] choices) throws MpcAbortException {
        setPtoInput(choices);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        List<byte[]> pkPayload = generatePkPayload();
        DataPacketHeader pkHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_R.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(pkHeader, pkPayload));
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pkTime);

        stopWatch.start();
        DataPacketHeader betaHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_B.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> betaPayload = rpc.receive(betaHeader).getPayload();
        BnotReceiverOutput receiverOutput = handleBetaPayload(betaPayload);
        stopWatch.stop();
        long betaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), betaTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
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
                ECPoint[] rPoints = new ECPoint[n];
                ByteBuffer hashBuffer = ByteBuffer.allocate(Integer.BYTES + ecPointByteLength * (n - 1));
                hashBuffer.putInt(choices[index]);
                for (int i = 0; i < n; i++) {
                    if (i != choices[index]) {
                        rPoints[i] = ecc.randomPoint(secureRandom);
                        hashBuffer.put(rPoints[i].getEncoded(false));
                    }
                }
                rPoints[choices[index]] = upperA.add(ecc.hashToCurve(hashBuffer.array()).negate());
                return rPoints;
            })
            .flatMap(Arrays::stream)
            .map(pk -> ecc.encode(pk, compressEncode))
            .collect(Collectors.toList());
    }

    private BnotReceiverOutput handleBetaPayload(List<byte[]> betaPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(betaPayload.size() == 1);
        Kdf kdf = KdfFactory.createInstance(envType);
        ECPoint senderBeta = ecc.decode(betaPayload.get(0));
        byte[][] rbArray = new byte[choices.length][];
        IntStream pkIntStream = IntStream.range(0, choices.length);
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

        return new BnotReceiverOutput(n, choices, rbArray);
    }
}
