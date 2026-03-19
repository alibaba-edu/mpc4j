package edu.alibaba.mpc4j.work.db.sketch.utils.mg;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MGNaiveImpl implements MG{

    private Map<BigInteger, BigInteger> data ;
    private final int size;

    public MGNaiveImpl(int size){
        this.size = size;
        data=new HashMap<>(size);
    }

    public void input(BigInteger... elements){
        for(BigInteger element:elements){
            input(element,BigInteger.ONE);
        }
    }
    @Override
    public void input(BigInteger element,BigInteger weight){
        if(data.containsKey(element)){
            data.put(element,data.get(element).add(weight));
        }
        else{
            if(data.size()<size){
                data.put(element,weight);
            }
            else{
                Iterator<Map.Entry<BigInteger, BigInteger>> iterator = data.entrySet().iterator();
                while(iterator.hasNext()){
                    Map.Entry<BigInteger, BigInteger> entry = iterator.next();
                    entry.setValue(entry.getValue().subtract(BigInteger.ONE));
                    if(entry.getValue().compareTo(BigInteger.ZERO)==0){
                        iterator.remove();
                    }
                }
            }

        }
    }
    @Override
    public void input(BigInteger element){
        input(element,BigInteger.ONE);
    }
    @Override
    public BigInteger query(BigInteger element){
        return data.getOrDefault(element, BigInteger.ZERO);
    }
    @Override
    public Map<BigInteger, BigInteger> query(int k){
        if(k>=this.size){
            k=this.data.size();
        }
        List<Map.Entry<BigInteger, BigInteger>> sortedArray=data.entrySet().stream().sorted(
                Map.Entry.comparingByValue()
        ).toList();
        Map<BigInteger,BigInteger> result=new HashMap<>();
        System.out.println(sortedArray);
        for(int i=0,length=sortedArray.size();i<k;i++){
            result.put(sortedArray.get(length-i-1).getKey(),sortedArray.get(length-i-1).getValue());
        }
        return result;
    }
    @Override
    public Map<BigInteger,BigInteger> query(){
        return query(this.size);
    }
}
