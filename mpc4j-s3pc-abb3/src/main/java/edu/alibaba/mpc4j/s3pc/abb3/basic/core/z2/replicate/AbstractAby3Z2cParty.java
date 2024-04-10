package edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.AbstractTripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate.Aby3Z2cPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * The abstract party of Replicated z2 sharing
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public abstract class AbstractAby3Z2cParty extends AbstractTripletZ2cParty implements TripletZ2cParty {

    protected AbstractAby3Z2cParty(Rpc rpc, Aby3Z2cConfig config, TripletProvider tripletProvider) {
        super(Aby3Z2cPtoDesc.getInstance(), rpc, config, tripletProvider);
    }

    @Override
    public void init() {
        if (partyState.equals(PartyState.INITIALIZED)) {
            return;
        }
        initState();
    }

    @Override
    public MpcZ2Vector create(boolean isPlain, BitVector[] bitVector) {
        if (isPlain) {
            assert bitVector.length == 1;
            return PlainZ2Vector.create(bitVector[0]);
        } else {
            assert bitVector.length == 2;
            return TripletRpZ2Vector.create(bitVector);
        }
    }

    @Override
    public MpcZ2Vector createOnes(int bitNum) {
        return PlainZ2Vector.createOnes(bitNum);
    }

    @Override
    public TripletZ2Vector createShareZeros(int bitNums){
        return TripletRpZ2Vector.createEmpty(bitNums);
    }

    @Override
    public MpcZ2Vector createZeros(int bitNum) {
        return PlainZ2Vector.createZeros(bitNum);
    }

    @Override
    public TripletRpZ2Vector[] setPublicValues(BitVector[] data) {
        return Arrays.stream(data).map(x -> TripletRpZ2Vector.create(
            selfId == 0 ? x : BitVectorFactory.createZeros(x.bitNum()),
            selfId == 2 ? x : BitVectorFactory.createZeros(x.bitNum()))).toArray(TripletRpZ2Vector[]::new);
    }

    @Override
    public TripletRpZ2Vector xor(MpcZ2Vector xiArray, MpcZ2Vector yiArray) {
        assert !(xiArray.isPlain() && yiArray.isPlain());
        if (xiArray.isPlain()) {
            return TripletRpZ2Vector.create(
                selfId == 0 ? yiArray.getBitVectors()[0].xor(xiArray.getBitVector()) : yiArray.getBitVectors()[0].copy(),
                selfId == 2 ? yiArray.getBitVectors()[1].xor(xiArray.getBitVector()) : yiArray.getBitVectors()[1].copy());
        } else if (yiArray.isPlain()) {
            return TripletRpZ2Vector.create(
                selfId == 0 ? xiArray.getBitVectors()[0].xor(yiArray.getBitVector()) : xiArray.getBitVectors()[0].copy(),
                selfId == 2 ? xiArray.getBitVectors()[1].xor(yiArray.getBitVector()) : xiArray.getBitVectors()[1].copy());
        } else {
            return TripletRpZ2Vector.create(IntStream.range(0, 2).mapToObj(i ->
                xiArray.getBitVectors()[i].xor(yiArray.getBitVectors()[i])).toArray(BitVector[]::new));
        }
    }

    @Override
    public void xori(MpcZ2Vector xi, MpcZ2Vector yi) {
        assert !xi.isPlain();
        if (yi.isPlain()) {
            if (selfId != 1) {
                xi.getBitVectors()[selfId >> 1].xori(yi.getBitVector());
            }
        } else {
            xi.getBitVectors()[0].xori(yi.getBitVectors()[0]);
            xi.getBitVectors()[1].xori(yi.getBitVectors()[1]);
        }
    }

    @Override
    public TripletRpZ2Vector[] and(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        for (int i = 0; i < xiArray.length; i++) {
            assert xiArray[i].isPlain() == xiArray[0].isPlain() && yiArray[i].isPlain() == yiArray[0].isPlain();
            assert xiArray[i].bitNum() == yiArray[i].bitNum();
        }
        assert !(xiArray[0].isPlain() && yiArray[0].isPlain());
        MpcZ2Vector[] left = xiArray[0].isPlain() ? yiArray : xiArray;
        MpcZ2Vector[] right = xiArray[0].isPlain() ? xiArray : yiArray;
        IntStream intStream = parallel ? IntStream.range(0, left.length).parallel() : IntStream.range(0, left.length);
        if (right[0].isPlain()) {
            return intStream.mapToObj(i -> TripletRpZ2Vector.create(
                right[i].getBitVector().and(left[i].getBitVectors()[0]),
                right[i].getBitVector().and(left[i].getBitVectors()[1]))).toArray(TripletRpZ2Vector[]::new);
        } else {
            int[] bitNums = Arrays.stream(left).mapToInt(MpcZ2Vector::bitNum).toArray();
            BitVector[] zeroShares = Arrays.stream(bitNums).mapToObj(x ->
                crProvider.randZeroBitVector(x)).toArray(BitVector[]::new);
            intStream.forEach(i -> {
                zeroShares[i].xori(left[i].getBitVectors()[0].and(right[i].getBitVectors()[0]));
                zeroShares[i].xori(left[i].getBitVectors()[1].and(right[i].getBitVectors()[0]));
                zeroShares[i].xori(left[i].getBitVectors()[0].and(right[i].getBitVectors()[1]));
            });
            sendBitVectors(PtoStep.AND_OP.ordinal(), leftParty(), zeroShares);
            BitVector[] fromRight = receiveBitVectors(PtoStep.AND_OP.ordinal(), rightParty(), bitNums);
            extraInfo++;
            intStream = parallel ? IntStream.range(0, left.length).parallel() : IntStream.range(0, left.length);
            TripletRpZ2Vector[] res = intStream.mapToObj(i ->
                TripletRpZ2Vector.create(zeroShares[i], fromRight[i])).toArray(TripletRpZ2Vector[]::new);
            intoBuffer(new TripletRpZ2Vector[][]{
                Arrays.stream(left).map(each -> (TripletRpZ2Vector)each).toArray(TripletRpZ2Vector[]::new),
                Arrays.stream(right).map(each -> (TripletRpZ2Vector)each).toArray(TripletRpZ2Vector[]::new), res});
            return res;
        }
    }

    @Override
    public void andi(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        for (int i = 0; i < xiArray.length; i++) {
            assert (!xiArray[i].isPlain()) && yiArray[i].isPlain() == yiArray[0].isPlain();
            assert xiArray[i].bitNum() == yiArray[i].bitNum();
        }
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        if(yiArray[0].isPlain()){
            intStream.forEach(i -> {
                xiArray[i].getBitVectors()[0].andi(yiArray[i].getBitVector());
                xiArray[i].getBitVectors()[1].andi(yiArray[i].getBitVector());
            });
        }else {
            int[] bitNums = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::bitNum).toArray();
            BitVector[] zeroShares = Arrays.stream(bitNums).mapToObj(x ->
                crProvider.randZeroBitVector(x)).toArray(BitVector[]::new);
            intStream.forEach(i -> {
                zeroShares[i].xori(xiArray[i].getBitVectors()[0].and(yiArray[i].getBitVectors()[0]));
                zeroShares[i].xori(xiArray[i].getBitVectors()[1].and(yiArray[i].getBitVectors()[0]));
                zeroShares[i].xori(xiArray[i].getBitVectors()[0].and(yiArray[i].getBitVectors()[1]));
            });
            sendBitVectors(PtoStep.AND_OP.ordinal(), leftParty(), zeroShares);
            BitVector[] fromRight = receiveBitVectors(PtoStep.AND_OP.ordinal(), rightParty(), bitNums);
            extraInfo++;
            intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
            TripletRpZ2Vector[] res = intStream.mapToObj(i ->
                TripletRpZ2Vector.create(zeroShares[i], fromRight[i])).toArray(TripletRpZ2Vector[]::new);
            intoBuffer(new TripletRpZ2Vector[][]{
                Arrays.stream(xiArray).map(each -> (TripletRpZ2Vector)each).toArray(TripletRpZ2Vector[]::new),
                Arrays.stream(yiArray).map(each -> (TripletRpZ2Vector)each).toArray(TripletRpZ2Vector[]::new), res});
            IntStream.range(0, xiArray.length).forEach(i -> xiArray[i].setBitVectors(res[i].getBitVectors()));
        }
    }

    @Override
    public MpcZ2Vector not(MpcZ2Vector xi) {
        if (xi.isPlain()) {
            return PlainZ2Vector.create(xi.getBitVector().not());
        } else {
            TripletRpZ2Vector res = (TripletRpZ2Vector) xi.copy();
            if (selfId != 1) {
                res.getBitVectors()[selfId >> 1].noti();
            }
            return res;
        }
    }

    @Override
    public void noti(MpcZ2Vector xi) {
        if (xi.isPlain()) {
            xi.getBitVector().noti();
        } else {
            Arrays.stream(xi.getBitVectors()).forEach(BitVector::noti);
        }
    }

    /**
     * put unverified multiplication tuples into buffer if malicious
     * @param unverifiedData unverified multiplication result in the form of [[x1, y1, z1], [x2, y2, z2], ... ]
     */
    protected void intoBuffer(TripletRpZ2Vector[][] unverifiedData){

    }
}
