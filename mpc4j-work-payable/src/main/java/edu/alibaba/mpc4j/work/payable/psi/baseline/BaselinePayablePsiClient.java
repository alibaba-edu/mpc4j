package edu.alibaba.mpc4j.work.payable.psi.baseline;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.psica.PsiCaFactory;
import edu.alibaba.mpc4j.s2pc.pso.psica.PsiCaServer;
import edu.alibaba.mpc4j.work.payable.psi.AbstractPayablePsiClient;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.mpc4j.work.payable.psi.baseline.BaselinePayablePsiPtoDesc.getInstance;

/**
 * Baseline payable PSI client.
 *
 * @author Liqiang Peng
 * @date 2024/7/1
 */
public class BaselinePayablePsiClient extends AbstractPayablePsiClient {

    /**
     * PSI-CA server
     */
    private final PsiCaServer<ByteBuffer> psiCaServer;
    /**
     * PSI client
     */
    private final PsiClient<ByteBuffer> psiClient;

    public BaselinePayablePsiClient(Rpc clientRpc, Party serverParty, BaselinePayablePsiConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        psiCaServer = PsiCaFactory.createServer(clientRpc, serverParty, config.getPsiCaConfig());
        addSubPto(psiCaServer);
        psiClient = PsiFactory.createClient(clientRpc, serverParty, config.getPsiConfig());
        addSubPto(psiClient);
    }


    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init PSI-CA server
        psiCaServer.init(maxClientElementSize, maxServerElementSize);
        // init PSI client
        psiClient.init(maxClientElementSize, maxServerElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<ByteBuffer> payablePsi(Set<ByteBuffer> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        psiCaServer.psiCardinality(clientElementSet, serverElementSize);
        stopWatch.stop();
        long psiCaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, psiCaTime, "Client executes PSI-CA");

        stopWatch.start();
        Set<ByteBuffer> intersectionSet = psiClient.psi(clientElementSet, serverElementSize);
        stopWatch.stop();
        long psiTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, psiTime, "Client executes PSI");

        return intersectionSet;
    }
}