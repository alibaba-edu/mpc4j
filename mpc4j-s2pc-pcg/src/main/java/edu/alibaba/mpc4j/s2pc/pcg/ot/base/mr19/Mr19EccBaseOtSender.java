package edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.AbstractBaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19.Mr19EccBaseOtPtoDesc.PtoStep;
import org.bouncycastle.math.ec.ECPoint;

/**
 * MR19-椭圆曲线-基础OT协议发送方。
 *
 * @author Weiran Liu, Hanwen Feng
 * @date 2020/10/03
 */
public class Mr19EccBaseOtSender extends AbstractBaseOtSender {
    /**
     * 是否压缩表示
     */
    private final boolean compressEncode;
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * β
     */
    private BigInteger beta;

    public Mr19EccBaseOtSender(Rpc senderRpc, Party receiverParty, Mr19EccBaseOtConfig config) {
        super(Mr19EccBaseOtPtoDesc.getInstance(), senderRpc, receiverParty, config);
        compressEncode = config.getCompressEncode();
        ecc = EccFactory.createInstance(envType);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);
        // empty init step
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BaseOtSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> betaPayload = generateBetaPayload();
        DataPacketHeader betaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_BETA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(betaHeader, betaPayload));
        stopWatch.stop();
        long betaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, betaTime);

        stopWatch.start();
        DataPacketHeader pkHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_R.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> pkPayload = rpc.receive(pkHeader).getPayload();
        BaseOtSenderOutput senderOutput = handlePkPayload(pkPayload);
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, pkTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private List<byte[]> generateBetaPayload() {
        // 发送方选择Z_p^*域中的一个随机数β
        beta = ecc.randomZn(secureRandom);
        List<byte[]> betaPayLoad = new ArrayList<>();
        // 发送方计算B = g^b
        betaPayLoad.add(ecc.encode(ecc.multiply(ecc.getG(), beta), compressEncode));
        return betaPayLoad;
    }

    private BaseOtSenderOutput handlePkPayload(List<byte[]> pkPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(pkPayload.size() == num * 2);
        // 压缩编码的解码很慢，需要开并发
        Stream<byte[]> pkStream = pkPayload.stream();
        pkStream = parallel ? pkStream.parallel() : pkStream;
        ECPoint[] rFlattenedArray = pkStream.map(ecc::decode).toArray(ECPoint[]::new);
        // 密钥对生成流
        IntStream keyPairArrayIntStream = IntStream.range(0, num);
        keyPairArrayIntStream = parallel ? keyPairArrayIntStream.parallel() : keyPairArrayIntStream;
        byte[][] r0Array = new byte[num][];
        byte[][] r1Array = new byte[num][];
        keyPairArrayIntStream.forEach(index -> {
            // 读取接收端参数对R0、R1
            ECPoint upperR0 = rFlattenedArray[index * 2];
            ECPoint upperR1 = rFlattenedArray[index * 2 + 1];
            // 计算A0 = Hash(R1) * R0、A1 = Hash(R0) * R1
            ECPoint upperA0 = ecc.hashToCurve(ecc.encode(upperR1, false)).add(upperR0);
            ECPoint upperA1 = ecc.hashToCurve(ecc.encode(upperR0, false)).add(upperR1);
            // 计算密钥k0 = H(index, b * A0)和k1 = H(index, b * A1)
            byte[] k0InputByteArray = ecc.encode(ecc.multiply(upperA0, beta), false);
            byte[] k1InputByteArray = ecc.encode(ecc.multiply(upperA1, beta), false);
            r0Array[index] = kdf.deriveKey(ByteBuffer
                .allocate(Integer.BYTES + k0InputByteArray.length)
                .putInt(index).put(k0InputByteArray)
                .array());
            r1Array[index] = kdf.deriveKey(ByteBuffer
                .allocate(Integer.BYTES + k1InputByteArray.length)
                .putInt(index).put(k1InputByteArray)
                .array());
        });
        beta = null;
        return new BaseOtSenderOutput(r0Array, r1Array);
    }
}
