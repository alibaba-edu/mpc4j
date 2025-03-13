package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.kks20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvOperations.ConvOp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.AbstractFillPermutationParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPerOperations.FillPerFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPerOperations.FillPerOp;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPermutationParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations.PgSortFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations.PgSortOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortParty;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * fills the incomplete permutation using butterfly net
 *
 * @author Feng Han
 * @date 2025/2/17
 */
public class Kks20FillPermutationParty extends AbstractFillPermutationParty implements FillPermutationParty {
    /**
     * permutation party
     */
    private final PermuteParty permuteParty;
    /**
     * sort party
     */
    private final PgSortParty sortParty;


    public Kks20FillPermutationParty(Abb3Party abb3Party, Kks20FillPermutationConfig config) {
        super(Kks20FillPermutationPtoDesc.getInstance(), abb3Party, config);
        permuteParty = PermuteFactory.createParty(abb3Party, config.getPermuteConfig());
        sortParty = PgSortFactory.createParty(abb3Party, config.getPgSortConfig());
        addMultiSubPto(permuteParty, sortParty);
    }

    @Override
    public long[] setUsage(FillPerFnParam... params) {
        if(!isMalicious()){
            return new long[]{0, 0};
        }
        long[] tupleNums = new long[]{0, 0};
        List<long[]> cost = new LinkedList<>();
        for(FillPerFnParam param : params){
            switch (param.op){
                case FILL_ONE_PER_A:
                    int keepBits = Math.max(1, LongUtils.ceilLog2(param.outputLen - param.inputLen[0] + 1));
                    int dataNum = CommonUtils.getByteLength(param.outputLen) << 3;
                    // permutation
                    permuteParty.setUsage(
                        new PermuteFnParam(PermuteOp.APPLY_INV_A_A, param.inputLen[0], 1, 64),
                        new PermuteFnParam(PermuteOp.APPLY_INV_A_A, param.outputLen, 1, 64));
                    // permutation generation
                    sortParty.setUsage(
                        new PgSortFnParam(PgSortOp.SORT_A, param.inputLen[0], 1),
                        new PgSortFnParam(PgSortOp.SORT_A, param.outputLen, 1));
                    // type conversion
                    cost.add(abb3Party.getConvParty().getTupleNum(ConvOp.A2B, param.inputLen[0], 1, keepBits));
                    cost.add(abb3Party.getConvParty().getTupleNum(ConvOp.BIT2A, param.outputLen, 1, keepBits));
                    // and operation
                    cost.add(new long[]{(long) dataNum * (keepBits + 3) * keepBits, 0});
                    break;
                case FILL_TWO_PER_A:
                    for(int in : param.inputLen){
                        long[] tmpNums = setUsage(new FillPerFnParam(FillPerOp.FILL_ONE_PER_A, param.outputLen, in));
                        tupleNums[0] += tmpNums[0];
                        tupleNums[1] += tmpNums[1];
                    }
                    break;
                default:
                    throw new IllegalArgumentException("illegal FillPerFnParam" + param.op.name());
            }
        }
        long bitTupleNum = cost.stream().mapToLong(x -> x[0]).sum();
        long longTupleNum = cost.stream().mapToLong(x -> x[1]).sum();
        tupleNums[0] += bitTupleNum;
        tupleNums[1] += longTupleNum;
        // update abb3
        abb3Party.updateNum(bitTupleNum, longTupleNum);
        return tupleNums;
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        abb3Party.init();
        permuteParty.init();
        sortParty.init();
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public TripletLongVector permutationCompletion(TripletLongVector index, TripletLongVector equalSign, int m) throws MpcAbortException {
        checkInput(index, equalSign, m);
        if(m == index.getNum()){
            return index;
        }
        int maxMoveLen = m - index.getNum();
        int keepBits = LongUtils.ceilLog2(maxMoveLen + 1);
        logPhaseInfo(PtoState.PTO_BEGIN, "permutationCompletion");

        stopWatch.start();
        // 0. 先将index变成有序的
        TripletLongVector pai4Order = sortParty.perGen4MultiDim(new TripletLongVector[]{(TripletLongVector) zl64cParty.add(zl64cParty.neg(equalSign), 1L)}, new int[]{1});
        TripletLongVector orderedIndex = permuteParty.applyInvPermutation(pai4Order, index)[0];
        logStepInfo(PtoState.PTO_STEP, 1, 4, resetAndGetTime());

        stopWatch.start();
        // 1. 先得到需要移位的数值
        TripletLongVector plainIndex = (TripletLongVector) zl64cParty.setPublicValue(LongVector.create(LongStream.range(0, index.getNum()).toArray()));
        TripletLongVector moveBitsA = zl64cParty.sub(orderedIndex, plainIndex);
        TripletZ2Vector[] bits = abb3Party.getConvParty().a2b(moveBitsA, keepBits);
        IntStream.range(0, bits.length).forEach(i -> bits[i] = bits[i].padShiftLeft(maxMoveLen));
        logStepInfo(PtoState.PTO_STEP, 2, 4, resetAndGetTime());

        stopWatch.start();
        // 2. 开始移位，如果是1则代表该位置对应的是一个真实的数据行
        boolean[] plainFlag = new boolean[m];
        Arrays.fill(plainFlag, 0, index.getNum(), true);
        TripletZ2Vector flag = (TripletZ2Vector) z2cParty.setPublicValues(
            new BitVector[]{BitVectorFactory.create(m, BinaryUtils.binaryToRoundByteArray(plainFlag))})[0];
        for (int i = 0; i < keepBits; i++) {
            int shiftBitLen = 1 << (keepBits - 1 - i);
            // 有哪些flag被移位了
            TripletZ2Vector shiftFlag = z2cParty.and(bits[i], flag);
            shiftFlag.fixShiftRighti(shiftBitLen);
            // 如果当前位置的flag需要移位，则将这个位置对应的flag暂且置为false
            flag = z2cParty.and(z2cParty.not(bits[i]), flag);
            // 如果当前位置是被移位后的，则置为移位后的；否则依旧是原来的
            z2cParty.xori(flag, shiftFlag);
            if (i < keepBits - 1) {
                // 同样的移位那些代表index的数据
                TripletZ2Vector[] extendCurrentBit = new TripletZ2Vector[keepBits - i - 1], extendCurrentInvBit = new TripletZ2Vector[keepBits - i - 1];
                Arrays.fill(extendCurrentBit, bits[i]);
                Arrays.fill(extendCurrentInvBit, z2cParty.not(bits[i]));
                TripletZ2Vector[] shiftIndex = z2cParty.and(extendCurrentBit, Arrays.copyOfRange(bits, i + 1, bits.length));
                IntStream.range(0, shiftIndex.length).forEach(k -> shiftIndex[k].fixShiftRighti(shiftBitLen));
                TripletZ2Vector[] tmpIndex = z2cParty.and(extendCurrentInvBit, Arrays.copyOfRange(bits, i + 1, bits.length));
                for (int k = 0; k < shiftIndex.length; k++) {
                    bits[i + 1 + k] = z2cParty.xor(tmpIndex[k], shiftIndex[k]);
                }
            }
        }
        logStepInfo(PtoState.PTO_STEP, 3, 4, resetAndGetTime());

        stopWatch.start();
        // 3. 得到置换
        TripletLongVector dummyFlag = abb3Party.getConvParty().bit2a(z2cParty.not(flag));
        TripletLongVector plainIndex4m = (TripletLongVector) zl64cParty.setPublicValue(LongVector.create(LongStream.range(0, m).toArray()));
        TripletLongVector dummyPai = sortParty.perGen4MultiDim(new TripletLongVector[]{dummyFlag}, new int[]{1});
        TripletLongVector sepRes = permuteParty.applyInvPermutation(dummyPai, plainIndex4m)[0];
        TripletLongVector paiRes = zl64cParty.createZeros(m);
        paiRes.setElements(index, 0, 0, index.getNum());
        paiRes.setElements(sepRes, index.getNum(), index.getNum(), maxMoveLen);
        logStepInfo(PtoState.PTO_STEP, 4, 4, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END, "permutationCompletion");
        return paiRes;
    }

    @Override
    public TripletLongVector[] twoPermutationCompletion(TripletLongVector leftIndex, TripletLongVector leftEqual,
                                                        TripletLongVector rightIndex, TripletLongVector rightEqual, int m) throws MpcAbortException{
        return new TripletLongVector[]{this.permutationCompletion(leftIndex, leftEqual, m), this.permutationCompletion(rightIndex, rightEqual, m)};
    }
}

