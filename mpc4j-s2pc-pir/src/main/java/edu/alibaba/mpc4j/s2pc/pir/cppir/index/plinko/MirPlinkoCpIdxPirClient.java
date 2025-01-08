package edu.alibaba.mpc4j.s2pc.pir.cppir.index.plinko;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.crypto.algs.iprf.InversePrf;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.StreamCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.MirCpIdxPirUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.plinko.MirPlinkoCpIdxPirPtoDesc.PtoStep;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * MIR-based Plinko client-preprocessing index PIR client.
 *
 * @author Weiran Liu
 * @date 2024/10/11
 */
public class MirPlinkoCpIdxPirClient extends AbstractCpIdxPirClient implements StreamCpIdxPirClient {
    /**
     * 1/2 - 1/16
     */
    private static final double CUTOFF_LOWER_BOUND = 1.0 / 2 - 1.0 / 16;
    /**
     * 1/2 + 1/16
     */
    private static final double CUTOFF_UPPER_BOUND = 1.0 / 2 + 1.0 / 16;
    /**
     * we use optimization when n >= 2^18
     */
    private static final int CUTOFF_CHUNK_NUM = MirCpIdxPirUtils.getChunkNum(1 << 18);
    /**
     * specific Q
     */
    private final int specificQ;
    /**
     * PRP, used to select sub-blocks
     */
    private final Prp prp;
    /**
     * number of primary hints
     */
    private int m1;
    /**
     * total number of hints
     */
    private int m;
    /**
     * block size
     */
    private int blockSize;
    /**
     * block num, must be an even number
     */
    private int blockNum;
    /**
     * query num for each preprocessing round
     */
    private int roundQueryNum;
    /**
     * current query num
     */
    private int currentQueryNum;
    /**
     * hint keys, used to select sub-blocks.
     */
    private byte[][] hintIds;
    /**
     * regular hint table H having λw + q slots, where λw regular hints are stored in slots 0, ..., λw - 1, and
     * candidate promoted backup hint will be stored in slots λw, ..., λw + q.
     */
    private byte[][] hs;
    /**
     * cutoffs, i.e., median of hints used to identify selected blocks.
     */
    private double[] cutoffs;
    /**
     * extra block ID
     */
    private int[] extraBlockIds;
    /**
     * extra offsets
     */
    private int[] extraOffsets;
    /**
     * if the promoted backup hint needs to be amended.
     */
    private boolean[] amends;
    /**
     * left backup hint table T_l having λw + q slots, where slots 0, ..., λw - 1 must be null, and q backup hints are
     * stored in slots λw, ..., λw + q.
     */
    private byte[][] tls;
    /**
     * right backup hint table T_r having λw + q slots, where slots 0, ..., λw - 1 must be null, and q backup hints are
     * stored in slots λw, ..., λw + q.
     */
    private byte[][] trs;
    /**
     * flips for promoted backup hints
     */
    private boolean[] flips;
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

    public MirPlinkoCpIdxPirClient(Rpc clientRpc, Party serverParty, MirPlinkoCpIdxPirConfig config) {
        super(MirPlinkoCpIdxPirPtoDesc.getInstance(), clientRpc, serverParty, config);
        specificQ = config.getQ();
        prp = PrpFactory.createInstance(envType);
        prp.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        setInitInput(n, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        blockSize = MirPlinkoCpIdxPirUtils.getBlockSize(n);
        blockNum = MirPlinkoCpIdxPirUtils.getBlockNum(n);
        assert blockSize * blockNum >= n
            : "BlockSize * BlockNum must be greater than or equal to n (" + n + "): " + blockSize * blockNum;
        int defaultQ = MirPlinkoCpIdxPirUtils.getRoundQueryNum(n);
        roundQueryNum = specificQ < 0 ? defaultQ : specificQ;
        m1 = MirPlinkoCpIdxPirUtils.getM1(n);
        int m2 = specificQ < 0 ? MirPlinkoCpIdxPirUtils.getDefaultM2(n) : MirPlinkoCpIdxPirUtils.getSpecificM2(n, specificQ);
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

    private double getDouble(int j, int blockId) {
        byte[] prpInput = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
            .put(hintIds[j])
            .putShort((short) blockId)
            // 0 for "select"
            .putShort((short) 0)
            .array();
        // return a positive double value
        return (double) Math.abs(ByteBuffer.wrap(prp.prp(prpInput)).getLong()) / Long.MAX_VALUE;
    }

    private boolean primaryContainsBlockId(int j, int blockId) {
        assert j >= 0 && j < m1;
        // the straightforward case is that the extra index e_j equals i
        if (blockId == extraBlockIds[j]) {
            return true;
        }
        // The other case is the selection process involving the median cutoff. For each hint j, the client computes
        // v_{j, l} and checks if v_{j, l} is smaller than ^v_j. If so, it means hint j selects partition l.
        double vl = getDouble(j, blockId);
        return vl < cutoffs[j];
    }

    private boolean backupPreprocessingCutoffContainsBlockId(int j, int blockId) {
        assert j >= m1 && j < m;
        // backup hint does not contain extra block ID and extra offset
        double vl = getDouble(j, blockId);
        return vl < cutoffs[j];
    }

    private void preprocessing() throws MpcAbortException {
        stopWatch.start();
        // For i = 1, ..., n/w: K[i] ← iF.Gen(1^λ)
        inversePrfs = new InversePrf[blockNum];
        for (int i = 0; i < blockNum; i++) {
            inversePrfs[i] = new InversePrf(envType);
            byte[] ki = CommonUtils.generateRandomKey(secureRandom);
            inversePrfs[i].init(m, blockSize, ki);
        }
        // For i = 1, ..., λw: generate cutoffs. We generate hint keys and use these hint keys to generate cutoffs.
        // Here we together generate  cutoffs for backup hints. For i = (λw + 1), ..., (λw + q): generate cutoffs
        // the PRG input is "Hint ID || (short) Chunk ID || (short) 0" or "Hint ID || (short) Chunk ID || (short) 1"
        hintIds = new byte[m][CommonConstants.BLOCK_BYTE_LENGTH - Short.BYTES - Short.BYTES];
        cutoffs = new double[m];
        extraBlockIds = new int[m];
        Arrays.fill(extraBlockIds, -1);
        extraOffsets = new int[m];
        Arrays.fill(extraOffsets, -1);
        amends = new boolean[m];
        Arrays.fill(amends, true);
        // create a temporary variables
        double[] vs = new double[blockNum];
        for (int i = 0; i < m; i++) {
            // sample ^v
            boolean cutoffSuccess = false;
            double tryCutoff = 0;
            while (!cutoffSuccess) {
                secureRandom.nextBytes(hintIds[i]);
                // compute V = [v_0, v_1, ..., v_{ChunkNum}]
                for (int blockId = 0; blockId < blockNum; blockId++) {
                    vs[blockId] = getDouble(i, blockId);
                }
                // we need all v in vs are distinct
                long count = Arrays.stream(vs).distinct().count();
                if (count == vs.length) {
                    // all v in vs are distinct, find the median
                    double[] copy = Arrays.copyOf(vs, vs.length);
                    if (vs.length <= CUTOFF_CHUNK_NUM) {
                        // copy and sort
                        Arrays.sort(copy);
                        double left = copy[blockNum / 2 - 1];
                        double right = copy[blockNum / 2];
                        // divide then add, otherwise we may meet overflow
                        tryCutoff = left / 2 + right / 2;
                    } else {
                        // We think of the random values as numbers between 0 and 1, choose two filtering bounds as 1/2 ± 1/16
                        int smallFilterCount = (int) Arrays.stream(copy).filter(v -> v < CUTOFF_LOWER_BOUND).count();
                        int largeFilterCount = (int) Arrays.stream(copy).filter(v -> v > CUTOFF_UPPER_BOUND).count();
                        if (smallFilterCount >= blockNum / 2 - 1 || largeFilterCount >= blockNum / 2 - 1) {
                            continue;
                        }
                        double[] filterVs = Arrays.stream(copy)
                            .filter(v -> v >= CUTOFF_LOWER_BOUND && v <= CUTOFF_UPPER_BOUND)
                            .toArray();
                        Arrays.sort(filterVs);
                        double left = filterVs[blockNum / 2 - 1 - smallFilterCount];
                        double right = filterVs[blockNum / 2 - smallFilterCount];
                        // divide then add, otherwise we may meet overflow
                        tryCutoff = left / 2 + right / 2;
                    }
                    cutoffSuccess = true;
                }
            }
            cutoffs[i] = tryCutoff;
            // for primary hints, generate one more element
            if (i < m1) {
                TIntSet vectorV = new TIntHashSet(blockNum / 2);
                for (int blockId = 0; blockId < blockNum; blockId++) {
                    if (vs[blockId] < cutoffs[i]) {
                        vectorV.add(blockId);
                    }
                }
                assert vectorV.size() == blockNum / 2 : "|V| must be equal to " + blockNum / 2 + ": " + vectorV.size();
                int tryExtraBlockId = -1;
                boolean success = false;
                while (!success) {
                    tryExtraBlockId = secureRandom.nextInt(blockNum);
                    success = (!vectorV.contains(tryExtraBlockId));
                }
                extraBlockIds[i] = tryExtraBlockId;
            }
        }
        // For i = 1, ..., λw: H[i] = 0^B
        hs = new byte[m][];
        for (int i = 0; i < m1; i++) {
            hs[i] = new byte[byteL];
        }
        // For i = (λw + 1), ..., (λw + λq): T[i] ← 0^B
        tls = new byte[m][];
        for (int i = m1; i < m; i++) {
            tls[i] = new byte[byteL];
        }
        trs = new byte[m][];
        for (int i = m1; i < m; i++) {
            trs[i] = new byte[byteL];
        }
        flips = new boolean[m];
        localCacheEntries = new TIntObjectHashMap<>();
        cacheHintIndexes = new TIntIntHashMap();
        stopWatch.stop();
        long allocateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, allocateTime, "Client allocates hints");

        stopWatch.start();
        // stream receiving the database
        for (int alpha = 0; alpha < blockNum; alpha++) {
            // download DB[k * √N : (k + 1) * √N - 1] from the server
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
                    if (j >= m1) {
                        continue;
                    }
                    if (primaryContainsBlockId(j, alpha)) {
                        hitMap[beta] = true;
                        break;
                    }
                }
                // For each j ∈ iF.F^{−1}(K[α], β):
                for (int j : js) {
                    assert j >= 0 && j < m;
                    if (j < m1) {
                        // If j < λw: If α ∈ P: H[j] ← (P, p ⊕ d). This is used to handle regular hints
                        if (primaryContainsBlockId(j, alpha)) {
                            BytesUtils.xori(hs[j], d);
                        }
                    } else {
                        if (backupPreprocessingCutoffContainsBlockId(j, alpha)) {
                            // If α ∈ P: T[j] ← (P, p_1 ⊕ d, p_2)
                            BytesUtils.xori(tls[j], d);
                        } else {
                            // If α ∈ !P: T[j] ← (P, p1, p2 ⊕ d)
                            BytesUtils.xori(trs[j], d);
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
        TByteArrayList hintFlipArrayList = new TByteArrayList();
        ArrayList<byte[]> parityArrayList = new ArrayList<>();
        for (int i : is) {
            if (localCacheEntries.containsKey(i) || actualQuerySet.contains(i)) {
                sendOtherPartyPayload(PianoPlinkoCpIdxPirPtoDesc.PtoStep.CLIENT_SEND_QUERY.ordinal(), new LinkedList<>());
            } else {
                stopWatch.start();
                actualQuerySet.add(i);
                // b ←_R {0, 1}
                boolean b = secureRandom.nextBoolean();
                byte byteFlip = b ? (byte) 1 : 0;
                hintFlipArrayList.add(byteFlip);
                // (α, β) ← (⌊i/w⌋, i mod w)
                int alpha = i / blockSize;
                alphaArrayList.add(alpha);
                int beta = Math.abs(i % blockSize);
                BitVector bitVector = BitVectorFactory.createZeros(blockNum);
                // real subset S and dummy subset S' share the same offset vector r
                int[] offsetVector = new int[blockNum];
                byte[] parity = new byte[byteL];
                // For j ∈ iF.F^{−1}(K[α], β) (in random order):
                int[] js = inversePrfs[alpha].inversePrf(beta);
                // we have cached missing entries, so here we must have js.length > 0
                assert js.length > 0;
                boolean success = false;
                for (int j : js) {
                    // we must make sure that j-th hint contains α
                    if (j < m1 && (!primaryContainsBlockId(j, alpha))) {
                        continue;
                    }
                    if (j >= m1 && !backupOnlineCutoffContainsBlockId(j, alpha)) {
                        continue;
                    }
                    // If j < λw and H[j] != ⊥:
                    if (j < m1 && (hs[j] != null)) {
                        // Parse p ← H[j]; expand offset
                        primaryExtendOffsets(j, alpha, beta, bitVector, offsetVector);
                        BytesUtils.xori(parity, hs[j]);
                        hintIndexArrayList.add(j);
                        parityArrayList.add(parity);
                        hs[j] = null;
                        success = true;
                        break;
                    }
                    // If j ≥ λw and H[j] != ⊥:
                    if (j >= m1 && (hs[j] != null)) {
                        backupExtendOffsets(j, alpha, beta, bitVector, offsetVector);
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
                if (b) {
                    BitVector flipBitVector = BitVectorFactory.createOnes(blockNum);
                    bitVector.xori(flipBitVector);
                }
                byte[] bitVectorByteArray = bitVector.getBytes();
                // send the punctured set to the server
                ByteBuffer offsetByteBuffer = ByteBuffer.allocate(Short.BYTES * blockNum);
                for (int blockId = 0; blockId < blockNum; blockId++) {
                    offsetByteBuffer.putShort((short) offsetVector[blockId]);
                }
                List<byte[]> queryRequestPayload = new LinkedList<>();
                queryRequestPayload.add(bitVectorByteArray);
                queryRequestPayload.add(offsetByteBuffer.array());
                sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), queryRequestPayload);
                // ahead of time replenish un-amended backup hints
                // j′ ← argmin_j (T[j] != ⊥)
                int jPrime = currentQueryNum + m1 + queryIndex;
                assert (tls[jPrime] != null) && (trs[jPrime] != null);
                // H[j′] ← (i, T[j′] ⊕ a) ; T[j′] ← ⊥
                hs[jPrime] = new byte[byteL];
                if (backupPreprocessingCutoffContainsBlockId(jPrime, alpha)) {
                    // If α ∈ P: H[j′] ← (!P, i, p2 ⊕ a)
                    BytesUtils.xori(hs[jPrime], trs[jPrime]);
                    flips[jPrime] = true;
                } else {
                    // If α !∈ P: H[j′] ← (P, i, p1 ⊕ a)
                    BytesUtils.xori(hs[jPrime], tls[jPrime]);
                    flips[jPrime] = false;
                }
                amends[jPrime] = false;
                extraBlockIds[jPrime] = alpha;
                extraOffsets[jPrime] = beta;
                cacheHintIndexes.put(i, jPrime);
                // T[j′] ← ⊥
                tls[jPrime] = null;
                trs[jPrime] = null;
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
                List<byte[]> queryResponsePayload = receiveOtherPartyPayload(PianoPlinkoCpIdxPirPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal());
                MpcAbortPreconditions.checkArgument(queryResponsePayload.isEmpty());
            } else {
                stopWatch.start();
                List<byte[]> queryResponsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
                MpcAbortPreconditions.checkArgument(queryResponsePayload.size() == 1);
                byte[] responseByteArray = queryResponsePayload.get(0);
                MpcAbortPreconditions.checkArgument(responseByteArray.length == byteL * 2);
                boolean b = hintFlipArrayList.get(queryIndex) == 1;
                byte[] parity = parityArrayList.get(queryIndex);
                int j = hintIndexArrayList.get(queryIndex);
                // pick the correct guess
                byte[] entry = b
                    ? Arrays.copyOfRange(responseByteArray, byteL, byteL * 2)
                    : Arrays.copyOfRange(responseByteArray, 0, byteL);
                // get value and update the local cache
                BytesUtils.xori(entry, parity);
                if (!amends[j]) {
                    int x = extraBlockIds[j] * blockSize + extraOffsets[j];
                    assert x >= 0;
                    assert bufferEntries.containsKey(x);
                    BytesUtils.xori(entry, bufferEntries.get(x));
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
                    int x = extraBlockIds[j] * blockSize + extraOffsets[j];
                    assert x >= 0;
                    BytesUtils.xori(hs[j], localCacheEntries.get(x));
                    amends[j] = true;
                }
            }
        }
        return entries;
    }

    private void primaryExtendOffsets(int j, int alpha, int beta, BitVector bitVector, int[] offsetVector) {
        assert j >= 0 && j < m1;
        assert primaryContainsBlockId(j, alpha);
        for (int blockId = 0; blockId < blockNum; blockId++) {
            if (blockId != alpha) {
                boolean contains = primaryContainsBlockId(j, blockId);
                bitVector.set(blockId, contains);
                offsetVector[blockId] = inversePrfs[blockId].prf(j);
            } else {
                assert inversePrfs[alpha].prf(j) == beta;
            }
        }
    }

    private boolean backupOnlineCutoffContainsBlockId(int j, int blockId) {
        assert j >= m1 && j < m;
        // backup hint additionally contains a flip
        double vl = getDouble(j, blockId);
        if (flips[j]) {
            return vl >= cutoffs[j];
        } else {
            return vl < cutoffs[j];
        }
    }

    private void backupExtendOffsets(int j, int alpha, int beta, BitVector bitVector, int[] offsetVector) {
        assert j >= m1 && j < m;
        assert extraBlockIds[j] != alpha || extraOffsets[j] != beta;
        for (int blockId = 0; blockId < blockNum; blockId++) {
            if (blockId != alpha) {
                if (blockId != extraBlockIds[j]) {
                    boolean contains = backupOnlineCutoffContainsBlockId(j, blockId);
                    bitVector.set(blockId, contains);
                    if (contains) {
                        offsetVector[blockId] = inversePrfs[blockId].prf(j);
                    }
                } else {
                    // replace with promoted α
                    bitVector.set(blockId, true);
                    offsetVector[blockId] = extraOffsets[j];
                }
            }
        }
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
                // If j < λw and H[j] != ⊥: If α ∈ P: H[j] ← H[j] ⊕ u. This handles regular hints
                if (j < m1 && hs[j] != null) {
                    if (primaryContainsBlockId(j, alpha)) {
                        BytesUtils.xori(hs[j], u);
                    }
                }
                // If j ≥ λw and H[j] != ⊥: If α ∈ P: H[j] ← (P, x, p ⊕ u) This handles promoted backup hints
                if (j >= m1 && hs[j] != null) {
                    if (backupOnlineCutoffContainsBlockId(j, alpha)) {
                        BytesUtils.xori(hs[j], u);
                    }
                }
                // If j ≥ λw and T[j] != ⊥. This handles backup hints
                if (j >= m1 && tls[j] != null) {
                    assert trs[j] != null;
                    if (backupOnlineCutoffContainsBlockId(j, alpha)) {
                        // If α ∈ P: T[j] ← (P, p1 ⊕ u, p2)
                        BytesUtils.xori(tls[j], u);
                    } else {
                        // If α !∈ P: T[j] ← (P, p1, p2 ⊕ u)
                        BytesUtils.xori(trs[j], u);
                    }
                }
            }
            // If Q[i] != ⊥: this handles target promoted backup hints
            if (cacheHintIndexes.containsKey(i)) {
                // (a, j) ← Q[i] (where a is the answer); (i, p) ← H[j].
                assert cacheHintIndexes.containsKey(i);
                int j = cacheHintIndexes.get(i);
                // update unused promoted backup hints
                if (hs[j] != null) {
                    BytesUtils.xori(hs[j], u);
                }
                // Here we also need to update entry in cache.
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
