package edu.alibaba.mpc4j.work.db.sketch.HLL.z2;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.db.sketch.HLL.AbstractHLLParty;
import edu.alibaba.mpc4j.work.db.sketch.HLL.AbstractHLLTable;
import edu.alibaba.mpc4j.work.db.sketch.HLL.HLLParty;
import edu.alibaba.mpc4j.work.db.sketch.HLL.HLLTable;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalParty;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Z2 Boolean circuit-based HyperLogLog (HLL) protocol implementation for the S³ Framework.
 *
 * This class implements the HLL sketch using Z2 Boolean circuits with ABB3 framework.
 * It provides secure cardinality estimation through update and query operations.
 *
 * Algorithm Implementation (from the paper):
 *
 * Update Operation:
 * - Elements are batched in a buffer to reduce communication overhead
 * - When buffer is full, merge protocol is triggered
 * - Merge: hash → sort → segmented prefix-max → compact
 *
 * Merge Protocol Steps:
 * 1. Hash: Compute h1(k) and h2(k) for all buffered keys using LowMC cipher
 * 2. LeadingOnes: Compute leading ones count using prefix-and circuit + sum
 * 3. Merge tables: Combine buffer data with existing sketch table
 * 4. Sort: Sort by h1(k) using page-based sorting
 * 5. Group Extreme: Compute segmented prefix-max (max of LeadingOnes(h2(k)) per h1(k))
 * 6. Compact: Remove dummy entries and reduce to sketch size
 *
 * Query Operation:
 * - If buffer not empty, trigger merge first
 * - Compute sum of all counters using LogLog estimator
 * - Return sum for plaintext post-processing: r̃ = α'm * s * 2^(r/s)
 */
public class HLLz2Party extends AbstractHLLParty implements HLLParty {
    /**
     * Permutation party for applying inverse permutations.
     * Used during sorting and compaction steps to reorder data.
     */
    private final PermuteParty permuteParty;

    /**
     * Aggregation party for summing operations.
     * Used in query to compute sum of all HLL counters.
     */
    private final AggParty aggParty;

    /**
     * Group extreme party for segmented prefix-max operations.
     * Used in merge to compute max of LeadingOnes(h2(k)) within each h1(k) group.
     */
    private final GroupExtremeParty groupExtremeParty;

    /**
     * Page-based sort party for sorting operations.
     * Used to sort buffered elements by their hash index h1(k).
     */
    private final PgSortParty pgSortParty;

    /**
     * SOPRP (LowMC) party for secure hash computation.
     * LowMC cipher is used to compute h1(k) and h2(k) hash functions.
     */
    private final SoprpParty lowMcParty;

    /**
     * Traversal party for prefix operations.
     * Used for prefix-and circuit to compute leading ones count.
     */
    private final TraversalParty traversalParty;

    /**
     * Constructs an HLLz2Party with the specified ABB3 party and configuration.
     * Initializes all sub-protocol parties required for HLL operations.
     *
     * @param abb3Party the ABB3 party providing secure computation primitives
     * @param config    the HLLz2 configuration
     */
    public HLLz2Party(Abb3Party abb3Party, HLLz2Config config) {
        super(HLLz2PtoDesc.getInstance(), abb3Party, config);
        permuteParty = PermuteFactory.createParty(abb3Party, config.getPermuteConfig());
        aggParty = AggFactory.createParty(abb3Party, config.getAggConfig());
        groupExtremeParty = GroupExtremeFactory.createParty(abb3Party, config.getExtremeConfig());
        pgSortParty = PgSortFactory.createParty(abb3Party, config.getPgSortConfig());
        lowMcParty = SoprpFactory.createParty(abb3Party, config.getSoprpConfig());
        traversalParty = TraversalFactory.createParty(abb3Party, config.getTraversalConfig());
        addMultiSubPto(permuteParty, aggParty, groupExtremeParty, pgSortParty, lowMcParty, traversalParty);
    }

    /**
     * Initializes the HLL protocol and all sub-protocols.
     *
     * @throws MpcAbortException if initialization fails
     */
    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);
        stopWatch.start();
        abb3Party.init();
        permuteParty.init();
        aggParty.init();
        groupExtremeParty.init();
        pgSortParty.init();
        lowMcParty.init();
        traversalParty.init();
        initState();
        logStepInfo(PtoState.INIT_STEP, 1, 2, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    /**
     * Updates the HLL sketch with a batch of elements.
     *
     * This method handles batching and triggers merge when buffer is full.
     * If adding elements would overflow the buffer, it splits the input:
     * - First part fills the buffer and triggers merge
     * - Second part is added to the now-empty buffer
     *
     * @param hllTable the HLL sketch table to update
     * @param elements the batch of elements to add
     * @throws MpcAbortException if update operation fails
     */
    @Override
    public void update(AbstractHLLTable hllTable, MpcVector[] elements) throws MpcAbortException {
        int inputSize = elements.length;
        if (inputSize == 0) {
            return;
        }
        // Check if buffer would be full after adding these elements
        if (inputSize + hllTable.getBufferIndex() >= hllTable.getTableSize()) {
            // Split input: fill buffer and merge, then add remaining
            int diff = hllTable.getTableSize() - hllTable.getBufferIndex();
            MpcVector[] splitElements = new MpcVector[diff];
            MpcVector[] remainElements = new MpcVector[inputSize - diff];
            System.arraycopy(elements, 0, splitElements, 0, diff);
            System.arraycopy(elements, diff, remainElements, 0, inputSize - diff);
            updateBuffer(hllTable, splitElements);
            update(hllTable, remainElements);
        } else {
            // Buffer has space, just add elements
            updateBuffer(hllTable, elements);
        }
    }

    /**
     * Updates the buffer with elements and triggers merge if buffer is full.
     *
     * @param hllTable the HLL sketch table
     * @param elements the elements to add to buffer
     * @throws MpcAbortException if buffer update or merge fails
     */
    private void updateBuffer(AbstractHLLTable hllTable, MpcVector[] elements) throws MpcAbortException {
        hllTable.getBufferTable().addAll(List.of(elements));
        // Trigger merge when buffer reaches full capacity
        if (hllTable.getBufferIndex() == hllTable.getTableSize()) {
            assert (hllTable instanceof HLLTable);
            merge((HLLTable) hllTable);
        }
    }

    /**
     * Merges buffered elements into the HLL sketch table.
     *
     * This implements the merge protocol from the paper:
     * 1. Hash: Compute h1(k) and h2(k) for all buffered keys
     * 2. LeadingOnes: Compute leading ones count using prefix-and circuit
     * 3. Merge tables: Combine buffer data with existing sketch
     * 4. Sort: Sort by h1(k) index
     * 5. Group Extreme: Compute segmented prefix-max (max per group)
     * 6. Compact: Remove dummies and reduce to sketch size
     *
     * @param hllTable the HLL sketch table
     * @throws MpcAbortException if merge operation fails
     */
    private void merge(HLLTable hllTable) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // Step 1: Hash computation
        // Transpose buffer data to column-major format for efficient processing
        TripletZ2Vector[] bufferData = z2cParty.matrixTranspose(hllTable.getBufferTable().toArray(TripletZ2Vector[]::new));
        // Compute h1(k) and h2(k) using LowMC cipher
        TripletZ2Vector[] hashData = hash(bufferData, hllTable.getEncKey());
        int logSketchSize = hllTable.getLogSketchSize();
        int hashBitLen = hllTable.getHashBitLen();
        int payloadBitLen = hllTable.getPayloadBitLen();
        int sketchSize = hllTable.getTableSize();
        int bufferSize = bufferData[0].getNum();
        assert (logSketchSize + hashBitLen <= hashData.length);
        logStepInfo(PtoState.PTO_STEP, 1, 5, resetAndGetTime(), "hash");

        stopWatch.start();
        // Step 2: Compute LeadingOnes count
        // Extract h2(k) part (hash2CountLen bits starting from logSketchSize)
        MpcZ2Vector[] onesCount = getLeadingZeroNum(Arrays.copyOfRange(hashData, logSketchSize, logSketchSize + hashBitLen), payloadBitLen);
        logStepInfo(PtoState.PTO_STEP, 2, 5, resetAndGetTime(), "getLeadingZeroNum");

        stopWatch.start();
        // Step 3: Merge buffer data with existing sketch table
        // Combine h1(k) (index) and LeadingOnes(h2(k)) (value)
        TripletZ2Vector[] bufferHashAndOnesCount = new TripletZ2Vector[logSketchSize + payloadBitLen];
        System.arraycopy(hashData, 0, bufferHashAndOnesCount, 0, logSketchSize);
        System.arraycopy(onesCount, 0, bufferHashAndOnesCount, logSketchSize, payloadBitLen);
        // Add public index values [0, 1, 2, ..., sketchSize-1] to represent existing sketch entries
        TripletZ2Vector[] indexes = (TripletZ2Vector[]) z2cParty.setPublicValues(Z2VectorUtils.getBinaryIndex(sketchSize));
        MathPreconditions.checkEqual("indexes.length", "hashData.length", indexes.length, logSketchSize);
        // Merge buffer indices with public indexes
        for (int i = 0; i < logSketchSize; i++) {
            bufferHashAndOnesCount[i].merge(indexes[i]);
        }
        // Merge buffer values with existing sketch table values
        for (int i = 0; i < payloadBitLen; i++) {
            bufferHashAndOnesCount[i + logSketchSize].merge(hllTable.getSketchTable()[i]);
        }
        // Sort by the index key h1(k) using page-based sorting for flexibility
        TripletZ2Vector[] keys = Arrays.copyOf(bufferHashAndOnesCount, logSketchSize);
        TripletZ2Vector[] values = Arrays.copyOfRange(bufferHashAndOnesCount, logSketchSize, logSketchSize + payloadBitLen);
        TripletZ2Vector[] perm = pgSortParty.perGenAndSortOrigin(keys);
        // Apply inverse permutation to values to match sorted keys
        TripletZ2Vector[] sortedValue = permuteParty.applyInvPermutation(perm, values);
        logStepInfo(PtoState.PTO_STEP, 3, 5, resetAndGetTime(), "sort and permute");

        stopWatch.start();
        // Step 4: Segmented prefix-max (group extreme)
        // Prepare keys with flag for grouping
        TripletZ2Vector[] sortKeysAndFlag = new TripletZ2Vector[logSketchSize + 1];
        System.arraycopy(keys, 0, sortKeysAndFlag, 0, logSketchSize);
        // Add flag bit initialized to 1 for all entries
        sortKeysAndFlag[logSketchSize] = z2cParty.createShareZeros(sketchSize + bufferSize);
        z2cParty.noti(sortKeysAndFlag[logSketchSize]);
        // Get group flag to identify group boundaries
        int[] keyIndexes = IntStream.range(0, logSketchSize).toArray();
        TripletZ2Vector flag = groupExtremeParty.getGroupFlag(sortKeysAndFlag, keyIndexes);
        // Compute max of LeadingOnes(h2(k)) within each h1(k) group
        TripletZ2Vector[] groupedValueTable = groupExtremeParty.groupExtreme(sortedValue, flag, GroupExtremeFactory.ExtremeType.MAX);
        logStepInfo(PtoState.PTO_STEP, 4, 5, resetAndGetTime(), "group extreme");

        stopWatch.start();
        // Step 5: Compaction - remove dummy entries and reduce to sketch size
        // Generate permutation to move real entries to front
        TripletZ2Vector[] perm4Compact = pgSortParty.perGen(new TripletZ2Vector[]{groupedValueTable[payloadBitLen]});
        // Apply permutation to all value bits
        groupedValueTable = permuteParty.applyInvPermutation(perm4Compact, groupedValueTable);
        // Reduce vectors to exact sketch size, discarding dummy entries
        Arrays.stream(groupedValueTable).forEach(ea -> ea.reduce(sketchSize));

        // Update sketch table and clear buffer
        hllTable.updateSketchTable(groupedValueTable);
        hllTable.clearBufferTable();
        logStepInfo(PtoState.PTO_STEP, 5, 5, resetAndGetTime(), "finish");

//        System.gc();
        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * Computes the number of leading zeros (leading ones after negation) for hash values.
     *
     * This implements the LeadingOnes function from the paper using prefix-and circuit + sum.
     * Algorithm:
     * 1. Negate hash values to convert leading zeros to leading ones
     * 2. Apply prefix-and operation to find first 0 (originally 1)
     * 3. Sum the prefix results to get count
     *
     * @param hashValues the h2(k) hash values to process
     * @param payloadBitLen the bit length for output (sufficient to store max count)
     * @return array of leading ones counts for each hash value
     * @throws MpcAbortException if computation fails
     */
    private MpcZ2Vector[] getLeadingZeroNum(TripletZ2Vector[] hashValues, int payloadBitLen) throws MpcAbortException {
        int eleNum = hashValues[0].getNum();
        int hash2CountLen = hashValues.length;
        // Transpose to row-major for per-element processing
        TripletZ2Vector[] transposedValue = z2cParty.matrixTranspose(hashValues);
        // Create flag initialized to 1 for prefix-and operation
        TripletZ2Vector flag = z2cParty.createShareZeros(hash2CountLen);
        z2cParty.noti(flag);
        // Apply prefix-or: traversedValue[i] = or(transposedValue[0..i])
        TripletZ2Vector[] traversedValue = traversalParty.traversalPrefix(transposedValue, flag, true, false);
        z2cParty.noti(traversedValue);
        // Convert to arithmetic domain and sum to get count
        TripletLongVector aValue = zl64cParty.createZeros(eleNum);
        TripletLongVector[] hashA = abb3Party.getConvParty().bit2a(traversedValue);
        IntStream.range(0, eleNum).forEach(i -> aValue.setElements(hashA[i].getSelfSum(), 0, i, 1));
        // Convert back to boolean domain with specified bit length
        return abb3Party.getConvParty().a2b(aValue, payloadBitLen);
    }

    /**
     * Queries the HLL sketch to compute the sum of all counters.
     *
     * This implements the query operation:
     * 1. If buffer not empty, trigger merge first
     * 2. Compute sum of all counters using LogLog estimator
     * 3. Return sum for plaintext post-processing: r̃ = α'm * s * 2^(r/s)
     *
     * @param hllTable the HLL sketch table to query
     * @return the sum of all counters as TripletZ2Vector array
     * @throws MpcAbortException if query operation fails
     */
    @Override
    public TripletZ2Vector[] query(AbstractHLLTable hllTable) throws MpcAbortException {
        assert (hllTable instanceof HLLTable);
        // Merge any remaining buffered elements before query
        if (!hllTable.getBufferTable().isEmpty()) {
            merge((HLLTable) hllTable);
        }
        // Extract counter values from sketch table
        TripletZ2Vector[] tableValue = (TripletZ2Vector[]) Arrays.copyOf(hllTable.getSketchTable(),((HLLTable) hllTable).getPayloadBitLen());
        // Convert to arithmetic domain for summation
        TripletLongVector tableValueLong = abb3Party.getConvParty().b2a(tableValue);
        // Compute sum of all counters (LogLog estimator)
        TripletLongVector tableSum = tableValueLong.getSelfSum();
        int payLoadBit = ((HLLTable) hllTable).getPayloadBitLen();
        int logSketchSize = hllTable.getLogSketchSize();
        // Convert back to boolean domain with expanded bit length for post-processing
        return abb3Party.getConvParty().a2b(tableSum, payLoadBit + logSketchSize);
    }

    /**
     * Computes hash values for elements using LowMC cipher.
     *
     * This implements the hash functions h1(k) and h2(k) from the paper.
     * LowMC cipher is used for secure hash computation in MPC.
     *
     * @param elements the elements to hash
     * @param key the encryption key for LowMC cipher
     * @return hash values containing h1(k) and h2(k)
     * @throws MpcAbortException if hash computation fails
     */
    private TripletZ2Vector[] hash(TripletZ2Vector[] elements, MpcVector key) throws MpcAbortException {
        lowMcParty.setKey((TripletZ2Vector) key);
        return lowMcParty.enc(elements);
    }
}
