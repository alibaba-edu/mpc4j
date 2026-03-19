package edu.alibaba.mpc4j.work.db.sketch.GK.v1;

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
 * v1 GK protocol
 */
public class v1GKParty extends AbstractGKParty implements GKParty {
    /**
     * permute party
     */
    public final PermuteParty permuteParty;
    /**
     * group-by-sum party
     */
    public final GroupSumParty groupSumParty;
    /**
     * sorting party
     */
    public final PgSortParty pgSortParty;
    /**
     * traversal party
     */
    public final TraversalParty traversalParty;
    /**
     * Z2 circuit
     */
    public final Z2IntegerCircuit circuit;

    public v1GKParty(Abb3Party abb3Party, v1GKConfig config) {
        super(v1GKPtoDesc.getInstance(), abb3Party, config);
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

    @Override
    public void update(SketchTable gkTable, MpcVector[] newData) throws MpcAbortException{
        setGkTable((GKTable) gkTable);
        gkTable.getBufferTable().add(newData[0]);
        int bIndex = gkTable.getBufferIndex();
        // key/g1/g2/delta1/delta2/t/dummy
        if(bIndex == gkTable.getTableSize()) {
            logPhaseInfo(PtoState.PTO_BEGIN);
            stopWatch.start();
            // set up
            int oldSize = gkTable.getTableSize();
            int dataSize = getGkTable().getDataSize();
            int newSize = ((GKTable) gkTable).resize(oldSize);
            int round = LongUtils.ceilLog2(oldSize);
            int keyBitLen = ((GKTable) gkTable).getKeyBitLen();
            int attributeBitLen = ((GKTable) gkTable).getAttributeBitLen();
            // merge buffer with table
            TripletZ2Vector[] bufferData = gkTable.getBufferTable().toArray(TripletZ2Vector[]::new);
            bufferData = z2cParty.matrixTranspose(bufferData);

            TripletZ2Vector[] dataCopy = (TripletZ2Vector[]) gkTable.getSketchTable();

            TripletZ2Vector[] g1Copy = Arrays.copyOfRange(dataCopy, keyBitLen, keyBitLen+attributeBitLen);
            TripletZ2Vector[] g2Copy = Arrays.copyOfRange(dataCopy, keyBitLen+attributeBitLen, keyBitLen+2*attributeBitLen);
            TripletZ2Vector[] delta1Copy = Arrays.copyOfRange(dataCopy, keyBitLen+2*attributeBitLen, keyBitLen+3*attributeBitLen);
            TripletZ2Vector[] delta2Copy = Arrays.copyOfRange(dataCopy, keyBitLen+3*attributeBitLen, keyBitLen+4*attributeBitLen);
            TripletZ2Vector[] tCopy = Arrays.copyOfRange(dataCopy, keyBitLen+4*attributeBitLen, keyBitLen+5*attributeBitLen);
            TripletZ2Vector flag = dataCopy[dataCopy.length-1];
            // binary index from t to t+oldSize
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
            // create group flag for prefix copy
            // [1 for buffer, 0 for data]
            TripletZ2Vector groupFlag = z2cParty.createShareZeros(oldSize);
            TripletZ2Vector zeros = z2cParty.createShareZeros(oldSize);
            z2cParty.noti(groupFlag);
            groupFlag.merge(zeros);
            logStepInfo(PtoState.PTO_STEP, 1, 5, resetAndGetTime(), "set up");

            stopWatch.start();
            // add the flag bit to the first bit
            TripletZ2Vector[] flagAndKey = new TripletZ2Vector[keyBitLen+1];
            //[0 for valid, 1 for invalid, so the valid ones would at head]
            flagAndKey[0] = (TripletZ2Vector) z2cParty.not(updateFlag);
            System.arraycopy(bufferData, 0, flagAndKey, 1, keyBitLen);
            // sort and permute
            TripletZ2Vector[] perm = pgSortParty.perGenAndSortOrigin(flagAndKey);
            logStepInfo(PtoState.PTO_STEP, 2, 5, resetAndGetTime(), "sort");

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
            logStepInfo(PtoState.PTO_STEP, 3, 5, resetAndGetTime(), "permute");

            stopWatch.start();
            // copy the back one (delta1)
            t1 = traversalParty.traversalPrefix(t1, groupFlag, true, true);
            // copy the previous one (delta2)
            t2 = traversalParty.traversalPrefix(t2, groupFlag, true, false);
            // construct the new table
            delta1Copy = z2cParty.mux(delta1Copy, t1, groupFlag);
            delta2Copy = z2cParty.mux(delta2Copy, t2, groupFlag);
            logStepInfo(PtoState.PTO_STEP, 4, 5, resetAndGetTime(), "delta for new elements");

            stopWatch.start();
            TripletZ2Vector[][] newGK = new TripletZ2Vector[][]{
                Arrays.copyOfRange(flagAndKey, 1, 1 + keyBitLen),
                g1Copy,
                g2Copy,
                delta1Copy,
                delta2Copy,
                t,
                // now 1 denotes valid data
                new TripletZ2Vector[]{(TripletZ2Vector) z2cParty.not(flagAndKey[0])}
            };
            for (int i = 0; i < round; i++) {
                newGK = compress(newGK);
            }
            logStepInfo(PtoState.PTO_STEP, 5, 5, resetAndGetTime(), "compress");

            stopWatch.start();
            TripletZ2Vector[] updateTable = MatrixUtils.flat(
                    Arrays.stream(newGK).map(ea ->
                            Arrays.stream(ea).map(i ->(TripletRpZ2Vector) i).toArray(TripletRpZ2Vector[]::new)
                    ).toArray(TripletRpZ2Vector[][]::new));

            // reduce to new size
            updateTable = Arrays.stream(updateTable).map(
                    ea -> ea.getPointsWithFixedSpace(0, newSize, 1)).toArray(TripletZ2Vector[]::new);

            gkTable.updateSketchTable(updateTable);
            gkTable.clearBufferTable();
            System.gc();
            logStepInfo(PtoState.PTO_STEP, 4, 4, resetAndGetTime(), "finish");
            logPhaseInfo(PtoState.PTO_END);
        }
    }

    private TripletZ2Vector[][] compress(TripletZ2Vector[][] data) throws MpcAbortException {
        TripletZ2Vector[][] r1;
        TripletZ2Vector[][] r2;
        Pair<TripletZ2Vector[][], TripletZ2Vector[][]> mergeRes;
        // start from index 0
        int halfNum = (data[0][0].bitNum() - 1) / 2;
        r1 = Arrays.stream(data).map(ea -> Arrays.stream(ea)
                .map(i -> i.getPointsWithFixedSpace(0, halfNum, 2))
                .toArray(TripletZ2Vector[]::new)
            ).toArray(TripletZ2Vector[][]::new);
        r2 = Arrays.stream(data).map(ea -> Arrays.stream(ea)
                .map(i -> i.getPointsWithFixedSpace(1, halfNum, 2))
                .toArray(TripletZ2Vector[]::new)
            ).toArray(TripletZ2Vector[][]::new);
        mergeRes = mergePair(r1, r2);
        r1 = mergeRes.getLeft();
        r2 = mergeRes.getRight();
        // recover
        for (int i = 0; i < r1.length; i++) {
            for (int j = 0; j < r1[i].length; j++) {
                data[i][j].setPointsWithFixedSpace(r1[i][j], 0, r1[i][j].bitNum(), 2);
                data[i][j].setPointsWithFixedSpace(r2[i][j], 1, r2[i][j].bitNum(), 2);
            }
        }

        // start from index 1
        r1 = Arrays.stream(data).map(ea -> Arrays.stream(ea)
                .map(i -> i.getPointsWithFixedSpace(1, halfNum, 2))
                .toArray(TripletZ2Vector[]::new)
            ).toArray(TripletZ2Vector[][]::new);
        r2 = Arrays.stream(data).map(ea -> Arrays.stream(ea)
                .map(i -> i.getPointsWithFixedSpace(2, halfNum, 2))
                .toArray(TripletZ2Vector[]::new)
            ).toArray(TripletZ2Vector[][]::new);
        mergeRes = mergePair(r1, r2);
        r1 = mergeRes.getLeft();
        r2 = mergeRes.getRight();
        // recover
        for (int i = 0; i < r1.length; i++) {
            for (int j = 0; j < r1[i].length; j++) {
                data[i][j].setPointsWithFixedSpace(r1[i][j], 1, r1[i][j].bitNum(), 2);
                data[i][j].setPointsWithFixedSpace(r2[i][j], 2, r2[i][j].bitNum(), 2);
            }
        }

        // compaction
        TripletZ2Vector[] perm4Compact = pgSortParty.perGen(new TripletZ2Vector[]{(TripletZ2Vector) z2cParty.not(data[6][0])});
        // permute all with one protocol invocation
        TripletZ2Vector[] allData = Arrays.stream(data).flatMap(Arrays::stream).toArray(TripletZ2Vector[]::new);
        allData = permuteParty.applyInvPermutation(perm4Compact, allData);
        for (int i = 0, index = 0; i < data.length; i++) {
            data[i] = Arrays.copyOfRange(allData, index, index + data[i].length);
            index += data[i].length;
        }

        return data;
    }

    private Pair<TripletZ2Vector[][], TripletZ2Vector[][]> mergePair(TripletZ2Vector[][] r1, TripletZ2Vector[][] r2) throws MpcAbortException {
        // set up
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
        //case 1 merge R1 to R2
        TripletLongVector sum1 = (TripletLongVector) zl64cParty.add(zl64cParty.add(zl64cParty.add(r1g1Long, r1g2Long), r2g1Long), 1);
        TripletLongVector threshold1 = zl64cParty.add(sum1, r2delta1Long);
        //case 2 merge R2 to R1
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
        // check this pair is not dummy
        TripletZ2Vector valid = z2cParty.and(r1Flag, r2Flag);
        TripletZ2Vector muxFlag1 = z2cParty.and(z2cParty.and(tRes, threshold1Res), valid);
        TripletZ2Vector muxFlag2 = z2cParty.and(z2cParty.and(z2cParty.not(tRes), threshold2Res), valid);
        TripletZ2Vector zero = z2cParty.createShareZeros(pairNum);
        // update r2g1 and r1Flag for the first case
        r2g1 = z2cParty.mux(r2g1, sum1Z2, muxFlag1);
        r1Flag = (TripletZ2Vector) z2cParty.mux(r1Flag, zero, muxFlag1);
        r2[1] = r2g1;
        r1[6][0] = r1Flag;
        // update r1g2 and r2Flag for the second case
        r1g2 = z2cParty.mux(r1g2, sum2Z2, muxFlag2);
        r2Flag = (TripletZ2Vector) z2cParty.mux(r2Flag, zero, muxFlag2);
        r1[2] = r1g2;
        r2[6][0] = r2Flag;
        return Pair.of(r1, r2);
    }

    @Override
    public MpcVector[] getQuery(SketchTable gkTable, MpcVector[] queryData) throws MpcAbortException{
        // query in sketch
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

        // get the index
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
        // query in buffer
        if (bufferSize > 0){
            TripletZ2Vector[] extQueryData = Arrays.stream((TripletZ2Vector[])queryData).map(
                    ea -> ea.extendSizeWithSameEle(bufferSize)
            ).toArray(TripletZ2Vector[]::new);

            TripletZ2Vector[] bufferData = gkTable.getBufferTable().toArray(TripletZ2Vector[]::new);
            bufferData = z2cParty.matrixTranspose(bufferData);

            TripletZ2Vector eqRes = (TripletZ2Vector) circuit.lessThan(bufferData, extQueryData);
            TripletLongVector bufferRes = (abb3Party.getConvParty().bit2a(eqRes)).getSelfSum();
            // return rmin+rmax+2*bufferRes
            TripletLongVector resLong = zl64cParty.add(zl64cParty.add(minAndMax, bufferRes), bufferRes);
            queryRes = abb3Party.getConvParty().a2b(resLong, attributeBitLen);
        }

        return queryRes;
    }
}
