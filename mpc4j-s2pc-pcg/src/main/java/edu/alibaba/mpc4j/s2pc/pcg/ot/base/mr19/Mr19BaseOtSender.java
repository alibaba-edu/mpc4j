package edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
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
import org.bouncycastle.math.ec.ECPoint;

/**
 * MR19-基础OT协议发送方。
 *
 * @author Weiran Liu, Hanwen Feng
 * @date 2020/10/03
 */
public class Mr19BaseOtSender extends AbstractBaseOtSender {
    /**
     * 配置项
     */
    private final Mr19BaseOtConfig config;
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * OT协议发送方参数
     */
    private BigInteger bInteger;

    public Mr19BaseOtSender(Rpc senderRpc, Party receiverParty, Mr19BaseOtConfig config) {
        super(Mr19BaseOtPtoDesc.getInstance(), senderRpc, receiverParty, config);
        ecc = EccFactory.createInstance(envType);
        this.config = config;
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
        List<byte[]> betaPayload = generateBetaPayload();
        DataPacketHeader betaHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Mr19BaseOtPtoDesc.PtoStep.SENDER_SEND_B.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(betaHeader, betaPayload));
        stopWatch.stop();
        long betaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), betaTime);

        stopWatch.start();
        DataPacketHeader pkHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Mr19BaseOtPtoDesc.PtoStep.RECEIVER_SEND_R.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> pkPayload = rpc.receive(pkHeader).getPayload();
        BaseOtSenderOutput senderOutput = handlePkPayload(pkPayload);
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
        betaPayLoad.add(ecc.encode(ecc.multiply(ecc.getG(), bInteger), config.getCompressEncode()));
        return betaPayLoad;
    }

    private BaseOtSenderOutput handlePkPayload(List<byte[]> pkPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(pkPayload.size() == num * 2);
        Kdf kdf = KdfFactory.createInstance(envType);
        ECPoint[] rFlattenedArray = pkPayload.stream()
                .map(ecc::decode)
                .toArray(ECPoint[]::new);
        // 密钥对生成流
        IntStream keyPairArrayIntStream = IntStream.range(0, num);
        keyPairArrayIntStream = parallel ? keyPairArrayIntStream.parallel() : keyPairArrayIntStream;
        byte[][] r0Array = new byte[num][];
        byte[][] r1Array = new byte[num][];
        keyPairArrayIntStream.forEach(index -> {
            // 读取接收端参数对R0、R1
            ECPoint upperR0 = rFlattenedArray[index * 2];
            ECPoint upperR1 = rFlattenedArray[index * 2 + 1];
            // 计算A0 = Hash(R0) * R0、A1 = Hash(R0) * R1
            ECPoint upperA0 = ecc.hashToCurve(ecc.encode(upperR1, false)).add(upperR0);
            ECPoint upperA1 = ecc.hashToCurve(ecc.encode(upperR0, false)).add(upperR1);
            // 计算密钥k0 = H(index, b * A0)和k1 = H(index, b * A1)
            byte[] k0InputByteArray = ecc.encode(ecc.multiply(upperA0, bInteger), false);
            byte[] k1InputByteArray = ecc.encode(ecc.multiply(upperA1, bInteger), false);
            r0Array[index] = kdf.deriveKey(ByteBuffer
                    .allocate(Integer.BYTES + k0InputByteArray.length)
                    .putInt(index).put(k0InputByteArray)
                    .array());
            r1Array[index] = kdf.deriveKey(ByteBuffer
                    .allocate(Integer.BYTES + k1InputByteArray.length)
                    .putInt(index).put(k1InputByteArray)
                    .array());
        });
        bInteger = null;
        return new BaseOtSenderOutput(r0Array, r1Array);
    }
}
