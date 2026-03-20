package edu.alibaba.mpc4j.work.db.sketch.utils.gk;

import java.math.BigInteger;

/**
 * Representative element in GK quantile sketch.
 * Stores a key with rank bounds (g1, delta1, g2, delta2).
 */
public class Representative implements Comparable<Representative>{
    private final long t;
    private final BigInteger key;
    //represent (g_i,delta_i)
    private BigInteger g1;
    private BigInteger delta1;
    //represent (g_i^o,delta_i^o)
    private BigInteger g2;
    private BigInteger delta2;

    /**
     * Constructs a representative element
     * @param key the element value
     * @param t timestamp/insertion order
     */
    public Representative(BigInteger key, long t)  {
        this.key = key;
        this.t = t;
        this.g1 =this.g2 =BigInteger.ZERO;
    }

    /**
     * Gets the element key
     * @return element key
     */
    public BigInteger getKey() {
        return key;
    }
    
    /**
     * Gets the timestamp
     * @return timestamp
     */
    public long getT() {
        return t;
    }
    
    /**
     * Gets g1 value (lower rank bound contribution)
     * @return g1 value
     */
    public BigInteger getG1() {
        return g1;
    }
    
    /**
     * Sets g1 value
     * @param g1 new g1 value
     */
    public void setG1(BigInteger g1) {
        this.g1 = g1;
    }
    
    /**
     * Gets g2 value (upper rank bound contribution)
     * @return g2 value
     */
    public BigInteger getG2() {
        return g2;
    }
    
    /**
     * Sets g2 value
     * @param g2 new g2 value
     */
    public void setG2(BigInteger g2) {
        this.g2 = g2;
    }
    
    /**
     * Gets delta1 value (lower rank error bound)
     * @return delta1 value
     */
    public BigInteger getDelta1() {
        return delta1;
    }
    
    /**
     * Sets delta1 value
     * @param delta1 new delta1 value
     */
    public void setDelta1(BigInteger delta1) {
        this.delta1 = delta1;
    }
    
    /**
     * Gets delta2 value (upper rank error bound)
     * @return delta2 value
     */
    public BigInteger getDelta2() {
        return delta2;
    }
    
    /**
     * Sets delta2 value
     * @param delta2 new delta2 value
     */
    public void setDelta2(BigInteger delta2) {
        this.delta2 = delta2;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Representative that = (Representative) o;
        // t can serve as an identifier of element
        return t == that.getT();
    }

    @Override
    public int compareTo(Representative that) {
        return this.key.compareTo(that.key);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("key=").append(key);
        sb.append(",g1=").append(g1);
        sb.append(",g2=").append(g2);
        sb.append(",delta1=").append(delta1);
        sb.append(",delta2=").append(delta2);
        sb.append(",t=").append(t);
        return sb.toString();
    }
}
