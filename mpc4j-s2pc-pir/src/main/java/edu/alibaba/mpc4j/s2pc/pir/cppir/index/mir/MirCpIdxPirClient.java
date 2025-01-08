package edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.crypto.prp.FixedKeyPrp;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.StreamCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.MirCpIdxPirPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.hint.*;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * MIR client-specific preprocessing index PIR client.
 *
 * @author Weiran Liu
 * @date 2023/9/4
 */
public class MirCpIdxPirClient extends AbstractCpIdxPirClient implements StreamCpIdxPirClient {
    /**
     * fixed key PRP
     */
    private final FixedKeyPrp fixedKeyPrp;
    /**
     * chunk size
     */
    private int chunkSize;
    /**
     * chunk num
     */
    private int chunkNum;
    /**
     * query num for each preprocessing round
     */
    private int roundQueryNum;
    /**
     * current query num
     */
    private int currentQueryNum;
    /**
     * M1, the total number of primary hints.
     */
    private int m1;
    /**
     * M2, the total number of backup hints.
     */
    private int m2;
    /**
     * primary hints
     */
    private MirPrimaryHint[] primaryHints;
    /**
     * backup hints
     */
    private LinkedList<MirBackupHint> backupHints;
    /**
     * local cache entries
     */
    private TIntObjectMap<byte[]> localCacheEntries;

    public long preprocessingCompTime = 0;
    public long preprocessingRecTime = 0;
    public long querySendTime = 0;
    public long queryReceiveTime = 0;
    public long queryPureReceiveTime = 0;

    public MirCpIdxPirClient(Rpc clientRpc, Party serverParty, MirCpIdxPirConfig config) {
        super(MirCpIdxPirPtoDesc.getInstance(), clientRpc, serverParty, config);
        this.fixedKeyPrp = config.getFixedKeyPrp();
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        setInitInput(n, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        chunkSize = MirCpIdxPirUtils.getChunkSize(n);
        chunkNum = MirCpIdxPirUtils.getChunkNum(n);
        assert chunkSize * chunkNum >= n
            : "chunkSize * chunkNum must be greater than or equal to n (" + n + "): " + chunkSize * chunkNum;
        roundQueryNum = MirCpIdxPirUtils.getRoundQueryNum(n);
        m1 = MirCpIdxPirUtils.getM1(n);
        m2 = MirCpIdxPirUtils.getM2(n);
        stopWatch.stop();
        long paramTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.INIT_STEP, 0, 1, paramTime,
            String.format(
                "Client sets params: n = %d, ChunkSize = %d, ChunkNum = %d, n (pad) = %d, Q = %d, M1 = %d, M2 = %d",
                n, chunkSize, chunkNum, chunkSize * chunkNum, roundQueryNum, m1, m2
            )
        );

        // preprocessing
        preprocessing();

        logPhaseInfo(PtoState.INIT_END);
    }

    private void preprocessing() throws MpcAbortException {
        stopWatch.start();
        // init primary hints and backup hints
        IntStream primaryHintIntStream = parallel ? IntStream.range(0, m1).parallel() : IntStream.range(0, m1);
        primaryHints = primaryHintIntStream
            .mapToObj(index -> new MirDirectPrimaryHint(fixedKeyPrp, chunkSize, chunkNum, l, secureRandom))
            .toArray(MirPrimaryHint[]::new);
        System.gc();
        IntStream backupHintIntStream = parallel ? IntStream.range(0, m2).parallel() : IntStream.range(0, m2);
        backupHints = backupHintIntStream
            .mapToObj(index -> new MirBackupHint(fixedKeyPrp, chunkSize, chunkNum, l, secureRandom))
            .collect(Collectors.toCollection(LinkedList::new));
        System.gc();
        localCacheEntries = new TIntObjectHashMap<>();
        stopWatch.stop();
        long allocateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, allocateTime, "Client allocates hints");

        stopWatch.start();
        // stream receiving the database
        for (int blockChunkId = 0; blockChunkId < chunkNum; blockChunkId += MirHint.PRP_BLOCK_OFFSET_NUM) {
            // send response before receive, such that the server can directly send the next one
            sendOtherPartyPayload(PtoStep.CLIENT_SEND_STREAM_DATABASE_RESPONSE.ordinal(), new LinkedList<>());
            ArrayList<byte[][]> chunkDataArrays = new ArrayList<>(MirHint.PRP_BLOCK_OFFSET_NUM);
            long time0 = System.currentTimeMillis();
            for (int chunkId = blockChunkId; chunkId < blockChunkId + MirHint.PRP_BLOCK_OFFSET_NUM && chunkId < chunkNum; chunkId++) {
                // download DB[k * √N : (k + 1) * √N - 1] from the server
                List<byte[]> streamRequestPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_STREAM_DATABASE_REQUEST.ordinal());
                MpcAbortPreconditions.checkArgument(streamRequestPayload.size() == 1);
                byte[] streamDataByteArray = streamRequestPayload.get(0);
                MpcAbortPreconditions.checkArgument(streamDataByteArray.length == byteL * chunkSize);
                // split the stream database
                ByteBuffer byteBuffer = ByteBuffer.wrap(streamDataByteArray);
                byte[][] chunkDataArray = new byte[chunkSize][byteL];
                for (int j = 0; j < chunkSize; j++) {
                    byteBuffer.get(chunkDataArray[j]);
                }
                chunkDataArrays.add(chunkDataArray);
            }
            long time1 = System.currentTimeMillis();
            int num = chunkDataArrays.size();
            final int finalBlockChunkId = blockChunkId;
            // update the parity for the primary hints (for j = 0, 1, 2, ..., M)
            // hitMap is irrelevant to the scheme. We want to know if any indices are missed.
            boolean[][] hitMaps = new boolean[num][chunkSize];
            Stream<MirPrimaryHint> primaryHintStream = parallel ? Arrays.stream(primaryHints).parallel() : Arrays.stream(primaryHints);
            primaryHintStream.forEach(primaryHint -> {
                int[] offsets = primaryHint.expandPrpBlockOffsets(finalBlockChunkId);
                boolean[] contains = primaryHint.containsChunks(finalBlockChunkId);
                assert offsets.length == num;
                assert contains.length == num;
                for (int i = 0; i < num; i++) {
                    if (contains[i]) {
                        // if v_{j,k} < ˆv_j then P_j = P_j ⊕ x, here we also include the case for the extra index e_j
                        hitMaps[i][offsets[i]] = true;
                        primaryHint.xori(chunkDataArrays.get(i)[offsets[i]]);
                    }
                }
            });
            // if some indices are missed, we need to fetch the corresponding elements
            for (int i = 0; i < num; i++) {
                for (int j = 0; j < chunkSize; j++) {
                    if (!hitMaps[i][j]) {
                        localCacheEntries.put(j + chunkSize * (finalBlockChunkId + i), chunkDataArrays.get(i)[j]);
                    }
                }
            }
            // update the parity for the backup hints (for j = M + 1, ..., 1.5M - 1)
            Stream<MirBackupHint> backupHintStream = parallel ? backupHints.stream().parallel() : backupHints.stream();
            backupHintStream.forEach(backupHint -> {
                int[] offsets = backupHint.expandPrpBlockOffsets(finalBlockChunkId);
                boolean[] contains = backupHint.containsChunks(finalBlockChunkId);
                assert offsets.length == num;
                for (int i = 0; i < num; i++) {
                    if (contains[i]) {
                        // if v_{j,k} < ˆv_j then P_j = P_j ⊕ x
                        backupHint.xoriLeftParity(chunkDataArrays.get(i)[offsets[i]]);
                    } else {
                        // else P'_j = P'_j ⊕ x
                        backupHint.xoriRightParity(chunkDataArrays.get(i)[offsets[i]]);
                    }
                }
            });
            long time2 = System.currentTimeMillis();
            preprocessingCompTime += time1 - time0;
            preprocessingRecTime += time2 - time1;
            System.gc();
        }
        System.out.println("pure preprocessingCompTime time: " + preprocessingCompTime + "ms");
        System.out.println("pure preprocessingRecTime time: " + preprocessingRecTime + "ms");
        // reset current query num
        currentQueryNum = 0;
        stopWatch.stop();
        long streamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, streamTime, "Client handles " + chunkNum + " chunk");
    }

    @Override
    public byte[][] pir(int[] xs) throws MpcAbortException {
        setPtoInput(xs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        TIntList queryBuffer = new TIntArrayList();
        TIntSet actualQuerySet = new TIntHashSet();
        byte[][] entries = new byte[xs.length][];
        int offset = 0;
        for (int x : xs) {
            queryBuffer.add(x);
            if (!localCacheEntries.containsKey(x) && !actualQuerySet.contains(x)) {
                // we need an actual query
                actualQuerySet.add(x);
            }
            if (currentQueryNum + actualQuerySet.size() > roundQueryNum) {
                // this means we need preprocessing, do batch query.
                // After that, all entries in the buffer are moved to caches, so we clear query buffer and actual set.
                byte[][] batchEntries = batchQuery(queryBuffer.toArray());
                queryBuffer.clear();
                actualQuerySet.clear();
                System.arraycopy(batchEntries, 0, entries, offset, batchEntries.length);
                offset += batchEntries.length;
            }
        }
        // if we still have remaining ones, do batch query one more time.
        if (!queryBuffer.isEmpty()) {
            byte[][] batchEntries = batchQuery(queryBuffer.toArray());
            queryBuffer.clear();
            actualQuerySet.clear();
            System.arraycopy(batchEntries, 0, entries, offset, batchEntries.length);
            offset += batchEntries.length;
        }
        assert offset == xs.length;

        logPhaseInfo(PtoState.PTO_END);
        return entries;
    }

    private byte[][] batchQuery(int[] xs) throws MpcAbortException {
        // generate queries
        TIntObjectMap<byte[]> bufferEntries = new TIntObjectHashMap<>();
        ArrayList<MirPrimaryHint> hintArrayList = new ArrayList<>();
        TByteArrayList hintFlipArrayList = new TByteArrayList();
        int queryIndex = 0;
        TIntSet actualQuerySet = new TIntHashSet();
        long time1 = System.currentTimeMillis();
        for (int x : xs) {
            if (localCacheEntries.containsKey(x) || actualQuerySet.contains(x)) {
                sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), new LinkedList<>());
            } else {
                stopWatch.start();
                actualQuerySet.add(x);
                // client finds a primary hint that contains x
                int primaryHintId = -1;
                for (int i = 0; i < m1; i++) {
                    if (primaryHints[i].contains(x)) {
                        primaryHintId = i;
                        break;
                    }
                }
                // if still no hit set found, then fail.
                MpcAbortPreconditions.checkArgument(primaryHintId >= 0);
                // expand the set and compute the query
                MirPrimaryHint primaryHint = primaryHints[primaryHintId];
                hintArrayList.add(primaryHint);
                int targetChunkId = x / chunkSize;
                BitVector bitVector = primaryHint.containsChunks();
                bitVector.set(targetChunkId, false);
                // randomly shuffle the two sets
                boolean flip = secureRandom.nextBoolean();
                if (flip) {
                    BitVector flipBitVector = BitVectorFactory.createOnes(chunkNum);
                    bitVector.xori(flipBitVector);
                }
                byte byteFlip = flip ? (byte) 1 : 0;
                hintFlipArrayList.add(byteFlip);
                byte[] bitVectorByteArray = bitVector.getBytes();
                // real subset S and dummy subset S' share the same offset vector r
                int[] offsets = primaryHint.expandOffsets();
                // send the punctured set to the server
                ByteBuffer offsetByteBuffer = ByteBuffer.allocate(Short.BYTES * chunkNum);
                for (int i = 0; i < chunkNum; i++) {
                    offsetByteBuffer.putShort((short) offsets[i]);
                }
                List<byte[]> queryRequestPayload = new LinkedList<>();
                queryRequestPayload.add(bitVectorByteArray);
                queryRequestPayload.add(offsetByteBuffer.array());
                sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), queryRequestPayload);
                // ahead of time replenish un-amended backup hints
                MpcAbortPreconditions.checkArgument(!backupHints.isEmpty());
                MirBackupHint backupHint = backupHints.remove(0);
                // adds x to the set and adds the set to the local set list
                primaryHints[primaryHintId] = new MirProgrammedPrimaryHint(backupHint, x);
                queryIndex++;
                stopWatch.stop();
                long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                stopWatch.reset();
                logStepInfo(
                    PtoState.PTO_STEP, 1, 2, queryTime,
                    "Client requests " + (currentQueryNum + queryIndex) + "-th actual query"
                );
            }
        }
        long time2 = System.currentTimeMillis();

        queryIndex = 0;
        actualQuerySet.clear();
        for (int x : xs) {
            if (localCacheEntries.containsKey(x) || actualQuerySet.contains(x)) {
                List<byte[]> queryResponsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
                MpcAbortPreconditions.checkArgument(queryResponsePayload.isEmpty());
            } else {
                stopWatch.start();
                long time01 = System.currentTimeMillis();
                List<byte[]> queryResponsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
                long time02 = System.currentTimeMillis();
                queryPureReceiveTime += time02 - time01;
                MpcAbortPreconditions.checkArgument(queryResponsePayload.size() == 1);
                byte[] responseByteArray = queryResponsePayload.get(0);
                MpcAbortPreconditions.checkArgument(responseByteArray.length == byteL * 2);
                // pick the correct guess
                byte[] value = hintFlipArrayList.get(queryIndex) == 1
                    ? Arrays.copyOfRange(responseByteArray, byteL, byteL * 2)
                    : Arrays.copyOfRange(responseByteArray, 0, byteL);
                // get value and update the local cache
                MirPrimaryHint primaryHint = hintArrayList.get(queryIndex);
                BytesUtils.xori(value, primaryHint.getParity());
                int amendIndex = primaryHint.getAmendIndex();
                if (amendIndex >= 0) {
                    // we need to amend
                    assert bufferEntries.containsKey(primaryHint.getAmendIndex());
                    BytesUtils.xori(value, bufferEntries.get(amendIndex));
                }
                // add x to the local cache
                actualQuerySet.add(x);
                bufferEntries.put(x, value);
                queryIndex++;
                stopWatch.stop();
                long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                stopWatch.reset();
                logStepInfo(
                    PtoState.PTO_STEP, 2, 2, responseTime,
                    "Client handles " + (currentQueryNum + queryIndex) + "-th actual response"
                );
            }
        }
        long time3 = System.currentTimeMillis();
        querySendTime += time2 - time1;
        queryReceiveTime += time3 - time2;
        System.out.println("pure querySendTime time: " + querySendTime + "ms");
        System.out.println("pure queryReceiveTime time: " + queryReceiveTime + "ms");
        System.out.println("queryPureReceiveTime time: " + queryPureReceiveTime + "ms");

        localCacheEntries.putAll(bufferEntries);
        byte[][] entries = Arrays.stream(xs)
            .mapToObj(x -> {
                assert localCacheEntries.containsKey(x);
                return localCacheEntries.get(x);
            })
            .toArray(byte[][]::new);
        currentQueryNum += queryIndex;
        assert currentQueryNum <= roundQueryNum + 1;
        // when query num exceeds the maximum, rerun preprocessing (and refresh the hints)
        if (currentQueryNum > roundQueryNum) {
            preprocessing();
        } else {
            // amend hints
            Arrays.stream(primaryHints).forEach(hint -> {
                int amendIndex = hint.getAmendIndex();
                if (amendIndex >= 0) {
                    hint.amendParity(localCacheEntries.get(amendIndex));
                }
            });
        }
        return entries;
    }

    @Override
    public void update(int updateNum) throws MpcAbortException {
        MathPreconditions.checkPositive("update_num", updateNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        for (int round = 0; round < updateNum; round++) {
            List<byte[]> updatePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_UPDATE.ordinal());

            stopWatch.start();
            MpcAbortPreconditions.checkArgument(updatePayload.size() == 2);
            // Parse (i, u) ← δ_ℓ
            int i = IntUtils.byteArrayToInt(updatePayload.get(0));
            byte[] u = updatePayload.get(1);
            int chunkId = i / chunkSize;
            int chunkIndex = Math.abs(i % chunkSize);
            // update primary hints
            Stream<MirPrimaryHint> primaryHintStream = parallel ? Arrays.stream(primaryHints).parallel() : Arrays.stream(primaryHints);
            primaryHintStream.forEach(primaryHint -> {
                if (primaryHint.contains(i)) {
                    primaryHint.xori(u);
                }
            });
            // update backup hints
            Stream<MirBackupHint> backupHintStream = parallel ? backupHints.stream().parallel() : backupHints.stream();
            backupHintStream.forEach(backupHint -> {
                int offset = backupHint.expandOffset(chunkId);
                if (offset == chunkIndex) {
                    if (backupHint.containsChunkId(chunkId)) {
                        backupHint.xoriLeftParity(u);
                    } else {
                        backupHint.xoriRightParity(u);
                    }
                }
            });
            // update cache entries
            if (localCacheEntries.containsKey(i)) {
                byte[] entry = localCacheEntries.get(i);
                BytesUtils.xori(entry, u);
            }
            stopWatch.stop();
            long updateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, updateTime, "Client updates " + (round + 1) + "-th entry");
        }

        logPhaseInfo(PtoState.PTO_END);
    }
}
