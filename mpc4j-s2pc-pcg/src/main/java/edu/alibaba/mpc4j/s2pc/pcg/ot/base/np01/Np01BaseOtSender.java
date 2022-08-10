package edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

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
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.AbstractBaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01.Np01BaseOtPtoDesc.PtoStep;
import org.bouncycastle.math.ec.ECPoint;

/**
 * NP01-基础OT协议发送方。
 *
 * @author Weiran Liu, Hanwen Feng
 * @date 2019/06/18
 */
public class Np01BaseOtSender extends AbstractBaseOtSender {
    /**
     * 配置项
     */
    private final Np01BaseOtConfig config;
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * C^r
     */
    private ECPoint upperC2r;
    /**
     * r
     */
    private BigInteger r;

    public Np01BaseOtSender(Rpc senderRpc, Party receiverParty, Np01BaseOtConfig config) {
        super(Np01BaseOtPtoDesc.getInstance(), senderRpc, receiverParty, config);
        this.config = config;
        ecc = EccFactory.createInstance(envType);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BaseOtSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        List<byte[]> initPayload = generateSenderPayload();
        DataPacketHeader initHeader = new DataPacketHeader(
                taskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_INIT.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(initHeader, initPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        stopWatch.start();
        DataPacketHeader publicKeyHeader = new DataPacketHeader(
                taskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_PK.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> publicKeyPayload = rpc.receive(publicKeyHeader).getPayload();
        BaseOtSenderOutput senderOutput = handleReceiverPayload(publicKeyPayload);
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyGenTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private List<byte[]> generateSenderPayload() {
        //（基础协议初始化部分）The sender chooses a random element C \in Z_q and publishes it.
        BigInteger exponentC = ecc.randomZn(secureRandom);
        ECPoint upperC = ecc.multiply(ecc.getG(), exponentC);
        //（优化协议初始化部分）The sender computes g^r
        r = ecc.randomZn(secureRandom);
        ECPoint g2r = ecc.multiply(ecc.getG(), r);
        //（优化协议初始化部分）The sender computes C^r.
        upperC2r = ecc.multiply(upperC, r);

        List<byte[]> senderPayload = new LinkedList<>();
        // 打包g^r、C
        senderPayload.add(ecc.encode(g2r, config.getCompressEncode()));
        senderPayload.add(ecc.encode(upperC, config.getCompressEncode()));

        return senderPayload;
    }

    private BaseOtSenderOutput handleReceiverPayload(List<byte[]> receiverPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(receiverPayload.size() == num);
        Kdf kdf = KdfFactory.createInstance(envType);
        // 公钥格式转换流
        ECPoint[] pk0 = receiverPayload.stream()
                // 转换为ECPoint
                .map(ecc::decode)
                // 转换为ECPoint[]
                .toArray(ECPoint[]::new);
        byte[][] r0Array = new byte[num][];
        byte[][] r1Array = new byte[num][];
        IntStream keyPairArrayStream = IntStream.range(0, num);
        keyPairArrayStream = parallel ? keyPairArrayStream.parallel() : keyPairArrayStream;
        keyPairArrayStream.forEach(index -> {
            // The sender computes PK_0^{r}
            pk0[index] = ecc.multiply(pk0[index], r);
            // The sender computes PK_1^r = C^r / (PK_0^r)
            ECPoint pk1 = upperC2r.add(pk0[index].negate());
            // The sender computes H(index, PK_0^r) and H(index, PK_1^r)
            byte[] k0InputByteArray = ecc.encode(pk0[index], false);
            r0Array[index] = kdf.deriveKey(ByteBuffer
                    .allocate(Integer.BYTES + k0InputByteArray.length)
                    .putInt(index).put(k0InputByteArray)
                    .array());
            byte[] k1InputByteArray = ecc.encode(pk1, false);
            r1Array[index] = kdf.deriveKey(ByteBuffer
                    .allocate(Integer.BYTES + k1InputByteArray.length)
                    .putInt(index).put(k1InputByteArray)
                    .array());
        });
        upperC2r = null;
        r = null;

        return new BaseOtSenderOutput(r0Array, r1Array);
    }
}
