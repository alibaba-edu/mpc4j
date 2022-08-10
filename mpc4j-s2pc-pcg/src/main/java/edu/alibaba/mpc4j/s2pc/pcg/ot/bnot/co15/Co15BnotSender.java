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
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BnotSenderOutput;

import org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;
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
public class Co15BnotSender extends AbstractBnotSender {
    /**
     * 配置项
     */
    private final Co15BnotConfig config;
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

    public Co15BnotSender(Rpc senderRpc, Party receiverParty, Co15BnotConfig config) {
        super(Co15BnotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        ecc = EccFactory.createInstance(envType);
        this.config = config;
    }

    @Override
    public void init(int n) throws MpcAbortException {
        setInitInput(n);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BnotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        List<byte[]> sPayload = generateSenderPayload();
        DataPacketHeader sDataPacketHeader = new DataPacketHeader(
                taskId, ptoDesc.getPtoId(), Co15BnotPtoDesc.PtoStep.SENDER_SEND_S.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sDataPacketHeader, sPayload));
        stopWatch.stop();
        long sTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), sTime);

        stopWatch.start();
        DataPacketHeader rHeader = new DataPacketHeader(
                taskId, ptoDesc.getPtoId(), Co15BnotPtoDesc.PtoStep.RECEIVER_SEND_R.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> rPayload = rpc.receive(rHeader).getPayload();
        BnotSenderOutput senderOutput = handleReceiverPayload(rPayload);
        stopWatch.stop();
        long rTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), rTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        y = null;
        capitalS = null;
        capitalT = null;
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
        senderPayload.add(ecc.encode(capitalS, config.getCompressEncode()));

        return senderPayload;
    }

    private BnotSenderOutput handleReceiverPayload(List<byte[]> receiverPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(receiverPayload.size() == num);
        Kdf kdf = KdfFactory.createInstance(envType);
        byte[][] rByteArray = receiverPayload.toArray(new byte[0][]);
        // 密钥数组生成流，涉及密码学操作，与输入数量相关，需要并行化处理
        IntStream keyArrayStream = IntStream.range(0, num);
        keyArrayStream = parallel ? keyArrayStream.parallel() : keyArrayStream;
        ECPoint[] capitalRs = keyArrayStream
                .mapToObj(index -> {
                    // 计算K = yR
                    ECPoint capitalR = ecc.decode(rByteArray[index]);
                    return ecc.multiply(capitalR, y);
                }).toArray(ECPoint[]::new);
        ECPoint pointT = capitalT;
        y = null;
        capitalS = null;
        capitalT = null;
        return new Co15BnotSenderOutput(n, num, capitalRs, pointT, envType);
    }
}
