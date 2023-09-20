package edu.alibaba.mpc4j.s2pc.pjc.pid;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.ArrayList;
import java.util.Set;

/**
 * 抽象PID协议服务端。
 *
 * @author Weiran Liu
 * @date 2022/01/19
 */
public abstract class AbstractPidParty<T> extends AbstractTwoPartyPto implements PidParty<T> {
    /**
     * 最大自己元素数量
     */
    private int maxOwnElementSetSize;
    /**
     * 最大对方元素数量
     */
    private int maxOtherElementSetSize;
    /**
     * 自己元素列表
     */
    protected ArrayList<T> ownElementArrayList;
    /**
     * 自己元素数量
     */
    protected int ownElementSetSize;
    /**
     * 对方元素数量
     */
    protected int otherElementSetSize;

    protected AbstractPidParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, PidConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxOwnElementSetSize, int maxOtherElementSetSize) {
        MathPreconditions.checkGreater("maxOwnElementSetSize", maxOwnElementSetSize, 1);
        this.maxOwnElementSetSize = maxOwnElementSetSize;
        MathPreconditions.checkGreater("maxOtherElementSetSize", maxOtherElementSetSize, 1);
        this.maxOtherElementSetSize = maxOtherElementSetSize;
        initState();
    }

    protected void setPtoInput(Set<T> ownElementSet, int otherElementSetSize) {
        checkInitialized();
        MathPreconditions.checkGreater("ownElementSetSize", ownElementSet.size(), 1);
        MathPreconditions.checkLessOrEqual("ownElementSetSize", ownElementSet.size(), maxOwnElementSetSize);
        ownElementArrayList = new ArrayList<>(ownElementSet);
        ownElementSetSize = ownElementArrayList.size();
        MathPreconditions.checkGreater("otherElementSetSize", otherElementSetSize, 1);
        MathPreconditions.checkLessOrEqual("otherElementSetSize", otherElementSetSize, maxOtherElementSetSize);
        this.otherElementSetSize = otherElementSetSize;
        extraInfo++;
    }
}
