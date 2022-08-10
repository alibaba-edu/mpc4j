package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np01;

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
import java.util.stream.Stream;

/**
 * NP01-基础n选1-OT协议接收方。
 *
 * @author Hanwen Feng
 * @date 2022/07/26
 */
public class Np01BnotReceiver extends AbstractBnotReceiver {
    /**
     * 是否压缩编码
     */
    private final boolean compressEncode;
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * C
     */
    private ECPoint[] upperCs;
    /**
     * g^r
     */
    private ECPoint g2r;
    /**
     * 选择比特值的消息
     */
    private byte[][] rbArray;

    public Np01BnotReceiver(Rpc receiverRpc, Party senderParty, Np01BnotConfig config) {
        super(Np01BnotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        compressEncode = config.getCompressEncode();
        ecc = EccFactory.createInstance(envType);
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
        DataPacketHeader initHeader = new DataPacketHeader(
            taskId, ptoDesc.getPtoId(), Np01BnotPtoDesc.PtoStep.SENDER_SEND_INIT.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> initPayload = rpc.receive(initHeader).getPayload();
        handleSenderPayload(initPayload);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        stopWatch.start();
        List<byte[]> publicKeyPayload = generateReceiverPayload();
        DataPacketHeader publicKeyHeader = new DataPacketHeader(
            taskId, ptoDesc.getPtoId(), Np01BnotPtoDesc.PtoStep.RECEIVER_SEND_PK.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(publicKeyHeader, publicKeyPayload));
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pkTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        BnotReceiverOutput receiverOutput = new BnotReceiverOutput(n, choices, rbArray);
        rbArray = null;

        return receiverOutput;
    }

    private void handleSenderPayload(List<byte[]> senderPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(senderPayload.size() == n);
        // 解包g^r、C
        g2r = ecc.decode(senderPayload.remove(0));
        Stream<byte[]> cStream = senderPayload.stream();
        cStream = parallel ? cStream.parallel() : cStream;
        upperCs = cStream
            .map(ecc::decode)
            .toArray(ECPoint[]::new);
    }

    private List<byte[]> generateReceiverPayload() {
        Kdf kdf = KdfFactory.createInstance(envType);
        rbArray = new byte[choices.length][];
        // 公钥生成流
        IntStream publicKeyIntStream = IntStream.range(0, choices.length);
        publicKeyIntStream = parallel ? publicKeyIntStream.parallel() : publicKeyIntStream;
        List<byte[]> receiverPayload = publicKeyIntStream
            .mapToObj(index -> {
                // The receiver picks a random k
                BigInteger k = ecc.randomZn(secureRandom);
                // The receiver sets public keys PK_{\sigma} = g^k
                ECPoint pkSigma = ecc.multiply(ecc.getG(), k);
                // and PK_{1 - \sigma} = C / PK_{\sigma}
                ECPoint pk0 = (choices[index] == 0) ? pkSigma : (upperCs[choices[index] - 1].subtract(pkSigma));
                // 存储OT的密钥key=H(index,g^rk)
                byte[] kInputByteArray = ecc.encode(ecc.multiply(g2r, k), false);
                rbArray[index] = kdf.deriveKey(ByteBuffer
                    .allocate(Integer.BYTES + kInputByteArray.length)
                    .putInt(index)
                    .put(kInputByteArray)
                    .array());
                // 返回密钥
                return pk0;
            })
            .map(pk -> ecc.encode(pk, compressEncode))
            .collect(Collectors.toList());
        upperCs = null;
        g2r = null;

        return receiverPayload;
    }
}
