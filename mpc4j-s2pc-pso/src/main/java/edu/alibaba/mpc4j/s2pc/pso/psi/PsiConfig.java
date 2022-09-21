package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * PSI协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public interface PsiConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    PsiFactory.PsiType getPtoType();
}
