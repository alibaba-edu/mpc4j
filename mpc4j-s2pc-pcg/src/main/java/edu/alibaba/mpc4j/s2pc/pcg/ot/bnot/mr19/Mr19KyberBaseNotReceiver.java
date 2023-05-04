package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.params.KyberKeyPair;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.utils.Poly;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBaseNotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BaseNotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19.Mr19KyberBaseNotPtoDesc.PtoStep;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * MR19-Kyber-基础n选1-OT协议接收方。
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/08/26
 */
public class Mr19KyberBaseNotReceiver extends AbstractBaseNotReceiver {
    /**
     * Kyber的参数k
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
     * OT协议接收方拥有的密钥对
     */
    private KyberKeyPair[] keyPairArray;

    public Mr19KyberBaseNotReceiver(Rpc receiverRpc, Party senderParty, Mr19KyberBaseNotConfig config) {
        super(Mr19KyberBaseNotPtoDesc.getInstance(), receiverRpc, senderParty, config);
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
    public BaseNotReceiverOutput receive(int[] choices) throws MpcAbortException {
        setPtoInput(choices);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> pkPayload = generatePkPayload();
        DataPacketHeader pkHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_PK.ordinal(), extraInfo,
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
        BaseNotReceiverOutput receiverOutput = handleBetaPayload(betaPayload);
        stopWatch.stop();
        long betaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, betaTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private List<byte[]> generatePkPayload() {
        keyPairArray = new KyberKeyPair[num];
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        return indexIntStream
            .mapToObj(index -> {
                // 生成公私钥对
                keyPairArray[index] = kyberEngine.generateKeyPair();
                byte[] pk = keyPairArray[index].getPublicKey();
                short[][] pkPolyVector = Poly.polyVectorFromBytes(pk);
                // 随机公钥
                byte[][] randomPks = new byte[maxChoice][];
                for (int i = 0; i < maxChoice; i++) {
                    if (i != choices[index]) {
                        // Hash(RandomPk)
                        randomPks[i] = kyberEngine.randomPublicKey();
                        byte[] hashRandomPk = pkHash.digestToBytes(randomPks[i]);
                        short[][] hashRandomPkPolyVector = Poly.decompressPolyVector(hashRandomPk, paramsK);
                        Poly.inPolyVectorBarrettReduce(hashRandomPkPolyVector);
                        // PK = PK ⊕ Hash(RandomPK）
                        Poly.inPolyVectorAdd(pkPolyVector, hashRandomPkPolyVector);
                        Poly.inPolyVectorBarrettReduce(pkPolyVector);
                        pk = Poly.polyVectorToByteArray(pkPolyVector);
                    }
                }
                byte[][] pkPayload = new byte[maxChoice + 1][];
                for (int i = 0; i < maxChoice; i++) {
                    if (i != choices[index]) {
                        pkPayload[i] = randomPks[i];
                    } else {
                        pkPayload[i] = pk;
                    }
                }
                pkPayload[maxChoice] = keyPairArray[index].getMatrixSeed();
                return pkPayload;
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
    }

    private BaseNotReceiverOutput handleBetaPayload(List<byte[]> betaPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(betaPayload.size() == num * maxChoice);
        byte[][] ciphertext = betaPayload.toArray(new byte[num * maxChoice][]);
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        byte[][] rbArray = indexIntStream
            .mapToObj(index -> {
                byte[] rb = kyberEngine.decapsulate(ciphertext[maxChoice * index + choices[index]],
                    keyPairArray[index].getSecretKey(), keyPairArray[index].getPublicKey(),
                    keyPairArray[index].getMatrixSeed());
                return kdf.deriveKey(rb);
            })
            .toArray(byte[][]::new);
        return new BaseNotReceiverOutput(maxChoice, choices, rbArray);
    }

}
