package edu.alibaba.mpc4j.common.tool.galoisfield;

import java.util.Set;

/**
 * 最大线性无关组（Maximal Linearly Independent System）查找器。给定m行n列（m × n）矩阵，查找器返回最大线性无关组的行集合。
 *
 * @author Weiran Liu
 * @date 2021/09/11
 */
public interface MaxLisFinder {
    /**
     * 返回最大线性无关组的行集合。
     *
     * @return 最大线性无关组的行集合。
     */
    Set<Integer> getLisRows();
}
