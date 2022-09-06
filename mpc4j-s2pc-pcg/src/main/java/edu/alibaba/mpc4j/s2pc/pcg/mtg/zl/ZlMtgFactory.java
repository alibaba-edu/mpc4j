package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

/**
 * l比特三元组生成协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public class ZlMtgFactory {
    /**
     * 私有构造函数
     */
    private ZlMtgFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum ZlMtgType {
        /**
         * 文件
         */
        FILE,
        /**
         * 离线
         */
        OFFLINE,
        /**
         * 缓存
         */
        CACHE,
    }
}
