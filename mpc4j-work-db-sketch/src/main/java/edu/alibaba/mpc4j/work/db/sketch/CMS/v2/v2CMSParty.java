package edu.alibaba.mpc4j.work.db.sketch.CMS.v2;

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
 * v2 CMS protocol
 */
public class v2CMSParty extends AbstractCMSParty implements CMSParty {
    /**
     * permute party
     */
    public final PermuteParty permuteParty;
    /**
     * group-by-sum party
     */
    public final Hzf22ExtGroupSumParty groupSumParty;
    /**
     * sorting party
     */
    public final PgSortParty pgSortParty;
    /**
     * truncate party
     */
    public final TruncateParty truncateParty;
    /**
     * lowMc party
     */
    private final SoprpParty lowMcParty;
    /**
     * Z2 circuit
     */
    public final Z2IntegerCircuit circuit;
    /**
     * CMS table
     */
    protected Z2CMSTable cmsTable;

    public v2CMSParty(Abb3Party abb3Party, v2CMSConfig config) {
        super(v2CMSPtoDesc.getInstance(), abb3Party, config);
        permuteParty = PermuteFactory.createParty(abb3Party, config.getPermuteConfig());
        groupSumParty = (Hzf22ExtGroupSumParty) GroupSumFactory.createParty(abb3Party, config.getGroupSumConfig());
        pgSortParty = PgSortFactory.createParty(abb3Party, config.getPgSortConfig());
        lowMcParty= SoprpFactory.createParty(abb3Party,config.getSoprpConfig());
        truncateParty = TruncateFactory.createParty(abb3Party, config.getTruncateConfig());
        circuit = new Z2IntegerCircuit(z2cParty, new Z2CircuitConfig.Builder().setComparatorType(ComparatorFactory.ComparatorType.TREE_COMPARATOR).build());
        addMultiSubPto(permuteParty, groupSumParty, pgSortParty, lowMcParty, truncateParty);
    }

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

    private void setCmsTable(Z2CMSTable cmsTable) {
        this.cmsTable = cmsTable;
    }

    @Override
    public void update(SketchTable cmsTable, MpcVector[] newData) throws MpcAbortException{
        setCmsTable((Z2CMSTable) cmsTable);
        int bIndex = cmsTable.getBufferIndex();
        int logSketchSize = ((Z2CMSTable) cmsTable).getLogSketchSize();
        int payloadBitNum = ((Z2CMSTable) cmsTable).getPayloadBitLen();
        // simple insert the new data
        cmsTable.getBufferTable().add(newData[0]);
        // merge buffer if need
        // data: id/payload/dummy
        if(bIndex == cmsTable.getTableSize() - 1){
            int sketchSize = cmsTable.getTableSize();
            logPhaseInfo(PtoState.PTO_BEGIN);

            stopWatch.start();
            // hash the value
            TripletZ2Vector[] bufferData = cmsTable.getBufferTable().toArray(TripletZ2Vector[]::new);
            bufferData = z2cParty.matrixTranspose(bufferData);
            lowMcParty.setKey((TripletZ2Vector) ((Z2CMSTable) cmsTable).getHashParameters().getEncKey());
            TripletZ2Vector[] hashData = lowMcParty.enc(bufferData);
            logStepInfo(PtoState.PTO_STEP, 1, 4, resetAndGetTime(), "soprp time");

            stopWatch.start();
            // merge two table
            TripletZ2Vector[] bufferHashAndOnes = new TripletZ2Vector[logSketchSize + payloadBitNum];
            // hash values
            System.arraycopy(hashData, 0, bufferHashAndOnes, 0, logSketchSize);
            TripletZ2Vector[] indexes = (TripletZ2Vector[]) z2cParty.setPublicValues(Z2VectorUtils.getBinaryIndex(sketchSize));
            MathPreconditions.checkEqual("indexes.length", "hashData.length", indexes.length, logSketchSize);
            for (int i = 0; i < logSketchSize; i++) {
                bufferHashAndOnes[i].merge(indexes[i]);
            }
            // set payload to be 1
            for (int i = logSketchSize; i < bufferHashAndOnes.length; i++) {
                bufferHashAndOnes[i] = z2cParty.createShareZeros(sketchSize);
            }
            // add 1
            z2cParty.noti(bufferHashAndOnes[bufferHashAndOnes.length - 1]);
            for (int i = 0; i < payloadBitNum; i++) {
                bufferHashAndOnes[i + logSketchSize].merge(cmsTable.getSketchTable()[i]);
            }
            // order by the index key, we use PgSort to enable flexible selection of sorting algorithms
            TripletZ2Vector[] keys = Arrays.copyOf(bufferHashAndOnes, logSketchSize);
            TripletZ2Vector[] perm = pgSortParty.perGenAndSortOrigin(keys);
            logStepInfo(PtoState.PTO_STEP, 2, 4, resetAndGetTime(), "sort time");

            stopWatch.start();
            TripletZ2Vector[] sortCount = permuteParty.applyInvPermutation(perm, Arrays.copyOfRange(bufferHashAndOnes, logSketchSize, logSketchSize + payloadBitNum));
            logStepInfo(PtoState.PTO_STEP, 3, 4, resetAndGetTime(), "permutation time");

            stopWatch.start();
            TripletZ2Vector[] sortKeysAndFlag = new TripletZ2Vector[logSketchSize + 1];
            System.arraycopy(keys, 0, sortKeysAndFlag, 0, logSketchSize);
            // add flag
            sortKeysAndFlag[logSketchSize] = z2cParty.createShareZeros(2 * sketchSize);
            z2cParty.noti(sortKeysAndFlag[logSketchSize]);
            int[] keyIndexes = IntStream.range(0, logSketchSize).toArray();
            TripletZ2Vector flag = groupSumParty.getGroupFlag(sortKeysAndFlag, keyIndexes);
            TripletLongVector convFlag = abb3Party.getConvParty().bit2a(flag);
            TripletLongVector convCount = abb3Party.getConvParty().b2a(sortCount);
            TripletLongVector[] aGroupSum = truncateParty.groupSumAndTruncate(new TripletLongVector[]{convCount}, convFlag, sketchSize);
            TripletZ2Vector[] bGroupSum = abb3Party.getConvParty().a2b(aGroupSum[0], payloadBitNum);
            // truncation the table and update table
            cmsTable.updateSketchTable(bGroupSum);
            cmsTable.clearBufferTable();
            logStepInfo(PtoState.PTO_STEP, 4, 4, resetAndGetTime());

            logPhaseInfo(PtoState.PTO_END);
            System.gc();
        }
    }

    /**
     * point query function
     *
     * @param cmsTable cms table
     * @param queryData query data (raw data)
     */
    @Override
    public TripletZ2Vector[] getQuery(SketchTable cmsTable, MpcVector[] queryData) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // set up for query
        int sketchTableSize = cmsTable.getTableSize();
        int logSketchSize = LongUtils.ceilLog2(sketchTableSize);
        int payloadBitLen = ((Z2CMSTable) cmsTable).getPayloadBitLen();
        TripletZ2Vector[] rawQuery = z2cParty.matrixTranspose((Arrays.stream(queryData).map(ea -> (TripletZ2Vector) ea)).toArray(TripletZ2Vector[]::new));
        // query in buffer
        int bufferSize = cmsTable.getBufferIndex();
        TripletLongVector resInBuffer = zl64cParty.createZeros(1);
        if (bufferSize > 0) {
            TripletZ2Vector[] buffer = cmsTable.getBufferTable().toArray(TripletZ2Vector[]::new);
            buffer = z2cParty.matrixTranspose(buffer);
            TripletZ2Vector[] extRawQ = Arrays.stream(rawQuery).map(ea -> ea.extendSizeWithSameEle(bufferSize)).toArray(TripletZ2Vector[]::new);
            TripletZ2Vector eqResBuffer = (TripletZ2Vector) circuit.eq(buffer, extRawQ);
            TripletLongVector eqResBufferA = abb3Party.getConvParty().bit2a(eqResBuffer);
            resInBuffer = eqResBufferA.getSelfSum();
        }

        TripletZ2Vector[] tmpHash = Arrays.copyOf(lowMcParty.enc(rawQuery), logSketchSize);

        // query in sketch
        TripletZ2Vector[] tableIndex = (TripletZ2Vector[]) z2cParty.setPublicValues(Z2VectorUtils.getBinaryIndex(cmsTable.getTableSize()));
        // extend bit to tableSize
        tmpHash = Arrays.stream(tmpHash).map(ea -> ea.extendSizeWithSameEle(sketchTableSize)).toArray(TripletZ2Vector[]::new);
        // check index
        TripletZ2Vector eqRes = (TripletZ2Vector) circuit.eq(tmpHash, tableIndex);
        TripletZ2Vector[] tableCount = z2cParty.and(eqRes, (TripletZ2Vector[]) cmsTable.getSketchTable());
        for (int i = 0; i < tableCount.length; i++) {
            tableCount[i] = (TripletZ2Vector) z2cParty.xorSelfAllElement(tableCount[i]);
        }
        TripletZ2Vector[] res;
        if (bufferSize > 0) {
            res = tableCount;
        }else{
            TripletLongVector resInTableA = abb3Party.getConvParty().b2a(tableCount);
            res = abb3Party.getConvParty().a2b(zl64cParty.add(resInTableA, resInBuffer), payloadBitLen);
        }
        logStepInfo(PtoState.PTO_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END);
        return res;
    }
}
