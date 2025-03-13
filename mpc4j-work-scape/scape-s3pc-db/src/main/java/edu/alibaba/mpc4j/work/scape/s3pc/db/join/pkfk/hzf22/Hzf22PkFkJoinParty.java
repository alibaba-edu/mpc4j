package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk.hzf22;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk.AbstractPkFkJoinParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk.PkFkJoinFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk.PkFkJoinParty;
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
 * HZF22 PkFk join party
 *
 * @author Feng Han
 * @date 2025/2/20
 */
public class Hzf22PkFkJoinParty extends AbstractPkFkJoinParty implements PkFkJoinParty {
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

    public Hzf22PkFkJoinParty(Abb3Party abb3Party, Hzf22PkFkJoinConfig config) {
        super(Hzf22PkFkJoinPtoDesc.getInstance(), abb3Party, config);
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
        permuteParty.init();
        sortParty.init();
        traversalParty.init();
        fillPermutationParty.init();
        sortSignParty.init();
        abb3Party.init();
        initState();
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public long[] setUsage(PkFkJoinFnParam... params) {
        long[] tuples = new long[]{0, 0};
        if (isMalicious) {
            for (PkFkJoinFnParam param : params) {
                int totalNum = param.uTabNum + param.nuTabNum;
                // functions
                long[] sortTuple = sortParty.setUsage(new PgSortFnParam(PgSortOperations.PgSortOp.SORT_A, totalNum, 2));
                long[] traversalTuple = traversalParty.setUsage(
                    new TraversalFnParam(TraversalOperations.TraversalOp.TRAVERSAL_A, totalNum, 3),
                    new TraversalFnParam(TraversalOperations.TraversalOp.TRAVERSAL_A, totalNum, 4),
                    new TraversalFnParam(TraversalOperations.TraversalOp.TRAVERSAL_A, totalNum, keyDim + param.uTabValueDim + 1)
                );
                long[] fillPermTuple = fillPermutationParty.setUsage(new FillPerFnParam(FillPerOperations.FillPerOp.FILL_ONE_PER_A, totalNum, param.uTabNum, param.nuTabNum));
                long[] sortSignTuple = sortSignParty.setUsage(new SortSignFnParam(param.inputIsSorted, param.keyDim, param.uTabNum, param.nuTabNum));
                tuples[0] += sortTuple[0] + traversalTuple[0] + fillPermTuple[0] + sortSignTuple[0];
                tuples[1] += sortTuple[1] + traversalTuple[1] + fillPermTuple[1] + sortSignTuple[1];
            }
        }
        return tuples;
    }

    @Override
    public TripletLongVector[] innerJoin(TripletLongVector[] uTable, TripletLongVector[] nuTable,
                                         int[] uKeyIndex, int[] nuKeyIndex, boolean inputIsSorted) throws MpcAbortException {
        setPtoInput(uTable, nuTable, uKeyIndex, nuKeyIndex, inputIsSorted);
        logPhaseInfo(PtoState.PTO_BEGIN, "PkFk innerJoin");

        stopWatch.start();
        // 1. posCal
        TripletLongVector[] posResult = groupDimension();
        logStepInfo(PtoState.PTO_STEP, 1, 5, resetAndGetTime(), "finish groupDimension");

        stopWatch.start();
        // 2. separation
        int leftLen = uTable[0].getNum(), rightLen = nuTable[0].getNum();
        TripletLongVector[][] sepResult = this.separation(posResult, leftLen, rightLen);
        logStepInfo(PtoState.PTO_STEP, 2, 5, resetAndGetTime(), "finish separation");

        stopWatch.start();
        // 3. 左表右表的dist操作
        TripletLongVector uPai = fillPermutationParty.permutationCompletion(sepResult[0][1], sepResult[0][0], leftLen + rightLen);
        TripletLongVector[] nuTableDistTmp = permuteParty.applyInvPermutation(sepResult[1][1], processedNuTab);
        TripletLongVector[] rightDistResult = InputProcessUtils.appendAttributes(nuTableDistTmp, new TripletLongVector[]{sepResult[1][0]});
        TripletLongVector[] leftDistResult = distributionWithPermutation(sepResult[0], uPai);
        logStepInfo(PtoState.PTO_STEP, 3, 5, resetAndGetTime(), "finish distribution");

        stopWatch.start();
        // 4. 左表的expansion
        TripletLongVector[] leftExpResult = expansion(leftDistResult);
        logStepInfo(PtoState.PTO_STEP, 4, 5, resetAndGetTime(), "finish expansion");

        stopWatch.start();
        // 5. 拼接两个结果 [(key, [payload], E, F)] * 2, but the length of nuTable is not change
        int leftDim = leftExpResult.length;
        int rightDim = rightDistResult.length;
        int eLeft = leftDim - 2, fRight = rightDim - 1;
        int keyDim = uKeyIndex.length;
        TripletLongVector[] result = new TripletLongVector[leftDim + rightDim - 3 - keyDim];
        if (eLeft >= 0) {
            IntStream.range(0, eLeft).forEach(i -> result[i] = leftExpResult[i].copyOfRange(0, nuTable[0].getNum()));
        }
        if (fRight - keyDim >= 0) {
            System.arraycopy(rightDistResult, keyDim, result, eLeft, fRight - keyDim);
        }
        logStepInfo(PtoState.PTO_STEP, 5, 5, resetAndGetTime(), "finish concat");

        logPhaseInfo(PtoState.PTO_END, "PkFk innerJoin");
        return result;
    }

    /**
     * Based on the sorted result, compute the group information of each join_key. Including:
     * (1). ID: the source of the sorted join_key. 0 if it comes from the left table; 1 otherwise.
     * (2). E: 1 if there exist the same join_key in the other table; 0 otherwise.
     * (3). A1: how many rows in the right table have the same join_key as the row
     * (4). kPai: the permutation representing the sorting of the join_key
     * (5). sPai: the permutation according to the table_id and E
     *
     * @return (ID, E, A1, kPai, sPai)
     */
    public TripletLongVector[] groupDimension() throws MpcAbortException {
        TripletLongVector[] sortRes = sortSignParty.preSort(
            Arrays.copyOf(processedUTab, keyDim), Arrays.copyOf(processedNuTab, keyDim),
            processedUTab[processedUTab.length - 1], processedNuTab[processedNuTab.length - 1], inputIsSorted);
        TripletLongVector e1 = sortRes[0];
        TripletLongVector eWithUpper = sortRes[1];
        TripletLongVector eWithBelow = sortRes[2];
        TripletLongVector shuffledId = sortRes[3];
        TripletLongVector kPai = sortRes[4];

        TripletLongVector[] firstErgodic = traversalParty.traversalPrefix(
            new TripletLongVector[]{e1, shuffledId, eWithUpper},
            false, false, true, false);

        TripletLongVector[] invInput = new TripletLongVector[firstErgodic.length + 1];
        System.arraycopy(firstErgodic, 0, invInput, 0, firstErgodic.length);
        invInput[firstErgodic.length] = eWithBelow;
        TripletLongVector[] secondErgodic = traversalParty.traversalPrefix(invInput, true, false, true, true);

        // 生成新的置换
        TripletLongVector invEqSign = (TripletLongVector) zl64cParty.add(zl64cParty.neg(secondErgodic[0]), 1L);
        TripletLongVector sPai = sortParty.perGen4MultiDim(new TripletLongVector[]{shuffledId, invEqSign}, new int[]{1, 1});
        // 将所有结果合在一起输出, (ID, E, A1, kPai, sPai)
        return new TripletLongVector[]{shuffledId, secondErgodic[0], secondErgodic[1], kPai, sPai};
    }

    /**
     * 实现separation过程，完成position的计算以及两个表格的分离
     *
     * @param input (5, n1+n2) : (ID, E, A1, kPai, sPai)
     * @param n1    左表的原始数量
     * @param n2    右表的原始数量
     * @return [[3, n1], [2, n2]] left:[E, P, skPaiLeft], right:[E, skPaiRight]
     */
    public TripletLongVector[][] separation(TripletLongVector[] input, int n1, int n2) throws MpcAbortException {
        Preconditions.checkArgument(input.length == 5 && input[0].getNum() == n1 + n2);
        // left:[E, P, skPaiLeft], right:[E, skPaiRight]
        TripletLongVector[] xPartLeft = new TripletLongVector[3], xPartRight = new TripletLongVector[2];

        // 1. 处理置换的结果，如果是预先排好序的，那么只需要用sPai即可，否则需要组合置换
        TripletLongVector skPai = permuteParty.composePermutation(input[3], input[4])[0];
        // 取固定长度并减去长度
        xPartLeft[2] = skPai.copyOfRange(0, n1);
        xPartRight[1] = skPai.copyOfRange(n1, n1 + n2);
        zl64cParty.addi(xPartRight[1], -n1);

        // 2. 处理主体的结果
        TripletLongVector[] xPart = Arrays.copyOfRange(input, 1, 3);
        // 后续只需保留  E, A1
        xPart = permuteParty.applyInvPermutation(input[4], xPart);
        // right 只保留 E
        xPartRight[0] = xPart[0].copyOfRange(n1, n1 + n2);
        // left 只保留 E, 但还是先将A0， A1 copy到指定位置，方便后面的distribution position
        xPartLeft[0] = xPart[0].copyOfRange(0, n1);
        // 3. 只需要为unique key的table计算distribution position
        xPartLeft[1] = positionDistribution(xPartLeft[0], xPart[1].copyOfRange(0, n1));
        return new TripletLongVector[][]{xPartLeft, xPartRight};
    }

    /**
     * 计算distribution position
     *
     * @param eSign  对应的E
     * @param aValue 左表输入A1，右表输入A0
     * @return 向量P，代表最终所需的位置
     */
    public TripletLongVector positionDistribution(TripletLongVector eSign, TripletLongVector aValue) {
        Preconditions.checkArgument(eSign.getNum() == aValue.getNum());
        TripletLongVector addValue = zl64cParty.sub(aValue, zl64cParty.add(eSign, -1L));
        TripletLongVector count = (TripletLongVector) zl64cParty.setPublicValue(LongVector.createZeros(1));
        TripletLongVector trueRank = zl64cParty.rowAdderWithPrefix(addValue, count, false);
        zl64cParty.addi(trueRank, -1L);
        return trueRank;
    }

    /**
     * 执行distribution步骤
     *
     * @param xPartLeft 对于left而言，输入为 [E, P, skPaiLeft]
     * @param dPaiLeft  补完之后的permutation P
     * @return 对于left而言，输出为 [key, [payload], E, F]
     */
    public TripletLongVector[] distributionWithPermutation(TripletLongVector[] xPartLeft,
                                                           TripletLongVector dPaiLeft) throws MpcAbortException {
        MathPreconditions.checkEqual("xPartLeft[0].getNum()", "allLeft[0].getNum()", xPartLeft[0].getNum(), processedUTab[0].getNum());
        MathPreconditions.checkGreaterOrEqual("dPaiLeft.getNum() >= xPartLeft[0].getNum()", dPaiLeft.getNum(), xPartLeft[0].getNum());
        // 先得到置换左表payload数据的置换，得到的方式是compose
        TripletLongVector skPaiLeft = (TripletLongVector) zl64cParty.setPublicValue(LongVector.create(LongStream.range(0, dPaiLeft.getNum()).toArray()));
        skPaiLeft.setElements(xPartLeft[2], 0, 0, xPartLeft[2].getNum());
        TripletLongVector leftPayloadPer = permuteParty.composePermutation(skPaiLeft, dPaiLeft)[0];
        // 置换E
        xPartLeft[0].paddingZeros(dPaiLeft.getNum() - xPartLeft[2].getNum());
        TripletLongVector eAfter = permuteParty.applyInvPermutation(dPaiLeft, xPartLeft[0])[0];
        // 置换原始的输入
        TripletLongVector[] payloadWithDummyLeft = Arrays.stream(processedUTab).map(x -> {
            TripletLongVector tmp = (TripletLongVector) x.copy();
            tmp.paddingZeros(dPaiLeft.getNum() - tmp.getNum());
            return tmp;
        }).toArray(TripletLongVector[]::new);
        TripletLongVector[] payloadAfter = permuteParty.applyInvPermutation(leftPayloadPer, payloadWithDummyLeft);
        return InputProcessUtils.appendAttributes(payloadAfter, new TripletLongVector[]{eAfter});
    }

    /**
     * 执行expansion步骤，之前的实现中P当错了，现在的P是最后一个位置，所以不会出现无限往下复制的情况，因为实际上是需要往前复制
     *
     * @param input 对于left而言，输入为 [key, [payload], E, F]
     * @return 对于left而言，输出为 [key, [payload], E, F]
     */
    public TripletLongVector[] expansion(TripletLongVector[] input) {
        input[input.length - 1] = (TripletLongVector) zl64cParty.add(zl64cParty.neg(input[input.length - 1]), 1L);
        TripletLongVector[] invTreeRes = traversalParty.traversalPrefix(input, true, true, true, true);
        invTreeRes[invTreeRes.length - 1] = (TripletLongVector) zl64cParty.add(zl64cParty.neg(invTreeRes[invTreeRes.length - 1]), 1L);
        return invTreeRes;
    }
}
