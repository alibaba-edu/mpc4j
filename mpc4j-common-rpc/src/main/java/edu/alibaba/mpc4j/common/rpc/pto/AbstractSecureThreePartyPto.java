package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.EnvType;

import java.security.SecureRandom;

/**
 * 抽象三方安全计算协议。
 *
 * @author Weiran Liu
 * @date 2022/4/29
 */
public abstract class AbstractSecureThreePartyPto extends AbstractThreePartyPto implements SecurePto {
    /**
     * 环境类型
     */
    protected final EnvType envType;
    /**
     * 随机状态
     */
    protected SecureRandom secureRandom;
    /**
     * 是否并发
     */
    public boolean parallel;
    /**
     * 是否完成初始化
     */
    protected boolean initialized;

    /**
     * 构建安全三方计算协议。
     *
     * @param ptoDesc    协议描述信息。
     * @param rpc        通信接口。
     * @param leftParty  左参与方。
     * @param rightParty 右参与方。
     * @param config     安全计算协议配置项。
     */
    protected AbstractSecureThreePartyPto(PtoDesc ptoDesc, Rpc rpc, Party leftParty, Party rightParty, SecurePtoConfig config) {
        super(ptoDesc, rpc, leftParty, rightParty);
        envType = config.getEnvType();
        secureRandom = new SecureRandom();
        parallel = false;
        initialized = false;
    }

    @Override
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    @Override
    public boolean getParallel() {
        return parallel;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }
}
