package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.AbstractZp64CoreMtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.Zp64Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * RSS19-核Zp64三元组生成协议接收方。
 *
 * @author Liqiang Peng
 * @date 2022/9/5
 */
public class Rss19Zp64CoreMtgReceiver extends AbstractZp64CoreMtgParty {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }
    /**
     * 加密方案参数
     */
    public byte[] encryptionParams;
    /**
     * 多项式阶
     */
    private final int polyModulusDegree;
    /**
     * 明文模数
     */
    private final long p;
    /**
     * zp64
     */
    private Zp64 zp64;
    /**
     * 缓存区
     */
    private Zp64Triple zp64TripleBuffer;

    public Rss19Zp64CoreMtgReceiver(Rpc receiverRpc, Party senderParty, Rss19Zp64CoreMtgConfig config) {
        super(Rss19Zp64CoreMtgPtoDesc.getInstance(), receiverRpc, senderParty, config);
        this.polyModulusDegree = config.getPolyModulusDegree();
        this.p = config.getZp();
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        info("{}{} Receiver Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        // 接收密钥
        stopWatch.start();
        DataPacketHeader fheParamsHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Rss19Zp64CoreMtgPtoDesc.PtoStep.SENDER_SEND_ENCRYPTION_PARAMS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> fheParams = rpc.receive(fheParamsHeader).getPayload();
        MpcAbortPreconditions.checkArgument(fheParams.size() == 1);
        encryptionParams = fheParams.get(0);
        zp64 = Zp64Factory.createInstance(envType, p);
        zp64TripleBuffer = Zp64Triple.createEmpty(p);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Receiver Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Receiver Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public Zp64Triple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Receiver begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int bigRoundIndex = num / polyModulusDegree;
        int updateRound = (num + polyModulusDegree - 1) / polyModulusDegree;
        DataPacketHeader ciphertextHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Rss19Zp64CoreMtgPtoDesc.PtoStep.SENDER_SEND_CT_A_CT_B.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        byte[][] ciphertextPayload = rpc.receive(ciphertextHeader).getPayload().toArray(new byte[0][]);
        MpcAbortPreconditions.checkArgument(ciphertextPayload.length == 2*updateRound);
        ArrayList<byte[]> responsePayload = new ArrayList<>();
        for (int round = 0; round < updateRound; round++) {
            int updateRoundNum = round < bigRoundIndex ? polyModulusDegree : num % polyModulusDegree;
            long[] a1 = generateRandom(updateRoundNum, p);
            long[] b1 = generateRandom(updateRoundNum, p);
            long[] mask = generateRandom(updateRoundNum, p);
            long[] c1 = IntStream.range(0, updateRoundNum)
                .mapToLong(i -> zp64.sub(zp64.mul(a1[i], b1[i]), mask[i]))
                .toArray();
            responsePayload.add(Rss19Zp64CoreMtgNativeUtils.computeResponse(
                encryptionParams, ciphertextPayload[2*round], ciphertextPayload[2*round+1], a1, b1, mask
            ));
            zp64TripleBuffer.merge(Zp64Triple.create(p, updateRoundNum, a1, b1, c1));
        }
        DataPacketHeader responseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Rss19Zp64CoreMtgPtoDesc.PtoStep.RECEIVER_SEND_CT_D.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responseHeader, responsePayload));
        stopWatch.stop();
        long genReplyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Receiver Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), genReplyTime);

        info("{}{} Receiver end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return zp64TripleBuffer;
    }
}