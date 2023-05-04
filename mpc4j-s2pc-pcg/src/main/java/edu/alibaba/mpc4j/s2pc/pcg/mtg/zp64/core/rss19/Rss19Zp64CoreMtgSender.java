package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.AbstractZp64CoreMtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19.Rss19Zp64CoreMtgPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.Zp64Triple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * RSS19-核Zp64三元组生成协议发送方。
 *
 * @author Liqiang Peng
 * @date 2022/9/5
 */
public class Rss19Zp64CoreMtgSender extends AbstractZp64CoreMtgParty {
    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }
    /**
     * 加密方案参数
     */
    public byte[] encryptionParams;
    /**
     * 公钥
     */
    public byte[] publicKey;
    /**
     * 私钥
     */
    public byte[] secretKey;
    /**
     * 多项式阶
     */
    private final int polyModulusDegree;
    /**
     * the prime
     */
    private final long p;
    /**
     * 缓存区
     */
    private Zp64Triple zp64TripleBuffer;

    public Rss19Zp64CoreMtgSender(Rpc senderRpc, Party receiverParty, Rss19Zp64CoreMtgConfig config) {
        super(Rss19Zp64CoreMtgPtoDesc.getInstance(), senderRpc, receiverParty, config);
        polyModulusDegree = config.getPolyModulusDegree();
        p = zp64.getPrime();
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        // 生成密钥
        stopWatch.start();
        ArrayList<byte[]> fheParams = Rss19Zp64CoreMtgNativeUtils.keyGen(polyModulusDegree, p);
        handleFheParams(fheParams);
        zp64TripleBuffer = Zp64Triple.createEmpty(zp64);
        DataPacketHeader fheParamsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_ENCRYPTION_PARAMS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(fheParamsHeader, Collections.singletonList(encryptionParams)));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Zp64Triple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int bigRoundIndex = num / polyModulusDegree;
        int updateRound = (num + polyModulusDegree - 1) / polyModulusDegree;
        ArrayList<byte[]> ciphertextPayload = new ArrayList<>();
        long[][] a0 = new long[updateRound][];
        long[][] b0 = new long[updateRound][];
        long[][] c0 = new long[updateRound][];
        IntStream.range(0, updateRound).forEach(round -> {
            int updateRoundNum = round < bigRoundIndex ? polyModulusDegree : num % polyModulusDegree;
            a0[round] = generateRandom(updateRoundNum, p);
            b0[round] = generateRandom(updateRoundNum, p);
            ArrayList<byte[]> ct = Rss19Zp64CoreMtgNativeUtils.encryption(
                encryptionParams, publicKey, secretKey, a0[round], b0[round]
            );
            ciphertextPayload.addAll(ct);
        });
        DataPacketHeader ciphertextHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CT_A_CT_B.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(ciphertextHeader, ciphertextPayload));
        stopWatch.stop();
        long encTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, encTime, "encrypt query");

        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CT_D.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        ArrayList<byte[]> response = (ArrayList<byte[]>) rpc.receive(responseHeader).getPayload();
        MpcAbortPreconditions.checkArgument(response.size() == updateRound);

        stopWatch.start();
        IntStream.range(0, updateRound).forEach(round -> {
            int updateRoundNum = round < bigRoundIndex ? polyModulusDegree : num % polyModulusDegree;
            long[] d = Rss19Zp64CoreMtgNativeUtils.decryption(encryptionParams, secretKey, response.get(round));
            c0[round] = IntStream.range(0, updateRoundNum)
                .mapToLong(i -> zp64.add(zp64.mul(a0[round][i], b0[round][i]), d[i]))
                .toArray();
            zp64TripleBuffer.merge(Zp64Triple.create(zp64, updateRoundNum, a0[round], b0[round], c0[round]));
        });
        stopWatch.stop();
        long decTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, decTime, "decrypt response");

        logPhaseInfo(PtoState.PTO_END);
        return zp64TripleBuffer;
    }

    /**
     * 处理FHE参数。
     *
     * @param fheParams FHE参数。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private void handleFheParams(ArrayList<byte[]> fheParams) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(fheParams.size() == 3);
        encryptionParams = fheParams.get(0);
        publicKey = fheParams.get(1);
        secretKey = fheParams.get(2);
    }
}
