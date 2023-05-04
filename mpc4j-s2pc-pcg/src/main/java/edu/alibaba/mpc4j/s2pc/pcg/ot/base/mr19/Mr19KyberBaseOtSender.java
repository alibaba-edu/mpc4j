package edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.utils.Poly;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.AbstractBaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19.Mr19KyberBaseOtPtoDesc.PtoStep;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * MR19-KYBER-基础OT协议发送方。
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/08/05
 */
public class Mr19KyberBaseOtSender extends AbstractBaseOtSender {
    /**
     * Kyber参数K
     */
    private final int paramsK;
    /**
     * Kyber引擎
     */
    private final KyberEngine kyberEngine;
    /**
     * 公钥哈希函数
     */
    private final Hash pkHash;
    /**
     * 发送方输出
     */
    private BaseOtSenderOutput senderOutput;

    public Mr19KyberBaseOtSender(Rpc senderRpc, Party receiverParty, Mr19KyberBaseOtConfig config) {
        super(Mr19KyberBaseOtPtoDesc.getInstance(), senderRpc, receiverParty, config);
        paramsK = config.getParamsK();
        kyberEngine = KyberEngineFactory.createInstance(config.getKyberType(), paramsK);
        pkHash = HashFactory.createInstance(HashFactory.HashType.BC_SHAKE_256, kyberEngine.publicKeyByteLength());
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
        DataPacketHeader pkHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_PK.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> pkPayload = rpc.receive(pkHeader).getPayload();
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, pkTime);

        stopWatch.start();
        List<byte[]> betaPayload = handlePkPayload(pkPayload);
        DataPacketHeader betaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_BETA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(betaHeader, betaPayload));
        stopWatch.stop();
        long betaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, betaTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private List<byte[]> handlePkPayload(List<byte[]> pkPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(pkPayload.size() == num * 3);
        byte[][] publicKey = pkPayload.toArray(new byte[0][]);
        IntStream keyPairArrayIntStream = IntStream.range(0, num);
        keyPairArrayIntStream = parallel ? keyPairArrayIntStream.parallel() : keyPairArrayIntStream;
        // OT协议的输出
        byte[][] r0Array = new byte[num][kyberEngine.keyByteLength()];
        byte[][] r1Array = new byte[num][kyberEngine.keyByteLength()];
        List<byte[]> betaPayload = keyPairArrayIntStream
            .mapToObj(index -> {
                // 读取公钥
                byte[] pk0 = publicKey[index * 3];
                short[][] pk0PolyVector = Poly.polyVectorFromBytes(pk0);
                byte[] pk1 = publicKey[index * 3 + 1];
                short[][] pk1PolyVector = Poly.polyVectorFromBytes(pk1);
                // Hash(R1)
                byte[] hashPk1 = pkHash.digestToBytes(pk1);
                short[][] hashPk1PolyVector = Poly.decompressPolyVector(hashPk1, paramsK);
                Poly.inPolyVectorBarrettReduce(hashPk1PolyVector);
                // A = R0 - Hash(R1)
                Poly.inPolyVectorSub(pk0PolyVector, hashPk1PolyVector);
                Poly.inPolyVectorBarrettReduce(pk0PolyVector);
                byte[] recoverPk0 = Poly.polyVectorToByteArray(pk0PolyVector);
                // Hash(R0)
                byte[] hashPk0 = pkHash.digestToBytes(pk0);
                short[][] hashPk0PolyVector = Poly.decompressPolyVector(hashPk0, paramsK);
                Poly.inPolyVectorBarrettReduce(hashPk0PolyVector);
                // B = R1 - Hash(R0)
                Poly.inPolyVectorSub(pk1PolyVector, hashPk0PolyVector);
                Poly.inPolyVectorBarrettReduce(pk1PolyVector);
                byte[] recoverPk1 = Poly.polyVectorToByteArray(pk1PolyVector);
                // 计算密文
                byte[][] ciphertext = new byte[2][];
                // KEM中的输入是秘密值、公钥（As+e）部分、生成元部分、随机数种子，安全参数k
                ciphertext[0] = kyberEngine.encapsulate(r0Array[index], recoverPk0, publicKey[index * 3 + 2]);
                r0Array[index] = kdf.deriveKey(r0Array[index]);
                ciphertext[1] = kyberEngine.encapsulate(r1Array[index], recoverPk1, publicKey[index * 3 + 2]);
                r1Array[index] = kdf.deriveKey(r1Array[index]);
                return ciphertext;
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
        senderOutput = new BaseOtSenderOutput(r0Array, r1Array);
        return betaPayload;
    }
}
