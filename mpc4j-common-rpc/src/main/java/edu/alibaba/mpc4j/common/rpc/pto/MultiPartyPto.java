package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.EnvType;

import java.security.SecureRandom;

/**
 * Multi-party protocol.
 *
 * @author Weiran Liu
 * @date 2021/12/19
 */
public interface MultiPartyPto {
    /**
     * Sets the task ID.
     *
     * @param taskId the task ID.
     */
    void setTaskId(int taskId);

    /**
     * Gets the task ID.
     *
     * @return the task ID.
     */
    int getTaskId();

    /**
     * Sets the encoded task ID.
     *
     * @param taskId       the task ID.
     * @param parentTreeId the parent tree ID.
     */
    void setEncodeTaskId(int taskId, int parentTreeId);

    /**
     * Adds the tree level.
     *
     * @param rowLevel     row level.
     * @param taskId       the task ID.
     * @param parentTreeId the parent tree ID.
     */
    void addTreeLevel(int rowLevel, int taskId, int parentTreeId);

    /**
     * Gets the encoded task ID.
     *
     * @return the encoded task ID.
     */
    long getEncodeTaskId();

    /**
     * Gets the invoked rpc instance.
     *
     * @return the invoked rpc instance.
     */
    Rpc getRpc();

    /**
     * Gets its own party information.
     *
     * @return its own party information.
     */
    default Party ownParty() {
        return getRpc().ownParty();
    }

    /**
     * Gets the protocol description.
     *
     * @return the protocol description.
     */
    PtoDesc getPtoDesc();

    /**
     * Gets other parties' information.
     *
     * @return other parties' information.
     */
    Party[] otherParties();

    /**
     * Sets parallel computing.
     *
     * @param parallel parallel computing.
     */
    void setParallel(boolean parallel);

    /**
     * Gets parallel computing.
     *
     * @return parallel computing.
     */
    boolean getParallel();

    /**
     * Sets the secure random state.
     *
     * @param secureRandom the secure random state.
     */
    void setSecureRandom(SecureRandom secureRandom);

    /**
     * Gets the environment.
     *
     * @return the environment.
     */
    EnvType getEnvType();

    /**
     * Gets the protocol name.
     *
     * @return the protocol name.
     */
    String getPtoName();

    /**
     * Checks if the protocol (and its sub-protocols) is initialized.
     *
     * @throws IllegalStateException if the protocol (or its sub-protocols) is not initialized.
     */
    void checkInitialized();

    /**
     * Destroys the protocol.
     *
     * @throws IllegalStateException if the protocol (or its sub-protocols) is not in the correct state.
     */
    void destroy();
}
