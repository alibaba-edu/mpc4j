package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.KyberEngine;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.KyberEngineFactory;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.utils.Poly;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBaseNotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BaseNotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19.Mr19KyberBaseNotPtoDesc.PtoStep;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * MR19-Kyber-基础n选1-OT协议发送方。
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/08/26
 */
public class Mr19KyberBaseNotSender extends AbstractBaseNotSender {
    /**
     * Kyber的参数K
     */
    private final int paramsK;
    /**
     * 使用的kyber实例
     */
    private final KyberEngine kyberEngine;
    /**
     * 公钥哈希函数
     */
    private final Hash pkHash;
    /**
     * 发送方输出
     */
    private BaseNotSenderOutput senderOutput;

    public Mr19KyberBaseNotSender(Rpc senderRpc, Party receiverParty, Mr19KyberBaseNotConfig config) {
        super(Mr19KyberBaseNotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        paramsK = config.getParamsK();
        kyberEngine = KyberEngineFactory.createInstance(config.getKyberType(), paramsK);
        pkHash = HashFactory.createInstance(HashFactory.HashType.BC_SHAKE_256, kyberEngine.publicKeyByteLength());
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
        MpcAbortPreconditions.checkArgument(pkPayload.size() == num * (maxChoice + 1));
        byte[][] publicKeyMatrix = pkPayload.toArray(new byte[0][]);
        byte[][][] rMatrix = new byte[num][maxChoice][kyberEngine.keyByteLength()];
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        List<byte[]> betaPayload = indexIntStream
            .mapToObj(index -> {
                short[][][] pkVectors = new short[maxChoice][][];
                short[][][] hashPkVectors = new short[maxChoice][][];
                byte[][] recoverPks = new byte[maxChoice][];
                for (int i = 0; i < maxChoice; i++) {
                    byte[] pk = publicKeyMatrix[index * (maxChoice + 1) + i];
                    pkVectors[i] = Poly.polyVectorFromBytes(pk);
                    byte[] hashPk = pkHash.digestToBytes(pk);
                    hashPkVectors[i] = Poly.decompressPolyVector(hashPk, paramsK);
                    Poly.inPolyVectorBarrettReduce(hashPkVectors[i]);
                }
                for (int i = 0; i < maxChoice; i++) {
                    for (int j = 0; j < maxChoice; j++) {
                        if (i != j) {
                            // A = R_i - Hash(R_j)
                            Poly.inPolyVectorSub(pkVectors[i], hashPkVectors[j]);
                            Poly.inPolyVectorBarrettReduce(pkVectors[i]);
                        }
                    }
                }
                for (int i = 0; i < maxChoice; i++) {
                    recoverPks[i] = Poly.polyVectorToByteArray(pkVectors[i]);
                }
                return IntStream.range(0, maxChoice)
                    .mapToObj(i -> {
                        byte[] ciphertext = kyberEngine.encapsulate(
                            rMatrix[index][i], recoverPks[i], publicKeyMatrix[index * (maxChoice + 1) + maxChoice]
                        );
                        rMatrix[index][i] = kdf.deriveKey(rMatrix[index][i]);
                        return ciphertext;
                    })
                    .toArray(byte[][]::new);
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
        senderOutput = new BaseNotSenderOutput(maxChoice, rMatrix);
        return betaPayload;
    }
}
