package edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate.Aby3Z2cPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * The party of Replicated z2 sharing with semi-honest security under honest-majority
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class Aby3SemiHonestZ2cParty extends AbstractAby3Z2cParty implements TripletZ2cParty {
    protected Aby3SemiHonestZ2cParty(Rpc rpc, Aby3Z2cConfig config, TripletProvider tripletProvider) {
        super(rpc, config, tripletProvider);
    }

    @Override
    public TripletRpZ2Vector[] shareOwn(BitVector[] xiArray) {
        int[] dataNums = Arrays.stream(xiArray).mapToInt(BitVector::bitNum).toArray();
        BitVector[] shareZero = crProvider.randZeroBitVector(dataNums);
        IntStream.range(0, xiArray.length).forEach(i -> shareZero[i].xori(xiArray[i]));
        sendBitVectors(PtoStep.INPUT_SHARE.ordinal(), leftParty(), shareZero);
        BitVector[] rightData = receiveBitVectors(PtoStep.INPUT_SHARE.ordinal(), rightParty(), dataNums);
        extraInfo++;
        return IntStream.range(0, xiArray.length).mapToObj(i ->
            TripletRpZ2Vector.create(shareZero[i], rightData[i])).toArray(TripletRpZ2Vector[]::new);
    }

    @Override
    public TripletZ2Vector[] shareOther(int[] bitNums, Party party) {
        BitVector[] shareZero = crProvider.randZeroBitVector(bitNums);
        sendBitVectors(PtoStep.INPUT_SHARE.ordinal(), leftParty(), shareZero);
        BitVector[] rightData = receiveBitVectors(PtoStep.INPUT_SHARE.ordinal(), rightParty(), bitNums);
        extraInfo++;
        return IntStream.range(0, bitNums.length).mapToObj(i ->
            TripletRpZ2Vector.create(shareZero[i], rightData[i])).toArray(TripletRpZ2Vector[]::new);
    }

    @Override
    public void revealOther(MpcZ2Vector[] xiArray, Party party) {
        if ((selfId + 1) % 3 == party.getPartyId()) {
            BitVector[] data = Arrays.stream(xiArray).map(x -> x.getBitVectors()[0]).toArray(BitVector[]::new);
            sendBitVectors(PtoStep.REVEAL_SHARE.ordinal(), party, data);
        }
        extraInfo++;
    }

    @Override
    public BitVector[] revealOwn(MpcZ2Vector[] xiArray) {
        BitVector[] data = receiveBitVectors(PtoStep.REVEAL_SHARE.ordinal(), leftParty(),
            Arrays.stream(xiArray).mapToInt(MpcZ2Vector::bitNum).toArray());
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> {
            data[i].xori(xiArray[i].getBitVectors()[0]);
            data[i].xori(xiArray[i].getBitVectors()[1]);
        });
        extraInfo++;
        return data;
    }

    @Override
    public BitVector[] open(TripletZ2Vector[] xiArray) {
        BitVector[] sendData = Arrays.stream(xiArray).map(x -> x.getBitVectors()[0]).toArray(BitVector[]::new);
        sendBitVectors(PtoStep.OPEN_SHARE.ordinal(), rightParty(), sendData);
        BitVector[] data = receiveBitVectors(PtoStep.OPEN_SHARE.ordinal(), leftParty(),
            Arrays.stream(xiArray).mapToInt(MpcZ2Vector::bitNum).toArray());
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> {
            data[i].xori(xiArray[i].getBitVectors()[0]);
            data[i].xori(xiArray[i].getBitVectors()[1]);
        });
        extraInfo++;
        return data;
    }
}
