package edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.structure.filter.Filter;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.MpOprfPsiPtoDesc.PtoStep;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * abstract mp-OPRF-based PSI server.
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
public abstract class AbstractMpOprfPsiServer<T> extends AbstractPsiServer<T> {
    /**
     * mp-OPRF sender
     */
    private final MpOprfSender mpOprfSender;
    /**
     * filter type
     */
    private final FilterType filterType;
    /**
     * PEQT hash
     */
    private Hash peqtHash;
    /**
     * SecurityModel
     */
    private final SecurityModel securityModel;

    public AbstractMpOprfPsiServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, MpOprfPsiConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
        securityModel = config.getSecurityModel();
        mpOprfSender = OprfFactory.createMpOprfSender(serverRpc, clientParty, config.getMpOprfConfig());
        addSubPto(mpOprfSender);
        filterType = config.getFilterType();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        mpOprfSender.init(maxClientElementSize);
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
        int peqtByteLength = securityModel.equals(SecurityModel.MALICIOUS) | securityModel.equals(SecurityModel.COVERT) ?
            PsiUtils.getMaliciousPeqtByteLength(serverElementSize, clientElementSize) :
            PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        stopWatch.stop();
        long prepareInputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, prepareInputTime, "Server prepares tools");

        stopWatch.start();
        MpOprfSenderOutput mpOprfSenderOutput = mpOprfSender.oprf(clientElementSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, oprfTime, "Server runs mp-OPRFs");

        stopWatch.start();
        Stream<T> serverElementStream = serverElementArrayList.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
        List<byte[]> serverPrfs = serverElementStream
            .map(element -> {
                byte[] elementByteArray = ObjectUtils.objectToByteArray(element);
                byte[] prf = mpOprfSenderOutput.getPrf(elementByteArray);
                return peqtHash.digestToBytes(prf);
            })
            .collect(Collectors.toList());
        Collections.shuffle(serverPrfs, secureRandom);
        // construct the filter
        Filter<byte[]> serverPrfFilter = FilterFactory.load(envType, filterType, serverElementSize, secureRandom);
        serverPrfs.forEach(serverPrfFilter::put);
        List<byte[]> serverPrfFilterPayload = serverPrfFilter.save();
        DataPacketHeader serverPrfFilterHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRFS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPrfFilterHeader, serverPrfFilterPayload));
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverPrfTime, "Server sends PRF filter");

        logPhaseInfo(PtoState.PTO_END);
    }
}
