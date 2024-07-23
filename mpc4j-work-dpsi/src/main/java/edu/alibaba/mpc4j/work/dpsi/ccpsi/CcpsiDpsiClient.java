package edu.alibaba.mpc4j.work.dpsi.ccpsi;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.work.dpsi.AbstractDpsiClient;
import edu.alibaba.mpc4j.work.dpsi.ccpsi.CcpsiDpsiPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiClientOutput;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * DPSI based on client-payload circuit PSI client.
 *
 * @author Yufei Wang, Weiran Liu
 * @date 2023/8/15
 */
public class CcpsiDpsiClient<T> extends AbstractDpsiClient<T> {
    /**
     * CCPSI
     */
    private final CcpsiClient<T> ccpsiClient;

    public CcpsiDpsiClient(Rpc clientRpc, Party serverParty, CcpsiDpsiConfig config) {
        super(CcpsiDpsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        ccpsiClient = CcpsiFactory.createClient(clientRpc, serverParty, config.getCcpsiConfig());
        addSubPto(ccpsiClient);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        ccpsiClient.init(maxClientElementSize, maxServerElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        CcpsiClientOutput<T> ccpsiClientOutput = ccpsiClient.psi(clientElementSet, serverElementSize);
        stopWatch.stop();
        long ccpsiTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, ccpsiTime, "Client runs ccpsi");

        DataPacketHeader randomizedServerVectorHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RANDOMIZED_VECTOR.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> randomizedServerVectorPayload = rpc.receive(randomizedServerVectorHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(randomizedServerVectorPayload.size() == 1);
        SquareZ2Vector actualClientVector = ccpsiClientOutput.getZ1();
        int bitNum = actualClientVector.bitNum();
        BitVector randomizedServerVector = BitVectorFactory.create(bitNum, randomizedServerVectorPayload.get(0));
        BitVector z = randomizedServerVector.xor(actualClientVector.getBitVector());
        // compute the intersection
        ArrayList<T> table = ccpsiClientOutput.getTable();
        Set<T> intersection = IntStream.range(0, bitNum)
            .mapToObj(i -> z.get(i) ? table.get(i) : null)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        stopWatch.stop();
        long intersectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, intersectionTime, "Client gets intersection");

        return intersection;
    }
}
