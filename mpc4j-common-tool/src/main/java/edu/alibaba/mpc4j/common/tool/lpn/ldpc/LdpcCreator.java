package edu.alibaba.mpc4j.common.tool.lpn.ldpc;

import edu.alibaba.mpc4j.common.tool.lpn.LpnParams;

/**
 * LdpceCreator接口
 *
 * @author Hanwen Feng
 * @date 2022.3.11
 */
public interface LdpcCreator {
    /**
     * 生成LdpcCoder的成员矩阵，创建LdpcCoder
     * @return LdpcCoder
     */
    LdpcCoder createLdpcCoder();
    /**
     * 返回所创建的LDPC对应的LPN参数
     * @return Lpn 参数
     */
    LpnParams getLpnParams();
}
