package edu.alibaba.mpc4j.common.tool.galoisfield;

/**
 * ByteField interface. Elements in ByteField are represented as byte.
 *
 * @author Weiran Liu
 * @date 2024/5/22
 */
public interface ByteField extends ByteRing {
    /**
     * Computes p / q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p / q.
     */
    byte div(byte p, byte q);

    /**
     * Computes 1 / p.
     *
     * @param p the element p.
     * @return 1 / p.
     */
    byte inv(byte p);

    /**
     * Gets the prime.
     *
     * @return the prime.
     */
    byte getPrime();
}
