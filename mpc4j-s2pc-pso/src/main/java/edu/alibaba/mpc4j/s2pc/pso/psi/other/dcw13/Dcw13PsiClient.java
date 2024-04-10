package edu.alibaba.mpc4j.s2pc.pso.psi.other.dcw13;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.DistinctGbfUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.structure.filter.DistinctBloomFilter;
import edu.alibaba.mpc4j.common.structure.filter.Filter;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.DistinctGbfGf2eDokvs;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.dcw13.Dcw13PsiPtoDesc.PtoStep;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * DCW13-PSI client.
 *
 * @author Weiran Liu
 * @date 2023/9/18
 */
public class Dcw13PsiClient<T> extends AbstractPsiClient<T> {
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * GBF keys
     */
    private byte[] gbfKey;

    public Dcw13PsiClient(Rpc clientRpc, Party serverParty, Dcw13PsiConfig config) {
        super(Dcw13PsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(clientRpc, serverParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // both parties need to insert elements into (G)BF.
        int maxN = Math.max(maxServerElementSize, maxClientElementSize);
        int maxM = DistinctGbfUtils.getM(maxN);
        int maxBfM = DistinctBloomFilter.bitSize(maxN);
        assert maxM == maxBfM : "GBF max(M) must be equal to BF max(M) (" + maxBfM + "): " + maxM;
        // init GBF key
        gbfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(gbfKey);
        List<byte[]> gbfKeyPayload = Collections.singletonList(gbfKey);
        DataPacketHeader gbfKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_GBF_KEY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(gbfKeyHeader, gbfKeyPayload));
        // init core COT
        coreCotReceiver.init(maxM);
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
        // init PEQT hash
        int peqtByteLength = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        Hash peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        int n = Math.max(serverElementSize, clientElementSize);
        int m = DistinctBloomFilter.bitSize(n);
        DistinctGbfGf2eDokvs<T> gbf = new DistinctGbfGf2eDokvs<>(
            envType, n, CommonConstants.BLOCK_BIT_LENGTH, gbfKey, secureRandom
        );
        gbf.setParallelEncode(parallel);
        // init BF
        DistinctBloomFilter<T> distinctBloomFilter = DistinctBloomFilter.create(envType, n, gbfKey);
        clientElementSet.forEach(distinctBloomFilter::put);
        BitVector bitVector = BitVectorFactory.create(m, distinctBloomFilter.getStorage());
        boolean[] choices = new boolean[m];
        IntStream storageIndexIntStream = IntStream.range(0, m);
        storageIndexIntStream = parallel ? storageIndexIntStream.parallel() : storageIndexIntStream;
        storageIndexIntStream.forEach(i -> choices[i] = bitVector.get(i));
        stopWatch.stop();
        long setupTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, setupTime, "Client inits tools");

        stopWatch.start();
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(choices);
        RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfType.MMO, cotReceiverOutput);
        stopWatch.stop();
        long rotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, rotTime);

        stopWatch.start();
        byte[][] storage = rotReceiverOutput.getRbArray();
        IntStream oprfIndexIntStream = IntStream.range(0, clientElementSize);
        oprfIndexIntStream = parallel ? oprfIndexIntStream.parallel() : oprfIndexIntStream;
        List<byte[]> clientOprfArrayList = oprfIndexIntStream
            .mapToObj(index -> gbf.decode(storage, clientElementArrayList.get(index)))
            .collect(Collectors.toCollection(ArrayList::new));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, oprfTime, "Client computes OPRFs");

        DataPacketHeader serverPrfFilterHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRF_FILTER.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverPrfFilterPayload = rpc.receive(serverPrfFilterHeader).getPayload();

        stopWatch.start();
        Filter<byte[]> serverPrfFilter = FilterFactory.load(envType, serverPrfFilterPayload);
        Set<T> intersection = IntStream.range(0, clientElementSize)
            .mapToObj(elementIndex -> {
                T y = clientElementArrayList.get(elementIndex);
                byte[] oprf = clientOprfArrayList.get(elementIndex);
                return serverPrfFilter.mightContain(peqtHash.digestToBytes(oprf)) ? y : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, serverPrfTime, "Client computes intersection");

        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }
}
