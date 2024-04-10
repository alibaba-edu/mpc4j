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
     * Sets task ID. Only root protocol can set and all sub-protocols are set automatically.
     *
     * @param taskId task ID.
     */
    void setTaskId(int taskId);

    /**
     * Gets task ID.
     *
     * @return task ID.
     */
    int getTaskId();

    /**
     * Sets encoded task ID. This is intended for internally use. Do not call this manually.
     *
     * @param taskId task ID.
     */
    void setEncodeTaskId(int taskId);

    /**
     * Updates protocol path. This is intended for internally use. Do not call this manually.
     *
     * @param ptoPath protocol path.
     */
    void updatePtoPath(int[] ptoPath);

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
     * Sets display log level. Only root protocol can set and all sub-protocols are set automatically.
     *
     * @param displayLogLevel display log level.
     */
    void setDisplayLogLevel(int displayLogLevel);

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
