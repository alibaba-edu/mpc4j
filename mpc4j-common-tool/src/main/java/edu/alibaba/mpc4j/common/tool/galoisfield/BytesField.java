package edu.alibaba.mpc4j.common.tool.galoisfield;

/**
 * BytesField interface. Elements in BytesField are represented as byte array.
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
public interface BytesField extends BytesRing {
    /**
     * Computes p / q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p / q.
     */
    byte[] div(byte[] p, byte[] q);

    /**
     * Computes p / q. The result is in-placed in q.
     *
     * @param p the element p.
     * @param q the element q.
     */
    void divi(byte[] p, byte[] q);

    /**
     * Computes 1 / p.
     *
     * @param p the element p.
     * @return 1 / p.
     */
    byte[] inv(byte[] p);

    /**
     * Computes 1 / p. The result is in-placed in p.
     *
     * @param p the element p.
     */
    void invi(byte[] p);
}
