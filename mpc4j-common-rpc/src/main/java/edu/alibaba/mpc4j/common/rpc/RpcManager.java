package edu.alibaba.mpc4j.common.rpc;

import java.util.Set;

/**
 * RPC管理器，永远简单完成RPC设置。
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
public interface RpcManager {
    /**
     * 返回参与方ID对应的RPC。
     *
     * @param partyId 参与方ID。
     * @return 参与方ID对应的RPC。
     */
    Rpc getRpc(int partyId);

    /**
     * 返回此RPC管理器设置的参与方数量。
     *
     * @return 参与方数量。
     */
    int getPartyNum();

    /**
     * 返回所有参与方集合。
     *
     * @return 所有参与方集合。
     */
    Set<Party> getPartySet();
}
