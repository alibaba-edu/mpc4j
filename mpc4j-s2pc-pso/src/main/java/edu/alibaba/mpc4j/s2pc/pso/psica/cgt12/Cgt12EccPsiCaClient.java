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
import edu.alibaba.mpc4j.s2pc.pso.psica.AbstractPsiCaClient;
import edu.alibaba.mpc4j.s2pc.pso.psica.cgt12.Cgt12EccPsiCaPtoDesc.PtoStep;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ECC-based CGT12 PSI Cardinality client.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public class Cgt12EccPsiCaClient<T> extends AbstractPsiCaClient<T> {
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

    public Cgt12EccPsiCaClient(Rpc clientRpc, Party serverParty, Cgt12EccPsiCaConfig config) {
        super(Cgt12EccPsiCaPtoDesc.getInstance(), clientRpc, serverParty, config);
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
        // client calculates H(y)^β, which not been randomly permuted.
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

        // client receives H(x)^α. Note that this value is hashed.
        DataPacketHeader hxAlphaPeqtHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HASH_HX_ALPHA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> hxAlphaPeqtPayload = rpc.receive(hxAlphaPeqtHeader).getPayload();

        stopWatch.start();
        // client receives H(y)^βα
        DataPacketHeader hyBetaAlphaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HY_BETA_ALPHA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> hyBetaAlphaPayload = rpc.receive(hyBetaAlphaHeader).getPayload();
        // (H(y)^{βα})^{β^{-1}}
        Set<ByteBuffer> peqtSet = handleHyBetaAlphaPayload(hyBetaAlphaPayload);
        int cardinality = calculateInteractionCardinality(hxAlphaPeqtPayload, peqtSet);
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, peqtTime);

        logPhaseInfo(PtoState.PTO_END);
        return cardinality;
    }

    private List<byte[]> generateHyBetaPayload() {
        Stream<T> clientElementStream = clientElementArrayList.stream();
        clientElementStream = parallel ? clientElementStream.parallel() : clientElementStream;
        // Note that no random permutation is required here
        return clientElementStream
            .map(ObjectUtils::objectToByteArray)
            .map(ecc::hashToCurve)
            .map(p -> ecc.multiply(p, beta))
            .map(p -> ecc.encode(p, compressEncode))
            .collect(Collectors.toList());
    }

    private Set<ByteBuffer> handleHyBetaAlphaPayload(List<byte[]> hyBetaAlphaPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(hyBetaAlphaPayload.size() == clientElementSize);
        Stream<byte[]> hxAlphaStream = hyBetaAlphaPayload.stream();
        hxAlphaStream = parallel ? hxAlphaStream.parallel() : hxAlphaStream;
        BigInteger betaInv = beta.modInverse(ecc.getN());
        return hxAlphaStream
            .map(ecc::decode)
            .map(p -> ecc.multiply(p, betaInv))
            .map(p -> ecc.encode(p, false))
            .map(p -> peqtHash.digestToBytes(p))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
    }

    private int calculateInteractionCardinality(List<byte[]> hxAlphaPeqtPayload, Set<ByteBuffer> peqtSet)
		throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(hxAlphaPeqtPayload.size() == serverElementSize);
        Set<ByteBuffer> hxAlphaPeqtSet = hxAlphaPeqtPayload.stream()
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(HashSet::new));
        // calculate intersection
        peqtSet.retainAll(hxAlphaPeqtSet);
        return peqtSet.size();
    }

}