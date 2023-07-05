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
import edu.alibaba.mpc4j.s2pc.pso.psica.AbstractPsiCaServer;
import edu.alibaba.mpc4j.s2pc.pso.psica.hfh99.Hfh99EccPsiCaPtoDesc.PtoStep;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ECC-based HFH99 PSI Cardinality server.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public class Hfh99EccPsiCaServer<T> extends AbstractPsiCaServer<T> {
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
     * secret key α
     */
    private BigInteger alpha;

    public Hfh99EccPsiCaServer(Rpc serverRpc, Party clientParty, Hfh99EccPsiCaConfig config) {
        super(Hfh99EccPsiCaPtoDesc.getInstance(), serverRpc, clientParty, config);
        ecc = EccFactory.createInstance(envType);
        compressEncode = config.getCompressEncode();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // generate alpha
        alpha = ecc.randomZn(secureRandom);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psiCardinality(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int peqtByteLength = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        // server calculates H(x)^α, randomly permutes, sends to client.
        List<byte[]> randomlyPermutedHxAlphaPayload = generateRandomlyPermuteHxAlphaPayload();
        DataPacketHeader randomlyPermutedHxAlphaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RANDOMLY_PERMUTED_HX_ALPHA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(randomlyPermutedHxAlphaHeader, randomlyPermutedHxAlphaPayload));
        stopWatch.stop();
        long hxAlphaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, hxAlphaTime);

        // server receiver H(y)^β , which has been randomly permuted.
        DataPacketHeader randomlyPermutedHyBetaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_RANDOMLY_PERMUTED_HY_BETA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> randomlyPermutedHyBetaPayload = rpc.receive(randomlyPermutedHyBetaHeader).getPayload();

        stopWatch.start();
        // server calculates H(y)^βα, randomly permutes, sends to client.
        List<byte[]> randomlyPermutedPeqtPayload = handleRandomlyPermutedHyBetaPayload(randomlyPermutedHyBetaPayload);
        DataPacketHeader randomlyPermutedPeqtHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RANDOMLY_PERMUTED_HY_BETA_ALPHA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(randomlyPermutedPeqtHeader, randomlyPermutedPeqtPayload));
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, peqtTime);

        logPhaseInfo(PtoState.PTO_END);
    }

    private List<byte[]> generateRandomlyPermuteHxAlphaPayload() {
        Stream<T> serverElementStream = serverElementArrayList.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;

        List<byte[]> result = serverElementStream
            .map(ObjectUtils::objectToByteArray)
            .map(ecc::hashToCurve)
            .map(p -> ecc.multiply(p, alpha))
            .map(p -> ecc.encode(p, compressEncode))
            .collect(Collectors.toList());
        // randomly permute
        Collections.shuffle(result, secureRandom);
        return result;
    }

    private List<byte[]> handleRandomlyPermutedHyBetaPayload(List<byte[]> hyBetaPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(hyBetaPayload.size() == clientElementSize);
        Stream<byte[]> hyBetaStream = hyBetaPayload.stream();
        hyBetaStream = parallel ? hyBetaStream.parallel() : hyBetaStream;

        List<byte[]> result = hyBetaStream
            .map(ecc::decode)
            .map(p -> ecc.multiply(p, alpha))
            .map(p -> ecc.encode(p, false))
            .map(p -> peqtHash.digestToBytes(p))
            .collect(Collectors.toList());
        // randomly permute
        Collections.shuffle(result, secureRandom);
        return result;
    }
}