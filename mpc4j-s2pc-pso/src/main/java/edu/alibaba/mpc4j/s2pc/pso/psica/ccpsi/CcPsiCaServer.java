package edu.alibaba.mpc4j.s2pc.pso.psica.ccpsi;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.HammingFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.HammingParty;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psica.AbstractPsiCaServer;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * client-payload circuit-PSI Cardinality Server.
 *
 * @author Qixian Zhou
 * @date 2023/4/24
 */
public class CcPsiCaServer<T> extends AbstractPsiCaServer<T> {
    /**
     * client-payload circuit PSI server
     */
    private final CcpsiServer<T> ccpsiServer;
    /**
     * client-payload circuit PSI config
     */
    private final CcpsiConfig ccpsiConfig;
    /**
     * Hamming Sender
     */
    private final HammingParty hammingSender;

    public CcPsiCaServer(Rpc serverRpc, Party clientParty, CcPsiCaConfig config) {
        super(CcPsiCaPtoDesc.getInstance(), serverRpc, clientParty, config);
        ccpsiConfig = config.getCcpsiConfig();
        ccpsiServer = CcpsiFactory.createServer(serverRpc, clientParty, ccpsiConfig);
        addSubPto(ccpsiServer);
        hammingSender = HammingFactory.createSender(serverRpc, clientParty, config.getHammingConfig());
        addSubPto(hammingSender);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        ccpsiServer.init(maxServerElementSize, maxClientElementSize);
        hammingSender.init(ccpsiConfig.getOutputBitNum(maxServerElementSize, maxClientElementSize));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psiCardinality(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        SquareZ2Vector ccpsiServerOutput = ccpsiServer.psi(serverElementSet, clientElementSize);
        stopWatch.stop();
        long psiTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, psiTime);

        stopWatch.start();
        hammingSender.sendHammingDistance(ccpsiServerOutput);
        stopWatch.stop();
        long hammingTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();

        logStepInfo(PtoState.PTO_STEP, 2, 2, hammingTime);
        logPhaseInfo(PtoState.PTO_END);
    }
}