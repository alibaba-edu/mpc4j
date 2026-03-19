package edu.alibaba.mpc4j.work.db.sketch.utils.cms;

import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.math.BigInteger;

public class CMSNaiveImpl implements CMS{
    private final int[][] data;
    private int n;
    private final int bitLength;
    private final int rowNum;
    private final int columnNum;
    private final BigInteger[][] hashParameter;
    private final int logSize;

    public CMSNaiveImpl(int d, int t,BigInteger[][] hashParameter, int elementBitLen) {
        this.rowNum=d;
        this.columnNum=t;
        this.bitLength=elementBitLen;
        this.data=new int[rowNum][columnNum];
        for(int i=0;i<rowNum;i++){
            for (int j=0;j<columnNum;j++){
                data[i][j]=0;
            }
        }
        this.n=0;
        this.logSize=LongUtils.ceilLog2(columnNum);

        assert(2==hashParameter.length):"two rows, first row is a and second row is b";
        assert(d==hashParameter[0].length):"row size must be equal to hash parameter length";
        this.hashParameter=hashParameter.clone();
    }

    @Override
    public void input(BigInteger... elements){
        for(BigInteger element: elements){
            input(element);
        }
    }

    @Override
    public void input(BigInteger element){
        for(int i=0;i<rowNum;i++){
            int index=hash(element,i);
            data[i][index]= data[i][index]+1;
        }
    }
    private int hash(BigInteger element,int index){
        BigInteger res= BigInteger.valueOf(1).shiftLeft(2*bitLength).subtract(BigInteger.valueOf(1)).and
                        (element.multiply(hashParameter[0][index]).add(hashParameter[1][index]))
                .shiftRight(2*bitLength-logSize);
        return res.intValue();
    }

    @Override
    public int query(BigInteger element) {
        return 0;
    }
    @Override
    public int[][] getTable(){
        return this.data.clone();
    }
}
