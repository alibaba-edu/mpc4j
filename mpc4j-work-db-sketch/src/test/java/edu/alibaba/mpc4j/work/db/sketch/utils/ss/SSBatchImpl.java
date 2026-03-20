package edu.alibaba.mpc4j.work.db.sketch.utils.ss;

import java.math.BigInteger;
import java.util.*;

/**
 * Batch-optimized implementation of Space-Saving sketch.
 * Uses buffering to batch updates and maintains top-k frequent items.
 */
public class SSBatchImpl implements SS {
    private final Map<BigInteger, BigInteger> buffer;
    private Map<BigInteger, BigInteger> data;
    private final long size;
    private long counter;

    /**
     * Constructs an SS batch implementation
     * @param size maximum number of items to track
     */
    public SSBatchImpl(long size) {
        this.size = size;
        this.buffer = new HashMap<>();
        this.data= new HashMap<>();
        this.counter = 0;
    }
    
    /**
     * Sums two BigIntegers
     * @param a first value
     * @param b second value
     * @return sum
     */
    private BigInteger sum(BigInteger a, BigInteger b) {
        return a.add(b);
    }
    
    /**
     * Merges buffered elements into the main data structure
     * and evicts low-frequency items if necessary
     */
    private void merge(){
        buffer.forEach((key,value)->data.merge(key,value,BigInteger::add));
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
        while(iterator.hasNext() && count < size){
            Map.Entry<BigInteger,BigInteger> entry=iterator.next();
            if(entry.getValue().compareTo(t)>=0){
                newMap.put(entry.getKey(),entry.getValue());
                count++;
            }
        }
        this.data=newMap;
    }
    /**
     * Inserts an element with specified weight
     * @param element element to insert
     * @param weight weight of the element
     */
    @Override
    public void input(BigInteger element, BigInteger weight) {
        counter++;
        if (buffer.containsKey(element)) {
            buffer.put(element, buffer.get(element).add(weight));
        }
        else {
            buffer.put(element, weight);
        }
        if (counter == size) {
            merge();
            counter = 0;
        }
    }
    
    /**
     * Inserts a single element with weight 1
     * @param element element to insert
     */
    @Override
    public void input(BigInteger element){
        input(element,BigInteger.ONE);
    }
    
    @Override
    public void input(BigInteger... elements) {
        for (BigInteger element : elements) {
            input(element, BigInteger.ONE);
        }
    }
    
    /**
     * Queries the estimated frequency of an element
     * @param element element to query
     * @return estimated frequency
     */
    @Override
    public BigInteger query(BigInteger element) {
        return buffer.getOrDefault(element, BigInteger.ZERO).add(data.getOrDefault(element, BigInteger.ZERO));
    }
    
    /**
     * Queries the top-k frequent items
     * @param k number of top items to return
     * @return map of top-k elements to their frequencies
     */
    public Map<BigInteger, BigInteger> query(int k) {
        merge();
        if (k >= this.data.size()) {
            k = this.data.size();
        }

        List<Map.Entry<BigInteger, BigInteger>> sortedArray = data.entrySet().stream().sorted(
                Map.Entry.comparingByValue()
        ).toList();
        assert (k <= sortedArray.size());
        Map<BigInteger, BigInteger> result = new HashMap<>();
        for (int i = 0, length = sortedArray.size(); i < k; i++) {
            result.put(sortedArray.get(length - i - 1).getKey(), sortedArray.get(length - i - 1).getValue());
        }
        return result;
    }
    
    @Override
    public Map<BigInteger, BigInteger> query() {
        return query((int) this.size);
    }
    
    /**
     * Gets the internal data map
     * @return data map
     */
    public Map<BigInteger, BigInteger> getData() {
        return data;
    }
}
