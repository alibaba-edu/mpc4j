package edu.alibaba.mpc4j.s2pc.pso.psi.pke.hfh99;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.pke.hfh99.Hfh99EccPsiPtoDesc.PtoStep;
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
 * HFH99-ecc PSI client
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class Hfh99EccPsiClient<T> extends AbstractPsiClient<T> {
    /**
     * ecc curve
     */
    private final Ecc ecc;
    /**
     * compress encode?
     */
    private final boolean compressEncode;
    /**
     * PEQT hash function
     */
    private Hash peqtHash;
    /**
     * client key: β
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
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // generate β
        beta = ecc.randomZn(secureRandom);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int peqtByteLength = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        // client computes and sends H(y)^β
        List<byte[]> hyBetaPayload = generateHyBetaPayload();
        DataPacketHeader hyBetaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_HY_BETA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hyBetaHeader, hyBetaPayload));
        stopWatch.stop();
        long hyBetaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, hyBetaTime);

        // client receives H(x)^α
        DataPacketHeader hxAlphaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HX_ALPHA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> hxAlphaPayload = rpc.receive(hxAlphaHeader).getPayload();

        stopWatch.start();
        // client computes H(H(x)^αβ)
        Set<ByteBuffer> peqtSet = handleHxAlphaPayload(hxAlphaPayload);
        // client receives H(H(y)^βα)
        DataPacketHeader peqtHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_HY_BETA_ALPHA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> peqtPayload = rpc.receive(peqtHeader).getPayload();
        Set<T> intersection = handlePeqtPayload(peqtPayload, peqtSet);
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, peqtTime);

        logPhaseInfo(PtoState.PTO_END);
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
