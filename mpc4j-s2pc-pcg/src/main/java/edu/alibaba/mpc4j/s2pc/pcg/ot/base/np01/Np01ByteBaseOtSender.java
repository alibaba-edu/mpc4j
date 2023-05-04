package edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.AbstractBaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01.Np01ByteBaseOtPtoDesc.PtoStep;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * NP01-字节基础OT协议发送方。
 *
 * @author Weiran Liu
 * @date 2023/4/24
 */
public class Np01ByteBaseOtSender extends AbstractBaseOtSender {
    /**
     * byte full ECC
     */
    private final ByteFullEcc byteFullEcc;
    /**
     * C^r
     */
    private byte[] c2r;
    /**
     * r
     */
    private BigInteger r;

    public Np01ByteBaseOtSender(Rpc senderRpc, Party receiverParty, Np01ByteBaseOtConfig config) {
        super(Np01ByteBaseOtPtoDesc.getInstance(), senderRpc, receiverParty, config);
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
        BigInteger exponentC = byteFullEcc.randomZn(secureRandom);
        byte[] c = byteFullEcc.mul(byteFullEcc.getG(), exponentC);
        //（优化协议初始化部分）The sender computes g^r
        r = byteFullEcc.randomZn(secureRandom);
        byte[] g2r = byteFullEcc.mul(byteFullEcc.getG(), r);
        //（优化协议初始化部分）The sender computes C^r.
        c2r = byteFullEcc.mul(c, r);

        List<byte[]> initPayload = new LinkedList<>();
        // 打包g^r、C
        initPayload.add(g2r);
        initPayload.add(c);

        return initPayload;
    }

    private BaseOtSenderOutput handlePkPayload(List<byte[]> pkPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(pkPayload.size() == num);
        byte[][] pk0 = pkPayload.toArray(new byte[0][]);
        byte[][] r0Array = new byte[num][];
        byte[][] r1Array = new byte[num][];
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(index -> {
            // The sender computes PK_0^{r}
            pk0[index] = byteFullEcc.mul(pk0[index], r);
            // The sender computes PK_1^r = C^r / (PK_0^r)
            byte[] pk1 = byteFullEcc.add(c2r, byteFullEcc.neg(pk0[index]));
            // The sender computes H(index, PK_0^r) and H(index, PK_1^r)
            r0Array[index] = kdf.deriveKey(ByteBuffer
                .allocate(Integer.BYTES + pk0[index].length)
                .putInt(index).put(pk0[index])
                .array());
            r1Array[index] = kdf.deriveKey(ByteBuffer
                .allocate(Integer.BYTES + pk1.length)
                .putInt(index).put(pk1)
                .array());
        });
        c2r = null;
        r = null;

        return new BaseOtSenderOutput(r0Array, r1Array);
    }
}
