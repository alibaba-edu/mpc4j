package edu.alibaba.mpc4j.work.db.sketch.utils.gk;

import java.math.BigInteger;

public class Representative implements Comparable<Representative>{
    private final long t;
    private final BigInteger key;
    //represent (g_i,delta_i)
    private BigInteger g1;
    private BigInteger delta1;
    //represent (g_i^o,delta_i^o)
    private BigInteger g2;
    private BigInteger delta2;


    public Representative(BigInteger key, long t)  {
        this.key = key;
        this.t = t;
        this.g1 =this.g2 =BigInteger.ZERO;
    }

    public BigInteger getKey() {
        return key;
    }
    public long getT() {
        return t;
    }
    public BigInteger getG1() {
        return g1;
    }
    public void setG1(BigInteger g1) {
        this.g1 = g1;
    }
    public BigInteger getG2() {
        return g2;
    }
    public void setG2(BigInteger g2) {
        this.g2 = g2;
    }
    public BigInteger getDelta1() {
        return delta1;
    }
    public void setDelta1(BigInteger delta1) {
        this.delta1 = delta1;
    }
    public BigInteger getDelta2() {
        return delta2;
    }
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
        int res=this.key.compareTo(that.key);
        return res;
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
