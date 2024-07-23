package edu.alibaba.mpc4j.s2pc.pso.psica.gmr21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtServer;
import edu.alibaba.mpc4j.s2pc.pso.psica.AbstractPsiCaServer;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * GMR21-PSI-CA server.
 *
 * @author Weiran Liu, Liqiang Peng
 * @date 2022/02/15
 */
public class Gmr21PsiCaServer<T> extends AbstractPsiCaServer<T> {
    /**
     * GMR21-mqRPMT
     */
    private final Gmr21MqRpmtServer gmr21MqRpmtServer;

    public Gmr21PsiCaServer(Rpc serverRpc, Party clientParty, Gmr21PsiCaConfig config) {
        super(Gmr21PsiCaPtoDesc.getInstance(), serverRpc, clientParty, config);
        gmr21MqRpmtServer = new Gmr21MqRpmtServer(serverRpc, clientParty, config.getGmr21MqRpmtConfig());
        addSubPto(gmr21MqRpmtServer);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        gmr21MqRpmtServer.init(maxServerElementSize, maxClientElementSize);
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
        Set<ByteBuffer> serverElementByteBufferSet = serverElementSet.stream()
            .map(ObjectUtils::objectToByteArray)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        gmr21MqRpmtServer.mqRpmt(serverElementByteBufferSet, clientElementSize);
        stopWatch.stop();
        long mqRpmtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, mqRpmtTime);

        logPhaseInfo(PtoState.PTO_END);
    }
}
