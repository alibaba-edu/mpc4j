package edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.structure.filter.Filter;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.SqOprfPsiPtoDesc.PtoStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * abstract sq-OPRF-based PSI client.
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
public abstract class AbstractSqOprfPsiClient<T> extends AbstractPsiClient<T> {
    /**
     * sq-OPRF receiver
     */
    private final SqOprfReceiver sqOprfReceiver;

    public AbstractSqOprfPsiClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, SqOprfPsiConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
        sqOprfReceiver = SqOprfFactory.createReceiver(clientRpc, serverParty, config.getSqOprfConfig());
        addSubPto(sqOprfReceiver);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        sqOprfReceiver.init(maxClientElementSize);
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
        int peqtByteLength = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        Hash peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        byte[][] clientElementByteArrays = clientElementArrayList.stream()
            .map(ObjectUtils::objectToByteArray)
            .toArray(byte[][]::new);
        stopWatch.stop();
        long setupTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, setupTime, "Client inits tools");

        stopWatch.start();
        SqOprfReceiverOutput oprfReceiverOutput = sqOprfReceiver.oprf(clientElementByteArrays);
        IntStream oprfIndexIntStream = IntStream.range(0, clientElementSize);
        oprfIndexIntStream = parallel ? oprfIndexIntStream.parallel() : oprfIndexIntStream;
        ArrayList<byte[]> clientOprfArrayList = oprfIndexIntStream
            .mapToObj(index -> peqtHash.digestToBytes(oprfReceiverOutput.getPrf(index)))
            .collect(Collectors.toCollection(ArrayList::new));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, oprfTime, "Client runs OPRF");

        DataPacketHeader serverPrfFilterHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRF_FILTER.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverPrfPayload = rpc.receive(serverPrfFilterHeader).getPayload();

        stopWatch.start();
        Filter<byte[]> serverPrfFilter = FilterFactory.load(envType, serverPrfPayload);
        Set<T> intersection = IntStream.range(0, clientElementSize)
            .mapToObj(elementIndex -> {
                T element = clientElementArrayList.get(elementIndex);
                byte[] elementPrf = clientOprfArrayList.get(elementIndex);
                return serverPrfFilter.mightContain(elementPrf) ? element : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverPrfTime, "Client computes intersection");

        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }
}
