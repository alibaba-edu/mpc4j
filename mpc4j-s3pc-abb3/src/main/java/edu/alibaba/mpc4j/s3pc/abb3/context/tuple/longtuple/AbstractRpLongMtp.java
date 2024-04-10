package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.basic.AbstractAbbThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;

import java.util.Arrays;

/**
 * abstract zl64 mtp for replicated 3p sharing
 *
 * @author Feng Han
 * @date 2024/01/25
 */
public abstract class AbstractRpLongMtp extends AbstractAbbThreePartyPto implements RpLongMtp {
    /**
     * the buffered mt
     */
    protected TripletRpLongVector[] buffer;
    /**
     * the starting index to reading elements
     */
    protected int currentLongIndex;
    /**
     * How many tuples are actually used
     */
    public long allTupleNum;

    protected AbstractRpLongMtp(PtoDesc ptoDesc, Rpc rpc, RpLongMtpConfig config) {
        super(ptoDesc, rpc, config);
        allTupleNum = 0L;
    }

    @Override
    public TripletRpLongVector[][] getTuple(int[] nums) throws MpcAbortException {
        if(buffer == null || currentLongIndex >= buffer[0].getNum()){
            fillBuffer();
        }
        TripletRpLongVector[][] res = new TripletRpLongVector[3][nums.length];
        for(int i = 0; i < nums.length; i++){
            int bufferResEleNum = buffer[0].getNum() - currentLongIndex;
            if(bufferResEleNum >= nums[i]){
                int endEleIndex = currentLongIndex + nums[i];
                for(int dim = 0; dim < 3; dim++){
                    res[dim][i] = buffer[dim].copyToNew(currentLongIndex, endEleIndex);
                }
                currentLongIndex = endEleIndex;
            }else{
                for(int dim = 0; dim < 3; dim++){
                    res[dim][i] = TripletRpLongVector.createZeros(nums[i]);
                    res[dim][i].setElements(buffer[dim], currentLongIndex, 0, bufferResEleNum);
                }
                int destStart = bufferResEleNum;
                while(destStart < nums[i]){
                    fillBuffer();
                    currentLongIndex = Math.min(buffer[0].getNum(), nums[i] - destStart);
                    for(int dim = 0; dim < 3; dim++){
                        res[dim][i].setElements(buffer[dim], 0, destStart, currentLongIndex);
                    }
                    destStart += currentLongIndex;
                }
            }
            if(currentLongIndex >= buffer[0].getNum() && i < nums.length - 1){
                fillBuffer();
            }
        }
        allTupleNum += Arrays.stream(nums).mapToLong(i -> (long) i).sum();
        return res;
    }

    @Override
    public long getAllTupleNum(){
        return allTupleNum;
    }

    protected abstract void fillBuffer() throws MpcAbortException;
}
