package edu.alibaba.mpc4j.work.payable.psi.baseline;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psica.PsiCaClient;
import edu.alibaba.mpc4j.s2pc.pso.psica.PsiCaFactory;
import edu.alibaba.mpc4j.work.payable.psi.AbstractPayablePsiServer;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.mpc4j.work.payable.psi.zlp24.Zlp24PayablePsiPtoDesc.getInstance;

/**
 * Baseline payable PSI server.
 *
 * @author Liqiang Peng
 * @date 2024/7/1
 */
public class BaselinePayablePsiServer extends AbstractPayablePsiServer {

    /**
     * PSI-CA client
     */
    private final PsiCaClient<ByteBuffer> psiCaClient;
    /**
     * PSI server
     */
    private final PsiServer<ByteBuffer> psiServer;

    public BaselinePayablePsiServer(Rpc serverRpc, Party clientParty, BaselinePayablePsiConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
        psiCaClient = PsiCaFactory.createClient(serverRpc, clientParty, config.getPsiCaConfig());
        addSubPto(psiCaClient);
        psiServer = PsiFactory.createServer(serverRpc, clientParty, config.getPsiConfig());
        addSubPto(psiServer);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        psiCaClient.init(maxServerElementSize, maxClientElementSize);
        psiServer.init(maxServerElementSize, maxClientElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public int payablePsi(Set<ByteBuffer> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int intersectionSetSize = psiCaClient.psiCardinality(serverElementSet, clientElementSize);
        stopWatch.stop();
        long psiCaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, psiCaTime, "Server executes PSI-CA");

        stopWatch.start();
        psiServer.psi(serverElementSet, clientElementSize);
        stopWatch.stop();
        long psiTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, psiTime, "Server executes PSI");

        logPhaseInfo(PtoState.PTO_END);
        return intersectionSetSize;
    }
}