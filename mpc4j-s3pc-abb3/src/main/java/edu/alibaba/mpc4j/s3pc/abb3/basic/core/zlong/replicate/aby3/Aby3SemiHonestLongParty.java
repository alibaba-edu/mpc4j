package edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.aby3;

import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.aby3.Aby3LongCpPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * The party of Replicated zl64 sharing with semi-honest security under honest-majority
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class Aby3SemiHonestLongParty extends AbstractAby3LongParty implements TripletLongParty {

    public Aby3SemiHonestLongParty(Rpc rpc, Aby3LongConfig config, TripletProvider tripletProvider) {
        super(rpc, config, tripletProvider);
    }

    @Override
    public void verifyMul() {

    }

    @Override
    public TripletRpLongVector[] shareOwn(LongVector[] xiArray) {
        int[] dataNums = Arrays.stream(xiArray).mapToInt(LongVector::getNum).toArray();
        LongVector[] shareZero = crProvider.randZeroZl64Vector(dataNums);
        IntStream.range(0, xiArray.length).forEach(i -> shareZero[i].addi(xiArray[i]));
        sendLongVectors(PtoStep.INPUT_SHARE.ordinal(), leftParty(), shareZero);
        LongVector[] rightData = receiveLongVectors(PtoStep.INPUT_SHARE.ordinal(), rightParty());
        extraInfo++;
        return IntStream.range(0, xiArray.length).mapToObj(i ->
            TripletRpLongVector.create(shareZero[i], rightData[i])).toArray(TripletRpLongVector[]::new);
    }

    @Override
    public TripletRpLongVector[] shareOther(int[] nums, Party party) {
        LongVector[] shareZero = crProvider.randZeroZl64Vector(nums);
        sendLongVectors(PtoStep.INPUT_SHARE.ordinal(), leftParty(), shareZero);
        LongVector[] rightData = receiveLongVectors(PtoStep.INPUT_SHARE.ordinal(), rightParty());
        extraInfo++;
        return IntStream.range(0, nums.length).mapToObj(i ->
            TripletRpLongVector.create(shareZero[i], rightData[i])).toArray(TripletRpLongVector[]::new);
    }

    @Override
    public LongVector[] revealOwn(int validBitLen, MpcLongVector... xiArray) {
        long[][] data = receiveLong(PtoStep.REVEAL_SHARE.ordinal(), leftParty());
        extraInfo++;
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        return intStream.mapToObj(i -> {
            LongVector tmp = LongVector.create(data[i]);
            tmp.addi(xiArray[i].getVectors()[0]);
            tmp.addi(xiArray[i].getVectors()[1]);
            tmp.format(validBitLen);
            return tmp;
        }).toArray(LongVector[]::new);
    }

    @Override
    public void revealOther(Party party, MpcLongVector... xiArray) {
        if ((selfId + 1) % 3 == party.getPartyId()) {
            LongVector[] data = Arrays.stream(xiArray).map(x -> x.getVectors()[0]).toArray(LongVector[]::new);
            sendLongVectors(PtoStep.REVEAL_SHARE.ordinal(), party, data);
        }
        extraInfo++;
    }

    @Override
    public LongVector[] open(int validBitLen, MpcLongVector... xiArray) {
        LongVector[] sendData = Arrays.stream(xiArray).map(x -> {
            x.getVectors()[0].format(validBitLen);
            return x.getVectors()[0];
        }).toArray(LongVector[]::new);
        sendLongVectors(PtoStep.REVEAL_SHARE.ordinal(), rightParty(), sendData);
        long[][] data = receiveLong(PtoStep.REVEAL_SHARE.ordinal(), leftParty());
        extraInfo++;
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        return intStream.mapToObj(i -> {
            LongVector tmp = LongVector.create(data[i]);
            tmp.addi(xiArray[i].getVectors()[0]);
            tmp.addi(xiArray[i].getVectors()[1]);
            tmp.format(validBitLen);
            return tmp;
        }).toArray(LongVector[]::new);
    }
}
