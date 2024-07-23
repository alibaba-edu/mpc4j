package edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz24;

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
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz24.Czz24CwOprfMqRpmtPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * CZZ24 cwOPRF-based mqRPMT client.
 *
 * @author Weiran Liu
 * @date 2022/9/11
 */
public class Czz24CwOprfMqRpmtClient extends AbstractMqRpmtClient {
    /**
     * ECC
     */
    private final ByteMulEcc byteMulEcc;
    /**
     * PEQT hash
     */
    private Hash peqtHash;
    /**
     * β
     */
    private byte[] beta;

    public Czz24CwOprfMqRpmtClient(Rpc clientRpc, Party serverParty, Czz24CwOprfMqRpmtConfig config) {
        super(Czz24CwOprfMqRpmtPtoDesc.getInstance(), clientRpc, serverParty, config);
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
        int peqtByteLength = Czz24CwOprfMqRpmtPtoDesc.getPeqtByteLength(serverElementSize, clientElementSize);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        // H(y)^β
        List<byte[]> hyBetaPayload = generateHyBetaPayload();
        DataPacketHeader hyBetaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_HY_BETA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hyBetaHeader, hyBetaPayload));
        stopWatch.stop();
        long hyBetaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, hyBetaTime, "Clients computes H(y)^β");

        // H(x)^α
        DataPacketHeader hxAlphaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HX_ALPHA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> hxAlphaPayload = rpc.receive(hxAlphaHeader).getPayload();

        stopWatch.start();
        // H(H(x)^αβ)
        ByteBuffer[] clientPeqtArray = handleHxAlphaPayload(hxAlphaPayload);
        stopWatch.stop();
        long hyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, hyTime, "Clients computes H(H(y)^βα)");

        // H(H(y)^βα)
        DataPacketHeader peqtHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_HY_BETA_ALPHA.ordinal(), extraInfo,
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

    private boolean[] handlePeqtPayload(List<byte[]> peqtPayload, ByteBuffer[] clientPeqtArray) {
        Filter<byte[]> filter = FilterFactory.load(envType, peqtPayload);
        boolean[] containVector = new boolean[serverElementSize];
        IntStream.range(0, serverElementSize).forEach(serverElementIndex -> {
            if (filter.mightContain(clientPeqtArray[serverElementIndex].array())) {
                containVector[serverElementIndex] = true;
            }
        });
        return containVector;
    }
}
