package edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.psz14;

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
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.psz14.Psz14PsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * PSI14-PSI client.
 *
 * @author Weiran Liu
 * @date 2023/9/18
 */
public class Psz14PsiClient<T> extends AbstractPsiClient<T> {
    /**
     * LCOT receiver
     */
    private final LcotReceiver lcotReceiver;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * cuckoo hash num
     */
    private final int cuckooHashNum;
    /**
     * cuckoo hash keys
     */
    private byte[][] cuckooHashKeys;
    /**
     * l (in byte)
     */
    private int byteL;
    /**
     * bin num
     */
    private int binNum;
    /**
     * stash size
     */
    private int stashSize;
    /**
     * h1: {0, 1}^* → {0, 1}^l
     */
    private Hash h1;

    public Psz14PsiClient(Rpc clientRpc, Party serverParty, Psz14PsiConfig config) {
        super(Psz14PsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        lcotReceiver = LcotFactory.createReceiver(clientRpc, serverParty, config.getLcotConfig());
        addSubPto(lcotReceiver);
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientElementSize);
        int maxStashNum = CuckooHashBinFactory.getStashSize(cuckooHashBinType, maxClientElementSize);
        int maxByteL = PsiUtils.getSemiHonestPeqtByteLength(maxServerElementSize, maxClientElementSize);
        // init cuckoo hash keys
        cuckooHashKeys = CommonUtils.generateRandomKeys(cuckooHashNum, secureRandom);
        List<byte[]> cuckooHashKeysPayload = Arrays.stream(cuckooHashKeys).collect(Collectors.toList());
        DataPacketHeader cuckooHashKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeysHeader, cuckooHashKeysPayload));
        // init LCOT
        lcotReceiver.init(Byte.SIZE, (maxBinNum + maxStashNum) * maxByteL);
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
        byteL = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientElementSize);
        stashSize = CuckooHashBinFactory.getStashSize(cuckooHashBinType, clientElementSize);
        h1 = HashFactory.createInstance(envType, byteL);
        // insert elements into the cuckoo hash
        CuckooHashBin<T> cuckooHashBin = CuckooHashBinFactory.createCuckooHashBin(
            envType, cuckooHashBinType, clientElementSize, cuckooHashKeys
        );
        cuckooHashBin.insertItems(clientElementArrayList);
        cuckooHashBin.insertPaddingItems(secureRandom);
        byte[][] hyArray = generateHyArray(cuckooHashBin);
        byte[][] flattenHyArray = Arrays.stream(hyArray)
            .map(hy -> {
                byte[][] flattenHy = new byte[byteL][1];
                for (int byteIndex = 0; byteIndex < byteL; byteIndex++) {
                    flattenHy[byteIndex] = new byte[]{hy[byteIndex]};
                }
                return flattenHy;
            })
            .flatMap(Arrays::stream)
            .toArray(byte[][]::new);
        stopWatch.stop();
        long setupTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, setupTime, "Client inits tools and inserts elements");

        stopWatch.start();
        LcotReceiverOutput lcotReceiverOutput = lcotReceiver.receive(flattenHyArray);
        stopWatch.stop();
        long lcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, lcotTime, "Client runs LCOT");

        stopWatch.start();
        IntStream lcotIndexIntStream = IntStream.range(0, binNum + stashSize);
        lcotIndexIntStream = parallel ? lcotIndexIntStream.parallel() : lcotIndexIntStream;
        ArrayList<byte[]> clientOprfArrayList = lcotIndexIntStream
            .mapToObj(index -> {
                byte[] prf = new byte[byteL];
                for (int byteIndex = 0; byteIndex < byteL; byteIndex++) {
                    byte[] byteIndexPrf = lcotReceiverOutput.getRb(index * byteL + byteIndex);
                    byteIndexPrf = h1.digestToBytes(byteIndexPrf);
                    BytesUtils.xori(prf, byteIndexPrf);
                }
                return prf;
            })
            .collect(Collectors.toCollection(ArrayList::new));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, oprfTime);

        // receive bin filters
        ArrayList<Filter<byte[]>> serverBinPrfFilterArrayList = new ArrayList<>(cuckooHashNum);
        serverBinPrfFilterArrayList.ensureCapacity(cuckooHashNum);
        for (int hashIndex = 0; hashIndex < cuckooHashNum; hashIndex++) {
            DataPacketHeader serverBinPrfFilterHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_BIN_PRFS.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> serverBinPrfFilterPayload = rpc.receive(serverBinPrfFilterHeader).getPayload();
            Filter<byte[]> serverBinPrfFilter = FilterFactory.load(envType, serverBinPrfFilterPayload);
            serverBinPrfFilterArrayList.add(serverBinPrfFilter);
            extraInfo++;
        }
        // receive stash filters
        ArrayList<Filter<byte[]>> serverStashPrfFilterArrayList = new ArrayList<>(stashSize);
        serverStashPrfFilterArrayList.ensureCapacity(stashSize);
        for (int stashIndex = 0; stashIndex < stashSize; stashIndex++) {
            DataPacketHeader serverStashPrfFilterHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_STASH_PRFS.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> serverStashPrfFilterPayload = rpc.receive(serverStashPrfFilterHeader).getPayload();
            Filter<byte[]> serverStashPrfFilter = FilterFactory.load(envType, serverStashPrfFilterPayload);
            serverStashPrfFilterArrayList.add(serverStashPrfFilter);
            extraInfo++;
        }

        stopWatch.start();
        // handle bin filter
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
        // handle stash filter
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
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, serverPrfTime, "Client computes intersection");

        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }

    private byte[][] generateHyArray(CuckooHashBin<T> cuckooHashBin) {
        // generate hy
        byte[][] hyArray = new byte[binNum + stashSize][];
        IntStream binIndexIntStream = IntStream.range(0, binNum);
        binIndexIntStream = parallel ? binIndexIntStream.parallel() : binIndexIntStream;
        binIndexIntStream.forEach(binIndex -> {
            HashBinEntry<T> hashBinEntry = cuckooHashBin.getHashBinEntry(binIndex);
            int hashIndex = hashBinEntry.getHashIndex();
            byte[] yBytes = hashBinEntry.getItemByteArray();
            hyArray[binIndex] = ByteBuffer.allocate(yBytes.length + Integer.BYTES)
                .put(yBytes)
                .putInt(hashIndex)
                .array();
            hyArray[binIndex] = h1.digestToBytes(hyArray[binIndex]);
        });
        ArrayList<HashBinEntry<T>> stash = cuckooHashBin.getStash();
        IntStream.range(0, stashSize).forEach(stashIndex -> {
            byte[] yBytes = stash.get(stashIndex).getItemByteArray();
            hyArray[binNum + stashIndex] = ByteBuffer.allocate(yBytes.length + Integer.BYTES)
                .put(yBytes)
                .putInt(binNum + stashIndex)
                .array();
            hyArray[binNum + stashIndex] = h1.digestToBytes(hyArray[binNum + stashIndex]);
        });
        return hyArray;
    }
}
