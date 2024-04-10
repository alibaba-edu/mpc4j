package edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.kkrt16;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.structure.filter.Filter;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.kkrt16.Kkrt16PsiPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * KKRT16-PSI client
 *
 * @author Weiran Liu
 * @date 2022/9/20
 */
public class Kkrt16PsiClient<T> extends AbstractPsiClient<T> {
    /**
     * OPRF receiver
     */
    private final OprfReceiver oprfReceiver;
    /**
     * The type of cuckoo hash
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * The number of hash functions
     */
    private final int cuckooHashNum;
    /**
     * PEQT hash function
     */
    private Hash peqtHash;
    /**
     * The number of cuckoo hash bin
     */
    private int binNum;
    /**
     * The maximum stash size of cuckoo hash
     */
    private int stashSize;
    /**
     * Cuckoo hash bin
     */
    private CuckooHashBin<T> cuckooHashBin;
    /**
     * The result of PRF of elements in client's cuckoo hash table
     */
    private ArrayList<byte[]> clientOprfArrayList;
    /**
     * The result of PRF of elements in server's hash table
     */
    private ArrayList<Filter<byte[]>> serverBinPrfFilterArrayList;
    /**
     * The result of PRF of elements in server's stash
     */
    private ArrayList<Filter<byte[]>> serverStashPrfFilterArrayList;

    public Kkrt16PsiClient(Rpc clientRpc, Party serverParty, Kkrt16PsiConfig config) {
        super(Kkrt16PsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        oprfReceiver = OprfFactory.createOprfReceiver(clientRpc, serverParty, config.getOprfConfig());
        addSubPto(oprfReceiver);
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // batchSize = n + s
        int maxBatchSize = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientElementSize)
            + CuckooHashBinFactory.getStashSize(cuckooHashBinType, maxClientElementSize);
        // m = (h + s) * n
        int maxPrfNum = (CuckooHashBinFactory.getHashNum(cuckooHashBinType)
            + CuckooHashBinFactory.getStashSize(cuckooHashBinType, maxServerElementSize)) * maxServerElementSize;
        oprfReceiver.init(maxBatchSize, maxPrfNum);
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
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientElementSize);
        stashSize = CuckooHashBinFactory.getStashSize(cuckooHashBinType, clientElementSize);
        List<byte[]> cuckooHashKeyPayload = generateCuckooHashKeyPayload();
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, keyTime);

        stopWatch.start();
        byte[][] clientExtendByteArrays = generateExtendElementByteArrays();
        OprfReceiverOutput oprfReceiverOutput = oprfReceiver.oprf(clientExtendByteArrays);
        IntStream oprfIndexIntStream = IntStream.range(0, binNum + stashSize);
        oprfIndexIntStream = parallel ? oprfIndexIntStream.parallel() : oprfIndexIntStream;
        clientOprfArrayList = oprfIndexIntStream
            .mapToObj(index -> peqtHash.digestToBytes(oprfReceiverOutput.getPrf(index)))
            .collect(Collectors.toCollection(ArrayList::new));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, oprfTime);

        stopWatch.start();
        // receiving filter of PRF from server
        serverBinPrfFilterArrayList = new ArrayList<>(cuckooHashNum);
        serverBinPrfFilterArrayList.ensureCapacity(cuckooHashNum);
        for (int hashIndex = 0; hashIndex < cuckooHashNum; hashIndex++) {
            DataPacketHeader serverBinPrfHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_BIN_PRFS.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> serverBinPrfPayload = rpc.receive(serverBinPrfHeader).getPayload();
            extraInfo++;
            handleServerBinPrfPayload(serverBinPrfPayload);
        }
        // receiving filter of PRF of elements in stash from server
        serverStashPrfFilterArrayList = new ArrayList<>(stashSize);
        serverStashPrfFilterArrayList.ensureCapacity(stashSize);
        for (int stashIndex = 0; stashIndex < stashSize; stashIndex++) {
            DataPacketHeader serverBinPrfHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_STASH_PRFS.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> serverBinPrfPayload = rpc.receive(serverBinPrfHeader).getPayload();
            extraInfo++;
            handleServerStashPrfPayload(serverBinPrfPayload);
        }
        // intersection
        Set<T> intersection = handleServerPrf();
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverPrfTime);

        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }

    private List<byte[]> generateCuckooHashKeyPayload() {
        cuckooHashBin = CuckooHashBinFactory.createCuckooHashBin(
            envType, cuckooHashBinType, clientElementSize, clientElementArrayList, secureRandom
        );
        cuckooHashBin.insertPaddingItems(secureRandom);
        return Arrays.stream(cuckooHashBin.getHashKeys()).collect(Collectors.toList());
    }

    private byte[][] generateExtendElementByteArrays() {
        // The front elements come from hash bin, and the following is the elements in the stash.
        byte[][] extendElementByteArrays = new byte[binNum + stashSize][];
        IntStream.range(0, binNum).forEach(binIndex -> {
            HashBinEntry<T> hashBinEntry = cuckooHashBin.getHashBinEntry(binIndex);
            int hashIndex = hashBinEntry.getHashIndex();
            byte[] elementByteArray = hashBinEntry.getItemByteArray();
            extendElementByteArrays[binIndex] = ByteBuffer.allocate(elementByteArray.length + Integer.BYTES)
                .put(elementByteArray)
                .putInt(hashIndex)
                .array();
        });
        ArrayList<HashBinEntry<T>> stash = cuckooHashBin.getStash();
        IntStream.range(0, stashSize).forEach(stashIndex ->
            extendElementByteArrays[binNum + stashIndex] = stash.get(stashIndex).getItemByteArray()
        );
        return extendElementByteArrays;
    }

    private void handleServerBinPrfPayload(List<byte[]> serverBinPrfPayload) throws MpcAbortException {
        try {
            Filter<byte[]> serverBinPrfFilter = FilterFactory.load(envType, serverBinPrfPayload);
            serverBinPrfFilterArrayList.add(serverBinPrfFilter);
        } catch (IllegalArgumentException e) {
            throw new MpcAbortException();
        }
    }

    private void handleServerStashPrfPayload(List<byte[]> serverStashPrfPayload) throws MpcAbortException {
        try {
            Filter<byte[]> serverStashPrfFilter = FilterFactory.load(envType, serverStashPrfPayload);
            serverStashPrfFilterArrayList.add(serverStashPrfFilter);
        } catch (IllegalArgumentException e) {
            throw new MpcAbortException();
        }
    }

    private Set<T> handleServerPrf() {
        // handle all elements in the hash table
        Set<T> intersection = IntStream.range(0, binNum)
            .mapToObj(binIndex -> {
                HashBinEntry<T> hashBinEntry = cuckooHashBin.getHashBinEntry(binIndex);
                if (hashBinEntry.getHashIndex() == HashBinEntry.DUMMY_ITEM_HASH_INDEX) {
                    // dummy item
                    return null;
                }
                T element = hashBinEntry.getItem();
                int hashIndex = hashBinEntry.getHashIndex();
                byte[] elementPrf = clientOprfArrayList.get(binIndex);
                return serverBinPrfFilterArrayList.get(hashIndex).mightContain(elementPrf) ? element : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        serverBinPrfFilterArrayList = null;
        // handle all elements in the stash
        ArrayList<HashBinEntry<T>> stash = cuckooHashBin.getStash();
        Set<T> stashIntersection = IntStream.range(0, cuckooHashBin.stashSize())
            .mapToObj(stashIndex -> {
                HashBinEntry<T> hashBinEntry = stash.get(stashIndex);
                if (hashBinEntry.getHashIndex() == HashBinEntry.DUMMY_ITEM_HASH_INDEX) {
                    // dummy item
                    return null;
                }
                T element = hashBinEntry.getItem();
                byte[] elementPrf = clientOprfArrayList.get(binNum + stashIndex);
                return serverStashPrfFilterArrayList.get(stashIndex).mightContain(elementPrf) ? element : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        intersection.addAll(stashIntersection);
        cuckooHashBin = null;
        clientOprfArrayList = null;
        serverStashPrfFilterArrayList = null;
        return intersection;
    }
}
