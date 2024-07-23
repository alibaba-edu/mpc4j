package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.aided;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.TrustDealerPtoDesc;
import edu.alibaba.mpc4j.s2pc.aby.pcg.TrustDealerPtoStep;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.ZlTriple;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.AbstractZlTripleGenParty;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Aided Zl triple generation party.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
public class AidedZlTripleGenParty extends AbstractZlTripleGenParty {

    public AidedZlTripleGenParty(Rpc ownRpc, Party otherParty, Party aiderParty, AidedZlTripleGenConfig config) {
        super(TrustDealerPtoDesc.getInstance(), ownRpc, otherParty, aiderParty, config);
    }

    @Override
    public void init(int maxL, int expectTotalNum) throws MpcAbortException {
        setInitInput(maxL, expectTotalNum);
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
    public ZlTriple generate(Zl zl, int num) throws MpcAbortException {
        setPtoInput(zl, num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> requestQueryPayload = new LinkedList<>();
        requestQueryPayload.add(IntUtils.intToByteArray(zl.getL()));
        requestQueryPayload.add(IntUtils.intToByteArray(num));
        sendAidPartyPayload(TrustDealerPtoStep.REQUEST_ZL_TRIPLE.ordinal(), requestQueryPayload);
        stopWatch.stop();
        long requestQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, requestQueryTime);

        List<byte[]> requestResponsePayload = receiveAiderPayload(TrustDealerPtoStep.REQUEST_RESPONSE.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(requestResponsePayload.size() == 3);
        ByteBuffer aiBuffer = ByteBuffer.wrap(requestResponsePayload.remove(0));
        ByteBuffer biBuffer = ByteBuffer.wrap(requestResponsePayload.remove(0));
        ByteBuffer ciBuffer = ByteBuffer.wrap(requestResponsePayload.remove(0));
        // convert to (ai, bi, ci)
        int byteL = zl.getByteL();
        byte[] byteBufferArray = new byte[byteL];
        BigInteger[] aiArray = new BigInteger[num];
        for (int index = 0; index < num; index++) {
            aiBuffer.get(byteBufferArray);
            aiArray[index] = BigIntegerUtils.byteArrayToNonNegBigInteger(byteBufferArray);
        }
        BigInteger[] biArray = new BigInteger[num];
        for (int index = 0; index < num; index++) {
            biBuffer.get(byteBufferArray);
            biArray[index] = BigIntegerUtils.byteArrayToNonNegBigInteger(byteBufferArray);
        }
        BigInteger[] ciArray = new BigInteger[num];
        for (int index = 0; index < num; index++) {
            ciBuffer.get(byteBufferArray);
            ciArray[index] = BigIntegerUtils.byteArrayToNonNegBigInteger(byteBufferArray);
        }
        stopWatch.stop();
        long requestResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, requestResponseTime);

        logPhaseInfo(PtoState.PTO_END);
        return ZlTriple.create(zl, aiArray, biArray, ciArray);
    }

    @Override
    public void destroy() {
        // destroy request
        sendAidPartyPayload(TrustDealerPtoStep.DESTROY_QUERY.ordinal(), new LinkedList<>());
        // destroy response
        receiveAiderPayload(TrustDealerPtoStep.DESTROY_RESPONSE.ordinal());
        super.destroy();
    }
}
