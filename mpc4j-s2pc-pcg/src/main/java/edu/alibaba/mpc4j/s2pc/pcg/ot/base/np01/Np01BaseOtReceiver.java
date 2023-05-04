package edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01;

import java.math.BigInteger;
import java.nio.ByteBuffer;
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
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01.Np01BaseOtPtoDesc.PtoStep;
import org.bouncycastle.math.ec.ECPoint;

/**
 * NP01-基础OT协议接收方。
 *
 * @author Weiran Liu, Hanwen Feng
 * @date 2019/06/17
 */
public class Np01BaseOtReceiver extends AbstractBaseOtReceiver {
    /**
     * 是否压缩表示
     */
    private final boolean compressEncode;
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * C
     */
    private ECPoint c;
    /**
     * g^r
     */
    private ECPoint g2r;
    /**
     * 选择比特对应的消息
     */
    private byte[][] rbArray;

    public Np01BaseOtReceiver(Rpc receiverRpc, Party senderParty, Np01BaseOtConfig config) {
        super(Np01BaseOtPtoDesc.getInstance(), receiverRpc, senderParty, config);
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
        DataPacketHeader initHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_INIT.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> initPayload = rpc.receive(initHeader).getPayload();
        handleInitPayload(initPayload);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, initTime);

        stopWatch.start();
        List<byte[]> pkPayload = generatePkPayload();
        DataPacketHeader pkHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_PK.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(pkHeader, pkPayload));
        BaseOtReceiverOutput receiverOutput = new BaseOtReceiverOutput(choices, rbArray);
        rbArray = null;
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, pkTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private void handleInitPayload(List<byte[]> initPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(initPayload.size() == 2);
        // 解包g^r、C
        g2r = ecc.decode(initPayload.remove(0));
        c = ecc.decode(initPayload.remove(0));
    }

    private List<byte[]> generatePkPayload() {
        rbArray = new byte[num][];
        // 公钥生成流
        IntStream publicKeyIntStream = IntStream.range(0, num);
        publicKeyIntStream = parallel ? publicKeyIntStream.parallel() : publicKeyIntStream;
        List<byte[]> receiverPayload = publicKeyIntStream
            .mapToObj(index -> {
                // The receiver picks a random k
                BigInteger k = ecc.randomZn(secureRandom);
                // The receiver sets public keys PK_{\sigma} = g^k
                ECPoint pkSigma = ecc.multiply(ecc.getG(), k);
                // and PK_{1 - \sigma} = C / PK_{\sigma}
                ECPoint pkOneMinusSigma = ecc.add(c, ecc.negate(pkSigma));
                // 存储OT的密钥key=H(index,g^rk)
                byte[] kInputByteArray = ecc.encode(ecc.multiply(g2r, k), false);
                rbArray[index] = kdf.deriveKey(ByteBuffer
                    .allocate(Integer.BYTES + kInputByteArray.length)
                    .putInt(index).put(kInputByteArray)
                    .array());
                // 返回密钥
                return choices[index] ? pkOneMinusSigma : pkSigma;
            })
            .map(pk -> ecc.encode(pk, compressEncode))
            .collect(Collectors.toList());
        c = null;
        g2r = null;

        return receiverPayload;
    }
}
