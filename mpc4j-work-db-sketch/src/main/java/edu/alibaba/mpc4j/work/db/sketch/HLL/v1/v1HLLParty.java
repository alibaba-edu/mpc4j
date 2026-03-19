package edu.alibaba.mpc4j.work.db.sketch.HLL.v1;

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
import edu.alibaba.mpc4j.work.db.sketch.utils.agg.AggFactory;
import edu.alibaba.mpc4j.work.db.sketch.utils.agg.AggParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeParty;
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
 * v1 HLL protocol
 */
public class v1HLLParty extends AbstractHLLParty implements HLLParty {
    /**
     * permute party
     */
    private final PermuteParty permuteParty;
    /**
     * agg party
     */
    private final AggParty aggParty;
    /**
     * group extreme party
     */
    private final GroupExtremeParty groupExtremeParty;
    /**
     *  sort party
     */
    private final PgSortParty pgSortParty;
    /**
     * lowMC party
     */
    private final SoprpParty lowMcParty;
    /**
     *  Traversal party
     */
    private final TraversalParty traversalParty;

    public v1HLLParty(Abb3Party abb3Party, v1HLLConfig config) {
        super(v1HLLPtoDesc.getInstance(), abb3Party, config);
        permuteParty = PermuteFactory.createParty(abb3Party, config.getPermuteConfig());
        aggParty = AggFactory.createParty(abb3Party, config.getAggConfig());
        groupExtremeParty = GroupExtremeFactory.createParty(abb3Party, config.getExtremeConfig());
        pgSortParty = PgSortFactory.createParty(abb3Party, config.getPgSortConfig());
        lowMcParty = SoprpFactory.createParty(abb3Party, config.getSoprpConfig());
        traversalParty = TraversalFactory.createParty(abb3Party, config.getTraversalConfig());
        addMultiSubPto(permuteParty, aggParty, groupExtremeParty, pgSortParty, lowMcParty,traversalParty);
    }


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

    @Override
    public void update(AbstractHLLTable hllTable, MpcVector[] elements) throws MpcAbortException {
        int inputSize = elements.length;
        if (inputSize == 0) {
            return;
        }
        // the buffer is full after update.
        if (inputSize + hllTable.getBufferIndex() >= hllTable.getTableSize()) {
            int diff = hllTable.getTableSize() - hllTable.getBufferIndex();
            MpcVector[] splitElements = new MpcVector[diff];
            MpcVector[] remainElements = new MpcVector[inputSize-diff];
            System.arraycopy(elements, 0, splitElements, 0, diff);
            System.arraycopy(elements, diff, remainElements, 0, inputSize- diff);
            updateBuffer(hllTable, splitElements);
            update(hllTable, remainElements);
        } else {
            updateBuffer(hllTable,  elements);
        }
    }

    private void updateBuffer(AbstractHLLTable hllTable, MpcVector[] elements) throws MpcAbortException {
        hllTable.getBufferTable().addAll(List.of(elements));
        if (hllTable.getBufferIndex() == hllTable.getTableSize()) {
            assert (hllTable instanceof HLLTable);
            merge((HLLTable) hllTable);
        }
    }

    private void merge(HLLTable hllTable) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        TripletZ2Vector[] bufferData=z2cParty.matrixTranspose(hllTable.getBufferTable().toArray(TripletZ2Vector[]::new));
        TripletZ2Vector[] hashData = hash(bufferData,hllTable.getEncKey());
        int logSketchSize = hllTable.getLogSketchSize();
        int hashBitLen = hllTable.getHashBitLen();
        int payloadBitLen = hllTable.getPayloadBitLen();
        int sketchSize = hllTable.getTableSize();
        int bufferSize = bufferData[0].getNum();
        assert (logSketchSize + hashBitLen <= hashData.length);
        logStepInfo(PtoState.PTO_STEP, 1, 5, resetAndGetTime(), "hash");

        stopWatch.start();
        MpcZ2Vector[] onesCount = getLeadingZeroNum(Arrays.copyOfRange(hashData, logSketchSize, logSketchSize + hashBitLen), payloadBitLen);
        logStepInfo(PtoState.PTO_STEP, 2, 5, resetAndGetTime(), "getLeadingZeroNum");

        stopWatch.start();
        // merge two table
        TripletZ2Vector[] bufferHashAndOnesCount = new TripletZ2Vector[logSketchSize + payloadBitLen];
        System.arraycopy(hashData, 0, bufferHashAndOnesCount, 0, logSketchSize);
        System.arraycopy(onesCount, 0, bufferHashAndOnesCount, logSketchSize, payloadBitLen);
        TripletZ2Vector[] indexes = (TripletZ2Vector[]) z2cParty.setPublicValues(Z2VectorUtils.getBinaryIndex(sketchSize));
        MathPreconditions.checkEqual("indexes.length", "hashData.length", indexes.length, logSketchSize);
        for (int i = 0; i < logSketchSize; i++) {
            bufferHashAndOnesCount[i].merge(indexes[i]);
        }
        for (int i = 0; i < payloadBitLen; i++) {
            bufferHashAndOnesCount[i + logSketchSize].merge(hllTable.getSketchTable()[i]);
        }
        // order by the index key, we use PgSort to enable flexible selection of sorting algorithms
        TripletZ2Vector[] keys = Arrays.copyOf(bufferHashAndOnesCount, logSketchSize);
        TripletZ2Vector[] values = Arrays.copyOfRange(bufferHashAndOnesCount, logSketchSize, logSketchSize + payloadBitLen);
        TripletZ2Vector[] perm = pgSortParty.perGenAndSortOrigin(keys);
        TripletZ2Vector[] sortedValue = permuteParty.applyInvPermutation(perm, values);
        logStepInfo(PtoState.PTO_STEP, 3, 5, resetAndGetTime(), "sort and permute");

        stopWatch.start();
        TripletZ2Vector[] sortKeysAndFlag = new TripletZ2Vector[logSketchSize + 1];
        System.arraycopy(keys, 0, sortKeysAndFlag, 0, logSketchSize);
        // add flag
        sortKeysAndFlag[logSketchSize] = z2cParty.createShareZeros(sketchSize + bufferSize);
        z2cParty.noti(sortKeysAndFlag[logSketchSize]);
        int[] keyIndexes = IntStream.range(0, logSketchSize).toArray();
        TripletZ2Vector flag = groupExtremeParty.getGroupFlag(sortKeysAndFlag, keyIndexes);
        TripletZ2Vector[] groupedValueTable = groupExtremeParty.groupExtreme(sortedValue, flag, GroupExtremeFactory.ExtremeType.MAX);
        logStepInfo(PtoState.PTO_STEP, 4, 5, resetAndGetTime(), "group extreme");

        stopWatch.start();
        // compaction
        TripletZ2Vector[] perm4Compact = pgSortParty.perGen(new TripletZ2Vector[]{groupedValueTable[payloadBitLen]});
        groupedValueTable = permuteParty.applyInvPermutation(perm4Compact, groupedValueTable);
        Arrays.stream(groupedValueTable).forEach(ea -> ea.reduce(sketchSize));

        hllTable.updateSketchTable(groupedValueTable);
        hllTable.clearBufferTable();
        logStepInfo(PtoState.PTO_STEP, 5, 5, resetAndGetTime(), "finish");

//        System.gc();
        logPhaseInfo(PtoState.PTO_END);
    }

    private MpcZ2Vector[] getLeadingZeroNum(TripletZ2Vector[] hashValues, int payloadBitLen) throws MpcAbortException {
        int eleNum = hashValues[0].getNum();
        int hash2CountLen=hashValues.length;
        TripletZ2Vector[] transposedValue=z2cParty.matrixTranspose(hashValues);
        TripletZ2Vector flag=z2cParty.createShareZeros(hash2CountLen);
        z2cParty.noti(flag);
        TripletZ2Vector[] traversedValue=traversalParty.traversalPrefix(transposedValue,flag,true,false);
        z2cParty.noti(traversedValue);
        TripletLongVector aValue= zl64cParty.createZeros(eleNum);
        TripletLongVector[] hashA = abb3Party.getConvParty().bit2a(traversedValue);
        IntStream.range(0, eleNum).forEach(i -> aValue.setElements(hashA[i].getSelfSum(), 0, i, 1));
        return abb3Party.getConvParty().a2b(aValue, payloadBitLen);
    }

    @Override
    public TripletZ2Vector[] query(AbstractHLLTable hllTable) throws MpcAbortException {
        assert (hllTable instanceof HLLTable);
        if (!hllTable.getBufferTable().isEmpty()){
            merge((HLLTable) hllTable);
        }
        TripletZ2Vector[] tableValue = (TripletZ2Vector[]) Arrays.copyOf(hllTable.getSketchTable(),((HLLTable) hllTable).getPayloadBitLen());
        TripletLongVector tableValueLong = abb3Party.getConvParty().b2a(tableValue);
        TripletLongVector tableSum = tableValueLong.getSelfSum();
        int payLoadBit = ((HLLTable) hllTable).getPayloadBitLen();
        int logSketchSize = hllTable.getLogSketchSize();
        return abb3Party.getConvParty().a2b(tableSum, payLoadBit + logSketchSize);
    }

    /**
     * @param elements: a list of elements
     * @return the hash values.
     */
    private TripletZ2Vector[] hash(TripletZ2Vector[] elements,MpcVector key) throws MpcAbortException {
        lowMcParty.setKey((TripletZ2Vector) key);
        return lowMcParty.enc(elements);
    }
}
