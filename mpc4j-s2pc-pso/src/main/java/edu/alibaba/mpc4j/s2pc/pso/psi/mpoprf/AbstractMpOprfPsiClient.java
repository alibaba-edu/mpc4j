package edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.structure.filter.Filter;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.MpOprfPsiPtoDesc.PtoStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * abstract mp-OPRF PSI client.
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
public abstract class AbstractMpOprfPsiClient<T> extends AbstractPsiClient<T> {
    /**
     * mp-OPRF receiver
     */
    private final MpOprfReceiver mpOprfReceiver;
    /**
     * PEQT hash
     */
    private Hash peqtHash;
    /**
     * SecurityModel
     */
    private final SecurityModel securityModel;

    public AbstractMpOprfPsiClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, MpOprfPsiConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
        securityModel = config.getSecurityModel();
        mpOprfReceiver = OprfFactory.createMpOprfReceiver(clientRpc, serverParty, config.getMpOprfConfig());
        addSubPto(mpOprfReceiver);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        mpOprfReceiver.init(maxClientElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet, int serverSetSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverSetSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int peqtByteLength = securityModel.equals(SecurityModel.MALICIOUS) | securityModel.equals(SecurityModel.COVERT) ?
            PsiUtils.getMaliciousPeqtByteLength(serverElementSize, clientElementSize) :
            PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        byte[][] clientElementByteArrays = clientElementArrayList.stream()
            .map(ObjectUtils::objectToByteArray)
            .toArray(byte[][]::new);
        stopWatch.stop();
        long prepareInputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, prepareInputTime, "Client prepares tools and inputs");

        stopWatch.start();
        MpOprfReceiverOutput mpOprfReceiverOutput = mpOprfReceiver.oprf(clientElementByteArrays);
        IntStream clientElementIndexIntStream = IntStream.range(0, clientElementSize);
        clientElementIndexIntStream = parallel ? clientElementIndexIntStream.parallel() : clientElementIndexIntStream;
        ArrayList<byte[]> clientOprfArrayList = clientElementIndexIntStream
            .mapToObj(index -> peqtHash.digestToBytes(mpOprfReceiverOutput.getPrf(index)))
            .collect(Collectors.toCollection(ArrayList::new));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, oprfTime, "Client runs OPRFs and hash outputs");

        DataPacketHeader serverPrfFilterHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRFS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverPrfFilterPayload = rpc.receive(serverPrfFilterHeader).getPayload();

        stopWatch.start();
        Filter<byte[]> serverPrfFilter = FilterFactory.load(envType, serverPrfFilterPayload);
        Set<T> intersection = IntStream.range(0, clientElementSize)
            .mapToObj(elementIndex -> {
                T element = clientElementArrayList.get(elementIndex);
                byte[] elementPrf = clientOprfArrayList.get(elementIndex);
                return serverPrfFilter.mightContain(elementPrf) ? element : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        stopWatch.stop();
        long intersectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, intersectionTime, "Client computes the intersection");

        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }
}
