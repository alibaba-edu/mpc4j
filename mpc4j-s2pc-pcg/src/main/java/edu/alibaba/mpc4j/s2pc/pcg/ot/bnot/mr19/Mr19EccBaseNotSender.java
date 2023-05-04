package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBaseNotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BaseNotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19.Mr19EccBaseNotPtoDesc.PtoStep;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * MR19-椭圆曲线-基础n选1-OT协议发送方。
 *
 * @author Hanwen Feng
 * @date 2022/07/25
 */
public class Mr19EccBaseNotSender extends AbstractBaseNotSender {
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

    public Mr19EccBaseNotSender(Rpc senderRpc, Party receiverParty, Mr19EccBaseNotConfig config) {
        super(Mr19EccBaseNotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        compressEncode = config.getCompressEncode();
        ecc = EccFactory.createInstance(envType);
    }

    @Override
    public void init(int maxChoice) throws MpcAbortException {
        setInitInput(maxChoice);
        logPhaseInfo(PtoState.INIT_BEGIN);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BaseNotSenderOutput send(int num) throws MpcAbortException {
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
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_PK.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> pkPayload = rpc.receive(pkHeader).getPayload();
        BaseNotSenderOutput senderOutput = handlePkPayload(pkPayload);
        beta = null;
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
        // 发送方计算B = g^β
        betaPayLoad.add(ecc.encode(ecc.multiply(ecc.getG(), beta), compressEncode));
        return betaPayLoad;
    }

    private BaseNotSenderOutput handlePkPayload(List<byte[]> pkPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(pkPayload.size() == num * maxChoice);
        Stream<byte[]> pStream = pkPayload.stream();
        pStream = parallel ? pStream.parallel() : pStream;
        ECPoint[] rArray = pStream.map(ecc::decode).toArray(ECPoint[]::new);
        byte[][] rByteArrays = Arrays.stream(rArray).map(r -> r.getEncoded(false)).toArray(byte[][]::new);
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        byte[][][] rMatrix = indexIntStream
            .mapToObj(index -> {
                byte[][] rnArray = new byte[maxChoice][];
                for (int choice = 0; choice < maxChoice; choice++) {
                    // 计算椭圆曲线点总字节长度
                    int pointByteLength = 0;
                    for (int i = 0; i < maxChoice; i++) {
                        if (i != choice) {
                            pointByteLength += rByteArrays[index * maxChoice + i].length;
                        }
                    }
                    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + pointByteLength);
                    buffer.putInt(choice);
                    for (int i = 0; i < maxChoice; i++) {
                        if (i != choice) {
                            buffer.put(rByteArrays[index * maxChoice + i]);
                        }
                    }
                    ECPoint k = ecc.hashToCurve(buffer.array());
                    byte[] kByteArray = ecc.encode(ecc.multiply(rArray[index * maxChoice + choice].add(k), beta), false);
                    rnArray[choice] = ByteBuffer.allocate(Integer.BYTES + kByteArray.length)
                        .putInt(index).put(kByteArray).array();
                    rnArray[choice] = kdf.deriveKey(rnArray[choice]);
                }
                return rnArray;
            })
            .toArray(byte[][][]::new);
        return new BaseNotSenderOutput(maxChoice, rMatrix);
    }
}
