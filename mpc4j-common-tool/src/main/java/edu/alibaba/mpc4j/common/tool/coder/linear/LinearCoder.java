package edu.alibaba.mpc4j.common.tool.coder.linear;

import edu.alibaba.mpc4j.common.tool.coder.Coder;

/**
 * 线性编码接口。线性编码具有线性特性，即对于任意码字m_0和m_1，有C(m_0) ⊕ C(m_1) = C(m_0 ⊕ m_1)。
 *
 * @author Weiran Liu
 * @date 2021/12/14
 */
public interface LinearCoder extends Coder {

}
