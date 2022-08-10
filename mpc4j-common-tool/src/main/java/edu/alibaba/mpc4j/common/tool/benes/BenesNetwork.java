package edu.alibaba.mpc4j.common.tool.benes;

import java.util.Vector;

/**
 * 贝奈斯网络（Benes Network）网络接口。
 *
 * @author Weiran Liu
 * @date 2021/09/20
 */
public interface BenesNetwork<T> {
    /**
     * 返回贝奈斯网络总层数，即交换网络的交换门有多少列。
     *
     * @return 网络总层数。
     */
    int getLevel();

    /**
     * 返回第{@code levelIndex}层贝奈斯网络。
     *
     * @param levelIndex 层数。
     * @return 第{@code levelIndex}层贝奈斯网络。
     */
    boolean[] getNetworkLevel(int levelIndex);

    /**
     * 返回贝奈斯网络输入总数量，即一次要输入多少个数据进行交换。
     *
     * @return 输入总数量。
     */
    int getN();

    /**
     * 返回贝奈斯网络宽度，即交换网络的交换门有多少行。
     *
     * @return 贝奈斯网络宽度。
     */
    int getWidth();

    /**
     * 根据贝奈斯网络将输入向量置换为输出向量。
     *
     * @param inputVector 输入向量。
     * @return 输出向量。
     */
    Vector<T> permutation(final Vector<T> inputVector);

    /**
     * 返回贝奈斯网络类型。
     *
     * @return 贝奈斯网络类型。
     */
    BenesNetworkFactory.BenesNetworkType getBenesNetworkType();
}
