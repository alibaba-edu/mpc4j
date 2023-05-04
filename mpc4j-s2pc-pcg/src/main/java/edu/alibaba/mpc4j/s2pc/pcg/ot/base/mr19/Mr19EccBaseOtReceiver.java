package edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.AbstractBaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19.Mr19EccBaseOtPtoDesc.PtoStep;
import org.bouncycastle.math.ec.ECPoint;

/**
 * MR19-椭圆曲线-基础OT协议接收方。
 *
 * @author Weiran Liu, Hanwen Feng
 * @date 2020/10/03
 */
public class Mr19EccBaseOtReceiver extends AbstractBaseOtReceiver {
    /**
     * 是否压缩表示
     */
    private final boolean compressEncode;
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * OT协议接收方参数
     */
    private BigInteger[] aArray;

    public Mr19EccBaseOtReceiver(Rpc receiverRpc, Party senderParty, Mr19EccBaseOtConfig config) {
        super(Mr19EccBaseOtPtoDesc.getInstance(), receiverRpc, senderParty, config);
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
    public BaseOtReceiverOutput receive(boolean[] choices) throws MpcAbortException {
        setPtoInput(choices);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> pkPayload = generatePkPayload();
        DataPacketHeader pkHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_R.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(pkHeader, pkPayload));
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, pkTime);

        stopWatch.start();
        DataPacketHeader betaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_BETA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> betaPayload = rpc.receive(betaHeader).getPayload();
        BaseOtReceiverOutput receiverOutput = handleBetaPayload(betaPayload);
        stopWatch.stop();
        long betaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, betaTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private List<byte[]> generatePkPayload() {
        aArray = new BigInteger[choices.length];
        // 公钥生成流
        IntStream pkIntStream = IntStream.range(0, choices.length);
        pkIntStream = parallel ? pkIntStream.parallel() : pkIntStream;
        return pkIntStream
            .mapToObj(index -> {
                // 生成一个随机的元素a
                aArray[index] = ecc.randomZn(secureRandom);
                // 设置两个参数R的乘积A
                ECPoint upperA = ecc.multiply(ecc.getG(), aArray[index]);
                // 生成一个随机的参数R_1-sigma
                ECPoint pkOneMinusSigma = ecc.randomPoint(secureRandom);
                // 计算另一个参数R_sigma
                ECPoint pkSigma = upperA.add(ecc.hashToCurve(ecc.encode(pkOneMinusSigma, false)).negate());
                // 根据选择值将两个参数R分别放入对应位置
                int sigma = choices[index] ? 1 : 0;
                ECPoint[] pkPair = new ECPoint[2];
                pkPair[sigma] = pkSigma;
                pkPair[1 - sigma] = pkOneMinusSigma;
                return pkPair;
            })
            .flatMap(Arrays::stream)
            .map(pk -> ecc.encode(pk, compressEncode))
            .collect(Collectors.toList());
    }

    private BaseOtReceiverOutput handleBetaPayload(List<byte[]> betaPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(betaPayload.size() == 1);
        ECPoint senderBeta = ecc.decode(betaPayload.get(0));
        byte[][] rbArray = new byte[choices.length][];
        IntStream pkIntStream = IntStream.range(0, choices.length);
        pkIntStream = this.parallel ? pkIntStream.parallel() : pkIntStream;
        pkIntStream.forEach(index -> {
                // 计算密钥 k = H(index, aB)。
                byte[] kInputByteArray = ecc.encode(ecc.multiply(senderBeta, aArray[index]), false);
                rbArray[index] = kdf.deriveKey(ByteBuffer
                    .allocate(Integer.BYTES + kInputByteArray.length)
                    .putInt(index).put(kInputByteArray)
                    .array());
            }
        );
        aArray = null;

        return new BaseOtReceiverOutput(choices, rbArray);
    }
}
