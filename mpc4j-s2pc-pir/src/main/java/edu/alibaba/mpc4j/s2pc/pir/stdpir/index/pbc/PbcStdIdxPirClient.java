package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.pbc;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.IntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.ArraySimpleIntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntNoStashCuckooHashBin;
import edu.alibaba.mpc4j.s2pc.pir.IdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.AbstractStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.util.IntList;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory.*;
import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.pbc.PbcStdIdxPirPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.pbc.PbcStdIdxPirPtoDesc.getInstance;

/**
 * cuckoo hash batch Index PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class PbcStdIdxPirClient extends AbstractStdIdxPirClient implements IdxPirClient {
    /**
     * cuckoo hash bin type
     */
    private final IntCuckooHashBinType cuckooHashBinType;
    /**
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * bin index and database index map
     */
    private TIntIntMap binIndexRetrievalIndexMap;
    /**
     * simple hash bin
     */
    private int[][] hashBin;
    /**
     * bin num
     */
    private int binNum;
    /**
     * PBC index PIR client
     */
    private PbcableStdIdxPirClient[] client;
    /**
     * PBC index PIR config
     */
    private final PbcableStdIdxPirConfig pbcableStdIdxPirConfig;

    public PbcStdIdxPirClient(Rpc clientRpc, Party serverParty, PbcStdIdxPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        cuckooHashBinType = config.getCuckooHashBinType();
        pbcableStdIdxPirConfig = config.getPbcStdIdxPirConfig();
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);
        checkInitInput(n, l, maxBatchNum);

        stopWatch.start();
        Pair<List<byte[]>, List<byte[]>> keyPair = keyGen();
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), keyPair.getRight());
        List<byte[]> hashKeysPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal());
        MpcAbortPreconditions.checkArgument(hashKeysPayload.size() == getHashNum(cuckooHashBinType));
        // client generate simple hash bin
        binNum = getBinNum(cuckooHashBinType, maxBatchNum);
        hashKeys = hashKeysPayload.toArray(new byte[0][]);
        int[] totalIndex = IntStream.range(0, n).toArray();
        IntHashBin intHashBin = new ArraySimpleIntHashBin(envType, binNum, n, hashKeys);
        intHashBin.insertItems(totalIndex);
        int maxBinSize = IntStream.range(0, binNum).map(intHashBin::binSize).max().orElse(0);
        hashBin = new int[binNum][];
        for (int i = 0; i < binNum; i++) {
            hashBin[i] = new int[intHashBin.binSize(i)];
            for (int j = 0; j < intHashBin.binSize(i); j++) {
                hashBin[i][j] = intHashBin.getBin(i)[j];
            }
        }
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, hashTime);

        stopWatch.start();
        // client init single index PIR client
        client = new PbcableStdIdxPirClient[binNum];
        for (int i = 0; i < binNum; i++) {
            client[i] = StdIdxPirFactory.createPbcableClient(getRpc(), otherParty(), pbcableStdIdxPirConfig);
            addSubPto(client[i]);
            client[i].init(keyPair.getLeft(), maxBinSize, l, 1);
        }
        stopWatch.stop();
        long initIndexPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, initIndexPirTime);

        initState();
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] pir(int[] xs) throws MpcAbortException {
        setPtoInput(xs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // convert database index to bucket index
        IntList indexList = new IntList();
        IntStream.range(0, batchNum).filter(i -> !indexList.contains(xs[i])).forEach(i -> indexList.add(xs[i]));
        TIntList binIndexList = updateBinIndex(indexList);
        stopWatch.stop();
        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, cuckooHashKeyTime, "Client generates cuckoo hash bin");

        stopWatch.start();
        IntStream intStream = parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        intStream.forEach(i -> client[i].query(binIndexList.get(i)));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, genQueryTime, "Client generates query");

        stopWatch.start();
        byte[][] entries = new byte[batchNum][];
        for (int i = 0; i < binNum; i++) {
            if (binIndexList.get(i) != -1) {
                byte[] entry = client[i].recover(binIndexList.get(i));
                for (int j = 0; j < batchNum; j++) {
                    if (binIndexRetrievalIndexMap.get(i) == xs[j]) {
                        entries[j] = entry.clone();
                    }
                }
            } else {
                client[i].dummyRecover();
            }
        }
        stopWatch.stop();
        long decodeResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, decodeResponseTime, "Client decodes response");

        return entries;
    }

    private TIntList updateBinIndex(IntList xs) throws MpcAbortException {
        IntNoStashCuckooHashBin cuckooHashBin = createInstance(
            envType, cuckooHashBinType, maxBatchNum, binNum, hashKeys
        );
        cuckooHashBin.insertItems(xs.toArray());
        TIntList binIndex = new TIntArrayList();
        binIndexRetrievalIndexMap = new TIntIntHashMap(batchNum);
        for (int i = 0; i < binNum; i++) {
            if (cuckooHashBin.getBinHashIndex(i) == -1) {
                binIndex.add(-1);
            } else {
                for (int j = 0; j < hashBin[i].length; j++) {
                    if (hashBin[i][j] == cuckooHashBin.getBinEntry(i)) {
                        binIndex.add(j);
                        binIndexRetrievalIndexMap.put(i, cuckooHashBin.getBinEntry(i));
                        break;
                    }
                }
            }
        }
        MpcAbortPreconditions.checkArgument(
            binIndex.size() == binNum && binIndexRetrievalIndexMap.size() == xs.size()
        );
        return binIndex;
    }

    public Pair<List<byte[]>, List<byte[]>> keyGen() {
        PbcableStdIdxPirClient tempClient = StdIdxPirFactory.createPbcableClient(getRpc(), otherParty(), pbcableStdIdxPirConfig);
        return tempClient.keyGen();
    }
}