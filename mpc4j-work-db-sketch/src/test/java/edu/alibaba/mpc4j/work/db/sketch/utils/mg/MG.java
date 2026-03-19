package edu.alibaba.mpc4j.work.db.sketch.utils.mg;

import java.math.BigInteger;
import java.util.Map;

public interface MG {
    void input(BigInteger... elements);
    void input(BigInteger element,BigInteger weight);
    void input(BigInteger element);
    BigInteger query(BigInteger element);
    Map<BigInteger, BigInteger> query(int k);
    Map<BigInteger, BigInteger> query();
}
