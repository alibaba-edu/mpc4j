package edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.AbstractBaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01.Np01ByteBaseOtPtoDesc.PtoStep;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * NP01-字节基础OT协议接收方。
 *
 * @author Weiran Liu, Hanwen Feng
 * @date 2019/06/17
 */
public class Np01ByteBaseOtReceiver extends AbstractBaseOtReceiver {
    /**
     * byte full ECC
     */
    private final ByteFullEcc byteFullEcc;
    /**
     * C
     */
    private byte[] c;
    /**
     * g^r
     */
    private byte[] g2r;
    /**
     * random choice message
     */
    private byte[][] rbArray;

    public Np01ByteBaseOtReceiver(Rpc receiverRpc, Party senderParty, Np01ByteBaseOtConfig config) {
        super(Np01ByteBaseOtPtoDesc.getInstance(), receiverRpc, senderParty, config);
        byteFullEcc = ByteEccFactory.createFullInstance(envType);
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
        g2r = initPayload.remove(0);
        c = initPayload.remove(0);
    }

    private List<byte[]> generatePkPayload() {
        rbArray = new byte[num][];
        // 公钥生成流
        IntStream publicKeyIntStream = IntStream.range(0, num);
        publicKeyIntStream = parallel ? publicKeyIntStream.parallel() : publicKeyIntStream;
        List<byte[]> receiverPayload = publicKeyIntStream
            .mapToObj(index -> {
                // The receiver picks a random k
                BigInteger k = byteFullEcc.randomZn(secureRandom);
                // The receiver sets public keys PK_{\sigma} = g^k
                byte[] pkSigma = byteFullEcc.mul(byteFullEcc.getG(), k);
                // and PK_{1 - \sigma} = C / PK_{\sigma}
                byte[] pkOneMinusSigma = byteFullEcc.add(c, byteFullEcc.neg(pkSigma));
                // 存储OT的密钥key=H(index,g^rk)
                byte[] kInputByteArray = byteFullEcc.mul(g2r, k);
                rbArray[index] = kdf.deriveKey(ByteBuffer
                    .allocate(Integer.BYTES + kInputByteArray.length)
                    .putInt(index).put(kInputByteArray)
                    .array());
                // 返回密钥
                return choices[index] ? pkOneMinusSigma : pkSigma;
            })
            .collect(Collectors.toList());
        c = null;
        g2r = null;

        return receiverPayload;
    }
}
