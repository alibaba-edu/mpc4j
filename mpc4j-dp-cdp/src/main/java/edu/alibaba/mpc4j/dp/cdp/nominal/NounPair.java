package edu.alibaba.mpc4j.dp.cdp.nominal;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * 名词对。
 *
 * @author Weiran Liu
 * @date 2022/4/23
 */
public class NounPair {
    /**
     * 字母排序较小的名词
     */
    private final String smallNoun;
    /**
     * 字母排序较大的名词
     */
    private final String largeNoun;

    /**
     * 构建名词对。
     *
     * @param noun1 第一个名词。
     * @param noun2 第二个名词。
     */
    public NounPair(String noun1, String noun2) {
        // 将字母排序较小的名词放在前面，字母排序较大的名词放在后面
        if (noun1.compareTo(noun2) <= 0) {
            smallNoun = noun1;
            largeNoun = noun2;
        } else {
            smallNoun = noun2;
            largeNoun = noun1;
        }
    }

    /**
     * 返回字母排序较小的名词。
     *
     * @return 字母排序较小的名词。
     */
    public String getSmallNoun() {
        return smallNoun;
    }

    /**
     * 返回字母排序较大的名词。
     *
     * @return 字母排序较大的名词。
     */
    public String getLargeNoun() {
        return largeNoun;
    }

    @Override
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof NounPair) {
            NounPair that = (NounPair) anObject;
            return new EqualsBuilder()
                .append(this.smallNoun, that.smallNoun)
                .append(this.largeNoun, that.largeNoun)
                .isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(smallNoun)
            .append(largeNoun)
            .toHashCode();
    }
}
