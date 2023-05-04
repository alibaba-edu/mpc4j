package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np01;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBaseNotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BaseNotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np01.Np01BaseNotPtoDesc.PtoStep;
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
public class Np01BaseNotReceiver extends AbstractBaseNotReceiver {
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
    private ECPoint[] cs;
    /**
     * g^r
     */
    private ECPoint g2r;
    /**
     * 选择比特值的消息
     */
    private byte[][] rbArray;

    public Np01BaseNotReceiver(Rpc receiverRpc, Party senderParty, Np01BaseNotConfig config) {
        super(Np01BaseNotPtoDesc.getInstance(), receiverRpc, senderParty, config);
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
    public BaseNotReceiverOutput receive(int[] choices) throws MpcAbortException {
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
        BaseNotReceiverOutput receiverOutput = new BaseNotReceiverOutput(maxChoice, choices, rbArray);
        rbArray = null;
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, pkTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private void handleInitPayload(List<byte[]> initPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(initPayload.size() == maxChoice);
        // 解包g^r、C
        g2r = ecc.decode(initPayload.remove(0));
        Stream<byte[]> cStream = initPayload.stream();
        cStream = parallel ? cStream.parallel() : cStream;
        cs = cStream.map(ecc::decode).toArray(ECPoint[]::new);
    }

    private List<byte[]> generatePkPayload() {
        rbArray = new byte[num][];
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        List<byte[]> pkPayload = indexIntStream
            .mapToObj(index -> {
                // The receiver picks a random k
                BigInteger k = ecc.randomZn(secureRandom);
                // The receiver sets public keys PK_{\sigma} = g^k
                ECPoint pkSigma = ecc.multiply(ecc.getG(), k);
                // and PK_{1 - \sigma} = C / PK_{\sigma}
                ECPoint pk0 = (choices[index] == 0) ? pkSigma : (cs[choices[index] - 1].subtract(pkSigma));
                // 存储OT的密钥key = H(index, g^rk)
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
        cs = null;
        g2r = null;
        return pkPayload;
    }
}
