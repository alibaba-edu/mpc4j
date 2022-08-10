package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BnotSenderOutput;
import org.apache.commons.lang3.time.StopWatch;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * MR19-基础n选1-OT协议发送方。
 *
 * @author Hanwen Feng
 * @date 2022/07/25
 */
public class Mr19BnotSender extends AbstractBnotSender {
    /**
     * 是否压缩表示
     */
    private final boolean compressEncode;
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * OT协议发送方参数
     */
    private BigInteger bInteger;

    public Mr19BnotSender(Rpc senderRpc, Party receiverParty, Mr19BnotConfig config) {
        super(Mr19BnotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        compressEncode = config.getCompressEncode();
        ecc = EccFactory.createInstance(envType);
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
        List<byte[]> betaPayload = generateBetaPayload();
        DataPacketHeader betaHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Mr19BnotPtoDesc.PtoStep.SENDER_SEND_B.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(betaHeader, betaPayload));
        stopWatch.stop();
        long betaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), betaTime);

        stopWatch.start();
        DataPacketHeader pkHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Mr19BnotPtoDesc.PtoStep.RECEIVER_SEND_R.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> pkPayload = rpc.receive(pkHeader).getPayload();
        BnotSenderOutput senderOutput = handlePkPayload(pkPayload);
        bInteger = null;
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pkTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private List<byte[]> generateBetaPayload() {
        // 发送方选择Z_p^*域中的一个随机数b
        bInteger = ecc.randomZn(secureRandom);
        List<byte[]> betaPayLoad = new ArrayList<>();
        // 发送方计算B = g^b
        betaPayLoad.add(ecc.encode(ecc.multiply(ecc.getG(), bInteger), compressEncode));
        return betaPayLoad;
    }

    private BnotSenderOutput handlePkPayload(List<byte[]> pkPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(pkPayload.size() == num * n);
        Stream<byte[]> pStream = pkPayload.stream();
        pStream = parallel ? pStream.parallel() : pStream;
        ECPoint[] rArray = pStream.map(ecc::decode).toArray(ECPoint[]::new);
        return new Mr19BnotSenderOutput(envType, n, num, bInteger, rArray);
    }
}
