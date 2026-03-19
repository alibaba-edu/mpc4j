package edu.alibaba.mpc4j.work.db.sketch.utils.cms;

import java.math.BigInteger;

public class CMSv1BatchImpl extends AbstractCMSBatchImpl implements CMS {

    public CMSv1BatchImpl(int d, int t, BigInteger[][] hashParameter, int elementBitLen){
        super(d,t,elementBitLen);
        assert(2==hashParameter.length):"two rows, first row is a and second row is b";
        assert(d==hashParameter[0].length):"row size must be equal to hash parameter length";
        this.hashParameter=hashParameter.clone();
    }

    @Override
    protected void merge(){
        for(int j=0;j<bufferSize;j++){
            BigInteger element=buffer[j];
            for(int i=0;i<rowNum;i++){
                int index=hash(element,i);
                data[i][index]= data[i][index]+1;
            }
        }
        bufferSize=0;
    }
    protected int hash(BigInteger element,int index){
        BigInteger res= BigInteger.valueOf(1).shiftLeft(2*bitLength).subtract(BigInteger.valueOf(1)).and
                (element.multiply(hashParameter[0][index]).add(hashParameter[1][index]))
                .shiftRight(2*bitLength-logSize);
        return res.intValue();
    }
    private final BigInteger[][] hashParameter;
}
