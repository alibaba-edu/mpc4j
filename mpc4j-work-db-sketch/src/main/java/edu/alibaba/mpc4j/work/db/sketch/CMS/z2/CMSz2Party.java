package edu.alibaba.mpc4j.work.db.sketch.CMS.z2;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2CircuitConfig;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory;
import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.db.sketch.CMS.AbstractCMSParty;
import edu.alibaba.mpc4j.work.db.sketch.CMS.CMSParty;
import edu.alibaba.mpc4j.work.db.sketch.CMS.Z2CMSTable;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTable;
import edu.alibaba.mpc4j.work.db.sketch.utils.truncate.TruncateFactory;
import edu.alibaba.mpc4j.work.db.sketch.utils.truncate.TruncateParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.hzf22ext.Hzf22ExtGroupSumParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpParty;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Z2 Boolean circuit implementation of Count-Min Sketch (CMS) protocol in the S³ framework.
 *
 * <p>This class implements secure CMS operations (update and query) using Z2 Boolean circuits.
 * The protocol follows the paper's Merge and Query protocols:</p>
 *
 * <p><b>Merge Protocol:</b>
 * 1. Create table T(id, value) from buffer
 * 2. Compute hash (h(k), 1) for each key using SOPRP
 * 3. Sort by hash indices using PgSort
 * 4. Perform segmented prefix-sum (group-by-sum) to aggregate counts
 * 5. Mark dummy entries and compact
 * 6. Return top s values for the sketch table</p>
 *
 * <p><b>Query Protocol:</b>
 * 1. Compute hash h(q) for the query key
 * 2. Use multiplexer to retrieve CMS[h(q)]
 * 3. Scan buffer to count matching keys
 * 4. Return min{CMS[i][h_i(q)]} across all hash rows</p>
 *
 * <p>The implementation uses several oblivious primitives:
 * - SOPRP for oblivious hash computation
 * - PgSort for oblivious sorting
 * - Oblivious Permutation for applying/inverting permutations
 * - Group-by-Sum for segmented prefix-sum
 * - Truncate for compaction</p>
 *
 * @author Jianzhe Yu, Qi Dong
 */
public class CMSz2Party extends AbstractCMSParty implements CMSParty {
    /**
     * Oblivious permutation party for applying/inverting permutations.
     *
     * <p>Used in the Merge protocol to apply the inverse permutation
     * to the counts after sorting.</p>
     */
    public final PermuteParty permuteParty;
    /**
     * Group-by-sum party for segmented prefix-sum computation.
     *
     * <p>Used in the Merge protocol to aggregate counts for identical
     * hash indices after sorting.</p>
     */
    public final Hzf22ExtGroupSumParty groupSumParty;
    /**
     * PgSort party for oblivious sorting.
     *
     * <p>Used in the Merge protocol to sort buffer data by hash indices,
     * enabling efficient group-by-sum operations.</p>
     */
    public final PgSortParty pgSortParty;
    /**
     * Truncate party for truncation operations.
     *
     * <p>Used in the Merge protocol to compact merged results and
     * truncate to the top s values.</p>
     */
    public final TruncateParty truncateParty;
    /**
     * SOPRP (Secure Oblivious Permutation with Random Permutation) party.
     *
     * <p>Used for oblivious hash computation, mapping keys to indices
     * without revealing the key-index relationship.</p>
     */
    private final SoprpParty lowMcParty;
    /**
     * Z2 integer circuit for binary operations.
     *
     * <p>Provides operations like equality comparison, AND, XOR, etc.
     * for secure computation on binary vectors.</p>
     */
    public final Z2IntegerCircuit circuit;
    /**
     * The CMS table structure.
     *
     * <p>Holds the sketch table (frequency counts) and buffer (pending updates).</p>
     */
    protected Z2CMSTable cmsTable;

    /**
     * Constructs a CMSz2Party with the specified ABB3 party and configuration.
     *
     * @param abb3Party the ABB3 party providing the underlying 3-party MPC functionality
     * @param config    the CMSz2 configuration specifying protocol parameters
     */
    public CMSz2Party(Abb3Party abb3Party, CMSz2Config config) {
        super(CMSz2PtoDesc.getInstance(), abb3Party, config);
        permuteParty = PermuteFactory.createParty(abb3Party, config.getPermuteConfig());
        groupSumParty = (Hzf22ExtGroupSumParty) GroupSumFactory.createParty(abb3Party, config.getGroupSumConfig());
        pgSortParty = PgSortFactory.createParty(abb3Party, config.getPgSortConfig());
        lowMcParty = SoprpFactory.createParty(abb3Party, config.getSoprpConfig());
        truncateParty = TruncateFactory.createParty(abb3Party, config.getTruncateConfig());
        circuit = new Z2IntegerCircuit(z2cParty, new Z2CircuitConfig.Builder().setComparatorType(ComparatorFactory.ComparatorType.TREE_COMPARATOR).build());
        addMultiSubPto(permuteParty, groupSumParty, pgSortParty, lowMcParty, truncateParty);
    }

    /**
     * Initializes the CMSz2 protocol and all sub-protocols.
     *
     * @throws MpcAbortException if initialization fails
     */
    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);
        stopWatch.start();
        abb3Party.init();
        permuteParty.init();
        groupSumParty.init();
        pgSortParty.init();
        lowMcParty.init();
        truncateParty.init();
        initState();
        logStepInfo(PtoState.INIT_STEP, 1, 2, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    /**
     * Sets the CMS table for this party.
     *
     * @param cmsTable the CMS table to use
     */
    private void setCmsTable(Z2CMSTable cmsTable) {
        this.cmsTable = cmsTable;
    }

    /**
     * Updates the CMS sketch with new data.
     *
     * <p>This implements the Merge protocol from the paper:
     * 1. Add new data to buffer
     * 2. When buffer is full, process all buffered updates:
     * a. Compute hash (h(k), 1) for each key using SOPRP
     * b. Merge with existing sketch table
     * c. Sort by hash indices using PgSort
     * d. Apply inverse permutation to counts
     * e. Perform segmented prefix-sum (group-by-sum)
     * f. Truncate to top s values and update sketch table</p>
     *
     * @param cmsTable the CMS sketch table to update
     * @param newData  the new data items to add (each item is a key-value pair)
     * @throws MpcAbortException if the protocol execution fails
     */
    @Override
    public void update(SketchTable cmsTable, MpcVector[] newData) throws MpcAbortException {
        setCmsTable((Z2CMSTable) cmsTable);
        int bIndex = cmsTable.getBufferIndex();
        int logSketchSize = ((Z2CMSTable) cmsTable).getLogSketchSize();
        int payloadBitNum = ((Z2CMSTable) cmsTable).getPayloadBitLen();
        // Step 1: Simple insert the new data into buffer
        cmsTable.getBufferTable().add(newData[0]);
        // Step 2: Merge buffer if it's full
        // Data format: id/payload/dummy
        if (bIndex == cmsTable.getTableSize() - 1) {
            int sketchSize = cmsTable.getTableSize();
            logPhaseInfo(PtoState.PTO_BEGIN);

            stopWatch.start();
            // Step 2a: Hash the buffered keys using SOPRP
            TripletZ2Vector[] bufferData = cmsTable.getBufferTable().toArray(TripletZ2Vector[]::new);
            bufferData = z2cParty.matrixTranspose(bufferData);
            lowMcParty.setKey((TripletZ2Vector) ((Z2CMSTable) cmsTable).getHashParameters().getEncKey());
            TripletZ2Vector[] hashData = lowMcParty.enc(bufferData);
            logStepInfo(PtoState.PTO_STEP, 1, 4, resetAndGetTime(), "soprp time");

            stopWatch.start();
            // Step 2b: Merge buffer data with existing sketch table
            TripletZ2Vector[] bufferHashAndOnes = new TripletZ2Vector[logSketchSize + payloadBitNum];
            // Copy hash values (indices) from SOPRP output
            System.arraycopy(hashData, 0, bufferHashAndOnes, 0, logSketchSize);
            // Add public index values for merging with sketch table
            TripletZ2Vector[] indexes = (TripletZ2Vector[]) z2cParty.setPublicValues(Z2VectorUtils.getBinaryIndex(sketchSize));
            MathPreconditions.checkEqual("indexes.length", "hashData.length", indexes.length, logSketchSize);
            for (int i = 0; i < logSketchSize; i++) {
                bufferHashAndOnes[i].merge(indexes[i]);
            }
            // Set payload (count) to 1 for new buffer entries
            for (int i = logSketchSize; i < bufferHashAndOnes.length; i++) {
                bufferHashAndOnes[i] = z2cParty.createShareZeros(sketchSize);
            }
            // Add 1 to the payload (binary NOT operation on zeros gives all ones)
            z2cParty.noti(bufferHashAndOnes[bufferHashAndOnes.length - 1]);
            // Merge with existing sketch table counts
            for (int i = 0; i < payloadBitNum; i++) {
                bufferHashAndOnes[i + logSketchSize].merge(cmsTable.getSketchTable()[i]);
            }
            // Step 2c: Sort by hash indices using PgSort
            // Order by the index key; PgSort enables flexible selection of sorting algorithms
            TripletZ2Vector[] keys = Arrays.copyOf(bufferHashAndOnes, logSketchSize);
            TripletZ2Vector[] perm = pgSortParty.perGenAndSortOrigin(keys);
            logStepInfo(PtoState.PTO_STEP, 2, 4, resetAndGetTime(), "sort time");

            stopWatch.start();
            // Step 2d: Apply inverse permutation to counts
            TripletZ2Vector[] sortCount = permuteParty.applyInvPermutation(perm, Arrays.copyOfRange(bufferHashAndOnes, logSketchSize, logSketchSize + payloadBitNum));
            logStepInfo(PtoState.PTO_STEP, 3, 4, resetAndGetTime(), "permutation time");

            stopWatch.start();
            // Step 2e: Perform segmented prefix-sum (group-by-sum)
            TripletZ2Vector[] sortKeysAndFlag = new TripletZ2Vector[logSketchSize + 1];
            System.arraycopy(keys, 0, sortKeysAndFlag, 0, logSketchSize);
            // Add flag column for group-by-sum (initialized to all ones)
            sortKeysAndFlag[logSketchSize] = z2cParty.createShareZeros(2 * sketchSize);
            z2cParty.noti(sortKeysAndFlag[logSketchSize]);
            // Get group flag for segmented prefix-sum
            int[] keyIndexes = IntStream.range(0, logSketchSize).toArray();
            TripletZ2Vector flag = groupSumParty.getGroupFlag(sortKeysAndFlag, keyIndexes);
            // Convert to arithmetic for group-sum operations
            TripletLongVector convFlag = abb3Party.getConvParty().bit2a(flag);
            TripletLongVector convCount = abb3Party.getConvParty().b2a(sortCount);
            // Perform group-sum and truncate to top s values
            TripletLongVector[] aGroupSum = truncateParty.groupSumAndTruncate(new TripletLongVector[]{convCount}, convFlag, sketchSize);
            // Convert back to binary
            TripletZ2Vector[] bGroupSum = abb3Party.getConvParty().a2b(aGroupSum[0], payloadBitNum);
            // Step 2f: Update sketch table and clear buffer
            cmsTable.updateSketchTable(bGroupSum);
            cmsTable.clearBufferTable();
            logStepInfo(PtoState.PTO_STEP, 4, 4, resetAndGetTime());

            logPhaseInfo(PtoState.PTO_END);
            System.gc();
        }
    }

    /**
     * Performs a point query on the CMS sketch.
     *
     * <p>This implements the Query protocol from the paper:
     * 1. Compute hash h(q) for the query key using SOPRP
     * 2. Query buffer: count matching keys using equality comparison
     * 3. Query sketch table:
     *    a. Use multiplexer to retrieve counts at hash indices
     *    b. Select counts where table index matches hash index
     *    c. Aggregate using XOR (binary addition)
     * 4. Combine buffer and sketch table results
     * 5. Return the minimum count across all hash rows</p>
     *
     * @param cmsTable  the CMS sketch table to query
     * @param queryData the query data (key to query in raw form)
     * @return the estimated frequency for the queried key (as binary vectors)
     * @throws MpcAbortException if the protocol execution fails
     */
    @Override
    public TripletZ2Vector[] getQuery(SketchTable cmsTable, MpcVector[] queryData) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // Set up query parameters
        int sketchTableSize = cmsTable.getTableSize();
        int logSketchSize = LongUtils.ceilLog2(sketchTableSize);
        int payloadBitLen = ((Z2CMSTable) cmsTable).getPayloadBitLen();
        TripletZ2Vector[] rawQuery = z2cParty.matrixTranspose((Arrays.stream(queryData).map(ea -> (TripletZ2Vector) ea)).toArray(TripletZ2Vector[]::new));

        // Query in buffer: count matching keys
        int bufferSize = cmsTable.getBufferIndex();
        TripletLongVector resInBuffer = zl64cParty.createZeros(1);
        if (bufferSize > 0) {
            TripletZ2Vector[] buffer = cmsTable.getBufferTable().toArray(TripletZ2Vector[]::new);
            buffer = z2cParty.matrixTranspose(buffer);
            // Extend query to match buffer size
            TripletZ2Vector[] extRawQ = Arrays.stream(rawQuery).map(ea -> ea.extendSizeWithSameEle(bufferSize)).toArray(TripletZ2Vector[]::new);
            // Equality comparison: find buffer entries matching query
            TripletZ2Vector eqResBuffer = (TripletZ2Vector) circuit.eq(buffer, extRawQ);
            // Sum the matches (convert to arithmetic and sum)
            TripletLongVector eqResBufferA = abb3Party.getConvParty().bit2a(eqResBuffer);
            resInBuffer = eqResBufferA.getSelfSum();
        }

        // Compute hash for query key using SOPRP
        TripletZ2Vector[] tmpHash = Arrays.copyOf(lowMcParty.enc(rawQuery), logSketchSize);

        // Query in sketch table using multiplexer
        TripletZ2Vector[] tableIndex = (TripletZ2Vector[]) z2cParty.setPublicValues(Z2VectorUtils.getBinaryIndex(cmsTable.getTableSize()));
        // Extend hash bits to table size for comparison
        tmpHash = Arrays.stream(tmpHash).map(ea -> ea.extendSizeWithSameEle(sketchTableSize)).toArray(TripletZ2Vector[]::new);
        // Check which table indices match the hash (equality comparison)
        TripletZ2Vector eqRes = (TripletZ2Vector) circuit.eq(tmpHash, tableIndex);
        // Select counts where indices match (AND operation acts as multiplexer)
        TripletZ2Vector[] tableCount = z2cParty.and(eqRes, (TripletZ2Vector[]) cmsTable.getSketchTable());
        // Aggregate selected counts using XOR (binary addition)
        for (int i = 0; i < tableCount.length; i++) {
            tableCount[i] = (TripletZ2Vector) z2cParty.xorSelfAllElement(tableCount[i]);
        }
        // Combine buffer and sketch table results
        TripletZ2Vector[] res;
        if (bufferSize > 0) {
            // If buffer has data, add buffer count to sketch table count
            TripletLongVector resInTableA = abb3Party.getConvParty().b2a(tableCount);
            res = abb3Party.getConvParty().a2b(zl64cParty.add(resInTableA, resInBuffer), payloadBitLen);
        } else {
            // If buffer is empty, return sketch table count
            res = tableCount;
        }
        logStepInfo(PtoState.PTO_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END);
        return res;
    }
}
