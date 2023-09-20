package edu.alibaba.mpc4j.s2pc.pso.aidpsi.passive;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.s2pc.pso.aidpsi.AbstractAidPsiAider;
import edu.alibaba.mpc4j.s2pc.pso.aidpsi.passive.Kmrs14ShAidPsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * KMRS14 semi-honest PSI aider.
 *
 * @author Weiran Liu
 * @date 2023/5/8
 */
public class Kmrs14ShAidPsiAider extends AbstractAidPsiAider {

    public Kmrs14ShAidPsiAider(Rpc aiderRpc, Party serverParty, Party clientParty, Kmrs14ShAidPsiConfig config) {
        super(Kmrs14ShAidPsiPtoDesc.getInstance(), aiderRpc, serverParty, clientParty, config);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        // empty

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psi(int serverElementSize, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSize, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // P1 sends T1 = π_1(F_K(S_1)) to the aider
        DataPacketHeader serverPrpElementHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_TO_AIDER_TS.ordinal(), extraInfo,
            leftParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverPrpElementPayload = rpc.receive(serverPrpElementHeader).getPayload();
        MpcAbortPreconditions.checkArgument(serverPrpElementPayload.size() == serverElementSize);

        // P2 sends T2 = π_2(F_K(S_2)) to the aider
        DataPacketHeader clientPrpElementHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_TO_AIDER_TC.ordinal(), extraInfo,
            rightParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> clientPrpElementPayload = rpc.receive(clientPrpElementHeader).getPayload();
        MpcAbortPreconditions.checkArgument(clientPrpElementPayload.size() == clientElementSize);

        stopWatch.start();
        // aider computes the intersection and returns it to all the parties
        Stream<byte[]> serverPrpElementStream = serverPrpElementPayload.stream();
        serverPrpElementStream = parallel ? serverPrpElementStream.parallel() : serverPrpElementStream;
        Set<ByteBuffer> serverPrpElementSet = serverPrpElementStream
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Stream<byte[]> clientPrpElementStream = clientPrpElementPayload.stream();
        clientPrpElementStream = parallel ? clientPrpElementStream.parallel() : clientPrpElementStream;
        List<byte[]> intersectionPrpPayload = clientPrpElementStream
            .map(ByteBuffer::wrap)
            .filter(serverPrpElementSet::contains)
            .map(ByteBuffer::array)
            .collect(Collectors.toList());
        // send to the server
        DataPacketHeader serverIntersectionPrpElementHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.AIDER_TO_SERVER_T_I.ordinal(), extraInfo,
            ownParty().getPartyId(), leftParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverIntersectionPrpElementHeader, intersectionPrpPayload));
        // send to the client
        DataPacketHeader clientIntersectionPrpElementHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.AIDER_TO_CLIENT_T_I.ordinal(), extraInfo,
            ownParty().getPartyId(), rightParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientIntersectionPrpElementHeader, intersectionPrpPayload));
        stopWatch.stop();
        long intersectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, intersectionTime);

        logPhaseInfo(PtoState.PTO_END);
    }
}
