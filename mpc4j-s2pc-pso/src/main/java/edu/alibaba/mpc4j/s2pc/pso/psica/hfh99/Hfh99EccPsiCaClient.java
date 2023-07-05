package edu.alibaba.mpc4j.s2pc.pso.psica.hfh99;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;
import edu.alibaba.mpc4j.s2pc.pso.psica.AbstractPsiCaClient;
import edu.alibaba.mpc4j.s2pc.pso.psica.hfh99.Hfh99EccPsiCaPtoDesc.PtoStep;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ECC-based HFH99 PSI Cardinality client.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public class Hfh99EccPsiCaClient<T> extends AbstractPsiCaClient<T> {
    /**
     * ECC
     */
    private final Ecc ecc;
    /**
     * compress encode
     */
    private final boolean compressEncode;
    /**
     * hash for private equality test
     */
    private Hash peqtHash;
    /**
     * secret key β
     */
    private BigInteger beta;

    public Hfh99EccPsiCaClient(Rpc clientRpc, Party serverParty, Hfh99EccPsiCaConfig config) {
        super(Hfh99EccPsiCaPtoDesc.getInstance(), clientRpc, serverParty, config);
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
    public int psiCardinality(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int peqtByteLength = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        // client calculates H(y)^β, randomly permutes, sends to server
        List<byte[]> randomlyPermuteHyBetaPayload = generateRandomlyPermuteHyBetaPayload();
        DataPacketHeader hyBetaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_RANDOMLY_PERMUTED_HY_BETA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hyBetaHeader, randomlyPermuteHyBetaPayload));
        stopWatch.stop();
        long hyBetaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, hyBetaTime);

        // client receives H(x)^α, which has been randomly permuted.
        DataPacketHeader randomlyPermutedHxAlphaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RANDOMLY_PERMUTED_HX_ALPHA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> randomlyPermuteHxAlphaPayload = rpc.receive(randomlyPermutedHxAlphaHeader).getPayload();

        stopWatch.start();
        // client calculates H(H(x)^αβ)
        Set<ByteBuffer> peqtSet = handleHxAlphaPayload(randomlyPermuteHxAlphaPayload);
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, peqtTime);

        // client receives H(H(y)^βα), which has been randomly permuted.
        DataPacketHeader randomlyPermutedPeqtHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RANDOMLY_PERMUTED_HY_BETA_ALPHA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> randomlyPermutedPeqtPayload = rpc.receive(randomlyPermutedPeqtHeader).getPayload();

        stopWatch.start();
        // client computes the cardinality
        int cardinality = handleRandomlyPermutedPeqtPayload(randomlyPermutedPeqtPayload, peqtSet);
        stopWatch.stop();
        long cardinalityTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, cardinalityTime);

        logPhaseInfo(PtoState.PTO_END);
        return cardinality;
    }

    private List<byte[]> generateRandomlyPermuteHyBetaPayload() {
        Stream<T> clientElementStream = clientElementArrayList.stream();
        clientElementStream = parallel ? clientElementStream.parallel() : clientElementStream;
        List<byte[]> result = clientElementStream
            .map(ObjectUtils::objectToByteArray)
            .map(ecc::hashToCurve)
            .map(p -> ecc.multiply(p, beta))
            .map(p -> ecc.encode(p, compressEncode))
            .collect(Collectors.toList());
        // randomly permute
        Collections.shuffle(result, secureRandom);

        return result;
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

    private int handleRandomlyPermutedPeqtPayload(List<byte[]> randomlyPermutedPeqtPayload, Set<ByteBuffer> peqtSet)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(randomlyPermutedPeqtPayload.size() == clientElementSize);
        Set<ByteBuffer> randomlyPermutedPeqtSet = randomlyPermutedPeqtPayload.stream()
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(HashSet::new));
        // calculate intersection
        peqtSet.retainAll(randomlyPermutedPeqtSet);
        return peqtSet.size();
    }
}
