package edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz22;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteMulEcc;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.structure.filter.Filter;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.AbstractMqRpmtClient;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * ZZL22-字节椭圆曲线mqRPMT协议客户端。
 *
 * @author Weiran Liu
 * @date 2022/9/11
 */
public class Czz22ByteEccCwMqRpmtClient extends AbstractMqRpmtClient {
    /**
     * 字节椭圆曲线
     */
    private final ByteMulEcc byteMulEcc;
    /**
     * PEQT哈希函数
     */
    private Hash peqtHash;
    /**
     * 客户端密钥β
     */
    private byte[] beta;

    public Czz22ByteEccCwMqRpmtClient(Rpc clientRpc, Party serverParty, Czz22ByteEccCwMqRpmtConfig config) {
        super(Czz22ByteEccCwMqRpmtPtoDesc.getInstance(), clientRpc, serverParty, config);
        byteMulEcc = ByteEccFactory.createMulInstance(envType);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        beta = byteMulEcc.randomScalar(secureRandom);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public boolean[] mqRpmt(Set<ByteBuffer> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int peqtByteLength = Czz22ByteEccCwMqRpmtPtoDesc.getPeqtByteLength(this.serverElementSize, clientElementSize);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        // 客户端计算并发送H(y)^β
        List<byte[]> hyBetaPayload = generateHyBetaPayload();
        DataPacketHeader hyBetaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Czz22ByteEccCwMqRpmtPtoDesc.PtoStep.CLIENT_SEND_HY_BETA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hyBetaHeader, hyBetaPayload));
        stopWatch.stop();
        long hyBetaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, hyBetaTime, "Clients computes H(y)^β");

        // 客户端接收H(x)^α
        DataPacketHeader hxAlphaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Czz22ByteEccCwMqRpmtPtoDesc.PtoStep.SERVER_SEND_HX_ALPHA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> hxAlphaPayload = rpc.receive(hxAlphaHeader).getPayload();

        stopWatch.start();
        // 客户端计算H(H(x)^αβ)
        ByteBuffer[] clientPeqtArray = handleHxAlphaPayload(hxAlphaPayload);
        stopWatch.stop();
        long hyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, hyTime, "Clients computes H(H(y)^βα)");

        // 客户端接收H(H(y)^βα)
        DataPacketHeader peqtHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Czz22ByteEccCwMqRpmtPtoDesc.PtoStep.CLIENT_SEND_HY_BETA_ALPHA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> peqtPayload = rpc.receive(peqtHeader).getPayload();

        stopWatch.start();
        boolean[] containVector = handlePeqtPayload(peqtPayload, clientPeqtArray);
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, peqtTime, "Clients obtains RPMT");

        logPhaseInfo(PtoState.PTO_END);
        return containVector;
    }

    private List<byte[]> generateHyBetaPayload() {
        Stream<ByteBuffer> clientElementStream = clientElementArrayList.stream();
        clientElementStream = parallel ? clientElementStream.parallel() : clientElementStream;
        return clientElementStream
            .map(clientElement -> byteMulEcc.hashToCurve(clientElement.array()))
            .map(p -> byteMulEcc.mul(p, beta))
            .collect(Collectors.toList());
    }

    private ByteBuffer[] handleHxAlphaPayload(List<byte[]> hxAlphaPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(hxAlphaPayload.size() == serverElementSize);
        Stream<byte[]> hxAlphaStream = hxAlphaPayload.stream();
        hxAlphaStream = parallel ? hxAlphaStream.parallel() : hxAlphaStream;
        return hxAlphaStream
            .map(p -> byteMulEcc.mul(p, beta))
            .map(p -> peqtHash.digestToBytes(p))
            .map(ByteBuffer::wrap)
            .toArray(ByteBuffer[]::new);
    }

    private boolean[] handlePeqtPayload(List<byte[]> peqtPayload, ByteBuffer[] clientPeqtArray) throws MpcAbortException {
        try {
            Filter<byte[]> filter = FilterFactory.load(envType, peqtPayload);
            boolean[] containVector = new boolean[serverElementSize];
            IntStream.range(0, serverElementSize).forEach(serverElementIndex -> {
                if (filter.mightContain(clientPeqtArray[serverElementIndex].array())) {
                    containVector[serverElementIndex] = true;
                }
            });
            return containVector;
        } catch (IllegalArgumentException e) {
            throw new MpcAbortException();
        }
    }
}
