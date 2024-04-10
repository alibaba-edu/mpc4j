package edu.alibaba.mpc4j.s2pc.pso.psi.other.dcw13;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.DistinctGbfUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.structure.filter.Filter;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.DistinctGbfGf2eDokvs;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.dcw13.Dcw13PsiPtoDesc.PtoStep;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DCW13-PSI server.
 *
 * @author Weiran Liu
 * @date 2023/9/18
 */
public class Dcw13PsiServer<T> extends AbstractPsiServer<T> {
    /**
     * core LOT sender
     */
    private final CoreCotSender coreCotSender;
    /**
     * filter type
     */
    private final FilterType filterType;
    /**
     * (Garbled) Bloom Filter keys
     */
    private byte[] gbfKey;

    public Dcw13PsiServer(Rpc serverRpc, Party clientParty, Dcw13PsiConfig config) {
        super(Dcw13PsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        coreCotSender = CoreCotFactory.createSender(serverRpc, clientParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        filterType = config.getFilterType();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // both parties need to insert elements into (G)BF.
        int maxN = Math.max(maxServerElementSize, maxClientElementSize);
        int maxM = DistinctGbfUtils.getM(maxN);
        // init GBF key
        DataPacketHeader gbfKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_GBF_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> gbfKeyPayload = rpc.receive(gbfKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(gbfKeyPayload.size() == DistinctGbfUtils.HASH_KEY_NUM);
        gbfKey = gbfKeyPayload.get(0);
        // init core COT
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        coreCotSender.init(delta, maxM);
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
        // init PEQT hash
        int peqtByteLength = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        Hash peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        // init GBF
        int n = Math.max(serverElementSize, clientElementSize);
        int m = DistinctGbfUtils.getM(n);
        DistinctGbfGf2eDokvs<T> gbf = new DistinctGbfGf2eDokvs<>(
            envType, n, CommonConstants.BLOCK_BIT_LENGTH, gbfKey, secureRandom
        );
        gbf.setParallelEncode(parallel);
        stopWatch.stop();
        long setupTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, setupTime, "Server inits tools");

        stopWatch.start();
        CotSenderOutput cotSenderOutput = coreCotSender.send(m);
        RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfType.MMO, cotSenderOutput);
        stopWatch.stop();
        long rotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, rotTime, "Server runs ROT");

        stopWatch.start();
        byte[][] storage = rotSenderOutput.getR1Array();
        Stream<T> serverElementStream = serverElementArrayList.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
        List<byte[]> serverPrfs = serverElementStream
            .map(x -> peqtHash.digestToBytes(gbf.decode(storage, x)))
            .collect(Collectors.toList());
        Collections.shuffle(serverPrfs, secureRandom);
        Filter<byte[]> serverPrfFilter = FilterFactory.load(envType, filterType, serverElementSize, secureRandom);
        serverPrfs.forEach(serverPrfFilter::put);
        List<byte[]> serverPrfFilterPayload = serverPrfFilter.save();
        DataPacketHeader serverPrfFilterHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRF_FILTER.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPrfFilterHeader, serverPrfFilterPayload));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, oprfTime);

        logPhaseInfo(PtoState.PTO_END);
    }
}
