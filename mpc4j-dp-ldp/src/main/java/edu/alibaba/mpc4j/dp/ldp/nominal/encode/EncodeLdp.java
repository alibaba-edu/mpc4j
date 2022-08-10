package edu.alibaba.mpc4j.dp.ldp.nominal.encode;

import edu.alibaba.mpc4j.dp.ldp.nominal.NominalLdp;

/**
 * 编码LDP机制。
 *
 * @author Weiran Liu
 * @date 2022/4/28
 */
public interface EncodeLdp extends NominalLdp {
    /**
     * 随机化输入值。
     *
     * @param value 输入值。
     * @return 随机化结果。
     */
    String randomize(String value);
}
