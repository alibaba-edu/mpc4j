package edu.alibaba.mpc4j.s2pc.pso.pid;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pso.pid.PidFactory.PidType;

import java.util.ArrayList;
import java.util.Set;

/**
 * 抽象PID协议服务端。
 *
 * @author Weiran Liu
 * @date 2022/01/19
 */
public abstract class AbstractPidParty<T> extends AbstractSecureTwoPartyPto implements PidParty<T> {
    /**
     * 配置项
     */
    private final PidConfig config;
    /**
     * 最大自己元素数量
     */
    private int maxOwnSetSize;
    /**
     * 最大对方元素数量
     */
    private int maxOtherSetSize;
    /**
     * 自己元素列表
     */
    protected ArrayList<T> ownElementArrayList;
    /**
     * 自己元素数量
     */
    protected int ownSetSize;
    /**
     * 对方元素数量
     */
    protected int otherSetSize;

    protected AbstractPidParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, PidConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    @Override
    public PidType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxOwnSetSize, int maxOtherSetSize) {
        assert maxOwnSetSize > 1 : "max(OwnSetSize) must be greater than 1";
        this.maxOwnSetSize = maxOwnSetSize;
        assert maxOtherSetSize > 1 : "max(OtherSetSize) must be greater than 1";
        this.maxOtherSetSize = maxOwnSetSize;
        initialized = false;
    }

    protected void setPtoInput(Set<T> ownElementSet, int otherSetSize) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert ownElementSet.size() > 1 && ownElementSet.size() <= maxOwnSetSize :
            "OwnSetSize must be in range (1, " + maxOwnSetSize + "]";
        ownElementArrayList = new ArrayList<>(ownElementSet);
        ownSetSize = ownElementArrayList.size();
        assert otherSetSize > 1 && otherSetSize <= maxOtherSetSize :
            "OtherSetSize must be in range (1, " + maxOtherSetSize + "]";
        this.otherSetSize = otherSetSize;
        extraInfo++;
    }
}
