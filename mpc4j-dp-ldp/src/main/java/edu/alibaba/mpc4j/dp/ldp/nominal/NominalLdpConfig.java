package edu.alibaba.mpc4j.dp.ldp.nominal;

import edu.alibaba.mpc4j.dp.ldp.LdpConfig;

import java.util.ArrayList;
import java.util.Set;

/**
 * 枚举LDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/28
 */
public interface NominalLdpConfig extends LdpConfig {
    /**
     * 获取标签数组列表。
     *
     * @return 标签枚举值数组列表。
     */
    ArrayList<String> getLabelArrayList();

    /**
     * 获取标签集合。
     *
     * @return 标签集合。
     */
    Set<String> getLabelSet();

    /**
     * 获取标签数量。
     *
     * @return 标签数量。
     */
    default int getLabelSize() {
        return getLabelArrayList().size();
    }
}
