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
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.hfh99.Hfh99EccPsiPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * HFH99-椭圆曲线PSI协议客户端。
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class Hfh99EccPsiClient<T> extends AbstractPsiClient<T> {
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
     * 客户端密钥β
     */
    private BigInteger beta;

    public Hfh99EccPsiClient(Rpc clientRpc, Party serverParty, Hfh99EccPsiConfig config) {
        super(Hfh99EccPsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        ecc = EccFactory.createInstance(envType);
        compressEncode = config.getCompressEncode();
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 生成β
        beta = ecc.randomZn(secureRandom);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int peqtByteLength = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        // 客户端计算并发送H(y)^β
        List<byte[]> hyBetaPayload = generateHyBetaPayload();
        DataPacketHeader hyBetaHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_HY_BETA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hyBetaHeader, hyBetaPayload));
        stopWatch.stop();
        long hyBetaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), hyBetaTime);

        stopWatch.start();
        // 客户端接收H(x)^α
        DataPacketHeader hxAlphaHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HX_ALPHA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> hxAlphaPayload = rpc.receive(hxAlphaHeader).getPayload();
        // 客户端计算H(H(x)^αβ)
        Set<ByteBuffer> peqtSet = handleHxAlphaPayload(hxAlphaPayload);
        // 客户端接收H(H(y)^βα)
        DataPacketHeader peqtHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_HY_BETA_ALPHA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> peqtPayload = rpc.receive(peqtHeader).getPayload();
        Set<T> intersection = handlePeqtPayload(peqtPayload, peqtSet);
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), peqtTime);

        info("{}{} Client end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return intersection;
    }

    private List<byte[]> generateHyBetaPayload() {
        Stream<T> clientElementStream = clientElementArrayList.stream();
        clientElementStream = parallel ? clientElementStream.parallel() : clientElementStream;
        return clientElementStream
            .map(ObjectUtils::objectToByteArray)
            .map(ecc::hashToCurve)
            .map(p -> ecc.multiply(p, beta))
            .map(p -> ecc.encode(p, compressEncode))
            .collect(Collectors.toList());
    }

    private Set<ByteBuffer> handleHxAlphaPayload(List<byte[]> hxAlphaPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(hxAlphaPayload.size() == serverElementSize);
        Stream<byte[]> hxAlphaStream = hxAlphaPayload.stream();
        hxAlphaStream = parallel ? hxAlphaStream.parallel() : hxAlphaStream;
        return hxAlphaStream
            .map(ecc::decode)
            .map(p -> ecc.multiply(p, beta))
            .map(p -> ecc.encode(p, false))
            .map(p -> peqtHash.digestToBytes(p))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
    }

    private Set<T> handlePeqtPayload(List<byte[]> peqtPayload, Set<ByteBuffer> peqtSet) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(peqtPayload.size() == clientElementSize);
        ArrayList<ByteBuffer> peqtArrayList = peqtPayload.stream()
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(ArrayList::new));
        return IntStream.range(0, clientElementSize)
            .mapToObj(index -> {
                if (peqtSet.contains(peqtArrayList.get(index))) {
                    return clientElementArrayList.get(index);
                } else {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }
}
