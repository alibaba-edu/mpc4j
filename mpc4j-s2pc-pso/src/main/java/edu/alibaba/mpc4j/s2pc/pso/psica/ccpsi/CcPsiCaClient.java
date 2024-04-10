package edu.alibaba.mpc4j.s2pc.pso.psica.ccpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.HammingFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.HammingParty;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiClientOutput;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.psica.AbstractPsiCaClient;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * client-payload circuit-PSI Cardinality client.
 *
 * @author Qixian Zhou
 * @date 2023/4/24
 */
public class CcPsiCaClient<T> extends AbstractPsiCaClient<T> {
    /**
     * client-payload circuit-PSI client
     */
    private final CcpsiClient<T> ccpsiClient;
    /**
     * client-payload circuit PSI config
     */
    private final CcpsiConfig ccpsiConfig;
    /**
     * Hamming Sender
     */
    private final HammingParty hammingReceiver;

    public CcPsiCaClient(Rpc serverRpc, Party clientParty, CcPsiCaConfig config) {
        super(CcPsiCaPtoDesc.getInstance(), serverRpc, clientParty, config);
        ccpsiConfig = config.getCcpsiConfig();
        ccpsiClient = CcpsiFactory.createClient(serverRpc, clientParty, ccpsiConfig);
        addSubPto(ccpsiClient);
        hammingReceiver = HammingFactory.createReceiver(serverRpc, clientParty, config.getHammingConfig());
        addSubPto(hammingReceiver);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        ccpsiClient.init(maxClientElementSize, maxServerElementSize);
        hammingReceiver.init(ccpsiConfig.getOutputBitNum(maxServerElementSize, maxClientElementSize));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public int psiCardinality(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        CcpsiClientOutput<T> ccpsiClientOutput = ccpsiClient.psi(clientElementSet, serverElementSize);
        stopWatch.stop();
        long psiTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, psiTime);

        stopWatch.start();
        int cardinality = hammingReceiver.receiveHammingDistance(ccpsiClientOutput.getZ1());
        stopWatch.stop();
        long hammingTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, hammingTime);

        logPhaseInfo(PtoState.PTO_END);
        return cardinality;
    }
}