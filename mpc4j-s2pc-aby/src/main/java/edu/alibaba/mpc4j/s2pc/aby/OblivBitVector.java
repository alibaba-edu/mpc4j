package edu.alibaba.mpc4j.s2pc.aby;

/**
 * 不经意比特向量（Oblivious Bit Vector）。
 * <p>
 * 对于布尔电路（Boolean Circuit），其实现为布尔秘密分享值（Boolean Secret-Shared Value）。
 * </p>
 * <p>
 * 对于乱码电路（Garbled Circuit），其实现为乱码电路的导线（Wire）。
 * </p>
 * <p>
 * 对于代数电路（Arithmetic Circuit），其实现为代数秘密分享值（Arithmetic Secret-Shared Value）。
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public interface OblivBitVector {
    /**
     * 返回是否为公开导线组。只要有一个导线非公开，则认为整体是非公开导线组。
     *
     * @return 是否为公开导线组。
     */
    boolean isPublic();

    /**
     * 返回指定导线是否为公开导线。
     *
     * @param index 索引值。
     * @return 指定导线是否为公开导线。
     */
    boolean isPublic(int index);

    /**
     * 返回导线组比特长度。
     *
     * @return 导线组比特长度。
     */
    int bitLength();

    /**
     * 返回导线组字节长度。
     *
     * @return 导线组字节长度。
     */
    int byteLength();
}
