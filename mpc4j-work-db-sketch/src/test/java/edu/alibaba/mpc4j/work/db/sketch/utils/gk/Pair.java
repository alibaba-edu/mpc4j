package edu.alibaba.mpc4j.work.db.sketch.utils.gk;


import java.math.BigInteger;

public class Pair implements Comparable<Pair>{
    private final BigInteger d;
    public final Representative e1;
    public final Representative e2;
    public Pair(Representative e1, Representative e2) {
        //e1 and e2 are sorted by key, so this pair should be ordered
//            assert e1.getKey()<=e2.getKey();
        if(e1.getKey().compareTo(e2.getKey())<=0){
            this.e1=e1;
            this.e2=e2;
        }
        else{
            this.e1=e2;
            this.e2=e1;
        }
        if(this.e1.getT()>this.e2.getT()){
            d=e1.getG1().add(e1.getG2()).add(e2.getG1()).add(e2.getDelta1()).add(BigInteger.ONE);
        }
        else{
            d=e2.getG1().add(e2.getG2()).add(e1.getG2()).add(e1.getDelta2()).add(BigInteger.ONE);
        }
    }
    public BigInteger getD() {
        return d;
    }
    @Override
    public int compareTo(Pair o) {
        return d.compareTo(o.getD());
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair pair = (Pair) o;
        return this.e1.equals(pair.e1) && this.e2.equals(pair.e2);
    }
}
