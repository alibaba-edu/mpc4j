package edu.alibaba.mpc4j.work.db.sketch.utils.cms;

import java.math.BigInteger;

public interface CMS {

    void input(BigInteger element);
    void input(BigInteger... elements);
    int query(BigInteger element);
    int[][] getTable();
}
