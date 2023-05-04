package edu.alibaba.mpc4j.common.tool.galoisfield.zp;

import edu.alibaba.mpc4j.common.tool.galoisfield.BigIntegerField;

import java.math.BigInteger;

/**
 * Zp有限域运算接口。
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
public interface Zp extends BigIntegerField {
    /**
     * Gets the Zp type.
     *
     * @return the Zp type.
     */
    ZpFactory.ZpType getZpType();

    /**
     * Gets the name.
     *
     * @return the name.
     */
    @Override
    default String getName() {
        return getZpType().name();
    }

    /**
     * Gets the prime.
     *
     * @return the prime.
     */
    BigInteger getPrime();

    /**
     * Computes a mod p.
     *
     * @param a the input a.
     * @return a mod p.
     */
    BigInteger module(final BigInteger a);
}
