package edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.aby3;

import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.circuit.zlong.PlainLongVector;
import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.AbstractTripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.aby3.Aby3LongCpPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongMacVector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * The abstract party of Replicated zl64 sharing
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public abstract class AbstractAby3LongParty extends AbstractTripletLongParty implements TripletLongParty {
    protected AbstractAby3LongParty(Rpc rpc, Aby3LongConfig config, TripletProvider tripletProvider) {
        super(Aby3LongCpPtoDesc.getInstance(), rpc, config, tripletProvider);
    }

    @Override
    public void init() {
        if (partyState.equals(PartyState.INITIALIZED)) {
            return;
        }
        initState();
    }

    @Override
    public MpcLongVector create(boolean isPlain, LongVector... longVector) {
        if (isPlain) {
            assert longVector.length == 1;
            return PlainLongVector.create(longVector[0]);
        } else {
            assert longVector.length == 2;
            return TripletRpLongVector.create(longVector);
        }
    }

    @Override
    public MpcLongVector create(boolean isPlain, long[]... longs) {
        if (isPlain) {
            assert longs.length == 1;
            return PlainLongVector.create(longs[0]);
        } else {
            assert longs.length == 2;
            return TripletRpLongVector.create(longs);
        }
    }

    @Override
    public TripletRpLongVector createZeros(int dataNum){
        return TripletRpLongVector.createZeros(dataNum);
    }

    @Override
    public TripletRpLongVector setPublicValue(LongVector xi) {
        return TripletRpLongVector.create(
            selfId == 0 ? xi : LongVector.createZeros(xi.getNum()),
            selfId == 2 ? xi : LongVector.createZeros(xi.getNum()));
    }

    @Override
    public TripletRpLongVector add(MpcLongVector xi, MpcLongVector yi) {
        assert !(xi.isPlain() && yi.isPlain());
        if (xi.isPlain()) {
            return (TripletRpLongVector) create(false,
                selfId == 0 ? yi.getVectors()[0].add(xi.getVectors()[0]) : yi.getVectors()[0].copy(),
                selfId == 2 ? yi.getVectors()[1].add(xi.getVectors()[0]) : yi.getVectors()[1].copy());
        } else if (yi.isPlain()) {
            return (TripletRpLongVector) create(false,
                selfId == 0 ? xi.getVectors()[0].add(yi.getVectors()[0]) : xi.getVectors()[0].copy(),
                selfId == 2 ? xi.getVectors()[1].add(yi.getVectors()[0]) : xi.getVectors()[1].copy());
        } else {
            return (TripletRpLongVector) create(false, IntStream.range(0, 2).mapToObj(i ->
                xi.getVectors()[i].add(yi.getVectors()[i])).toArray(LongVector[]::new));
        }
    }

    @Override
    public void addi(MpcLongVector xi, MpcLongVector yi) {
        assert !xi.isPlain();
        if (yi.isPlain()) {
            if (selfId != 1) {
                xi.getVectors()[selfId >> 1].addi(yi.getVectors()[0]);
            }
        } else {
            xi.getVectors()[0].addi(yi.getVectors()[0]);
            xi.getVectors()[1].addi(yi.getVectors()[1]);
        }
        if(xi instanceof TripletRpLongMacVector){
            ((TripletRpLongMacVector) xi).deleteMac();
        }
    }

    @Override
    public TripletRpLongVector sub(MpcLongVector xi, MpcLongVector yi) {
        assert !(xi.isPlain() && yi.isPlain());
        if (xi.isPlain()) {
            return (TripletRpLongVector) create(false,
                selfId == 0 ? xi.getVectors()[0].sub(yi.getVectors()[0]) : yi.getVectors()[0].neg(),
                selfId == 2 ? xi.getVectors()[0].sub(yi.getVectors()[1]) : yi.getVectors()[1].neg());
        } else if (yi.isPlain()) {
            return (TripletRpLongVector) create(false,
                selfId == 0 ? xi.getVectors()[0].sub(yi.getVectors()[0]) : xi.getVectors()[0].copy(),
                selfId == 2 ? xi.getVectors()[1].sub(yi.getVectors()[0]) : xi.getVectors()[1].copy());
        } else {
            return (TripletRpLongVector) create(false, IntStream.range(0, 2).mapToObj(i ->
                xi.getVectors()[i].sub(yi.getVectors()[i])).toArray(LongVector[]::new));
        }
    }

    @Override
    public void subi(MpcLongVector xi, MpcLongVector yi) {
        assert !xi.isPlain();
        if (yi.isPlain()) {
            if (selfId != 1) {
                xi.getVectors()[selfId >> 1].subi(yi.getVectors()[0]);
            }
        } else {
            xi.getVectors()[0].subi(yi.getVectors()[0]);
            xi.getVectors()[1].subi(yi.getVectors()[1]);
        }
        if(xi instanceof TripletRpLongMacVector){
            ((TripletRpLongMacVector) xi).deleteMac();
        }
    }

    @Override
    public MpcLongVector neg(MpcLongVector xi) {
        if (xi.isPlain()) {
            return create(false, xi.getVectors()[0].neg());
        } else {
            return create(false, Arrays.stream(xi.getVectors()).map(LongVector::neg).toArray(LongVector[]::new));
        }
    }

    @Override
    public void negi(MpcLongVector xi) {
        if (xi.isPlain()) {
            xi.getVectors()[0].negi();
        } else {
            xi.getVectors()[0].negi();
            xi.getVectors()[1].negi();
        }
        if(xi instanceof TripletRpLongMacVector){
            ((TripletRpLongMacVector) xi).deleteMac();
        }
    }

    @Override
    protected TripletLongVector[] mulPrivate(TripletLongVector[] xiArray, TripletLongVector[] yiArray){
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        int[] bitNums = Arrays.stream(xiArray).mapToInt(MpcLongVector::getNum).toArray();
        LongVector[] zeroShares = crProvider.randZeroZl64Vector(bitNums);
        intStream.forEach(i -> {
            zeroShares[i].addi(xiArray[i].getVectors()[0].mul(yiArray[i].getVectors()[0]));
            zeroShares[i].addi(xiArray[i].getVectors()[1].mul(yiArray[i].getVectors()[0]));
            zeroShares[i].addi(xiArray[i].getVectors()[0].mul(yiArray[i].getVectors()[1]));
        });
        sendLongVectors(PtoStep.MUL_OP.ordinal(), leftParty(), zeroShares);
        LongVector[] fromRight = receiveLongVectors(PtoStep.MUL_OP.ordinal(), rightParty());
        extraInfo++;
        intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        TripletRpLongVector[] res = intStream.mapToObj(i ->
                (TripletRpLongVector) create(false, zeroShares[i], fromRight[i]))
            .toArray(TripletRpLongVector[]::new);
        intoBuffer(new TripletRpLongVector[][]{
            Arrays.stream(xiArray).map(each -> (TripletRpLongVector) each).toArray(TripletRpLongVector[]::new),
            Arrays.stream(yiArray).map(each -> (TripletRpLongVector) each).toArray(TripletRpLongVector[]::new), res});
        return res;
    }

    @Override
    protected TripletLongVector mulPublic(TripletLongVector xi, PlainLongVector yi){
        return (TripletRpLongVector) create(false,
            yi.getVectors()[0].mul(xi.getVectors()[0]),
            yi.getVectors()[0].mul(xi.getVectors()[1]));
    }

    @Override
    public void muli(MpcLongVector[] xiArray, PlainLongVector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        for (int i = 0; i < xiArray.length; i++) {
            assert !xiArray[i].isPlain();
            assert xiArray[i].getNum() == yiArray[i].getNum();
        }
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> {
            xiArray[i].getVectors()[0].muli(yiArray[i].getVectors()[0]);
            xiArray[i].getVectors()[1].muli(yiArray[i].getVectors()[0]);
            if(xiArray[i] instanceof TripletRpLongMacVector){
                ((TripletRpLongMacVector) xiArray[i]).deleteMac();
            }
        });
    }

    @Override
    public MpcLongVector add(MpcLongVector xi, long constValue){
        if (xi.isPlain()) {
            return PlainLongVector.create(Arrays.stream(xi.getVectors()[0].getElements()).map(each -> each + constValue).toArray());
        } else {
            return create(false,
                selfId == 0
                    ? LongVector.create(Arrays.stream(xi.getVectors()[0].getElements()).map(each -> each + constValue).toArray())
                    : xi.getVectors()[0].copy(),
                selfId == 2
                    ? LongVector.create(Arrays.stream(xi.getVectors()[1].getElements()).map(each -> each + constValue).toArray())
                    : xi.getVectors()[1].copy());
        }
    }

    @Override
    public void addi(MpcLongVector xi, long constValue){
        if (xi.isPlain()) {
            long[] newArray = Arrays.stream(xi.getVectors()[0].getElements()).map(each -> each + constValue).toArray();
            xi.setVectors(LongVector.create(newArray));
        } else {
            if(selfId == 0){
                long[] newArray = Arrays.stream(xi.getVectors()[0].getElements()).map(each -> each + constValue).toArray();
                xi.setVectors(LongVector.create(newArray), xi.getVectors()[1]);
            }
            if(selfId == 2){
                long[] newArray = Arrays.stream(xi.getVectors()[1].getElements()).map(each -> each + constValue).toArray();
                xi.setVectors(xi.getVectors()[0], LongVector.create(newArray));
            }
        }
    }

    /**
     * put unverified multiplication tuples into buffer if malicious
     *
     * @param unverifiedData unverified multiplication result in the form of [[x1, y1, z1], [x2, y2, z2], ... ]
     */
    protected void intoBuffer(TripletRpLongVector[][] unverifiedData) {

    }
}
