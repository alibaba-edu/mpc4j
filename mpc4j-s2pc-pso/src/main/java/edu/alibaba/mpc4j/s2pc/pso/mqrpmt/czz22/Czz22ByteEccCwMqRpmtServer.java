package edu.alibaba.mpc4j.s2pc.pso.mqrpmt.czz22;

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
import edu.alibaba.mpc4j.common.tool.filter.Filter;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory;
import edu.alibaba.mpc4j.s2pc.pso.mqrpmt.AbstractMqRpmtServer;
import edu.alibaba.mpc4j.s2pc.pso.mqrpmt.czz22.Czz22ByteEccCwMqRpmtPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ZZL22-字节椭圆曲线mqRPMT协议服务端。
 *
 * @author Weiran Liu
 * @date 2022/9/10
 */
public class Czz22ByteEccCwMqRpmtServer extends AbstractMqRpmtServer {
    /**
     * 字节椭圆曲线
     */
    private final ByteMulEcc byteMulEcc;
    /**
     * 过滤器类型
     */
    private final FilterFactory.FilterType filterType;
    /**
     * PEQT哈希函数
     */
    private Hash peqtHash;
    /**
     * 服务端密钥α
     */
    private byte[] alpha;

    public Czz22ByteEccCwMqRpmtServer(Rpc serverRpc, Party clientParty, Czz22ByteEccCwMqRpmtConfig config) {
        super(Czz22ByteEccCwMqRpmtPtoDesc.getInstance(), serverRpc, clientParty, config);
        byteMulEcc = ByteEccFactory.createMulInstance(envType);
        filterType = config.getFilterType();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) {
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
    public ByteBuffer[] mqRpmt(Set<ByteBuffer> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        info("{}{} Server begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int peqtByteLength = Czz22ByteEccCwMqRpmtPtoDesc.getPeqtByteLength(serverElementSize, clientElementSize);
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
        return serverElementArrayList.toArray(new ByteBuffer[0]);
    }

    private List<byte[]> generateHxAlphaPayload() {
        Stream<ByteBuffer> serverElementStream = serverElementArrayList.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
        return serverElementStream
            .map(serverElement -> byteMulEcc.hashToCurve(serverElement.array()))
            .map(p -> byteMulEcc.mul(p, alpha))
            .collect(Collectors.toList());
    }

    private List<byte[]> handleHyBetaPayload(List<byte[]> hyBetaPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(hyBetaPayload.size() == clientElementSize);
        Stream<byte[]> hyBetaStream = hyBetaPayload.stream();
        hyBetaStream = parallel ? hyBetaStream.parallel() : hyBetaStream;
        List<byte[]> peqtPayload = hyBetaStream
            .map(p -> byteMulEcc.mul(p, alpha))
            .map(p -> peqtHash.digestToBytes(p))
            .collect(Collectors.toList());
        // 置乱顺序
        Collections.shuffle(peqtPayload, secureRandom);
        // 将元素插入到过滤器中
        Filter<byte[]> filter = FilterFactory.createFilter(envType, filterType, serverElementSize, secureRandom);
        peqtPayload.forEach(filter::put);
        return filter.toByteArrayList();
    }
}
