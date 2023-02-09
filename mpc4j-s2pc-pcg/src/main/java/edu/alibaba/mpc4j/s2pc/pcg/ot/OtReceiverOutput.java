package edu.alibaba.mpc4j.s2pc.pcg.ot;

/**
 * Oblivious transfer receiver output interface.
 *
 * @author Weiran Liu
 * @date 2022/12/21
 */
public interface OtReceiverOutput {
    /**
     * Get the choice bit at the index.
     *
     * @param index the index.
     * @return the choice bit.
     */
    boolean getChoice(int index);

    /**
     * Get the choice bit array.
     *
     * @return the choice bit array.
     */
    boolean[] getChoices();

    /**
     * Get the value of Rb at the index.
     *
     * @param index the index.
     * @return Rb.
     */
    byte[] getRb(int index);

    /**
     * Get the Rb array.
     *
     * @return the Rb array.
     */
    byte[][] getRbArray();

    /**
     * Get the number of oblivious transfer output.
     *
     * @return the number of oblivious transfer output.
     */
    int getNum();
}
