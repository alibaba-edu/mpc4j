package edu.alibaba.mpc4j.s2pc.pso.psi.hfh99;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteMulEcc;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.hfh99.Hfh99ByteEccPsiPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * HFH99-字节椭圆曲线PSI协议服务端。
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class Hfh99ByteEccPsiServer<T> extends AbstractPsiServer<T> {
    /**
     * 字节椭圆曲线
     */
    private final ByteMulEcc byteMulEcc;
    /**
     * PEQT哈希函数
     */
    private Hash peqtHash;
    /**
     * 密钥
     */
    private byte[] alpha;

    public Hfh99ByteEccPsiServer(Rpc serverRpc, Party clientParty, Hfh99ByteEccPsiConfig config) {
        super(Hfh99ByteEccPsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        byteMulEcc = ByteEccFactory.createMulInstance(envType);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 生成α
        alpha = byteMulEcc.randomScalar(secureRandom);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Server Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int peqtByteLength = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        // 服务端计算并发送H(x)^α
        List<byte[]> hxAlphaPayload = generateHxAlphaPayload();
        DataPacketHeader hxAlphaHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HX_ALPHA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hxAlphaHeader, hxAlphaPayload));
        stopWatch.stop();
        long hxAlphaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), hxAlphaTime);

        stopWatch.start();
        // 服务端接收H(y)^β
        DataPacketHeader hyBetaHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_HY_BETA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> hyBetaPayload = rpc.receive(hyBetaHeader).getPayload();
        // 服务端计算并发送H(y)^βα
        List<byte[]> peqtPayload = handleHyBetaPayload(hyBetaPayload);
        DataPacketHeader peqtHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_HY_BETA_ALPHA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(peqtHeader, peqtPayload));
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), peqtTime);

        info("{}{} Server end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    private List<byte[]> generateHxAlphaPayload() {
        Stream<T> serverElementStream = serverElementArrayList.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
        return serverElementStream
            .map(ObjectUtils::objectToByteArray)
            .map(byteMulEcc::hashToCurve)
            .map(p -> byteMulEcc.mul(p, alpha))
            .collect(Collectors.toList());
    }

    private List<byte[]> handleHyBetaPayload(List<byte[]> hyBetaPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(hyBetaPayload.size() == clientElementSize);
        Stream<byte[]> hyBetaStream = hyBetaPayload.stream();
        hyBetaStream = parallel ? hyBetaStream.parallel() : hyBetaStream;
        return hyBetaStream
            .map(p -> byteMulEcc.mul(p, alpha))
            .map(p -> peqtHash.digestToBytes(p))
            .collect(Collectors.toList());
    }
}
