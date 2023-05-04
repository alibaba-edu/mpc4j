package edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.LinkedList;
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
     * 是否压缩表示
     */
    private final boolean compressEncode;
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * C^r
     */
    private ECPoint c2r;
    /**
     * r
     */
    private BigInteger r;

    public Np01BaseOtSender(Rpc senderRpc, Party receiverParty, Np01BaseOtConfig config) {
        super(Np01BaseOtPtoDesc.getInstance(), senderRpc, receiverParty, config);
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
        List<byte[]> initPayload = generateInitPayload();
        DataPacketHeader initHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_INIT.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(initHeader, initPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, initTime);

        stopWatch.start();
        DataPacketHeader pkHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_PK.ordinal(), extraInfo,
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

    private List<byte[]> generateInitPayload() {
        //（基础协议初始化部分）The sender chooses a random element C \in Z_q and publishes it.
        BigInteger exponentC = ecc.randomZn(secureRandom);
        ECPoint c = ecc.multiply(ecc.getG(), exponentC);
        //（优化协议初始化部分）The sender computes g^r
        r = ecc.randomZn(secureRandom);
        ECPoint g2r = ecc.multiply(ecc.getG(), r);
        //（优化协议初始化部分）The sender computes C^r.
        c2r = ecc.multiply(c, r);

        List<byte[]> initPayload = new LinkedList<>();
        // 打包g^r、C
        initPayload.add(ecc.encode(g2r, compressEncode));
        initPayload.add(ecc.encode(c, compressEncode));

        return initPayload;
    }

    private BaseOtSenderOutput handlePkPayload(List<byte[]> pkPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(pkPayload.size() == num);
        // 压缩编码的解码很慢，需要开并发
        Stream<byte[]> pkStream = pkPayload.stream();
        pkStream = parallel ? pkStream.parallel() : pkStream;
        ECPoint[] pk0 = pkStream.map(ecc::decode).toArray(ECPoint[]::new);
        byte[][] r0Array = new byte[num][];
        byte[][] r1Array = new byte[num][];
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(index -> {
            // The sender computes PK_0^{r}
            pk0[index] = ecc.multiply(pk0[index], r);
            // The sender computes PK_1^r = C^r / (PK_0^r)
            ECPoint pk1 = ecc.add(c2r, ecc.negate(pk0[index]));
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
        c2r = null;
        r = null;

        return new BaseOtSenderOutput(r0Array, r1Array);
    }
}
