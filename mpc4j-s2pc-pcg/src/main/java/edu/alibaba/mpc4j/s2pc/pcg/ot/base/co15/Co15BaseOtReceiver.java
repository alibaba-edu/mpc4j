package edu.alibaba.mpc4j.s2pc.pcg.ot.base.co15;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.AbstractBaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.co15.Co15BaseOtPtoDesc.PtoStep;
import org.bouncycastle.math.ec.ECPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CO15-基础OT协议接收方。
 *
 * @author Weiran Liu, Hanwen Feng
 * @date 2020/06/04
 */
public class Co15BaseOtReceiver extends AbstractBaseOtReceiver {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwoPartyPto.class);
    /**
     * 配置项
     */
    private final Co15BaseOtConfig config;
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * 选择比特数组对应的密钥
     */
    private byte[][] rbArray;

    public Co15BaseOtReceiver(Rpc receiverRpc, Party senderParty, Co15BaseOtConfig config) {
        super(Co15BaseOtPtoDesc.getInstance(), receiverRpc, senderParty, config);
        ecc = EccFactory.createInstance(envType);
        this.config = config;
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        LOGGER.info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        initialized = true;
        LOGGER.info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BaseOtReceiverOutput receive(boolean[] choices) throws MpcAbortException {
        setPtoInput(choices);
        LOGGER.info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        DataPacketHeader sHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_S.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> sPayload = rpc.receive(sHeader).getPayload();
        stopWatch.stop();
        long sTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        LOGGER.info("{}{} Recv. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), sTime);

        stopWatch.start();
        List<byte[]> rPayload = generateReceiverPayload(sPayload);
        DataPacketHeader rHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_R.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(rHeader, rPayload));
        stopWatch.stop();
        long rTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        LOGGER.info("{}{} Recv. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), rTime);

        LOGGER.info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());

        BaseOtReceiverOutput receiverOutput = new BaseOtReceiverOutput(choices, rbArray);
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
                    // 如果c = 1，则R = S + xB；如果c = 0，则R = xB。
                    ECPoint capitalR = choices[index] ? ecc.multiply(ecc.getG(), x).add(capitalS) : ecc.multiply(ecc.getG(), x);
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