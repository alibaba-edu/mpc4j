package edu.alibaba.mpc4j.s2pc.pso.psi.pke.hfh99;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteMulEcc;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.pke.hfh99.Hfh99ByteEccPsiPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * HFH99-byte ecc PSI server
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class Hfh99ByteEccPsiServer<T> extends AbstractPsiServer<T> {
    /**
     * byte ecc
     */
    private final ByteMulEcc byteMulEcc;
    /**
     * PEQT hash function
     */
    private Hash peqtHash;
    /**
     * key
     */
    private byte[] alpha;

    public Hfh99ByteEccPsiServer(Rpc serverRpc, Party clientParty, Hfh99ByteEccPsiConfig config) {
        super(Hfh99ByteEccPsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        byteMulEcc = ByteEccFactory.createMulInstance(envType);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // generate α
        alpha = byteMulEcc.randomScalar(secureRandom);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int peqtByteLength = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        // server computes and sends H(x)^α
        List<byte[]> hxAlphaPayload = generateHxAlphaPayload();
        DataPacketHeader hxAlphaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HX_ALPHA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hxAlphaHeader, hxAlphaPayload));
        stopWatch.stop();
        long hxAlphaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, hxAlphaTime);

        // server receives H(y)^β
        DataPacketHeader hyBetaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_HY_BETA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> hyBetaPayload = rpc.receive(hyBetaHeader).getPayload();

        stopWatch.start();
        // server computes and sends H(y)^βα
        List<byte[]> peqtPayload = handleHyBetaPayload(hyBetaPayload);
        DataPacketHeader peqtHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_HY_BETA_ALPHA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(peqtHeader, peqtPayload));
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, peqtTime);

        logPhaseInfo(PtoState.PTO_END);
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
