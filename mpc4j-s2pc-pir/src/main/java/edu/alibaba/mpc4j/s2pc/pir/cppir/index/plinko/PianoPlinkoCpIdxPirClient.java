package edu.alibaba.mpc4j.s2pc.pir.cppir.index.plinko;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.crypto.algs.iprf.InversePrf;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.StreamCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.plinko.PianoPlinkoCpIdxPirPtoDesc.PtoStep;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Piano-based Plinko client-specific preprocessing index PIR client.
 *
 * @author Weiran Liu
 * @date 2024/10/9
 */
public class PianoPlinkoCpIdxPirClient extends AbstractCpIdxPirClient implements StreamCpIdxPirClient {
    /**
     * specific Q
     */
    private final int specificQ;
    /**
     * block size
     */
    private int blockSize;
    /**
     * block num
     */
    private int blockNum;
    /**
     * query num for each preprocessing round
     */
    private int roundQueryNum;
    /**
     * number of primary hints
     */
    private int m1;
    /**
     * total number of hints
     */
    private int m;
    /**
     * current query num
     */
    private int currentQueryNum;
    /**
     * regular hint table H having λw + λq slots, where λw regular hints are stored in slots 0, ..., λw - 1, and
     * candidate promoted backup hint will be stored in slots λw, ..., λw + λq.
     */
    private byte[][] hs;
    /**
     * backup hint table T having λw + λq slots, where slots 0, ..., λw - 1 must be null, and λq backup hints are stored
     * in slots λw, ..., λw + λq.
     */
    private byte[][] ts;
    /**
     * candidate promoted backup hint int table having λw + λq slots, where slots 0, ..., λw - 1 must be null, and λq
     * backup hints are stored in slots λw, ..., λw + λq.
     */
    private int[] his;
    /**
     * if the promoted backup hint needs to be amended.
     */
    private boolean[] amends;
    /**
     * cache entries, i.e., Q[i].
     */
    private TIntObjectMap<byte[]> localCacheEntries;
    /**
     * cache indexes, used to update promoted backup hints.
     */
    private TIntIntMap cacheHintIndexes;
    /**
     * inverse PRFs
     */
    private InversePrf[] inversePrfs;

    public PianoPlinkoCpIdxPirClient(Rpc clientRpc, Party serverParty, PianoPlinkoCpIdxPirConfig config) {
        super(PianoPlinkoCpIdxPirPtoDesc.getInstance(), clientRpc, serverParty, config);
        specificQ = config.getQ();
    }

    @Override
    public void init(int n, int l, int matchBatchNum) throws MpcAbortException {
        setInitInput(n, l, matchBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        blockSize = PianoPlinkoCpIdxPirUtils.getBlockSize(n);
        blockNum = PianoPlinkoCpIdxPirUtils.getBlockNum(n);
        assert blockSize * blockNum >= n
            : "blockSize * blockNum must be greater than or equal to n (" + n + "): " + blockSize * blockNum;
        int defaultQ = PianoPlinkoCpIdxPirUtils.getRoundQueryNum(n);
        roundQueryNum = specificQ < 0 ? defaultQ : specificQ;
        m1 = PianoPlinkoCpIdxPirUtils.getM1(n);
        int m2 = specificQ < 0 ? PianoPlinkoCpIdxPirUtils.getDefaultM2(n) : PianoPlinkoCpIdxPirUtils.getSpecificM2(n, specificQ);
        m = m1 + m2;
        stopWatch.stop();
        long paramTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.INIT_STEP, 0, 1, paramTime,
            String.format(
                "Client sets params: n = %d, BlockSize = %d, BlockNum = %d, n (pad) = %d, default Q = %d, specific Q = %d, q = %d",
                n, blockSize, blockNum, blockSize * blockNum, defaultQ, specificQ, roundQueryNum
            )
        );
        // preprocessing
        preprocessing();

        logPhaseInfo(PtoState.INIT_END);
    }

    private void preprocessing() throws MpcAbortException {
        stopWatch.start();
        // For i = 1, ..., n/w: K[i] ← iF.Gen(1^λ)
        inversePrfs = new InversePrf[blockNum];
        for (int i = 0; i < blockNum; i++) {
            inversePrfs[i] = new InversePrf(envType);
            byte[] ki = BlockUtils.randomBlock(secureRandom);
            inversePrfs[i].init(m, blockSize, ki);
        }
        // For i = 1, ..., λw: H[i] = 0^B
        hs = new byte[m][];
        for (int i = 0; i < m1; i++) {
            hs[i] = new byte[byteL];
        }
        // For i = (λw + 1), ..., (λw + λq): T[i] ← 0^B
        ts = new byte[m][];
        for (int i = m1; i < m; i++) {
            ts[i] = new byte[byteL];
        }
        // we also need to initialize candidate promoted backup hint int table
        his = new int[m];
        Arrays.fill(his, -1);
        amends = new boolean[m];
        Arrays.fill(amends, true);
        localCacheEntries = new TIntObjectHashMap<>();
        cacheHintIndexes = new TIntIntHashMap();
        stopWatch.stop();
        long allocateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, allocateTime, "Client allocates hints");

        stopWatch.start();
        // stream receiving the database
        for (int alpha = 0; alpha < blockNum; alpha++) {
            // receive stream request
            List<byte[]> streamRequestPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_STREAM_DATABASE_REQUEST.ordinal());
            MpcAbortPreconditions.checkArgument(streamRequestPayload.size() == 1);
            byte[] streamDataByteArray = streamRequestPayload.get(0);
            MpcAbortPreconditions.checkArgument(streamDataByteArray.length == byteL * blockSize);
            // split the stream database
            ByteBuffer byteBuffer = ByteBuffer.wrap(streamDataByteArray);
            byte[][] blockEntries = new byte[blockSize][byteL];
            for (int j = 0; j < blockSize; j++) {
                byteBuffer.get(blockEntries[j]);
            }
            // hitMap is irrelevant to the scheme. We want to know if any indices are missed.
            boolean[] hitMap = new boolean[blockSize];
            // (α, β) ← (⌊i/w⌋, i mod w), here we handle β in a batch
            int finalAlpha = alpha;
            IntStream betaIntStream = IntStream.range(0, blockSize);
            betaIntStream = parallel ? betaIntStream.parallel() : betaIntStream;
            int[][] jsArray = betaIntStream.mapToObj(beta -> inversePrfs[finalAlpha].inversePrf(beta)).toArray(int[][]::new);
            for (int beta = 0; beta < blockSize; beta++) {
                byte[] d = blockEntries[beta];
                int[] js = jsArray[beta];
                // this means the i-th entry is touched by at least one regular hint
                for (int j : js) {
                    if (j < m1) {
                        hitMap[beta] = true;
                        break;
                    }
                }
                // For each j ∈ iF.F^{−1}(K[α], β):
                for (int j : js) {
                    assert j >= 0 && j < m;
                    if (j < m1) {
                        // If j < λw: H[j] ← H[j] ⊕ d. This is used to handle regular hints
                        BytesUtils.xori(hs[j], d);
                    } else {
                        // Else if α != j mod (n/w): T[j] ← T[j] ⊕ d. This is used to handle backup hints
                        if (alpha != Math.abs(j % blockNum)) {
                            BytesUtils.xori(ts[j], d);
                        }
                    }
                }
            }
            // if some indices are missed, we need to fetch the corresponding elements
            for (int beta = 0; beta < blockSize; beta++) {
                if (!hitMap[beta]) {
                    localCacheEntries.put(beta + blockSize * alpha, blockEntries[beta]);
                }
            }
            // send response
            sendOtherPartyPayload(PtoStep.CLIENT_SEND_STREAM_DATABASE_RESPONSE.ordinal(), new LinkedList<>());
        }
        // reset current query num
        currentQueryNum = 0;
        stopWatch.stop();
        long streamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.INIT_STEP, 1, 1, streamTime,
            "Client handles " + blockNum + " block(s), missingEntries.size() = " + localCacheEntries.size()
        );
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

    private byte[][] batchQuery(int[] is) throws MpcAbortException {
        // generate queries
        TIntObjectMap<byte[]> bufferEntries = new TIntObjectHashMap<>();
        int queryIndex = 0;
        TIntSet actualQuerySet = new TIntHashSet();
        TIntArrayList alphaArrayList = new TIntArrayList();
        TIntArrayList hintIndexArrayList = new TIntArrayList();
        ArrayList<byte[]> parityArrayList = new ArrayList<>();
        for (int i : is) {
            if (localCacheEntries.containsKey(i) || actualQuerySet.contains(i)) {
                sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), new LinkedList<>());
            } else {
                stopWatch.start();
                actualQuerySet.add(i);
                // client finds a primary hint that contains x
                // (α, β) ← (⌊i/w⌋, i mod w)
                int alpha = i / blockSize;
                alphaArrayList.add(alpha);
                int beta = Math.abs(i % blockSize);
                int[] offsetVector = new int[blockNum];
                byte[] parity = new byte[byteL];
                // For j ∈ iF.F^{−1}(K[α], β) (in random order):
                int[] js = inversePrfs[alpha].inversePrf(beta);
                // we have cached missing entries, so here we must have js.length > 0
                assert js.length > 0;
                boolean success = false;
                for (int j : js) {
                    // If j < λw and H[j] != ⊥:
                    if (j < m1 && (hs[j] != null)) {
                        // Parse p ← H[j]; return hint set
                        for (int offsetVectorIndex = 0; offsetVectorIndex < blockNum; offsetVectorIndex++) {
                            offsetVector[offsetVectorIndex] = inversePrfs[offsetVectorIndex].prf(j);
                        }
                        BytesUtils.xori(parity, hs[j]);
                        hintIndexArrayList.add(j);
                        parityArrayList.add(parity);
                        hs[j] = null;
                        success = true;
                        break;
                    }
                    // If j ≥ λw and H[j] != ⊥:
                    if (j >= m1 && (hs[j] != null)) {
                        // hs[j] is a promoted backup hint, so we must have cached entries
                        assert his[j] >= 0;
                        // Parse (x, p) ← H[j]
                        int x = his[j];
                        // (α′, β′) ← (⌊x/w⌋, x mod w)
                        int alphaPrime = x / blockSize;
                        int betaPrime = Math.abs(x % blockSize);
                        // If α = α′ and β != β′: Continue; otherwise run the following procedure
                        // this means the backup hint sets α' = α as the punctured block but use β′ != β.
                        // in this case, we cannot use this hint because it does not contain the required (α, β)
                        if (alpha == alphaPrime && beta != betaPrime) {
                            continue;
                        }
                        for (int offsetVectorIndex = 0; offsetVectorIndex < blockNum; offsetVectorIndex++) {
                            offsetVector[offsetVectorIndex] = inversePrfs[offsetVectorIndex].prf(j);
                        }
                        // o_{α′} ← β′
                        offsetVector[alphaPrime] = betaPrime;
                        BytesUtils.xori(parity, hs[j]);
                        hintIndexArrayList.add(j);
                        parityArrayList.add(parity);
                        hs[j] = null;
                        success = true;
                        break;
                    }
                }
                // if still no hit set found, then fail.
                MpcAbortPreconditions.checkArgument(success);
                // q ← (o_j)_{j ∈ [n/w] \ {α}}
                int[] puncturedOffsetVector = new int[blockNum - 1];
                // puncture the set by removing x from the offset vector
                for (int j = 0; j < blockNum; j++) {
                    if (j < alpha) {
                        puncturedOffsetVector[j] = offsetVector[j];
                    } else if (j == alpha) {
                        // skip the punctured Block ID
                    } else {
                        puncturedOffsetVector[j - 1] = offsetVector[j];
                    }
                }
                // send the punctured set to the server
                ByteBuffer queryByteBuffer = ByteBuffer.allocate(Short.BYTES * (blockNum - 1));
                for (int j = 0; j < blockNum - 1; j++) {
                    queryByteBuffer.putShort((short) puncturedOffsetVector[j]);
                }
                List<byte[]> queryRequestPayload = Collections.singletonList(queryByteBuffer.array());
                sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), queryRequestPayload);
                // ahead of time replenish un-amended backup hints
                // j′ ← argmin_{j = α mod (n/w)} (T[j] != ⊥)
                int jPrime = -1;
                for (int j = alpha; j < ts.length; j += blockNum) {
                    if (ts[j] != null) {
                        jPrime = j;
                        break;
                    }
                }
                MpcAbortPreconditions.checkArgument(jPrime >= 0);
                // H[j′] ← (i, T[j′] ⊕ a) ; T[j′] ← ⊥
                his[jPrime] = i;
                hs[jPrime] = new byte[byteL];
                BytesUtils.xori(hs[jPrime], ts[jPrime]);
                amends[jPrime] = false;
                ts[jPrime] = null;
                cacheHintIndexes.put(i, jPrime);
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
        for (int i : is) {
            if (localCacheEntries.containsKey(i) || actualQuerySet.contains(i)) {
                List<byte[]> queryResponsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
                MpcAbortPreconditions.checkArgument(queryResponsePayload.isEmpty());
            } else {
                stopWatch.start();
                List<byte[]> queryResponsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
                MpcAbortPreconditions.checkArgument(queryResponsePayload.size() == 1);
                byte[] responseByteArray = queryResponsePayload.get(0);
                MpcAbortPreconditions.checkArgument(responseByteArray.length == byteL * blockNum);
                int alpha = alphaArrayList.get(queryIndex);
                byte[] parity = parityArrayList.get(queryIndex);
                int j = hintIndexArrayList.get(queryIndex);
                // a ← p ⊕ r_α
                byte[] entry = Arrays.copyOfRange(responseByteArray, alpha * byteL, (alpha + 1) * byteL);
                BytesUtils.xori(entry, parity);
                if (!amends[j]) {
                    assert bufferEntries.containsKey(his[j]);
                    BytesUtils.xori(entry, bufferEntries.get(his[j]));
                    amends[j] = true;
                }
                // add x to the local cache
                actualQuerySet.add(i);
                bufferEntries.put(i, entry);
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
        byte[][] entries = Arrays.stream(is)
            .mapToObj(i -> {
                assert localCacheEntries.containsKey(i);
                return localCacheEntries.get(i);
            })
            .toArray(byte[][]::new);
        currentQueryNum += queryIndex;
        assert currentQueryNum <= roundQueryNum + 1;
        // when query num exceeds the maximum, rerun preprocessing (and refresh the hints)
        if (currentQueryNum > roundQueryNum) {
            preprocessing();
        } else {
            // amend hints
            for (int j = m1; j < m; j++) {
                if (hs[j] != null && !amends[j]) {
                    assert his[j] >= 0;
                    BytesUtils.xori(hs[j], localCacheEntries.get(his[j]));
                    amends[j] = true;
                }
            }
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
            // (α, β) ← (⌊i/w⌋, i mod w)
            int alpha = i / blockSize;
            int beta = Math.abs(i % blockSize);
            // For each j ∈ iF.F^{−1}(K[α], β):
            int[] js = inversePrfs[alpha].inversePrf(beta);
            for (int j : js) {
                // If j < λw and H[j] != ⊥: H[j] ← H[j] ⊕ u. This handles regular hints
                if (j < m1 && hs[j] != null) {
                    BytesUtils.xori(hs[j], u);
                }
                // If j ≥ λw and H[j] != ⊥: This handles promoted backup hints
                if (j >= m1 && hs[j] != null) {
                    // hs[j] is a promoted backup hint, so we must have cached entries
                    assert his[j] >= 0;
                    // Parse (x, p) ← H[j]
                    int x = his[j];
                    // If α != ⌊x/w⌋: H[j] ← (x, p ⊕ u)
                    if (alpha != x / blockSize) {
                        BytesUtils.xori(hs[j], u);
                    }
                }
                // If j ≥ λw and α != j mod (n/w): T[j] ← T[j] ⊕ u. This handles backup hints
                if (j >= m1 && ts[j] != null) {
                    if (alpha != Math.abs(j % blockNum)) {
                        BytesUtils.xori(ts[j], u);
                    }
                }
            }
            // If Q[i] != ⊥: this handles target promoted backup hints
            if (cacheHintIndexes.containsKey(i)) {
                // (a, j) ← Q[i] (where a is the answer); (i, p) ← H[j].
                int j = cacheHintIndexes.get(i);
                // update unused promoted backup hints
                if (hs[j] != null) {
                    BytesUtils.xori(hs[j], u);
                }
                // Here we also need to update entry in cache.
                byte[] newEntry = BytesUtils.xor(localCacheEntries.get(i), u);
                localCacheEntries.put(i, newEntry);
            }
            stopWatch.stop();
            long updateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, updateTime, "Client updates " + (round + 1) + "-th entry");
        }

        logPhaseInfo(PtoState.PTO_END);
    }
}
