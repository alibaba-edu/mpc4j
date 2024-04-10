package edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong;

import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.circuit.zlong.PlainLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.AbstractAbbThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.aby3.Aby3LongCpPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Abstract class for three-party zl64c
 *
 * @author Feng Han
 * @date 2023/01/08
 */
public abstract class AbstractTripletLongParty extends AbstractAbbThreePartyPto implements TripletLongParty {
    /**
     * context party for 3pc
     */
    protected final TripletProvider tripletProvider;
    /**
     * correlated randomness generator for 3pc
     */
    protected S3pcCrProvider crProvider;
    /**
     * z2 mtg for 3pc
     */
    protected RpLongMtp zl64MtProvider;
    /**
     * 0,1,2; indicating the index of the current party
     */
    protected final int selfId;
    /**
     * flag of opening process
     */
    protected boolean duringVerificationFlag;

    protected AbstractTripletLongParty(PtoDesc ptoDesc, Rpc rpc, TripletLongConfig config, TripletProvider tripletProvider) {
        super(ptoDesc, rpc,
            rpc.getParty((rpc.ownParty().getPartyId() + 2) % 3),
            rpc.getParty((rpc.ownParty().getPartyId() + 1) % 3), config);
        this.tripletProvider = tripletProvider;
        crProvider = tripletProvider.getCrProvider();
        zl64MtProvider = tripletProvider.getZl64MtProvider();
        tripletProvider.getVerificationMsg().addParty(this);
        selfId = rpc.ownParty().getPartyId();
        duringVerificationFlag = false;
    }

    @Override
    public TripletProvider getTripletProvider(){
        return tripletProvider;
    }

    @Override
    public TripletLongVector[] mul(MpcLongVector[] xiArray, MpcLongVector[] yiArray){
        MpcLongVector[][] formatInput = checkAndOrganizeInput(xiArray, yiArray);
        TripletLongVector[] res = new TripletLongVector[xiArray.length];
        int[] publicIndexes = IntStream.range(0, yiArray.length)
            .filter(index -> formatInput[1][index].isPlain())
            .toArray();
        for (int index : publicIndexes) {
            res[index] = mulPublic((TripletLongVector) formatInput[0][index], (PlainLongVector) formatInput[1][index]);
        }
        int[] privateIndexes = IntStream.range(0, yiArray.length)
            .filter(index -> !formatInput[1][index].isPlain())
            .toArray();
        if(privateIndexes.length > 0){
            TripletLongVector[] left = Arrays.stream(privateIndexes).mapToObj(i -> (TripletLongVector) formatInput[0][i]).toArray(TripletLongVector[]::new);
            TripletLongVector[] right = Arrays.stream(privateIndexes).mapToObj(i -> (TripletLongVector) formatInput[1][i]).toArray(TripletLongVector[]::new);
            TripletLongVector[] tmpRes = mulPrivate(left, right);
            IntStream.range(0, privateIndexes.length).forEach(i -> res[privateIndexes[i]] = tmpRes[i]);
        }
        return res;
    }

    @Override
    public void compareView4Zero(int validBitLen, TripletLongVector... data) throws MpcAbortException {
        // 1. generate hash for x1^x2, and send it to right party
        Stream<TripletLongVector> stream = parallel ? Arrays.stream(data).parallel() : Arrays.stream(data);
        LongVector[] addRes = stream.map(x -> {
            LongVector tmp = x.getVectors()[0].add(x.getVectors()[1]);
            tmp.format(validBitLen);
            return tmp;
        }).toArray(LongVector[]::new);
        byte[] addHash = crProvider.genHash(addRes);
        send(PtoStep.COMPARE_VIEW.ordinal(), rightParty(), Collections.singletonList(addHash));
        // 2. generate hash for x2, and compare it with the data from the left party
        byte[] x2Hash = crProvider.genHash(Arrays.stream(data).map(x -> {
            LongVector tmp = x.getVectors()[1].neg();
            tmp.format(validBitLen);
            return tmp;
        }).toArray(LongVector[]::new));
        byte[] recData = receive(PtoStep.COMPARE_VIEW.ordinal(), leftParty()).get(0);
        if (!Arrays.equals(x2Hash, recData)) {
            throw new MpcAbortException("data is not consistent");
        }
    }

    /**
     * verify the data is the same by sending data to the right party. The input data should be formatted
     *
     * @param data to be checked
     * @throws MpcAbortException if the protocol is abort.
     */
    protected void compareView(LongVector... data) throws MpcAbortException {
        List<byte[]> hash = Collections.singletonList(crProvider.genHash(data));
        send(PtoStep.COMPARE_VIEW.ordinal(), rightParty(), hash);
        byte[] recData = receive(PtoStep.COMPARE_VIEW.ordinal(), leftParty()).get(0);
        if (!Arrays.equals(hash.get(0), recData)) {
            throw new MpcAbortException("data is not consistent");
        }
    }

    /**
     * check and re-organize the inputs: we hope the left one should be: private
     */
    protected MpcLongVector[][] checkAndOrganizeInput(MpcLongVector[] xiArray, MpcLongVector[] yiArray){
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        for (int i = 0; i < xiArray.length; i++) {
            assert !(xiArray[i].isPlain() && yiArray[i].isPlain());
            assert xiArray[i].getNum() == yiArray[i].getNum();
        }

        int arrayLen = xiArray.length;
        TripletLongVector[] left = new TripletLongVector[arrayLen];
        MpcLongVector[] right = new MpcLongVector[arrayLen];

        for(int i = 0; i < arrayLen; i++){
            if(xiArray[i].isPlain()){
                left[i] = (TripletLongVector) yiArray[i];
                right[i] = xiArray[i];
            }else{
                left[i] = (TripletLongVector) xiArray[i];
                right[i] = yiArray[i];
            }
        }
        return new MpcLongVector[][]{left, right};
    }

    protected abstract TripletLongVector[] mulPrivate(TripletLongVector[] xiArray, TripletLongVector[] yiArray);

    protected abstract TripletLongVector mulPublic(TripletLongVector xi, PlainLongVector yi);


    @Override
    public boolean getDuringVerificationFlag(){
        return duringVerificationFlag;
    }

    @Override
    public void setDuringVerificationFlag(boolean duringVerificationFlag){
        this.duringVerificationFlag = duringVerificationFlag;
    }

    @Override
    public void checkUnverified() throws MpcAbortException {
        if(!duringVerificationFlag){
            tripletProvider.getVerificationMsg().checkUnverified();
        }
    }
}
