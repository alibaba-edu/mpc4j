package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.crypto.prp.FixedKeyPrp;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.StreamCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoCpIdxPirPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint.*;
import gnu.trove.list.TIntList;
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
 * PIANO client-specific preprocessing index PIR client.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public class PianoCpIdxPirClient extends AbstractCpIdxPirClient implements StreamCpIdxPirClient {
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
     * M2 (per group), the number of backup hints for each Chunk ID.
     */
    private int m2PerGroup;
    /**
     * primary hints
     */
    private PianoPrimaryHint[] primaryHints;
    /**
     * backup hint group
     */
    private ArrayList<ArrayList<PianoBackupHint>> backupHintGroup;
    /**
     * local cache entries
     */
    private TIntObjectMap<byte[]> localCacheEntries;

    public PianoCpIdxPirClient(Rpc clientRpc, Party serverParty, PianoCpIdxPirConfig config) {
        super(PianoCpIdxPirPtoDesc.getInstance(), clientRpc, serverParty, config);
        fixedKeyPrp = config.getFixedKeyPrp();
    }

    @Override
    public void init(int n, int l, int matchBatchNum) throws MpcAbortException {
        setInitInput(n, l, matchBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        chunkSize = PianoCpIdxPirUtils.getChunkSize(n);
        chunkNum = PianoCpIdxPirUtils.getChunkNum(n);
        assert chunkSize * chunkNum >= n
            : "chunkSize * chunkNum must be greater than or equal to n (" + n + "): " + chunkSize * chunkNum;
        roundQueryNum = PianoCpIdxPirUtils.getRoundQueryNum(n);
        m1 = PianoCpIdxPirUtils.getM1(n);
        m2PerGroup = PianoCpIdxPirUtils.getM2PerGroup(n);
        stopWatch.stop();
        long paramTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.INIT_STEP, 0, 1, paramTime,
            String.format(
                "Client sets params: n = %d, ChunkSize = %d, ChunkNum = %d, n (pad) = %d, Q = %d, M1 = %d, M2 (per group) = %d",
                n, chunkSize, chunkNum, chunkSize * chunkNum, roundQueryNum, m1, m2PerGroup
            )
        );

        // preprocessing
        preprocessing();

        logPhaseInfo(PtoState.INIT_END);
    }

    private void preprocessing() throws MpcAbortException {
        stopWatch.start();
        // init primary hints and backup hints data structure
        IntStream primaryHintIntStream = parallel ? IntStream.range(0, m1).parallel() : IntStream.range(0, m1);
        primaryHints = primaryHintIntStream
            .mapToObj(index -> new PianoDirectPrimaryHint(fixedKeyPrp, chunkSize, chunkNum, l, secureRandom))
            .toArray(PianoPrimaryHint[]::new);
        IntStream backupHintGroupIntStream = parallel ? IntStream.range(0, chunkNum).parallel() : IntStream.range(0, chunkNum);
        backupHintGroup = backupHintGroupIntStream
            .mapToObj(chunkId ->
                IntStream.range(0, m2PerGroup)
                    .mapToObj(index -> new PianoBackupHint(fixedKeyPrp, chunkSize, chunkNum, l, chunkId, secureRandom))
                    .collect(Collectors.toCollection(ArrayList::new))
            )
            .collect(Collectors.toCollection(ArrayList::new));
        localCacheEntries = new TIntObjectHashMap<>();
        stopWatch.stop();
        long allocateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, allocateTime, "Client allocates hints");

        stopWatch.start();
        // stream receiving the database
        for (int blockChunkId = 0; blockChunkId < chunkNum; blockChunkId += PianoHint.PRP_BLOCK_OFFSET_NUM) {
            // send response before receive, such that the server can directly send the next one
            sendOtherPartyPayload(PtoStep.CLIENT_SEND_STREAM_DATABASE_RESPONSE.ordinal(), new LinkedList<>());
            ArrayList<byte[][]> chunkDataArrays = new ArrayList<>(PianoHint.PRP_BLOCK_OFFSET_NUM);
            for (int chunkId = blockChunkId; chunkId < blockChunkId + PianoHint.PRP_BLOCK_OFFSET_NUM && chunkId < chunkNum; chunkId++) {
                // receive stream request
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

            int num = chunkDataArrays.size();
            final int finalChunkId = blockChunkId;
            // update the parity for the primary hints
            // hitMap is irrelevant to the scheme. We want to know if any indices are missed.
            boolean[][] hitMaps = new boolean[num][chunkSize];
            primaryHintIntStream = parallel ? IntStream.range(0, m1).parallel() : IntStream.range(0, m1);
            primaryHintIntStream.forEach(primaryHintIndex -> {
                PianoPrimaryHint primaryHint = primaryHints[primaryHintIndex];
                int[] offsets = primaryHint.expandPrpBlockOffsets(finalChunkId);
                assert offsets.length == num;
                for (int i = 0; i < num; i++) {
                    hitMaps[i][offsets[i]] = true;
                    // XOR parity
                    primaryHint.xori(chunkDataArrays.get(i)[offsets[i]]);
                }
            });
            // if some indices are missed, we need to fetch the corresponding elements
            for (int i = 0; i < num; i++) {
                for (int j = 0; j < chunkSize; j++) {
                    if (!hitMaps[i][j]) {
                        localCacheEntries.put(j + chunkSize * (finalChunkId + i), chunkDataArrays.get(i)[j]);
                    }
                }
            }
            // update the parity for the backup hints
            backupHintGroupIntStream = parallel ? IntStream.range(0, chunkNum).parallel() : IntStream.range(0, chunkNum);
            backupHintGroupIntStream.forEach(backupHintGroupIndex -> {
                ArrayList<PianoBackupHint> backupHints = backupHintGroup.get(backupHintGroupIndex);
                for (PianoBackupHint backupHint : backupHints) {
                    int[] offsets = backupHint.expandPrpBlockOffsets(finalChunkId);
                    assert offsets.length == num;
                    for (int i = 0; i < num; i++) {
                        // we need to ignore the group for the chunk ID.
                        if (backupHintGroupIndex != finalChunkId + i) {
                            backupHint.xori(chunkDataArrays.get(i)[offsets[i]]);
                        }
                    }
                }
            });
            System.gc();
        }

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
        // return results
        return entries;
    }

    private byte[][] batchQuery(int[] xs) throws MpcAbortException {
        // generate queries
        TIntObjectMap<byte[]> bufferEntries = new TIntObjectHashMap<>();
        ArrayList<PianoPrimaryHint> hintArrayList = new ArrayList<>();
        int queryIndex = 0;
        TIntSet actualQuerySet = new TIntHashSet();
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
                // expand the set
                PianoPrimaryHint primaryHint = primaryHints[primaryHintId];
                hintArrayList.add(primaryHint);
                int[] offsets = primaryHint.expandOffsets();
                int[] puncturedOffsets = new int[chunkNum - 1];
                // puncture the set by removing x from the offset vector
                int puncturedChunkId = x / chunkSize;
                for (int i = 0; i < chunkNum; i++) {
                    if (i < puncturedChunkId) {
                        puncturedOffsets[i] = offsets[i];
                    } else if (i == puncturedChunkId) {
                        // skip the punctured chunk ID
                    } else {
                        puncturedOffsets[i - 1] = offsets[i];
                    }
                }
                // send the punctured set to the server
                ByteBuffer queryByteBuffer = ByteBuffer.allocate(Short.BYTES * (chunkNum - 1));
                for (int i = 0; i < chunkNum - 1; i++) {
                    queryByteBuffer.putShort((short) puncturedOffsets[i]);
                }
                List<byte[]> queryRequestPayload = Collections.singletonList(queryByteBuffer.array());
                sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), queryRequestPayload);
                // ahead of time replenish un-amended backup hints
                ArrayList<PianoBackupHint> backupHints = backupHintGroup.get(puncturedChunkId);
                MpcAbortPreconditions.checkArgument(!backupHints.isEmpty());
                PianoBackupHint backupHint = backupHints.remove(0);
                // adds x to the set and adds the set to the local set list
                primaryHints[primaryHintId] = new PianoProgrammedPrimaryHint(backupHint, x);
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
        queryIndex = 0;
        actualQuerySet.clear();
        for (int x : xs) {
            if (localCacheEntries.containsKey(x) || actualQuerySet.contains(x)) {
                List<byte[]> queryResponsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
                MpcAbortPreconditions.checkArgument(queryResponsePayload.isEmpty());
            } else {
                stopWatch.start();
                List<byte[]> queryResponsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
                MpcAbortPreconditions.checkArgument(queryResponsePayload.size() == 1);
                byte[] responseByteArray = queryResponsePayload.get(0);
                MpcAbortPreconditions.checkArgument(responseByteArray.length == byteL * chunkNum);
                // pick the correct guess
                int puncturedChunkId = x / chunkSize;
                byte[] value = Arrays.copyOfRange(responseByteArray, puncturedChunkId * byteL, (puncturedChunkId + 1) * byteL);
                // get value and update the local cache
                PianoPrimaryHint primaryHint = hintArrayList.get(queryIndex);
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
            // enumerate all primary hints and update when necessary
            Stream<PianoPrimaryHint> primaryHintIntStream = parallel ? Arrays.stream(primaryHints).parallel() : Arrays.stream(primaryHints);
            primaryHintIntStream.forEach(primaryHint -> {
                if (primaryHint.expandOffset(chunkId) == chunkIndex) {
                    primaryHint.xori(u);
                }
            });
            // enumerate all backup hints and update when necessary
            IntStream backupHintGroupIntStream = parallel ? IntStream.range(0, chunkNum).parallel() : IntStream.range(0, chunkNum);
            backupHintGroupIntStream.forEach(backupHintGroupIndex -> {
                // we need to ignore the group for the chunk ID.
                if (backupHintGroupIndex != chunkId) {
                    ArrayList<PianoBackupHint> backupHints = backupHintGroup.get(backupHintGroupIndex);
                    for (PianoBackupHint backupHint : backupHints) {
                        if (backupHint.expandOffset(chunkId) == chunkIndex) {
                            backupHint.xori(u);
                        }
                    }
                }
            });
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
