package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.co15;

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
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BnotReceiverOutput;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * CO15-基础n选1-OT协议接收方。
 *
 * @author Hanwen Feng
 * @date 2022/07/25
 */
public class Co15BnotReceiver extends AbstractBnotReceiver {
    /**
     * 配置项
     */
    private final Co15BnotConfig config;
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * 选择比特数组对应的密钥
     */
    private byte[][] rbArray;

    public Co15BnotReceiver(Rpc receiverRpc, Party senderParty, Co15BnotConfig config) {
        super(Co15BnotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        ecc = EccFactory.createInstance(envType);
        this.config = config;
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
        DataPacketHeader sHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Co15BnotPtoDesc.PtoStep.SENDER_SEND_S.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> sPayload = rpc.receive(sHeader).getPayload();
        stopWatch.stop();
        long sTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), sTime);

        stopWatch.start();
        List<byte[]> rPayload = generateReceiverPayload(sPayload);
        DataPacketHeader rHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Co15BnotPtoDesc.PtoStep.RECEIVER_SEND_R.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(rHeader, rPayload));
        stopWatch.stop();
        long rTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), rTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());

        BnotReceiverOutput receiverOutput = new BnotReceiverOutput(n, choices, rbArray);
        rbArray = null;

        return receiverOutput;
    }

    private List<byte[]> generateReceiverPayload(List<byte[]> sPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(sPayload.size() == 1);
        Kdf kdf = KdfFactory.createInstance(envType);
        ECPoint capitalS = ecc.decode(sPayload.remove(0));
        rbArray = new byte[choices.length][];
        // r_i计算流，涉及密码学操作，与输入数量相关，需要并行化处理
        IntStream rIntStream = IntStream.range(0, choices.length);
        rIntStream = this.parallel ? rIntStream.parallel() : rIntStream;
        return rIntStream
                .mapToObj(index -> {
                    // 采样x
                    BigInteger x = ecc.randomZn(secureRandom);
                    // 计算R = cS + xB。
                    ECPoint capitalR = ecc.multiply(ecc.getG(), x).add(ecc.multiply(capitalS, BigInteger.valueOf(choices[index])));
                    // 计算密钥 k = H(index, xS)。
                    byte[] kInputByteArray = ecc.encode(ecc.multiply(capitalS, x), false);
                    rbArray[index] = kdf.deriveKey(ByteBuffer
                            .allocate(Integer.BYTES + kInputByteArray.length)
                            .putInt(index).put(kInputByteArray)
                            .array());
                    return capitalR;
                })
                .map(capitalR -> ecc.encode(capitalR, config.getCompressEncode()))
                .collect(Collectors.toList());
    }
}