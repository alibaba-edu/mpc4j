package edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.kkrt16;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.structure.filter.Filter;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.kkrt16.Kkrt16PsiPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * KKRT16-PSI server
 *
 * @author Weiran Liu
 * @date 2022/9/20
 */
public class Kkrt16PsiServer<T> extends AbstractPsiServer<T> {
    /**
     * OPRF sender
     */
    private final OprfSender oprfSender;
    /**
     * OPRF senderOutput
     */
    private OprfSenderOutput oprfSenderOutput;
    /**
     * The type of cuckoo hash
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * The number of hash functions
     */
    private final int cuckooHashNum;
    /**
     * The type of filter
     */
    private final FilterType filterType;
    /**
     * PEQT hash function
     */
    private Hash peqtHash;
    /**
     * The hash functions used in cuckoo hash
     */
    private Prf[] binHashes;
    /**
     * The bin number of cuckoo hash
     */
    private int binNum;

    public Kkrt16PsiServer(Rpc serverRpc, Party clientParty, Kkrt16PsiConfig config) {
        super(Kkrt16PsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        oprfSender = OprfFactory.createOprfSender(serverRpc, clientParty, config.getOprfConfig());
        addSubPto(oprfSender);
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
        filterType = config.getFilterType();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // batchSize = n + s
        int maxBatchSize = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientElementSize)
            + CuckooHashBinFactory.getStashSize(cuckooHashBinType, maxClientElementSize);
        int maxPrfNum = (CuckooHashBinFactory.getHashNum(cuckooHashBinType)
            + CuckooHashBinFactory.getStashSize(cuckooHashBinType, maxServerElementSize)) * maxServerElementSize;
        oprfSender.init(maxBatchSize, maxPrfNum);
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
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientElementSize);
        int stashSize = CuckooHashBinFactory.getStashSize(cuckooHashBinType, clientElementSize);
        // receiving the keys of cuckoo hash
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        handleCuckooHashKeyPayload(cuckooHashKeyPayload);
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, keyTime);

        stopWatch.start();
        oprfSenderOutput = oprfSender.oprf(binNum + stashSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, oprfTime);

        stopWatch.start();
        // sending the filter of PRFs in server's hash table
        for (int hashIndex = 0; hashIndex < cuckooHashNum; hashIndex++) {
            List<byte[]> serverBinPrfPayload = generateBinPrfPayload(hashIndex);
            DataPacketHeader serverBinPrfHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_BIN_PRFS.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(serverBinPrfHeader, serverBinPrfPayload));
            extraInfo++;
        }
        // sending the filter of PRFs in server's stash
        for (int stashIndex = 0; stashIndex < stashSize; stashIndex++) {
            List<byte[]> serverStashPrfPayload = generateStashPrfPayload(stashIndex);
            DataPacketHeader serverStashPrfHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_STASH_PRFS.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(serverStashPrfHeader, serverStashPrfPayload));
            extraInfo++;
        }
        oprfSenderOutput = null;
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverPrfTime);

        logPhaseInfo(PtoState.PTO_END);
    }

    private void handleCuckooHashKeyPayload(List<byte[]> cuckooHashKeyPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(cuckooHashKeyPayload.size() == cuckooHashNum);
        binHashes = cuckooHashKeyPayload.stream()
            .map(key -> {
                Prf prf = PrfFactory.createInstance(envType, Integer.BYTES);
                prf.setKey(key);
                return prf;
            })
            .toArray(Prf[]::new);
    }

    private List<byte[]> generateBinPrfPayload(int hashIndex) {
        Stream<T> serverElementStream = serverElementArrayList.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
        List<byte[]> binPrfList = serverElementStream
            .map(element -> {
                byte[] elementByteArray = ObjectUtils.objectToByteArray(element);
                int keyIndex = binHashes[hashIndex].getInteger(elementByteArray, binNum);
                // OPRF(x || hashIndex)
                byte[] extendElementByteArray = ByteBuffer.allocate(elementByteArray.length + Integer.BYTES)
                    .put(elementByteArray)
                    .putInt(hashIndex)
                    .array();
                byte[] binPrf = oprfSenderOutput.getPrf(keyIndex, extendElementByteArray);
                return peqtHash.digestToBytes(binPrf);
            })
            .collect(Collectors.toList());
        Collections.shuffle(binPrfList, secureRandom);
        // constructing filter
        Filter<byte[]> binPrfFilter = FilterFactory.load(envType, filterType, serverElementSize, secureRandom);
        binPrfList.forEach(binPrfFilter::put);
        return binPrfFilter.save();
    }

    private List<byte[]> generateStashPrfPayload(int stashIndex) {
        Stream<T> serverElementStream = serverElementArrayList.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
        List<byte[]> serverStashPrfList = serverElementStream
            .map(element -> {
                byte[] elementByteArray = ObjectUtils.objectToByteArray(element);
                int keyIndex = binNum + stashIndex;
                byte[] stashPrf = oprfSenderOutput.getPrf(keyIndex, elementByteArray);
                return peqtHash.digestToBytes(stashPrf);
            }).collect(Collectors.toList());
        Collections.shuffle(serverStashPrfList, secureRandom);
        // constructing filter
        Filter<byte[]> stashPrfFilter = FilterFactory.load(envType, filterType, serverElementSize, secureRandom);
        serverStashPrfList.forEach(stashPrfFilter::put);
        return stashPrfFilter.save();
    }
}
