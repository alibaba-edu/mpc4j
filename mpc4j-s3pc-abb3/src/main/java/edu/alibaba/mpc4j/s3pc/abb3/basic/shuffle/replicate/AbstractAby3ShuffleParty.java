package edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.replicate;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.AbstractAbbThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.replicate.Aby3ShufflePtoDesc.PtoStep;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.ShuffleUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * The abstract party of Replicated-sharing shuffling
 *
 * @author Feng Han
 * @date 2024/01/18
 */
public abstract class AbstractAby3ShuffleParty extends AbstractAbbThreePartyPto implements Aby3ShuffleParty {
    protected final boolean isMalicious;
    /**
     * id of self
     */
    protected final int selfId;
    /**
     * z2c party
     */
    protected final TripletZ2cParty z2cParty;
    /**
     * zl64c party
     */
    protected final TripletLongParty zl64cParty;
    /**
     * TripletProvider
     */
    protected final S3pcCrProvider crProvider;

    protected AbstractAby3ShuffleParty(TripletZ2cParty z2cParty, TripletLongParty zl64cParty, Aby3ShuffleConfig config) {
        super(Aby3ShufflePtoDesc.getInstance(), z2cParty.getRpc(), z2cParty.leftParty(), z2cParty.rightParty(), config);
        this.z2cParty = z2cParty;
        this.zl64cParty = zl64cParty;

        isMalicious = config.isMalicious();
        selfId = z2cParty.getRpc().ownParty().getPartyId();
        crProvider = z2cParty.getTripletProvider().getCrProvider();
    }

    @Override
    public TripletProvider getProvider(){
        return z2cParty.getTripletProvider();
    }

    @Override
    public TripletZ2cParty getZ2cParty(){
        return z2cParty;
    }

    @Override
    public TripletLongParty getZl64cParty(){
        return zl64cParty;
    }

    @Override
    public void init() {
        z2cParty.init();
        zl64cParty.init();
    }

    /**
     * randomize the 2p shared value
     *
     * @param input   the shared data
     * @param withWho who is the other party
     */
    public void reRand2pShare(BitVector[] input, Party withWho) {
        assert withWho.equals(leftParty()) || withWho.equals(rightParty());
        BitVector[] rand = crProvider.randBitVector(Arrays.stream(input).mapToInt(BitVector::bitNum).toArray(), withWho);
        IntStream.range(0, input.length).forEach(i -> input[i].xori(rand[i]));
    }

    /**
     * randomize the 2p shared value
     *
     * @param input   the shared data
     * @param withWho who is the other party
     */
    public void reRand2pShare(LongVector[] input, Party withWho) {
        assert withWho.equals(leftParty()) || withWho.equals(rightParty());
        LongVector[] rand = crProvider.randZl64Vector(Arrays.stream(input).mapToInt(LongVector::getNum).toArray(), withWho);
        if (withWho.equals(leftParty())) {
            IntStream.range(0, input.length).forEach(i -> input[i].addi(rand[i]));
        } else {
            IntStream.range(0, input.length).forEach(i -> input[i].subi(rand[i]));
        }
    }

    @Override
    public TripletRpZ2Vector[] shuffleRow(MpcZ2Vector[] data) throws MpcAbortException {
        int[][] rand = crProvider.getRandIntArray(data.length);
        int[][] pai = Arrays.stream(rand).map(ShuffleUtils::permutationGeneration).toArray(int[][]::new);
        return (TripletRpZ2Vector[]) shuffleRow(pai, data);
    }

    @Override
    public TripletRpZ2Vector[] shuffleColumn(MpcZ2Vector... data) throws MpcAbortException {
        int[][] rand = crProvider.getRandIntArray(data[0].bitNum());
        int[][] pai = Arrays.stream(rand).map(ShuffleUtils::permutationGeneration).toArray(int[][]::new);
        return (TripletRpZ2Vector[]) shuffleColumn(pai, data);
    }

    @Override
    public TripletRpLongVector[] shuffle(MpcLongVector... data) throws MpcAbortException {
        int[][] rand = crProvider.getRandIntArray(data[0].getNum());
        int[][] pai = Arrays.stream(rand).map(ShuffleUtils::permutationGeneration).toArray(int[][]::new);
        return (TripletRpLongVector[]) shuffle(pai, data);
    }

    /**
     * transform three-party sharing into two-party sharing
     *
     * @param input input data
     * @param p0    party0
     * @param p1    party1
     */
    protected BitVector[] trans3To2Sharing(TripletRpZ2Vector[] input, Party p0, Party p1) {
        if (rpc.ownParty().equals(p0) || rpc.ownParty().equals(p1)) {
            Party withWho = rpc.ownParty().equals(p0) ? p1 : p0;
            int[] bitNums = Arrays.stream(input).mapToInt(TripletRpZ2Vector::bitNum).toArray();
            BitVector[] randWithWho = crProvider.randBitVector(bitNums, withWho);
            IntStream.range(0, input.length).forEach(i -> randWithWho[i].xori(input[i].getBitVectors()[0]));
            if (withWho.equals(leftParty())) {
                IntStream.range(0, input.length).forEach(i -> randWithWho[i].xori(input[i].getBitVectors()[1]));
            }
            return randWithWho;
        } else {
            return null;
        }
    }

    /**
     * transform three-party sharing into two-party sharing, and perform matrix transpose and data padding
     *
     * @param input     input data
     * @param targetLen the target data length
     * @param p0        party0
     * @param p1        party1
     */
    protected BitVector[] trans3To2SharingTranspose(TripletRpZ2Vector[] input, int targetLen, Party p0, Party p1) {
        if (rpc.ownParty().equals(p0) || rpc.ownParty().equals(p1)) {
            BitVector[] d2p = trans3To2Sharing(input, p0, p1);
            d2p = Arrays.stream(ZlDatabase.create(envType, parallel, d2p).getBytesData())
                .map(each -> BitVectorFactory.create(input.length, each)).toArray(BitVector[]::new);
            if (d2p.length < targetLen) {
                BitVector[] res = new BitVector[targetLen];
                IntStream.range(d2p.length, targetLen).forEach(i -> res[i] = BitVectorFactory.createZeros(input.length));
                System.arraycopy(d2p, 0, res, 0, d2p.length);
                return res;
            }
            return d2p;
        } else {
            return null;
        }
    }

    /**
     * transform two-party sharing into three-party sharing
     *
     * @param input         shared data
     * @param targetBitNums the bit lengths of the result 3p binary sharing values
     * @param p0            party0
     * @param p1            party1
     */
    public TripletRpZ2Vector[] trans2To3Sharing(BitVector[] input, Party p0, Party p1, int[] targetBitNums) {
        if (rpc.ownParty().equals(p0) || rpc.ownParty().equals(p1)) {
            Party withWho = rpc.ownParty().equals(p0) ? p1 : p0;
            Party toWho = withWho.equals(leftParty()) ? rightParty() : leftParty();
            int[] bitNums = Arrays.stream(input).mapToInt(BitVector::bitNum).toArray();

            BitVector[] rand = crProvider.randBitVector(bitNums, toWho);
            BitVector[] xorRes = IntStream.range(0, input.length).mapToObj(i -> input[i].xor(rand[i])).toArray(BitVector[]::new);
            sendBitVectors(PtoStep.TWO_SHARE_INTO_THREE_SHARE.ordinal(), withWho, xorRes);

            BitVector[] others = receiveBitVectors(PtoStep.TWO_SHARE_INTO_THREE_SHARE.ordinal(), withWho, bitNums);
            IntStream.range(0, input.length).forEach(i -> xorRes[i].xori(others[i]));
            if (withWho.equals(leftParty())) {
                return IntStream.range(0, input.length).mapToObj(i -> TripletRpZ2Vector.create(xorRes[i], rand[i])).toArray(TripletRpZ2Vector[]::new);
            } else {
                return IntStream.range(0, input.length).mapToObj(i -> TripletRpZ2Vector.create(rand[i], xorRes[i])).toArray(TripletRpZ2Vector[]::new);
            }
        } else {
            BitVector[] rWithLeft = crProvider.randBitVector(targetBitNums, leftParty());
            BitVector[] rWithRight = crProvider.randBitVector(targetBitNums, rightParty());
            return IntStream.range(0, targetBitNums.length).mapToObj(i ->
                TripletRpZ2Vector.create(rWithLeft[i], rWithRight[i])).toArray(TripletRpZ2Vector[]::new);
        }
    }

    /**
     * transform two-party sharing into three-party sharing, and perform matrix transpose
     *
     * @param input         shared data
     * @param targetBitNums the bit lengths of the result 3p binary sharing values
     * @param dataDim       the dimension of output
     * @param p0            party0
     * @param p1            party1
     */
    public TripletRpZ2Vector[] trans2To3SharingTranspose(BitVector[] input, Party p0, Party p1, int targetBitNums, int dataDim) {
        int[] bitNums = IntStream.range(0, dataDim).map(i -> targetBitNums).toArray();
        if (rpc.ownParty().equals(p0) || rpc.ownParty().equals(p1)) {
            MathPreconditions.checkEqual("input.length", "targetBitNums", input.length, targetBitNums);
            input = Arrays.stream(ZlDatabase.create(envType, parallel, input).getBytesData())
                .map(each -> BitVectorFactory.create(targetBitNums, each)).toArray(BitVector[]::new);
        }
        return trans2To3Sharing(input, p0, p1, bitNums);
    }

    /**
     * transform three-party sharing into two-party sharing; if the true length is smaller than targetLen, padding zeros behinds array
     *
     * @param input     input data
     * @param p0        party0
     * @param p1        party1
     * @param targetLen the target length,
     */
    abstract LongVector[] trans3To2Sharing(TripletRpLongVector[] input, Party p0, Party p1, int targetLen);

    /**
     * transform two-party sharing into three-party sharing
     *
     * @param input    shared data
     * @param p0       party0
     * @param p1       party1
     * @param dataNums the number of the elements in each shared vectors
     */
    abstract TripletRpLongVector[] trans2To3Sharing(LongVector[] input, Party p0, Party p1, int[] dataNums);

    public BitVector[] permuteNetworkImplWithData(BitVector[] d2p, int[] pai, int maxLength, int targetLen, int originDim,
                                                  Party programmer, Party sender, Party receiver) throws MpcAbortException {
        if (rpc.ownParty().equals(programmer)) {
            MathPreconditions.checkEqual("targetLen", "pai.length", targetLen, pai.length);
            // 1. role=0的和自己的next，role=1的和自己的previous生成一个随机的置换
            int[] sigma1 = ShuffleUtils.permutationGeneration(crProvider.getRandIntArray(maxLength, sender));
            // 2. role=0的得到第二个置换，并发送给previous
            int[][] sigma2AndPaiExtend = ShuffleUtils.getSigma2(pai, sigma1, isMalicious);
            sendInt(PtoStep.PERMUTE_MSG.ordinal(), receiver, sigma2AndPaiExtend[0]);
            // 3. 先与自己的next一起置换，再与自己的previous置换
            return ShuffleUtils.applyPermutation(d2p, sigma2AndPaiExtend[1]);
        } else if (rpc.ownParty().equals(sender)) {
            int[] sigma1 = ShuffleUtils.permutationGeneration(crProvider.getRandIntArray(maxLength, programmer));
            d2p = ShuffleUtils.applyPermutation(d2p, sigma1);
            sendBitVectors(PtoStep.PERMUTE_MSG.ordinal(), receiver, d2p);
            return null;
        } else {
            int[] sigma2 = receiveInt(PtoStep.PERMUTE_MSG.ordinal(), programmer)[0];
            d2p = receiveBitVectors(PtoStep.PERMUTE_MSG.ordinal(), sender, IntStream.range(0, maxLength).map(i -> originDim).toArray());
            return ShuffleUtils.applyPermutation(d2p, sigma2);
        }
    }

    public LongVector[] permuteNetworkImplWithData(LongVector[] d2p, int[] pai, int maxLength,
                                                   Party programmer, Party sender, Party receiver) throws MpcAbortException {
        if(rpc.ownParty().equals(programmer)){
            // 1. role=0的和自己的next，role=1的和自己的previous生成一个随机的置换
            int[] sigma1 = ShuffleUtils.permutationGeneration(crProvider.getRandIntArray(maxLength, sender));
            // 2. role=0的得到第二个置换，并发送给previous
            int[][] sigma2AndPaiExtend = ShuffleUtils.getSigma2(pai, sigma1, isMalicious);
            sendInt(PtoStep.PERMUTE_MSG.ordinal(), receiver, sigma2AndPaiExtend[0]);
            // 3. 先与自己的next一起置换，再与自己的previous置换
            return ShuffleUtils.applyPermutationToRows(d2p, sigma2AndPaiExtend[1]);
        }else if(rpc.ownParty().equals(sender)){
            int[] sigma1 = ShuffleUtils.permutationGeneration(crProvider.getRandIntArray(maxLength, programmer));
            d2p = ShuffleUtils.applyPermutationToRows(d2p, sigma1);
            sendLongVectors(PtoStep.PERMUTE_MSG.ordinal(), receiver, d2p);
            return null;
        }else{
            int[] sigma2 = receiveInt(PtoStep.PERMUTE_MSG.ordinal(), programmer)[0];
            d2p = receiveLongVectors(PtoStep.PERMUTE_MSG.ordinal(), sender);
            return ShuffleUtils.applyPermutationToRows(d2p, sigma2);
        }
    }
}
