package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.AbstractAbbThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;

import java.util.Arrays;

/**
 * abstract z2 mtp for replicated 3p sharing
 *
 * @author Feng Han
 * @date 2024/01/25
 */
public abstract class AbstractRpZ2Mtp extends AbstractAbbThreePartyPto implements RpZ2Mtp {
    /**
     * the buffered mt
     */
    protected TripletRpZ2Vector[] buffer;
    /**
     * Which byte is the current byte to start reading data from?
     */
    protected int currentByteIndex;
    /**
     * How many tuples are actually used
     */
    public long allTupleNum;

    protected AbstractRpZ2Mtp(PtoDesc ptoDesc, Rpc rpc, RpZ2MtpConfig config) {
        super(ptoDesc, rpc, config);
        allTupleNum = 0L;
    }

    @Override
    public TripletRpZ2Vector[][] getTuple(int[] bitNums) throws MpcAbortException {
        if(buffer == null || currentByteIndex >= buffer[0].byteNum()){
            fillBuffer();
        }
        TripletRpZ2Vector[][] res = new TripletRpZ2Vector[3][bitNums.length];
        for(int i = 0; i < bitNums.length; i++){
            int byteNum = CommonUtils.getByteLength(bitNums[i]);
            int bufferResByteNum = buffer[0].byteNum() - currentByteIndex;
            if(bufferResByteNum >= byteNum){
                int endByteIndex = currentByteIndex + byteNum;
                for(int dim = 0; dim < 3; dim++){
                    res[dim][i] = TripletRpZ2Vector.copyOfByte(buffer[dim], currentByteIndex, endByteIndex, bitNums[i]);
                }
                currentByteIndex = endByteIndex;
            }else{
                for(int dim = 0; dim < 3; dim++){
                    res[dim][i] = TripletRpZ2Vector.createEmpty(byteNum << 3);
                    res[dim][i].setBytes(buffer[dim], currentByteIndex, 0, bufferResByteNum);
                }
                int destStart = bufferResByteNum;
                while(destStart < byteNum){
                    fillBuffer();
                    currentByteIndex = Math.min(buffer[0].byteNum(), byteNum - destStart);
                    for(int dim = 0; dim < 3; dim++){
                        res[dim][i].setBytes(buffer[dim], 0, destStart, currentByteIndex);
                    }
                    destStart += currentByteIndex;
                }
                for(int dim = 0; dim < 3; dim++){
                    res[dim][i].reduce(bitNums[i]);
                }
            }
            if(currentByteIndex >= buffer[0].byteNum() && i < bitNums.length - 1){
                fillBuffer();
            }
        }
        allTupleNum += Arrays.stream(bitNums).mapToLong(i -> (long) i).sum();
        return res;
    }

    @Override
    public long getAllTupleNum(){
        return allTupleNum;
    }

    protected abstract void fillBuffer() throws MpcAbortException;
}
