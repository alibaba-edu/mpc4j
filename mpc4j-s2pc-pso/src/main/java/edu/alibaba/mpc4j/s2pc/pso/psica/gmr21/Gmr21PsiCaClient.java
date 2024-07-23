package edu.alibaba.mpc4j.s2pc.pso.psica.gmr21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtClient;
import edu.alibaba.mpc4j.s2pc.pso.psica.AbstractPsiCaClient;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GMR21-PSI-CA client.
 *
 * @author Weiran Liu, Liqiang Peng
 * @date 2022/02/15
 */
public class Gmr21PsiCaClient<T> extends AbstractPsiCaClient<T> {
    /**
     * GMR21-mqRPMT server
     */
    private final Gmr21MqRpmtClient gmr21MqRpmtClient;

    public Gmr21PsiCaClient(Rpc clientRpc, Party serverParty, Gmr21PsiCaConfig config) {
        super(Gmr21PsiCaPtoDesc.getInstance(), clientRpc, serverParty, config);
        gmr21MqRpmtClient = new Gmr21MqRpmtClient(clientRpc, serverParty, config.getGmr21MqRpmtConfig());
        addSubPto(gmr21MqRpmtClient);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        gmr21MqRpmtClient.init(maxClientElementSize, maxServerElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public int psiCardinality(Set<T> clientElementSet, int serverElementSize)
        throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        Set<ByteBuffer> clientElementByteBufferSet = clientElementSet.stream()
            .map(ObjectUtils::objectToByteArray)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        boolean[] choices = gmr21MqRpmtClient.mqRpmt(clientElementByteBufferSet, serverElementSize);
        stopWatch.stop();
        long mqRpmtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, mqRpmtTime);

        logPhaseInfo(PtoState.PTO_END);
        return (int) IntStream.range(0, choices.length).filter(i -> choices[i]).count();
    }
}
