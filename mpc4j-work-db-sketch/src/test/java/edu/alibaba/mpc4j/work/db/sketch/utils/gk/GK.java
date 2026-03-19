package edu.alibaba.mpc4j.work.db.sketch.utils.gk;

import java.math.BigInteger;

public interface GK {
    void input(BigInteger element);
    void input(BigInteger... elements);
    BigInteger query(BigInteger element);
}
