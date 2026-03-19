package edu.alibaba.mpc4j.work.db.sketch.utils.hll;

import java.math.BigInteger;

public interface HLL {
    void input(BigInteger element);
    void input(BigInteger... elements);
    double query();
}
