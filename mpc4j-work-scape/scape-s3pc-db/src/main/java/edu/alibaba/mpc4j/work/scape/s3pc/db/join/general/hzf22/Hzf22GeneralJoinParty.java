package edu.alibaba.mpc4j.work.scape.s3pc.db.join.general.hzf22;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.zlong.PlainLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.general.AbstractGeneralJoinParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.general.GeneralJoinFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.general.GeneralJoinParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.InputProcessUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPerOperations;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPerOperations.FillPerFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPermutationFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPermutationParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.SortSignFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.SortSignFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.SortSignParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations.PgSortFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalOperations;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalOperations.TraversalFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalParty;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * HZF22 general join party
 *
 * @author Feng Han
 * @date 2025/2/20
 */
public class Hzf22GeneralJoinParty extends AbstractGeneralJoinParty implements GeneralJoinParty {
    /**
     * sorting party
     */
    protected final PgSortParty sortParty;
    /**
     * permute party
     */
    protected final PermuteParty permuteParty;
    /**
     * oblivious traversal party
     */
    protected final TraversalParty traversalParty;
    /**
     * the party to fill the injective function into a permutation
     */
    protected final FillPermutationParty fillPermutationParty;
    /**
     * the party to fill the injective function into a permutation
     */
    protected final SortSignParty sortSignParty;

    public Hzf22GeneralJoinParty(Abb3Party abb3Party, Hzf22GeneralJoinConfig config) {
        super(Hzf22GeneralJoinPtoDesc.getInstance(), abb3Party, config);
        sortParty = PgSortFactory.createParty(abb3Party, config.getPgSortConfig());
        permuteParty = PermuteFactory.createParty(abb3Party, config.getPermuteConfig());
        traversalParty = TraversalFactory.createParty(abb3Party, config.getTraversalConfig());
        fillPermutationParty = FillPermutationFactory.createParty(abb3Party, config.getFillPermutationConfig());
        sortSignParty = SortSignFactory.createParty(abb3Party, config.getSortSignConfig());
        addMultiSubPto(sortParty, permuteParty, traversalParty, fillPermutationParty, sortSignParty);
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        abb3Party.init();
        permuteParty.init();
        sortParty.init();
        traversalParty.init();
        fillPermutationParty.init();
        sortSignParty.init();
        initState();
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }


    @Override
    public long[] setUsage(GeneralJoinFnParam... params) {
        long[] tuples = new long[]{0, 0};
        if (isMalicious) {
            for (GeneralJoinFnParam param : params) {
                int totalNum = param.resultUpperBound;
                // alignment mul
                abb3Party.updateNum(0, 4L * totalNum);
                tuples[1] += 4L * totalNum;
                // functions
                long[] sortTuple = sortParty.setUsage(new PgSortFnParam(PgSortOperations.PgSortOp.SORT_A, totalNum, 1, 1));
                long[] traversalTuple = traversalParty.setUsage(
                    new TraversalFnParam(TraversalOperations.TraversalOp.TRAVERSAL_A, totalNum, 4),
                    new TraversalFnParam(TraversalOperations.TraversalOp.TRAVERSAL_A, totalNum, 5),
                    new TraversalFnParam(TraversalOperations.TraversalOp.TRAVERSAL_A, totalNum, keyDim + param.leftValueDim + 1),
                    new TraversalFnParam(TraversalOperations.TraversalOp.TRAVERSAL_A, totalNum, keyDim + param.rightValueDim + 1)
                );
                long[] fillPermTuple = fillPermutationParty.setUsage(new FillPerFnParam(FillPerOperations.FillPerOp.FILL_ONE_PER_A, totalNum, param.leftDataNum, param.rightDataNum));
                long[] sortSignTuple = sortSignParty.setUsage(new SortSignFnParam(param.inputIsSorted, param.keyDim, param.leftDataNum, param.rightDataNum));
                tuples[0] += sortTuple[0] + traversalTuple[0] + fillPermTuple[0] + sortSignTuple[0];
                tuples[1] += sortTuple[1] + traversalTuple[1] + fillPermTuple[1] + sortSignTuple[1];
            }
        }
        return tuples;
    }

    @Override
    public TripletLongVector[] innerJoin(TripletLongVector[] left, TripletLongVector[] right,
                                         int[] leftKeyIndex, int[] rightKeyIndex, int m, boolean inputIsSorted) throws MpcAbortException {
        setPtoInput(left, right, leftKeyIndex, rightKeyIndex, m, inputIsSorted);
        logPhaseInfo(PtoState.PTO_BEGIN, "general innerJoin");

        stopWatch.start();
        // 1. posCal
        TripletLongVector[] posResult = groupDimension();
        logStepInfo(PtoState.PTO_STEP, 1, 6, resetAndGetTime(), "finish groupDimension");

        stopWatch.start();
        // 2. separation
        int leftLen = left[0].getNum(), rightLen = right[0].getNum();
        TripletLongVector trueLen = zl64cParty.createZeros(2);
        TripletLongVector[][] sepResult = this.separation(posResult, leftLen, rightLen, trueLen);
        whetherSizeEnough(trueLen, m);
        logStepInfo(PtoState.PTO_STEP, 2, 6, resetAndGetTime(), "finish separation");

        stopWatch.start();
        // 3. 左表右表的dist操作
        TripletLongVector[] twoPai = fillPermutationParty.twoPermutationCompletion(sepResult[0][sepResult[0].length - 2], sepResult[0][0],
            sepResult[1][sepResult[1].length - 2], sepResult[1][0], m);
        TripletLongVector[][] twoDistResult = this.distributionWithPermutation(sepResult[0], twoPai[0], sepResult[1], twoPai[1]);
        TripletLongVector[] leftDistResult = twoDistResult[0];
        TripletLongVector[] rightDistResult = twoDistResult[1];
        logStepInfo(PtoState.PTO_STEP, 3, 6, resetAndGetTime(), "finish distribution");

        stopWatch.start();
        // 4. 左表右表的expansion操作
        TripletLongVector[] leftExpResult = this.expansion(leftDistResult, false);
        TripletLongVector[] rightExpResult = this.expansion(rightDistResult, true);
        logStepInfo(PtoState.PTO_STEP, 4, 6, resetAndGetTime(), "finish expansion");

        stopWatch.start();
        // 5. 右表的alignment操作
        TripletLongVector[] alignResult = this.alignment(rightExpResult, rightKeyIndex.length);
        logStepInfo(PtoState.PTO_STEP, 5, 6, resetAndGetTime(), "finish alignment");

        stopWatch.start();
        // 6. 当前的结果 [(key, [payload], E, F)] * 2，拼接得到最终结果
        int leftDim = leftExpResult.length;
        int rightDim = alignResult.length;
        int eLeft = leftDim - 2, fRight = rightDim - 1;
        int keyDim = leftKeyIndex.length;
        TripletLongVector[] result = new TripletLongVector[leftDim + rightDim - 3 - keyDim];
        if (eLeft >= 0) {
            System.arraycopy(leftExpResult, 0, result, 0, eLeft);
        }
        if (fRight - keyDim >= 0) {
            System.arraycopy(alignResult, keyDim, result, eLeft, fRight - keyDim);
        }
        logStepInfo(PtoState.PTO_STEP, 6, 6, resetAndGetTime(), "re-organize the output");

        logPhaseInfo(PtoState.PTO_END, "general innerJoin");
        return result;
    }

    /**
     * Based on the sorted result, compute the group information of each join_key. Including:
     * (1). ID: the source of the sorted join_key. 0 if it comes from the left table; 1 otherwise.
     * (2). E: 1 if there exist the same join_key in the other table; 0 otherwise.
     * (3). A0: how many rows in the left table have the same join_key as the row
     * (4). A1: how many rows in the right table have the same join_key as the row
     * (5). RI: the index of the current row in the group that consists of all rows in the right table with the same join_key
     * (6). kPai: the permutation representing the sorting of the join_key
     * (7). sPai: the permutation according to the table_id and E
     *
     * @return (ID, E, A0, A1, RI, kPai, sPai)
     */
    private TripletLongVector[] groupDimension() throws MpcAbortException {
        TripletLongVector[] sortRes = sortSignParty.preSort(
            Arrays.copyOf(newLeft, keyDim), Arrays.copyOf(newRight, keyDim),
            newLeft[newLeft.length - 1], newRight[newRight.length - 1], inputIsSorted);
        TripletLongVector e1 = sortRes[0];
        TripletLongVector eWithUpper = sortRes[1];
        TripletLongVector eWithBelow = sortRes[2];
        TripletLongVector shuffledId = sortRes[3];
        TripletLongVector kPai = sortRes[4];

        TripletLongVector[] firstErgodic = traversalParty.traversalPrefix(
            new TripletLongVector[]{
                e1,
                (TripletLongVector) zl64cParty.add(zl64cParty.neg(shuffledId), 1L),
                shuffledId,
                eWithUpper},
            false, false, true, false);
        TripletLongVector ri = (TripletLongVector) firstErgodic[2].copy();

        TripletLongVector[] invInput = new TripletLongVector[firstErgodic.length + 1];
        System.arraycopy(firstErgodic, 0, invInput, 0, firstErgodic.length);
        invInput[firstErgodic.length] = eWithBelow;
        TripletLongVector[] secondErgodic = traversalParty.traversalPrefix(invInput, true, false, true, true);

        // 6. 生成新的置换
        TripletLongVector invEqSign = (TripletLongVector) zl64cParty.add(zl64cParty.neg(secondErgodic[0]), 1L);
        TripletLongVector sPai = sortParty.perGen4MultiDim(new TripletLongVector[]{shuffledId, invEqSign}, new int[]{1, 1});
        // 7. 将所有结果合在一起输出, (ID, E, A0, A1, RI, kPai, sPai)
        return new TripletLongVector[]{shuffledId, secondErgodic[0], secondErgodic[1], secondErgodic[2], ri, kPai, sPai};
    }

    /**
     * 实现separation过程，完成position的计算以及两个表格的分离
     *
     * @param input   (7, n1+n2) : (ID, E, A0, A1, RI, kPai, sPai)
     * @param n1      左表的原始数量
     * @param n2      右表的原始数量
     * @param trueLen 用于记录左、右表延长之后的真实长度
     * @return [[3, n1], [6, n2]] left:[E, P, skPaiLeft], right:[E, A0, A1, RI, P, skPaiRight]
     */
    private TripletLongVector[][] separation(TripletLongVector[] input, int n1, int n2, TripletLongVector trueLen) throws MpcAbortException {
        // 输入的input为    ID, E, A0, A1, RI, kPai, sPai
        Preconditions.checkArgument(input.length == 7 && input[0].getNum() == n1 + n2);
        // left:[E, P, skPaiLeft], right:[E, A0, A1, RI, P, skPaiRight]
        TripletLongVector[] xPartLeft = new TripletLongVector[3], xPartRight = new TripletLongVector[6];
        // 1. 处理置换的结果，如果是预先排好序的，那么只需要用sPai即可，否则需要组合置换
        TripletLongVector skPai = permuteParty.composePermutation(input[5], input[6])[0];
        // 取固定长度并减去长度
        xPartLeft[2] = skPai.copyOfRange(0, n1);
        xPartRight[5] = skPai.copyOfRange(n1, n1 + n2);
        zl64cParty.addi(xPartRight[5], -n1);
        // 2. 处理主体的结果
        TripletLongVector[] xPart = Arrays.copyOfRange(input, 1, 5);
        // 后续只需保留  E, A0, A1, RI
        xPart = permuteParty.applyInvPermutation(input[6], xPart);
        int lastLen = 2;
        TripletLongVector[] xPartFinal = xPart;
        // left 只保留 E, 但还是先将A0， A1 copy到指定位置，方便后面的distribution position
        xPartLeft[0] = xPart[0].copyOfRange(0, n1);
        xPartLeft[1] = xPart[2].copyOfRange(0, n1);
        // right 保留 E, A0, A1, RI
        IntStream.range(0, xPartRight.length - lastLen).forEach(i -> xPartRight[i] = xPartFinal[i].copyOfRange(n1, n1 + n2));
        // 3. 计算distribution position
        TripletLongVector leftPos = this.positionDistribution(xPartLeft[0], xPartLeft[1], trueLen, 0);
        TripletLongVector rightPos = this.positionDistribution(xPartRight[0], xPartRight[1], trueLen, 1);
        // 4. 与原始的方案比较，不再需要计算Pm了，因为可以简单通过 Pm=P-(RI-1)·A0 得到，故直接放在最后一步进行
        xPartLeft[1] = leftPos;
        xPartRight[4] = rightPos;
        return new TripletLongVector[][]{xPartLeft, xPartRight};
    }

    /**
     * 计算distribution position
     *
     * @param eSign  对应的E
     * @param aValue 左表输入A1，右表输入A0
     * @return 向量P，代表最终所需的位置
     */
    private TripletLongVector positionDistribution(TripletLongVector eSign, TripletLongVector aValue, TripletLongVector trueLen, int whichOne) {
        Preconditions.checkArgument(eSign.getNum() == aValue.getNum());

        TripletLongVector addValue = zl64cParty.sub(aValue, zl64cParty.add(eSign, -1L));
        TripletLongVector count = (TripletLongVector) zl64cParty.setPublicValue(LongVector.createZeros(1));
        TripletLongVector trueRank = zl64cParty.rowAdderWithPrefix(addValue, count, false);
        zl64cParty.addi(trueRank, -1L);
        trueLen.setElements(count, 0, whichOne, 1);
        return trueRank;
    }

    /**
     * 执行distribution步骤
     *
     * @param xPartLeft/xPartRight 对于left而言，输入为 [E, P, skPaiLeft]， 对于right而言，输入为 [E, A0, A1, RI, P, skPaiRight]
     * @param dPaiLeft/dPaiRight   补完之后的permutation P
     * @return 对于left而言，输出为 [key, [payload], E, F]， 对于right而言，输入为 [key, [payload], E, A0, A1, RI, F]
     */
    private TripletLongVector[][] distributionWithPermutation(
        TripletLongVector[] xPartLeft, TripletLongVector dPaiLeft,
        TripletLongVector[] xPartRight, TripletLongVector dPaiRight) throws MpcAbortException {
        MathPreconditions.checkEqual("xPartLeft[0].getNum()", "allLeft[0].getNum()", xPartLeft[0].getNum(), newLeft[0].getNum());
        MathPreconditions.checkEqual("xPartRight[0].getNum()", "allRight[0].getNum()", xPartRight[0].getNum(), newRight[0].getNum());
        MathPreconditions.checkEqual("dPaiLeft.getNum()", "dPaiRight.getNum()", dPaiLeft.getNum(), dPaiRight.getNum());
        MathPreconditions.checkGreaterOrEqual("dPaiLeft.getNum() >= xPartLeft[0].getNum()", dPaiLeft.getNum(), xPartLeft[0].getNum());
        MathPreconditions.checkGreaterOrEqual("dPaiRight.getNum() >= xPartRight[0].getNum()", dPaiRight.getNum(), xPartRight[0].getNum());

        int m = dPaiRight.getNum();
        int dimLeft = xPartLeft.length, dimRight = xPartRight.length;
        int lenLeft = xPartLeft[0].getNum(), lenRight = xPartRight[0].getNum();
        // 1. 生成假置换，并组合得到payload的置换
        LongVector indexVec = LongVector.create(LongStream.range(0, (long) m << 1).toArray());
        TripletLongVector skPaiFull = (TripletLongVector) zl64cParty.setPublicValue(indexVec);
        TripletLongVector dPaiTwo = zl64cParty.createZeros(m << 1);
        // 处理left
        skPaiFull.setElements(xPartLeft[dimLeft - 1], 0, 0, lenLeft);
        dPaiTwo.setElements(dPaiLeft, 0, 0, m);
        // 对right的所有数都加上个m
        skPaiFull.setElements((TripletLongVector) zl64cParty.add(xPartRight[dimRight - 1], m), 0, m, lenRight);
        dPaiTwo.setElements((TripletLongVector) zl64cParty.add(dPaiRight, m), 0, m, m);
        // 一次性置换
        TripletLongVector payloadPerTwo = permuteParty.composePermutation(skPaiFull, dPaiTwo)[0];
        // 2. 如果是malicious，那么在结束per的生成之后，对数据生成mac
        TripletLongVector[] xPartLeftShuffle = Arrays.copyOf(xPartLeft, dimLeft - 2);
        TripletLongVector[] xPartRightShuffle = Arrays.copyOf(xPartRight, dimRight - 2);
        TripletLongVector payloadPerLeft = payloadPerTwo.copyOfRange(0, m);
        TripletLongVector payloadPerRight = (TripletLongVector) zl64cParty.add(payloadPerTwo.copyOfRange(m, m << 1), -m);
        // 3. 生成假数据
        TripletLongVector[] xPartWithDummyLeft = Arrays.stream(xPartLeftShuffle).map(tripletZl64Vector -> {
            TripletLongVector tmp = (TripletLongVector) tripletZl64Vector.copy();
            tmp.paddingZeros(m - tmp.getNum());
            return tmp;
        }).toArray(TripletLongVector[]::new);
        TripletLongVector[] xPartWithDummyRight = Arrays.stream(xPartRightShuffle).map(tripletZl64Vector -> {
            TripletLongVector tmp = (TripletLongVector) tripletZl64Vector.copy();
            tmp.paddingZeros(m - tmp.getNum());
            return tmp;
        }).toArray(TripletLongVector[]::new);
        // 4. 对payload生成假数据
        TripletLongVector[] payloadWithDummyLeft = Arrays.stream(newLeft).map(x -> {
            TripletLongVector tmp = (TripletLongVector) x.copy();
            tmp.paddingZeros(m - tmp.getNum());
            return tmp;
        }).toArray(TripletLongVector[]::new);
        TripletLongVector[] payloadWithDummyRight = Arrays.stream(newRight).map(x -> {
            TripletLongVector tmp = (TripletLongVector) x.copy();
            tmp.paddingZeros(m - tmp.getNum());
            return tmp;
        }).toArray(TripletLongVector[]::new);
        // 3. 对 xPartWithDummy 进行置换
        xPartWithDummyLeft = permuteParty.applyInvPermutation(dPaiLeft, xPartWithDummyLeft);
        xPartWithDummyRight = permuteParty.applyInvPermutation(dPaiRight, xPartWithDummyRight);
        // 对payload进行置换
        payloadWithDummyLeft = permuteParty.applyInvPermutation(payloadPerLeft, payloadWithDummyLeft);
        payloadWithDummyRight = permuteParty.applyInvPermutation(payloadPerRight, payloadWithDummyRight);
        // 4.拼接得到结果
        TripletLongVector[] leftResult = InputProcessUtils.appendAttributes(payloadWithDummyLeft, xPartWithDummyLeft);
        TripletLongVector[] rightResult = InputProcessUtils.appendAttributes(payloadWithDummyRight, xPartWithDummyRight);
        return new TripletLongVector[][]{leftResult, rightResult};
    }

    /**
     * 执行expansion步骤，之前的实现中P当错了，现在的P是最后一个位置，所以不会出现无限往下复制的情况，因为实际上是需要往前复制
     *
     * @param input   对于left而言，输入为 [key, [payload], E, F]， 对于right而言，输入为 [key, [payload], E, A0, A1, RI, F]
     * @param isRight 是否处理right表
     * @return 对于left而言，输出为 [key, [payload], E, F]， 对于right而言，输出为 [key, [payload], E, A0, A1, RI, C_D, F]
     */
    private TripletLongVector[] expansion(TripletLongVector[] input, boolean isRight) {
        input[input.length - 1] = (TripletLongVector) zl64cParty.add(zl64cParty.neg(input[input.length - 1]), 1L);
        TripletLongVector[] invTreeRes = traversalParty.traversalPrefix(input, true, true, true, true);
        invTreeRes[invTreeRes.length - 1] = (TripletLongVector) zl64cParty.add(zl64cParty.neg(invTreeRes[invTreeRes.length - 1]), 1L);
        TripletLongVector[] finalRes = invTreeRes;
        if (isRight) {
            TripletLongVector beforeA0 = input[input.length - 4];
            finalRes = new TripletLongVector[invTreeRes.length + 1];
            System.arraycopy(invTreeRes, 0, finalRes, 0, invTreeRes.length - 1);
            // 计算cd的方式是: cd[i] = cd[i+1] + a0[i] - e[i]
            TripletLongVector one = (TripletLongVector) zl64cParty.setPublicValue(LongVector.createOnes(1));
            finalRes[invTreeRes.length - 1] = zl64cParty.rowAdderWithPrefix(zl64cParty.sub(beforeA0, invTreeRes[invTreeRes.length - 5]), one, true);
            finalRes[invTreeRes.length] = invTreeRes[invTreeRes.length - 1];
        }
        return finalRes;
    }

    /**
     * 对右表需要进行alignment运算
     * 只需要置换payload部分
     *
     * @param x 输入为 [key, [payload], E, A0, A1, RI, C_D, F]
     * @return [key, [payload], E, F]
     */
    private TripletLongVector[] alignment(TripletLongVector[] x, int keyDim) throws MpcAbortException {
        int dim = x.length, len = x[0].getNum();
        int eIndex = dim - 6, a0Index = dim - 5, a1Index = dim - 4, riIndex = dim - 3, cdIndex = dim - 2;
        TripletLongVector[] result = new TripletLongVector[dim - 4];
        IntStream.range(0, keyDim).forEach(i -> result[i] = x[i]);
        if (keyDim < eIndex) {
            // 1. 计算 Pm = i-(RI-1)·A0 - (C_D-1)
            TripletLongVector[] firstMul = zl64cParty.mul(
                new TripletLongVector[]{x[riIndex], x[cdIndex]},
                new TripletLongVector[]{x[a0Index], x[a1Index]});
            TripletLongVector pm = zl64cParty.sub(zl64cParty.sub(x[a0Index], firstMul[0]), x[cdIndex]);
            pm = zl64cParty.add(pm, PlainLongVector.create(LongStream.range(0, len).toArray()));
            // 2. 计算 aPai = Pm + (C_D-1)·A1 + RI - 1
            TripletLongVector aPai = zl64cParty.add(firstMul[1], pm);
            aPai = zl64cParty.add(zl64cParty.sub(aPai, x[a1Index]), x[riIndex]);
            // 3. 计算 aPai = E·(aPai-i)+i
            long[] rangePlain = LongStream.range(0, len).toArray();
            PlainLongVector rangePlainVec = PlainLongVector.create(rangePlain);
            TripletLongVector eConstMul = zl64cParty.mul(x[eIndex], rangePlainVec);
            TripletLongVector secondMul = zl64cParty.mul(x[eIndex], aPai);
            aPai = zl64cParty.add(zl64cParty.sub(secondMul, eConstMul), rangePlainVec);
            // 4. 置换输入
            System.arraycopy(permuteParty.applyInvPermutation(aPai, Arrays.copyOfRange(x, keyDim, eIndex)),
                0, result, keyDim, eIndex - keyDim);
        }
        result[dim - 6] = x[eIndex];
        result[dim - 5] = x[dim - 1];
        return result;
    }

    private void whetherSizeEnough(TripletLongVector trueLen, int m) throws MpcAbortException {
        TripletLongVector additionalNum = (TripletLongVector) zl64cParty.add(zl64cParty.neg(trueLen), m);
        TripletZ2Vector result = abb3Party.getConvParty().bitExtraction(additionalNum, 0);
        z2cParty.compareView4Zero(result);
    }
}
