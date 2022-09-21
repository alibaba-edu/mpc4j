package edu.alibaba.mpc4j.s2pc.pso.psi.hfh99;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.hfh99.Hfh99EccPsiPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * HFH99-椭圆曲线PSI协议服务端。
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class Hfh99EccPsiServer<T> extends AbstractPsiServer<T> {
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * 是否压缩编码
     */
    private final boolean compressEncode;
    /**
     * PEQT哈希函数
     */
    private Hash peqtHash;
    /**
     * 密钥
     */
    private BigInteger alpha;

    public Hfh99EccPsiServer(Rpc serverRpc, Party clientParty, Hfh99EccPsiConfig config) {
        super(Hfh99EccPsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        ecc = EccFactory.createInstance(envType);
        compressEncode = config.getCompressEncode();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 生成α
        alpha = ecc.randomZn(secureRandom);
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
            .map(ecc::hashToCurve)
            .map(p -> ecc.multiply(p, alpha))
            .map(p -> ecc.encode(p, compressEncode))
            .collect(Collectors.toList());
    }

    private List<byte[]> handleHyBetaPayload(List<byte[]> hyBetaPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(hyBetaPayload.size() == clientElementSize);
        Stream<byte[]> hyBetaStream = hyBetaPayload.stream();
        hyBetaStream = parallel ? hyBetaStream.parallel() : hyBetaStream;
        return hyBetaStream
            .map(ecc::decode)
            .map(p -> ecc.multiply(p, alpha))
            .map(p -> ecc.encode(p, false))
            .map(p -> peqtHash.digestToBytes(p))
            .collect(Collectors.toList());
    }
}
