package edu.alibaba.mpc4j.work.db.sketch.utils.mg;

import java.math.BigInteger;
import java.util.*;

public class MGBatchImpl implements MG {
    private final Map<BigInteger, BigInteger> buffer;
    private Map<BigInteger,BigInteger> data;
    private final long size;
    private long counter;

    public MGBatchImpl(long size) {
        this.size = size;
        this.buffer = new HashMap<>();
        this.data= new HashMap<>();
        this.counter = 0;
    }
    private BigInteger sum(BigInteger a, BigInteger b) {
        return a.add(b);
    }
    private void merge(){
        buffer.forEach((key,value)->data.merge(key,value,this::sum));
        buffer.clear();
        if(data.size()<=size){
            return;
        }
        BigInteger[] values=data.values().toArray(new BigInteger[0]);
        Arrays.sort(values);
        BigInteger t=values[(int)(values.length-size-1)];

        Iterator<Map.Entry<BigInteger,BigInteger>> iterator=data.entrySet().iterator();
        Map<BigInteger,BigInteger> newMap=new HashMap<>();
        int count=0;
        while(iterator.hasNext()){
            Map.Entry<BigInteger,BigInteger> entry=iterator.next();
            if(entry.getValue().compareTo(t)>0){
                newMap.put(entry.getKey(),entry.getValue().subtract(t));
                count++;
            }
        }
        this.data=newMap;
    }
    @Override
    public void input(BigInteger element,BigInteger weight){
        counter++;
        if(buffer.containsKey(element)){
            buffer.put(element,buffer.get(element).add(weight));
        }
        else{
            buffer.put(element,weight);
        }
        if(counter==size){
            merge();
            counter=0;
        }
    }
    @Override
    public void input(BigInteger element){
        input(element,BigInteger.ONE);
    }
    @Override
    public void input(BigInteger... elements){
        for(BigInteger element:elements){
            input(element,BigInteger.ONE);
        }
    }
    @Override
    public BigInteger query(BigInteger element){
        return buffer.getOrDefault(element,BigInteger.ZERO).add(data.getOrDefault(element,BigInteger.ZERO));
    }
    public Map<BigInteger,BigInteger> query(int k){
        merge();
        if(k>=this.data.size()){
            k=this.data.size();
        }

        List<Map.Entry<BigInteger, BigInteger>> sortedArray=data.entrySet().stream().sorted(
                Map.Entry.comparingByValue()
        ).toList();
        assert (k<=sortedArray.size());
        Map<BigInteger,BigInteger> result=new HashMap<>();
        for(int i=0,length=sortedArray.size();i<k;i++){
            result.put(sortedArray.get(length-i-1).getKey(),sortedArray.get(length-i-1).getValue());
        }
        return result;
    }
    @Override
    public Map<BigInteger,BigInteger> query(){
        return query((int) this.size);
    }
    public Map<BigInteger,BigInteger> getData(){
        return data;
    }
}
