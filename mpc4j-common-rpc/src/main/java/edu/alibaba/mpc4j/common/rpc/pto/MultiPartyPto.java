package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

/**
 * 多参与方协议接口。
 *
 * @author Weiran Liu
 * @date 2021/12/19
 */
public interface MultiPartyPto {

    /**
     * 设置任务ID。
     *
     * @param taskId 任务ID。
     */
    void setTaskId(long taskId);

    /**
     * 返回任务ID>
     *
     * @return 任务ID。
     */
    long getTaskId();

    /**
     * 返回通信接口。
     *
     * @return 通信接口。
     */
    Rpc getRpc();

    /**
     * 返回自己的参与方信息。
     *
     * @return 自己的参与方信息。
     */
    default Party ownParty() {
        return getRpc().ownParty();
    }

    /**
     * 返回协议描述。
     *
     * @return 协议描述。
     */
    PtoDesc getPtoDesc();

    /**
     * 增加日志层次。
     */
    void addLogLevel();

    /**
     * 返回其他参与方信息。
     *
     * @return 其他参与方信息。
     */
    Party[] otherParties();
}
