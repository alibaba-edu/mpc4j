package edu.alibaba.mpc4j.work.dpsi.ccpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.dp.ldp.nominal.binary.BinaryLdp;
import edu.alibaba.mpc4j.dp.ldp.nominal.binary.BinaryLdpFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import edu.alibaba.mpc4j.work.dpsi.AbstractDpsiServer;
import edu.alibaba.mpc4j.work.dpsi.ccpsi.CcpsiDpsiPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiServer;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * DPSI based on client-payload circuit PSI server.
 *
 * @author Yufei Wang, Weiran Liu
 * @date 2023/8/15
 */
public class CcpsiDpsiServer<T> extends AbstractDpsiServer<T> {
    /**
     * CCPSI
     */
    private final CcpsiServer<T> ccpsiServer;
    /**
     * LDP encoding
     */
    private final BinaryLdp binaryLdp;

    public CcpsiDpsiServer(Rpc serverRpc, Party clientParty, CcpsiDpsiConfig config) {
        super(CcpsiDpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        ccpsiServer = CcpsiFactory.createServer(serverRpc, clientParty, config.getCcpsiConfig());
        addSubPto(ccpsiServer);
        binaryLdp = BinaryLdpFactory.createInstance(config.getBinaryLdpConfig());
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        ccpsiServer.init(maxServerElementSize, maxClientElementSize);
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
        SquareZ2Vector ccpsiServerOutput = ccpsiServer.psi(serverElementSet, clientElementSize);
        stopWatch.stop();
        long ccpsiTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, ccpsiTime, "Server runs ccpsi");

        stopWatch.start();
        // Random Response
        int bitNum = ccpsiServerOutput.bitNum();
        BitVector actualServerVector = ccpsiServerOutput.getBitVector();
        BitVector randomServerVector = BitVectorFactory.createZeros(bitNum);
        IntStream.range(0, bitNum).forEach(index ->
            randomServerVector.set(index, binaryLdp.randomize(actualServerVector.get(index)))
        );
        List<byte[]> randomizedServerVectorPayload = Collections.singletonList(randomServerVector.getBytes());
        DataPacketHeader randomizedServerVectorHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RANDOMIZED_VECTOR.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(randomizedServerVectorHeader, randomizedServerVectorPayload));
        stopWatch.stop();
        long ldpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, ldpTime);

        logPhaseInfo(PtoState.PTO_END);
    }
}
