package edu.alibaba.mpc4j.s2pc.pso.psi.aid.kmrs14;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyAidPto;
import edu.alibaba.mpc4j.s2pc.pso.psi.aid.kmrs14.Kmrs14AidPsiPtoDesc.PtoStep;

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
public class Kmrs14AidPsiAider extends AbstractTwoPartyAidPto {

    public Kmrs14AidPsiAider(Rpc aidRpc, Party serverParty, Party clientParty, Kmrs14AidPsiConfig config) {
        super(Kmrs14AidPsiPtoDesc.getInstance(), aidRpc, serverParty, clientParty, config);
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);
        // empty
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void aid() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        // P1 sends T1 = π_1(F_K(S_1)) to the aider
        List<byte[]> serverPrpElementPayload = receiveLeftPartyPayload(PtoStep.SERVER_TO_AIDER_TS.ordinal());
        // P2 sends T2 = π_2(F_K(S_2)) to the aider
        List<byte[]> clientPrpElementPayload = receiveRightPartyPayload(PtoStep.CLIENT_TO_AIDER_TC.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(serverPrpElementPayload.size() > 0);
        MpcAbortPreconditions.checkArgument(clientPrpElementPayload.size() > 0);
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
        // send to the client
        sendRightPartyPayload(PtoStep.AIDER_TO_CLIENT_T_I.ordinal(), intersectionPrpPayload);
        stopWatch.stop();
        long intersectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, intersectionTime);

        logPhaseInfo(PtoState.PTO_END);
    }
}
