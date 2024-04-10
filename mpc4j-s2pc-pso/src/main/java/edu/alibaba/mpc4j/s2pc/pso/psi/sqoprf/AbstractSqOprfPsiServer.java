package edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.structure.filter.Filter;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfSender;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.SqOprfPsiPtoDesc.PtoStep;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * abstract sq-OPRF-based PSI server.
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
public abstract class AbstractSqOprfPsiServer<T> extends AbstractPsiServer<T> {
    /**
     * sq-OPRF sender
     */
    private final SqOprfSender sqOprfSender;
    /**
     * filter type
     */
    private final FilterType filterType;
    /**
     * sq-OPRF key
     */
    private SqOprfKey key;

    public AbstractSqOprfPsiServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, SqOprfPsiConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
        sqOprfSender = SqOprfFactory.createSender(serverRpc, clientParty, config.getSqOprfConfig());
        addSubPto(sqOprfSender);
        filterType = config.getFilterType();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        key = sqOprfSender.keyGen();
        sqOprfSender.init(maxClientElementSize, key);
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
        int peqtByteLength = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        Hash peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        stopWatch.stop();
        long setupTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, setupTime, "Server inits tools");

        stopWatch.start();
        sqOprfSender.oprf(clientElementSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, oprfTime, "Server runs sq-OPRF");

        stopWatch.start();
        Stream<T> serverElementStream = serverElementArrayList.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
        List<byte[]> serverPrfs = serverElementStream
            .map(x -> {
                byte[] xBytes = ObjectUtils.objectToByteArray(x);
                byte[] prf = key.getPrf(xBytes);
                return peqtHash.digestToBytes(prf);
            })
            .collect(Collectors.toList());
        Collections.shuffle(serverPrfs, secureRandom);
        Filter<byte[]> serverPrfFilter = FilterFactory.load(envType, filterType, serverElementSize, secureRandom);
        serverPrfs.forEach(serverPrfFilter::put);
        List<byte[]> serverPrfFilterPayload = serverPrfFilter.save();
        DataPacketHeader serverPrfHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRF_FILTER.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPrfHeader, serverPrfFilterPayload));
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverPrfTime, "Server computes PRFs");

        logPhaseInfo(PtoState.PTO_END);
    }
}
