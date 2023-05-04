package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np01;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBaseNotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BaseNotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np01.Np01BaseNotPtoDesc.PtoStep;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * NP01-基础n选1-OT协议发送方。
 *
 * @author Hanwen Feng
 * @date 2022/07/26
 */
public class Np01BaseNotSender extends AbstractBaseNotSender {
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
    private ECPoint[] c2rArray;
    /**
     * r
     */
    private BigInteger r;

    public Np01BaseNotSender(Rpc senderRpc, Party receiverParty, Np01BaseNotConfig config) {
        super(Np01BaseNotPtoDesc.getInstance(), senderRpc, receiverParty, config);
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
        DataPacketHeader publicKeyHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_PK.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> publicKeyPayload = rpc.receive(publicKeyHeader).getPayload();
        BaseNotSenderOutput senderOutput = handlePkPayload(publicKeyPayload);
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, pkTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private List<byte[]> generateInitPayload() {
        //（优化协议初始化部分）The sender computes g^r
        r = ecc.randomZn(secureRandom);
        ECPoint g2r = ecc.multiply(ecc.getG(), r);
        // The sender chooses random group elements {C_1, ..., C_n} and publishes them. Also Compute {rC_1, ..., rC_n}
        byte[][] cs = new byte[maxChoice - 1][];
        IntStream initStream = IntStream.range(0, maxChoice - 1);
        initStream = parallel ? initStream.parallel() : initStream;
        c2rArray = initStream.mapToObj(index -> {
            ECPoint c = ecc.multiply(ecc.getG(), ecc.randomZn(secureRandom));
            cs[index] = ecc.encode(c, compressEncode);
            return ecc.multiply(c, r);
        }).toArray(ECPoint[]::new);

        List<byte[]> initPayload = new LinkedList<>();
        // 打包g^r、{C_1, ..., C_{n-1}}
        initPayload.add(ecc.encode(g2r, compressEncode));
        for (int i = 0; i < maxChoice - 1; i++) {
            initPayload.add(cs[i]);
        }
        return initPayload;
    }

    private BaseNotSenderOutput handlePkPayload(List<byte[]> pkPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(pkPayload.size() == num);
        // 压缩编码的解码很慢，需要开并发
        Stream<byte[]> pkStream = pkPayload.stream();
        pkStream = parallel ? pkStream.parallel() : pkStream;
        ECPoint[] pk2rArray = pkStream.map(ecc::decode).map(pk -> ecc.multiply(pk, r)).toArray(ECPoint[]::new);
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        byte[][][] rMatrix = indexIntStream
            .mapToObj(index -> {
                byte[][] rnArray = new byte[maxChoice][];
                for (int choice = 0; choice < maxChoice; choice++) {
                    ECPoint k = (choice == 0) ? pk2rArray[index] : c2rArray[choice - 1].subtract(pk2rArray[index]);
                    byte[] kByteArray = k.getEncoded(false);
                    rnArray[choice] = ByteBuffer.allocate(Integer.BYTES + kByteArray.length)
                        .putInt(index).put(kByteArray).array();
                    rnArray[choice] = kdf.deriveKey(rnArray[choice]);
                }
                return rnArray;
            })
            .toArray(byte[][][]::new);
        BaseNotSenderOutput senderOutput = new BaseNotSenderOutput(maxChoice, rMatrix);
        r = null;
        c2rArray = null;
        return senderOutput;
    }
}
