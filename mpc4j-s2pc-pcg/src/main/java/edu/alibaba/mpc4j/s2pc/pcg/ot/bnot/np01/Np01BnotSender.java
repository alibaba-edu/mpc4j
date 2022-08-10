package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np01;

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
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * NP01-基础n选1-OT协议发送方。
 *
 * @author Hanwen Feng
 * @date 2022/07/26
 */
public class Np01BnotSender extends AbstractBnotSender {
    /**
     * 是否压缩表示
     */
    private final boolean compressEncode;
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * 群元素数组[rC_0, rC_1,...,rC_n]
     */
    private ECPoint[] upperC2rArray;
    /**
     * r
     */
    private BigInteger r;

    public Np01BnotSender(Rpc senderRpc, Party receiverParty, Np01BnotConfig config) {
        super(Np01BnotPtoDesc.getInstance(), senderRpc, receiverParty, config);
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
        List<byte[]> initPayload = generateSenderPayload();
        DataPacketHeader initHeader = new DataPacketHeader(
                taskId, ptoDesc.getPtoId(), Np01BnotPtoDesc.PtoStep.SENDER_SEND_INIT.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(initHeader, initPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        stopWatch.start();
        DataPacketHeader publicKeyHeader = new DataPacketHeader(
                taskId, ptoDesc.getPtoId(), Np01BnotPtoDesc.PtoStep.RECEIVER_SEND_PK.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> publicKeyPayload = rpc.receive(publicKeyHeader).getPayload();
        BnotSenderOutput senderOutput = handleReceiverPayload(publicKeyPayload);
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyGenTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private List<byte[]> generateSenderPayload() {
        //（优化协议初始化部分）The sender computes g^r
        r = ecc.randomZn(secureRandom);
        ECPoint g2r = ecc.multiply(ecc.getG(), r);
        // The sender chooses random group elements {C_1, ..., C_n} and publishes them. Also Compute {rC_1, ..., rC_n}
        byte[][] upperCArray = new byte[n - 1][];
        IntStream initStream = IntStream.range(0, n - 1);
        initStream = parallel ? initStream.parallel() : initStream;
        upperC2rArray = initStream.mapToObj(index -> {
            ECPoint upperC = ecc.multiply(ecc.getG(), ecc.randomZn(secureRandom));
            upperCArray[index] = ecc.encode(upperC, compressEncode);
            return ecc.multiply(upperC, r);
        }).toArray(ECPoint[]::new);

        List<byte[]> senderPayload = new LinkedList<>();
        // 打包g^r、{C_1, ..., C_{n-1}}
        senderPayload.add(ecc.encode(g2r, compressEncode));
        for (int i = 0; i < n - 1; i++) {
            senderPayload.add(upperCArray[i]);
        }
        return senderPayload;
    }

    private BnotSenderOutput handleReceiverPayload(List<byte[]> receiverPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(receiverPayload.size() == num);
        // 公钥格式转换流
        Stream<byte[]> pkStream = receiverPayload.stream();
        pkStream = parallel ? pkStream.parallel() : pkStream;
        ECPoint[] pk2rArray = pkStream
                // 转换为ECPoint
                .map(ecc::decode)
                // 转换pk^r
                .map(pk -> ecc.multiply(pk, r))
                // 转换为ECPoint[]
                .toArray(ECPoint[]::new);
        Np01BnotSenderOutput senderOutput = new Np01BnotSenderOutput(envType, n, num, upperC2rArray, pk2rArray);
        r = null;
        upperC2rArray = null;
        return senderOutput;
    }
}
