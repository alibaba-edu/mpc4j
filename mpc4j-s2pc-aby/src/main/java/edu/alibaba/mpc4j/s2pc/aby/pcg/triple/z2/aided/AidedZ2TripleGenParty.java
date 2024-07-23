package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.aided;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.TrustDealerPtoDesc;
import edu.alibaba.mpc4j.s2pc.aby.pcg.TrustDealerPtoStep;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.Z2Triple;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.AbstractZ2TripleGenParty;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * aided Z2 triple generation party.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
public class AidedZ2TripleGenParty extends AbstractZ2TripleGenParty {

    public AidedZ2TripleGenParty(Rpc ownRpc, Party otherParty, Party aiderParty, AidedZ2TripleGenConfig config) {
        super(TrustDealerPtoDesc.getInstance(), ownRpc, otherParty, aiderParty, config);
    }

    @Override
    public void init(int expectTotalNum) throws MpcAbortException {
        setInitInput(expectTotalNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        sendAidPartyPayload(TrustDealerPtoStep.REGISTER_QUERY.ordinal(), new LinkedList<>());
        List<byte[]> registerResponsePayload = receiveAiderPayload(TrustDealerPtoStep.REGISTER_RESPONSE.ordinal());
        MpcAbortPreconditions.checkArgument(registerResponsePayload.isEmpty());
        stopWatch.stop();
        long registerTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, registerTime, "register");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Z2Triple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> requestQueryPayload = new LinkedList<>();
        requestQueryPayload.add(IntUtils.intToByteArray(num));
        sendAidPartyPayload(TrustDealerPtoStep.REQUEST_Z2_TRIPLE.ordinal(), requestQueryPayload);
        stopWatch.stop();
        long requestQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, requestQueryTime);

        List<byte[]> requestResponsePayload = receiveAiderPayload(TrustDealerPtoStep.REQUEST_RESPONSE.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(requestResponsePayload.size() == 3);
        byte[] a0 = requestResponsePayload.remove(0);
        byte[] b0 = requestResponsePayload.remove(0);
        byte[] c0 = requestResponsePayload.remove(0);
        stopWatch.stop();
        long requestResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, requestResponseTime);

        logPhaseInfo(PtoState.PTO_END);
        return Z2Triple.create(num, a0, b0, c0);
    }

    @Override
    public void destroy() {
        switch (partyState) {
            case NON_INITIALIZED:
            case INITIALIZED:
                // destroy request
                sendAidPartyPayload(TrustDealerPtoStep.DESTROY_QUERY.ordinal(), new LinkedList<>());
                // destroy response
                receiveAiderPayload(TrustDealerPtoStep.DESTROY_RESPONSE.ordinal());
                break;
            case DESTROYED:
                break;
            default:
                throw new IllegalStateException("Illegal state: " + partyState);
        }
        super.destroy();
    }
}
