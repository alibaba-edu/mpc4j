package edu.alibaba.mpc4j.work.db.sketch.GK.z2;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.MatrixUtils;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.db.sketch.GK.AbstractGKParty;
import edu.alibaba.mpc4j.work.db.sketch.GK.GKParty;
import edu.alibaba.mpc4j.work.db.sketch.GK.GKTable;
import edu.alibaba.mpc4j.work.db.sketch.structure.SketchTable;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalParty;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Z2 Boolean circuit implementation of GK (Greenwald-Khanna) sketch protocol in the S³ framework.
 *
 * This class implements the GK sketch algorithm using Z2 Boolean circuits for secure multi-party computation.
 * The GK sketch maintains an ordered set of tuples (k_i, g1_i, g2_i, delta1_i, delta2_i) for ε-approximate
 * quantile queries on streaming data.
 *
 * The implementation follows the Merge protocol:
 * 1. Buffer new elements until buffer is full
 * 2. Merge buffer with sketch table
 * 3. Sort by key
 * 4. Calculate gaps (g1, g2) and deltas (delta1, delta2)
 * 5. Compact to maintain size bound: s' = 2*ln(n/s + 2)/ε + 2
 *
 * The Query protocol:
 * 1. Search sketch table for keys in rank range [r-ε*n, r+ε*n]
 * 2. Return key values within the range
 *
 * Uses MPC primitives:
 * - PermuteParty: oblivious permutation for secure shuffling
 * - GroupSumParty: group-by-sum for aggregating values
 * - PgSortParty: page-sort for sorting by key
 * - TraversalParty: prefix operations for compaction and delta calculation
 *
 * Reference: "Sketch-based Secure Query Processing for Streaming Data" (S³ framework)
 */
public class GKz2Party extends AbstractGKParty implements GKParty {
    /**
     * Permute party for oblivious permutation operations.
     * Used for secure shuffling during merge and compaction phases.
     */
    public final PermuteParty permuteParty;

    /**
     * Group-by-sum party for aggregation operations.
     * Used for summing gap and delta values during merge operations.
     */
    public final GroupSumParty groupSumParty;

    /**
     * Page-sort party for sorting operations.
     * Used for sorting tuples by key values in the GK sketch.
     */
    public final PgSortParty pgSortParty;

    /**
     * Traversal party for prefix operations.
     * Used for prefix sums and copying during compaction and delta calculation.
     */
    public final TraversalParty traversalParty;

    /**
     * Z2 integer circuit for arithmetic operations on Boolean shares.
     * Provides comparison, addition, and other operations on Z2 vectors.
     */
    public final Z2IntegerCircuit circuit;

    /**
     * Constructs a GKz2Party with the specified ABB3 party and configuration.
     *
     * @param abb3Party the ABB3 party providing the underlying MPC primitives
     * @param config    the GKz2 configuration with security model and protocol settings
     */
    public GKz2Party(Abb3Party abb3Party, GKz2Config config) {
        super(GKz2PtoDesc.getInstance(), abb3Party, config);
        permuteParty = PermuteFactory.createParty(abb3Party, config.getPermuteConfig());
        groupSumParty = GroupSumFactory.createParty(abb3Party, config.getGroupSumConfig());
        pgSortParty = PgSortFactory.createParty(abb3Party, config.getPgSortConfig());
        traversalParty = TraversalFactory.createParty(abb3Party, config.getTraversalConfig());
        circuit = new Z2IntegerCircuit(abb3Party.getZ2cParty());
        addMultiSubPto(permuteParty, groupSumParty, pgSortParty, traversalParty);
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        abb3Party.init();
        permuteParty.init();
        groupSumParty.init();
        pgSortParty.init();
        traversalParty.init();
        initState();
        logStepInfo(PtoState.INIT_STEP, 1, 2, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    /**
     * Updates the GK sketch by inserting new data.
     *
     * This method implements the Merge protocol of the GK algorithm:
     * 1. Add new element to buffer
     * 2. When buffer is full, merge with sketch table
     * 3. Sort by key
     * 4. Calculate gaps and deltas
     * 5. Compact to maintain size bound
     *
     * The merge process:
     * - Step 1: Set up - prepare data structures and parameters
     * - Step 2: Sort - sort tuples by key with valid elements at head
     * - Step 3: Permute - apply permutation to all data columns
     * - Step 4: Delta - calculate deltas for new elements using prefix copy
     * - Step 5: Compress - apply log2(n) rounds of pairwise merging and compaction
     *
     * @param gkTable the GK sketch table to update
     * @param newData the new data element to insert (key value)
     * @throws MpcAbortException if the protocol fails
     */
    @Override
    public void update(SketchTable gkTable, MpcVector[] newData) throws MpcAbortException{
        setGkTable((GKTable) gkTable);
        gkTable.getBufferTable().add(newData[0]);
        int bIndex = gkTable.getBufferIndex();
        // Table structure: [key_bits, g1_bits, g2_bits, delta1_bits, delta2_bits, t_bits, dummy_flag]
        if(bIndex == gkTable.getTableSize()) {
            logPhaseInfo(PtoState.PTO_BEGIN);
            stopWatch.start();
            // Step 1: Set up - prepare data structures and parameters
            int oldSize = gkTable.getTableSize();
            int dataSize = getGkTable().getDataSize();
            int newSize = ((GKTable) gkTable).resize(oldSize);
            int round = LongUtils.ceilLog2(oldSize);
            int keyBitLen = ((GKTable) gkTable).getKeyBitLen();
            int attributeBitLen = ((GKTable) gkTable).getAttributeBitLen();
            // Merge buffer with table: concatenate buffer elements with existing sketch data
            TripletZ2Vector[] bufferData = gkTable.getBufferTable().toArray(TripletZ2Vector[]::new);
            bufferData = z2cParty.matrixTranspose(bufferData);

            TripletZ2Vector[] dataCopy = (TripletZ2Vector[]) gkTable.getSketchTable();

            TripletZ2Vector[] g1Copy = Arrays.copyOfRange(dataCopy, keyBitLen, keyBitLen+attributeBitLen);
            TripletZ2Vector[] g2Copy = Arrays.copyOfRange(dataCopy, keyBitLen+attributeBitLen, keyBitLen+2*attributeBitLen);
            TripletZ2Vector[] delta1Copy = Arrays.copyOfRange(dataCopy, keyBitLen+2*attributeBitLen, keyBitLen+3*attributeBitLen);
            TripletZ2Vector[] delta2Copy = Arrays.copyOfRange(dataCopy, keyBitLen+3*attributeBitLen, keyBitLen+4*attributeBitLen);
            TripletZ2Vector[] tCopy = Arrays.copyOfRange(dataCopy, keyBitLen+4*attributeBitLen, keyBitLen+5*attributeBitLen);
            TripletZ2Vector flag = dataCopy[dataCopy.length-1];
            // Binary index from t to t+oldSize
            TripletZ2Vector[] t;
            BigInteger[] intArr = IntStream.range(dataSize, dataSize + oldSize).mapToObj(BigInteger::valueOf).toArray(BigInteger[]::new);
            BitVector[] bitVector = Arrays.stream(intArr).map(ea -> BitVectorFactory.create(attributeBitLen, ea)).toArray(BitVector[ ]::new);
            t = z2cParty.matrixTranspose((TripletZ2Vector[]) z2cParty.setPublicValues(bitVector));
            for(int i = 0; i < attributeBitLen; i++){
                t[i].merge(tCopy[i]);
            }

            TripletZ2Vector[] t1 = Arrays.stream(circuit.add(g1Copy, delta1Copy)).map(ea -> (TripletZ2Vector) ea).toArray(TripletZ2Vector[]::new);
            TripletZ2Vector[] t2 = Arrays.stream(circuit.add(g2Copy, delta2Copy)).map(ea -> (TripletZ2Vector) ea).toArray(TripletZ2Vector[]::new);
            Arrays.stream(delta1Copy).forEach(ea -> ea.extendLength(2 * oldSize));
            Arrays.stream(delta2Copy).forEach(ea -> ea.extendLength(2 * oldSize));

            // [Buffer,Data]
            for(int i = 0; i < bufferData.length; i++){
                bufferData[i].merge(dataCopy[i]);
            }
            //[Buffer:all 1, Data: 1 for valid and 0 for invalid]
            TripletZ2Vector updateFlag = z2cParty.createShareZeros(oldSize);
            z2cParty.noti(updateFlag);
            updateFlag.merge(flag);
            Arrays.stream(t1).forEach(ea -> ea.extendLength(2 * oldSize));
            Arrays.stream(t2).forEach(ea -> ea.extendLength(2 * oldSize));
            // Create group flag for prefix copy
            // [1 for buffer, 0 for data]
            TripletZ2Vector groupFlag = z2cParty.createShareZeros(oldSize);
            TripletZ2Vector zeros = z2cParty.createShareZeros(oldSize);
            z2cParty.noti(groupFlag);
            groupFlag.merge(zeros);
            logStepInfo(PtoState.PTO_STEP, 1, 5, resetAndGetTime(), "Set up");

            stopWatch.start();
            // Add the flag bit to the first bit
            TripletZ2Vector[] flagAndKey = new TripletZ2Vector[keyBitLen+1];
            //[0 for valid, 1 for invalid, so the valid ones would at head]
            flagAndKey[0] = (TripletZ2Vector) z2cParty.not(updateFlag);
            System.arraycopy(bufferData, 0, flagAndKey, 1, keyBitLen);
            // Sort and permute
            TripletZ2Vector[] perm = pgSortParty.perGenAndSortOrigin(flagAndKey);
            logStepInfo(PtoState.PTO_STEP, 2, 5, resetAndGetTime(), "Sort");

            stopWatch.start();
            Arrays.stream(g1Copy).forEach(ea -> ea.extendLength(2 * oldSize));
            Arrays.stream(g2Copy).forEach(ea -> ea.extendLength(2 * oldSize));
            TripletZ2Vector[][] allDataColumns = new TripletZ2Vector[][]{t1, t2, delta1Copy, delta2Copy, g1Copy, g2Copy, t, new TripletZ2Vector[]{groupFlag}};
            TripletZ2Vector[] allDataFlat = Arrays.stream(allDataColumns)
                .flatMap(Arrays::stream)
                .toArray(TripletZ2Vector[]::new);
            allDataFlat = permuteParty.applyInvPermutation(perm, allDataFlat);
            t1 = Arrays.copyOf(allDataFlat, attributeBitLen);
            t2 = Arrays.copyOfRange(allDataFlat, attributeBitLen, 2 * attributeBitLen);
            delta1Copy = Arrays.copyOfRange(allDataFlat, 2 * attributeBitLen, 3 * attributeBitLen);
            delta2Copy = Arrays.copyOfRange(allDataFlat, 3 * attributeBitLen, 4 * attributeBitLen);
            g1Copy = Arrays.copyOfRange(allDataFlat, 4 * attributeBitLen, 5 * attributeBitLen);
            g2Copy = Arrays.copyOfRange(allDataFlat, 5 * attributeBitLen, 6 * attributeBitLen);
            t = Arrays.copyOfRange(allDataFlat, 6 * attributeBitLen, 7 * attributeBitLen);
            groupFlag = allDataFlat[7 * attributeBitLen];
            logStepInfo(PtoState.PTO_STEP, 3, 5, resetAndGetTime(), "Permute");

            stopWatch.start();
            // Copy the back one (delta1)
            t1 = traversalParty.traversalPrefix(t1, groupFlag, true, true);
            // Copy the previous one (delta2)
            t2 = traversalParty.traversalPrefix(t2, groupFlag, true, false);
            // Construct the new table
            delta1Copy = z2cParty.mux(delta1Copy, t1, groupFlag);
            delta2Copy = z2cParty.mux(delta2Copy, t2, groupFlag);
            logStepInfo(PtoState.PTO_STEP, 4, 5, resetAndGetTime(), "Delta for new elements");

            stopWatch.start();
            TripletZ2Vector[][] newGK = new TripletZ2Vector[][]{
                Arrays.copyOfRange(flagAndKey, 1, 1 + keyBitLen),
                g1Copy,
                g2Copy,
                delta1Copy,
                delta2Copy,
                t,
                // Now 1 denotes valid data
                new TripletZ2Vector[]{(TripletZ2Vector) z2cParty.not(flagAndKey[0])}
            };
            for (int i = 0; i < round; i++) {
                newGK = compress(newGK);
            }
            logStepInfo(PtoState.PTO_STEP, 5, 5, resetAndGetTime(), "Compress");

            stopWatch.start();
            TripletZ2Vector[] updateTable = MatrixUtils.flat(
                    Arrays.stream(newGK).map(ea ->
                            Arrays.stream(ea).map(i ->(TripletRpZ2Vector) i).toArray(TripletRpZ2Vector[]::new)
                    ).toArray(TripletRpZ2Vector[][]::new));

            // Reduce to new size
            updateTable = Arrays.stream(updateTable).map(
                    ea -> ea.getPointsWithFixedSpace(0, newSize, 1)).toArray(TripletZ2Vector[]::new);

            gkTable.updateSketchTable(updateTable);
            gkTable.clearBufferTable();
            System.gc();
            logStepInfo(PtoState.PTO_STEP, 4, 4, resetAndGetTime(), "Finish");
            logPhaseInfo(PtoState.PTO_END);
        }
    }

    /**
     * Compresses the GK sketch by merging adjacent tuples.
     *
     * This method implements the compaction phase of the GK algorithm:
     * 1. Pairwise merge: merge adjacent tuples if they satisfy the compaction condition
     * 2. The compaction condition: g1_i + g2_{i+1} + delta2_{i+1} ≤ ε*n
     * 3. Perform log2(n) rounds of pairwise merging
     * 4. Sort to move valid tuples to the front
     *
     * The pairwise merge process:
     * - Split data into pairs (r1, r2)
     * - Check if pairs can be merged based on threshold
     * - If mergeable, combine gaps and mark one tuple as dummy
     * - Repeat for multiple rounds to achieve full compaction
     *
     * @param data the GK sketch data to compress [key, g1, g2, delta1, delta2, t, dummy]
     * @return the compressed GK sketch data
     * @throws MpcAbortException if the protocol fails
     */
    private TripletZ2Vector[][] compress(TripletZ2Vector[][] data) throws MpcAbortException {
        TripletZ2Vector[][] r1;
        TripletZ2Vector[][] r2;
        Pair<TripletZ2Vector[][], TripletZ2Vector[][]> mergeRes;
        // Start from index 0
        int firstHalfNum = data[0][0].bitNum() / 2;
        r1 = Arrays.stream(data).map(ea -> Arrays.stream(ea)
                .map(i -> i.getPointsWithFixedSpace(0, firstHalfNum, 2))
                .toArray(TripletZ2Vector[]::new)
            ).toArray(TripletZ2Vector[][]::new);
        r2 = Arrays.stream(data).map(ea -> Arrays.stream(ea)
                .map(i -> i.getPointsWithFixedSpace(1, firstHalfNum, 2))
                .toArray(TripletZ2Vector[]::new)
            ).toArray(TripletZ2Vector[][]::new);
        mergeRes = mergePair(r1, r2);
        r1 = mergeRes.getLeft();
        r2 = mergeRes.getRight();
        // Recover
        for (int i = 0; i < r1.length; i++) {
            for (int j = 0; j < r1[i].length; j++) {
                data[i][j].setPointsWithFixedSpace(r1[i][j], 0, r1[i][j].bitNum(), 2);
                data[i][j].setPointsWithFixedSpace(r2[i][j], 1, r2[i][j].bitNum(), 2);
            }
        }
        int secondHalfNum = (data[0][0].bitNum() - 1) / 2;
        // Start from index 1
        r1 = Arrays.stream(data).map(ea -> Arrays.stream(ea)
                .map(i -> i.getPointsWithFixedSpace(1, secondHalfNum, 2))
                .toArray(TripletZ2Vector[]::new)
            ).toArray(TripletZ2Vector[][]::new);
        r2 = Arrays.stream(data).map(ea -> Arrays.stream(ea)
                .map(i -> i.getPointsWithFixedSpace(2, secondHalfNum, 2))
                .toArray(TripletZ2Vector[]::new)
            ).toArray(TripletZ2Vector[][]::new);
        mergeRes = mergePair(r1, r2);
        r1 = mergeRes.getLeft();
        r2 = mergeRes.getRight();
        // Recover
        for (int i = 0; i < r1.length; i++) {
            for (int j = 0; j < r1[i].length; j++) {
                data[i][j].setPointsWithFixedSpace(r1[i][j], 1, r1[i][j].bitNum(), 2);
                data[i][j].setPointsWithFixedSpace(r2[i][j], 2, r2[i][j].bitNum(), 2);
            }
        }

        // Compaction
        TripletZ2Vector[] perm4Compact = pgSortParty.perGen(new TripletZ2Vector[]{(TripletZ2Vector) z2cParty.not(data[6][0])});
        // Permute all with one protocol invocation
        TripletZ2Vector[] allData = Arrays.stream(data).flatMap(Arrays::stream).toArray(TripletZ2Vector[]::new);
        allData = permuteParty.applyInvPermutation(perm4Compact, allData);
        for (int i = 0, index = 0; i < data.length; i++) {
            data[i] = Arrays.copyOfRange(allData, index, index + data[i].length);
            index += data[i].length;
        }

        return data;
    }

    /**
     * Merges a pair of adjacent GK tuples if they satisfy the compaction condition.
     *
     * This method implements the pairwise merge logic:
     * - Case 1 (merge R1 to R2): if t2 < t1 and g1_1 + g2_1 + g1_2 + 1 + delta1_2 ≤ ε*n
     *   Then: g1_2 = g1_1 + g2_1 + g1_2 + 1, mark R1 as dummy
     * - Case 2 (merge R2 to R1): if t1 < t2 and g1_2 + g2_2 + g2_1 + 1 + delta2_1 ≤ ε*n
     *   Then: g2_1 = g1_2 + g2_2 + g2_1 + 1, mark R2 as dummy
     *
     * The compaction condition ensures the ε-approximation guarantee is maintained.
     *
     * @param r1 the first tuple in the pair [key, g1, g2, delta1, delta2, t, dummy]
     * @param r2 the second tuple in the pair [key, g1, g2, delta1, delta2, t, dummy]
     * @return a pair of (updated_r1, updated_r2) with one possibly marked as dummy
     * @throws MpcAbortException if the protocol fails
     */
    private Pair<TripletZ2Vector[][], TripletZ2Vector[][]> mergePair(TripletZ2Vector[][] r1, TripletZ2Vector[][] r2) throws MpcAbortException {
        // Set up
        int threshold = gkTable.getThreshold();
        int attributeBitLen = gkTable.getAttributeBitLen();
        TripletZ2Vector[] r1t = r1[5];
        TripletZ2Vector[] r1g1 = r1[1];
        TripletLongVector r1g1Long = abb3Party.getConvParty().b2a(r1g1);
        TripletZ2Vector[] r1g2 = r1[2];
        TripletLongVector r1g2Long = abb3Party.getConvParty().b2a(r1g2);
        TripletZ2Vector[] r1delta2 = r1[4];
        TripletLongVector r1delta2Long = abb3Party.getConvParty().b2a(r1delta2);
        TripletZ2Vector r1Flag = r1[6][0];
        TripletZ2Vector[] r2t = r2[5];
        TripletZ2Vector[] r2g1 = r2[1];
        TripletLongVector r2g1Long = abb3Party.getConvParty().b2a(r2g1);
        TripletZ2Vector[] r2g2 = r2[2];
        TripletLongVector r2g2Long = abb3Party.getConvParty().b2a(r2g2);
        TripletZ2Vector[] r2delta1 = r2[3];
        TripletLongVector r2delta1Long = abb3Party.getConvParty().b2a(r2delta1);
        TripletZ2Vector r2Flag = r2[6][0];
        int pairNum = r1g1Long.getNum();
        // Case 1 merge R1 to R2
        TripletLongVector sum1 = (TripletLongVector) zl64cParty.add(zl64cParty.add(zl64cParty.add(r1g1Long, r1g2Long), r2g1Long), 1);
        TripletLongVector threshold1 = zl64cParty.add(sum1, r2delta1Long);
        // Case 2 merge R2 to R1
        TripletLongVector sum2 = (TripletLongVector) zl64cParty.add(zl64cParty.add(zl64cParty.add(r2g1Long, r2g2Long), r1g2Long), 1);
        TripletLongVector threshold2 = zl64cParty.add(sum2, r1delta2Long);
        // 1 for case 1; 0 for case 2
        TripletZ2Vector tRes = (TripletZ2Vector) circuit.lessThan(r2t, r1t);
        TripletZ2Vector[] thresholdVector = (TripletZ2Vector[]) z2cParty.setPublicValues(
                new BitVector[]{BitVectorFactory.create(attributeBitLen, BigInteger.valueOf(threshold))});
        thresholdVector = Arrays.stream(z2cParty.matrixTranspose(thresholdVector))
            .map(ea -> ea.extendSizeWithSameEle(pairNum))
            .toArray(TripletZ2Vector[]::new);

        TripletZ2Vector[] threshold1Z2 = abb3Party.getConvParty().a2b(threshold1, attributeBitLen);
        TripletZ2Vector[] threshold2Z2 = abb3Party.getConvParty().a2b(threshold2, attributeBitLen);
        TripletZ2Vector[] sum1Z2 = abb3Party.getConvParty().a2b(sum1, attributeBitLen);
        TripletZ2Vector[] sum2Z2 = abb3Party.getConvParty().a2b(sum2, attributeBitLen);

        TripletZ2Vector threshold1Res = (TripletZ2Vector) circuit.leq(threshold1Z2, thresholdVector);
        TripletZ2Vector threshold2Res = (TripletZ2Vector) circuit.leq(threshold2Z2, thresholdVector);
        // Check this pair is not dummy
        TripletZ2Vector valid = z2cParty.and(r1Flag, r2Flag);
        TripletZ2Vector muxFlag1 = z2cParty.and(z2cParty.and(tRes, threshold1Res), valid);
        TripletZ2Vector muxFlag2 = z2cParty.and(z2cParty.and(z2cParty.not(tRes), threshold2Res), valid);
        TripletZ2Vector zero = z2cParty.createShareZeros(pairNum);
        // Update r2g1 and r1Flag for the first case
        r2g1 = z2cParty.mux(r2g1, sum1Z2, muxFlag1);
        r1Flag = (TripletZ2Vector) z2cParty.mux(r1Flag, zero, muxFlag1);
        r2[1] = r2g1;
        r1[6][0] = r1Flag;
        // Update r1g2 and r2Flag for the second case
        r1g2 = z2cParty.mux(r1g2, sum2Z2, muxFlag2);
        r2Flag = (TripletZ2Vector) z2cParty.mux(r2Flag, zero, muxFlag2);
        r1[2] = r1g2;
        r2[6][0] = r2Flag;
        return Pair.of(r1, r2);
    }

    /**
     * Performs a quantile query on the GK sketch.
     *
     * This method implements the Query protocol of the GK algorithm:
     * Given a query rank r, returns the key whose rank is in [r-ε*n, r+ε*n].
     *
     * The query process:
     * 1. Search sketch table for the element at queried rank
     * 2. Use prefix sums to find the rank range [r_min, r_max]
     * 3. Calculate r_min = prefix_sum - delta2 - g2
     * 4. Calculate r_max = prefix_sum + delta1 - g2
     * 5. If buffer has elements, adjust result by buffer count
     *
     * The search uses binary search via comparison operations to find the
     * element whose rank range contains the queried rank.
     *
     * @param gkTable the GK sketch table to query
     * @param queryData the query data containing the rank to search for
     * @return the query result containing the key at the queried rank range
     * @throws MpcAbortException if the protocol fails
     */
    @Override
    public MpcVector[] getQuery(SketchTable gkTable, MpcVector[] queryData) throws MpcAbortException {
        // Query in sketch
        TripletZ2Vector[] extendQueryData = Arrays.stream((TripletZ2Vector[])queryData).map(
                ea -> ea.extendSizeWithSameEle(gkTable.getTableSize())
        ).toArray(TripletZ2Vector[]::new);
        int keyBitLen = ((GKTable) gkTable).getKeyBitLen();
        int attributeBitLen = ((GKTable) gkTable).getAttributeBitLen();
        TripletZ2Vector[] dataCopy = (TripletZ2Vector[]) gkTable.getSketchTable();
        TripletZ2Vector[] keyCopy = Arrays.copyOfRange(dataCopy, 0, keyBitLen);
        TripletZ2Vector[] g1Copy = Arrays.copyOfRange(dataCopy, keyBitLen, keyBitLen+attributeBitLen);
        TripletZ2Vector[] g2Copy = Arrays.copyOfRange(dataCopy, keyBitLen+attributeBitLen, keyBitLen+2*attributeBitLen);
        TripletZ2Vector[] delta1Copy = Arrays.copyOfRange(dataCopy, keyBitLen+2*attributeBitLen, keyBitLen+3*attributeBitLen);
        TripletZ2Vector[] delta2Copy = Arrays.copyOfRange(dataCopy, keyBitLen+3*attributeBitLen, keyBitLen+4*attributeBitLen);
        TripletZ2Vector flag = dataCopy[dataCopy.length-1];
        TripletLongVector g1CopyLong = abb3Party.getConvParty().b2a(g1Copy);
        TripletLongVector delta1CopyLong = abb3Party.getConvParty().b2a(delta1Copy);
        TripletLongVector g2CopyLong = abb3Party.getConvParty().b2a(g2Copy);
        TripletLongVector delta2CopyLong = abb3Party.getConvParty().b2a(delta2Copy);
        TripletLongVector flagLong = abb3Party.getConvParty().bit2a(flag);

        TripletLongVector sum = (TripletLongVector) zl64cParty.add(zl64cParty.add(g1CopyLong, g2CopyLong), 1);
        TripletLongVector[] prefixSum = traversalParty.traversalPrefix(new TripletLongVector[]{sum, flagLong}, false, false, false, false);

        // Get the index
        TripletZ2Vector leqRes = (TripletZ2Vector) circuit.leq(keyCopy, extendQueryData);
        leqRes = z2cParty.and(leqRes, flag);
        TripletZ2Vector leqResShift = leqRes.padShiftLeft(1);
        leqResShift.reduce(leqRes.bitNum());

        TripletZ2Vector muxFlag = z2cParty.xor(leqRes, leqResShift);
        TripletLongVector rMin = zl64cParty.sub(zl64cParty.sub(prefixSum[0] , delta2CopyLong), g2CopyLong);
        TripletLongVector rMax = zl64cParty.sub(zl64cParty.add(prefixSum[0], delta1CopyLong), g2CopyLong);

        TripletZ2Vector[] rMinZ2 = abb3Party.getConvParty().a2b(rMin, attributeBitLen);
        TripletZ2Vector[] rMaxZ2 = abb3Party.getConvParty().a2b(rMax, attributeBitLen);
        TripletZ2Vector[] zero = IntStream.range(0, attributeBitLen)
            .mapToObj(i -> z2cParty.createShareZeros(gkTable.getTableSize()))
            .toArray(TripletZ2Vector[]::new);

        TripletZ2Vector[] queryRMin = (TripletZ2Vector[]) circuit.mux(zero, rMinZ2, muxFlag);
        TripletZ2Vector[] queryRMax = (TripletZ2Vector[]) circuit.mux(zero, rMaxZ2, muxFlag);

        queryRMin = Arrays.stream(queryRMin).map(z2cParty::xorSelfAllElement).toArray(TripletZ2Vector[]::new);
        queryRMax = Arrays.stream(queryRMax).map(z2cParty::xorSelfAllElement).toArray(TripletZ2Vector[]::new);

        TripletLongVector minAndMax = zl64cParty.add(abb3Party.getConvParty().b2a(queryRMin), abb3Party.getConvParty().b2a(queryRMax));
        TripletZ2Vector[] queryRes = abb3Party.getConvParty().a2b(minAndMax, attributeBitLen);

        int bufferSize = gkTable.getBufferIndex();
        // Query in buffer
        if (bufferSize > 0){
            TripletZ2Vector[] extQueryData = Arrays.stream((TripletZ2Vector[])queryData).map(
                    ea -> ea.extendSizeWithSameEle(bufferSize)
            ).toArray(TripletZ2Vector[]::new);

            TripletZ2Vector[] bufferData = gkTable.getBufferTable().toArray(TripletZ2Vector[]::new);
            bufferData = z2cParty.matrixTranspose(bufferData);

            TripletZ2Vector eqRes = (TripletZ2Vector) circuit.lessThan(bufferData, extQueryData);
            TripletLongVector bufferRes = (abb3Party.getConvParty().bit2a(eqRes)).getSelfSum();
            // Return rmin+rmax+2*bufferRes
            TripletLongVector resLong = zl64cParty.add(zl64cParty.add(minAndMax, bufferRes), bufferRes);
            queryRes = abb3Party.getConvParty().a2b(resLong, attributeBitLen);
        }

        return queryRes;
    }
}