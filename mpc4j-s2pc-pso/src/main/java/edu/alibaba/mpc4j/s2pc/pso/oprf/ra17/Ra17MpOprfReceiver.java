package edu.alibaba.mpc4j.s2pc.pso.oprf.ra17;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprf.AbstractMpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pso.oprf.ra17.Ra17MpOprfPtoDesc.PtoStep;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RA17-MPOPRF协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/02/07
 */
public class Ra17MpOprfReceiver extends AbstractMpOprfReceiver {
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * 是否压缩编码
     */
    private final boolean compressEncode;
    /**
     * 伪随机函数输出字节长度
     */
    private final int prfByteLength;
    /**
     * β^{-1}
     */
    private BigInteger[] inverseBetas;

    public Ra17MpOprfReceiver(Rpc receiverRpc, Party senderParty, Ra17MpOprfConfig config) {
        super(Ra17MpOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        ecc = EccFactory.createInstance(envType);
        compressEncode = config.getCompressEncode();
        prfByteLength = ecc.encode(ecc.getG(), false).length;
    }

    @Override
    public void init(int maxBatchSize) throws MpcAbortException {
        setInitInput(maxBatchSize);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public MpOprfReceiverOutput oprf(byte[][] inputs) throws MpcAbortException {
        setPtoInput(inputs);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        List<byte[]> blindPayload = generateBlindPayload();
        DataPacketHeader blindHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_BLIND.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindHeader, blindPayload));
        stopWatch.stop();
        long blindTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), blindTime);

        stopWatch.start();
        DataPacketHeader blindPrfHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_BLIND_PRF.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPrfPayload = rpc.receive(blindPrfHeader).getPayload();
        MpOprfReceiverOutput receiverOutput = handleBlindPrfPayload(blindPrfPayload);
        stopWatch.stop();
        long deBlindTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), deBlindTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }

    private List<byte[]> generateBlindPayload() {
        BigInteger n = ecc.getN();
        inverseBetas = new BigInteger[batchSize];
        IntStream batchIntStream = IntStream.range(0, batchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;

        return batchIntStream
            .mapToObj(index -> {
                // 生成盲化因子
                BigInteger beta = ecc.randomZn(secureRandom);
                inverseBetas[index] = beta.modInverse(n);
                // hash to point
                ECPoint element = ecc.hashToCurve(inputs[index]);
                // 盲化
                return ecc.multiply(element, beta);
            })
            .map(element -> ecc.encode(element, compressEncode))
            .collect(Collectors.toList());
    }

    private MpOprfReceiverOutput handleBlindPrfPayload(List<byte[]> blindPrfPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(blindPrfPayload.size() == batchSize);
        byte[][] blindPrfArray = blindPrfPayload.toArray(new byte[0][]);
        IntStream batchIntStream = IntStream.range(0, batchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        byte[][] prfs = batchIntStream
            .mapToObj(index -> {
                // 解码
                ECPoint element = ecc.decode(blindPrfArray[index]);
                // 去盲化
                return ecc.multiply(element, inverseBetas[index]);
            })
            .map(element -> ecc.encode(element, false))
            .toArray(byte[][]::new);
        return new MpOprfReceiverOutput(prfByteLength, inputs, prfs);
    }
}
