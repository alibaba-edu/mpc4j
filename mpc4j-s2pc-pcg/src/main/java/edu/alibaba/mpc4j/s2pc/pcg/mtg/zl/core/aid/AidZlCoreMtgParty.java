package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.aid;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyPto;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pcg.aid.TrustDealPtoDesc;
import edu.alibaba.mpc4j.s2pc.pcg.aid.TrustDealType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlTriple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgParty;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * aid Zl core multiplication triple generation sender.
 *
 * @author Weiran Liu
 * @date 2023/6/14
 */
public class AidZlCoreMtgParty extends AbstractThreePartyPto implements ZlCoreMtgParty {
    /**
     * config
     */
    private final AidZlCoreMtgConfig config;
    /**
     * Zl instance
     */
    private final Zl zl;
    /**
     * max num
     */
    private int maxNum;

    public AidZlCoreMtgParty(Rpc ownRpc, Party otherParty, Party aiderParty, AidZlCoreMtgConfig config) {
        super(TrustDealPtoDesc.getInstance(), ownRpc, otherParty, aiderParty, config);
        this.config = config;
        zl = config.getZl();
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init aider
        int aidTypeIndex = TrustDealType.ZL_TRIPLE.ordinal();
        List<byte[]> initQueryPayload = new LinkedList<>();
        initQueryPayload.add(IntUtils.intToByteArray(aidTypeIndex));
        initQueryPayload.add(IntUtils.intToByteArray(zl.getL()));
        DataPacketHeader initQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), TrustDealPtoDesc.AidPtoStep.INIT_QUERY.ordinal(), extraInfo,
            ownParty().getPartyId(), rightParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(initQueryHeader, initQueryPayload));
        stopWatch.stop();
        long initQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initQueryTime);

        DataPacketHeader initResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), TrustDealPtoDesc.AidPtoStep.INIT_RESPONSE.ordinal(), extraInfo,
            rightParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> initResponsePayload = rpc.receive(initResponseHeader).getPayload();

        stopWatch.start();
        // handle init response
        MpcAbortPreconditions.checkArgument(initResponsePayload.size() == 0);
        stopWatch.stop();
        long initResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, initResponseTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    private void setInitInput(int maxNum) {
        MathPreconditions.checkPositiveInRangeClosed("maxNum", maxNum, config.maxNum());
        this.maxNum = maxNum;
        initState();
    }

    @Override
    public ZlTriple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> requestQueryPayload = new LinkedList<>();
        requestQueryPayload.add(IntUtils.intToByteArray(num));
        DataPacketHeader requestQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), TrustDealPtoDesc.AidPtoStep.REQUEST_QUERY.ordinal(), extraInfo,
            ownParty().getPartyId(), rightParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(requestQueryHeader, requestQueryPayload));
        stopWatch.stop();
        long requestQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, requestQueryTime);

        DataPacketHeader requestResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), TrustDealPtoDesc.AidPtoStep.REQUEST_RESPONSE.ordinal(), extraInfo,
            rightParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> requestResponsePayload = rpc.receive(requestResponseHeader).getPayload();

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
        return ZlTriple.create(zl, num, aiArray, biArray, ciArray);
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        extraInfo++;
    }

    @Override
    public void destroy() {
        // destroy request
        DataPacketHeader destroyQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), TrustDealPtoDesc.AidPtoStep.DESTROY_QUERY.ordinal(), extraInfo,
            ownParty().getPartyId(), rightParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(destroyQueryHeader,  new LinkedList<>()));
        // destroy response
        DataPacketHeader destroyResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), TrustDealPtoDesc.AidPtoStep.DESTROY_RESPONSE.ordinal(), extraInfo,
            rightParty().getPartyId(), ownParty().getPartyId()
        );
        rpc.receive(destroyResponseHeader);
        super.destroy();
    }
}
