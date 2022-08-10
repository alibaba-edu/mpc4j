package edu.alibaba.mpc4j.crypto.phe.params;

import edu.alibaba.mpc4j.crypto.phe.PheFactory;

/**
 * 半同态加密密钥参数。
 *
 * @author Weiran Liu
 * @date 2021/12/24
 */
public interface PheKeyParams extends PheParams {
    /**
     * 此密钥是否为私钥。
     *
     * @return 如果是私钥，返回{@code true}，否则返回{@code false}。
     */
    boolean isPrivate();

    /**
     * 返回半同态加密类型。
     *
     * @return 半同态加密类型。
     */
    PheFactory.PheType getPheType();
}
