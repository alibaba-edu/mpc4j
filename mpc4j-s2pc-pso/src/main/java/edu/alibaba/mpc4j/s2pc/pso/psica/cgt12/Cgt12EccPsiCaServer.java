package edu.alibaba.mpc4j.s2pc.pso.psica.cgt12;

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
import edu.alibaba.mpc4j.s2pc.pso.psica.cgt12.Cgt12EccPsiCaPtoDesc.PtoStep;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ECC-based CGT12 PSI Cardinality server.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public class Cgt12EccPsiCaServer<T> extends AbstractPsiCaServer<T> {
    /**
     * ECC
     */
    private final Ecc ecc;
    /**
     * compress code
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

    public Cgt12EccPsiCaServer(Rpc serverRpc, Party clientParty, Cgt12EccPsiCaConfig config) {
        super(Cgt12EccPsiCaPtoDesc.getInstance(), serverRpc, clientParty, config);
        ecc = EccFactory.createInstance(envType);
        compressEncode = config.getCompressEncode();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // generate α
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
        // Note that hash needs to be performed on the once-encrypted value
        List<byte[]> hxAlphaPeqtPayload = generateHxAlphaPeqtPayload();
        DataPacketHeader hxAlphaPeqtHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HASH_HX_ALPHA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hxAlphaPeqtHeader, hxAlphaPeqtPayload));
        stopWatch.stop();
        long hxAlphaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, hxAlphaTime);

        // Server receives H(y)^β, which not been randomly permuted.
        DataPacketHeader randomlyPermutedHyBetaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_HY_BETA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> randomlyPermutedHyBetaPayload = rpc.receive(randomlyPermutedHyBetaHeader).getPayload();

        stopWatch.start();
        // Server calculate H(y)^βα and randomly permute it. Note that Peqt Hash is not performed here
        List<byte[]> randomlyPermutedHyBetaAlphaPayload = handleRandomlyPermutedHyBetaPayload(randomlyPermutedHyBetaPayload);
        DataPacketHeader randomlyPermutedHyBetaAlphaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Cgt12EccPsiCaPtoDesc.PtoStep.SERVER_SEND_HY_BETA_ALPHA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(randomlyPermutedHyBetaAlphaHeader, randomlyPermutedHyBetaAlphaPayload));
        stopWatch.stop();
        long hyBetaAlphaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, hyBetaAlphaTime);

        logPhaseInfo(PtoState.PTO_END);
    }

    private List<byte[]> generateHxAlphaPeqtPayload() {
        // randomly permute the plaintext array
        Collections.shuffle(serverElementArrayList, secureRandom);
        Stream<T> serverElementStream = serverElementArrayList.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;

        // Note that hash needs to be performed on the once-encrypted value, refer to Figure 1. in CGT12 paper
        return serverElementStream
            .map(ObjectUtils::objectToByteArray)
            .map(ecc::hashToCurve)
            .map(p -> ecc.multiply(p, alpha))
            .map(p -> ecc.encode(p, false))
            .map(p -> peqtHash.digestToBytes(p))
            .collect(Collectors.toList());
    }

    private List<byte[]> handleRandomlyPermutedHyBetaPayload(List<byte[]> hyBetaPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(hyBetaPayload.size() == clientElementSize);
        Stream<byte[]> hyBetaStream = hyBetaPayload.stream();
        hyBetaStream = parallel ? hyBetaStream.parallel() : hyBetaStream;
        // Note that hash is not performed here, refer to Figure 1. in CGT12
        List<byte[]> result = hyBetaStream
            .map(ecc::decode)
            .map(p -> ecc.multiply(p, alpha))
            .map(p -> ecc.encode(p, compressEncode))
            .collect(Collectors.toList());
        // randomly permute
        Collections.shuffle(result, secureRandom);
        return result;
    }
}
