package edu.alibaba.mpc4j.common.tool.crypto.commit;

import org.bouncycastle.crypto.Commitment;

/**
 * commitment.
 *
 * @author Weiran Liu
 * @date 2023/3/17
 */
public interface Commit {
    /**
     * Gets the commitment scheme type.
     *
     * @return the commitment scheme type.
     */
    CommitFactory.CommitType getType();

    /**
     * Gets the commitment scheme name.
     *
     * @return the commitment scheme name.
     */
    default String getName() {
        return getType().name();
    }

    /**
     * Generate a commitment for the passed-in message.
     *
     * @param message the message to be committed to.
     * @return a commitment.
     */
    Commitment commit(byte[] message);

    /**
     * Return true if the passed-in commitment represents a commitment to the passed-in message.
     *
     * @param message    the message that was expected to have been committed to.
     * @param commitment a commitment previously generated.
     * @return true if commitment matches message, false otherwise.
     */
    boolean isRevealed(byte[] message, Commitment commitment);

    /**
     * Gets the maximal message byte length for the commitment.
     *
     * @return the maximal message byte length for the commitment.
     */
    int maxMessageByteLength();
}
