package edu.alibaba.mpc4j.common.tool;

import java.util.List;

/**
 * 可打包接口，实现此接口的对象可以打包成{@code List<byte[]>}。
 *
 * @author Weiran Liu
 * @date 2021/12/24
 */
public interface Packable {
    /**
     * 将对象打包为{@code List<byte[]>}。
     *
     * @return 打包结果。
     */
    List<byte[]> toByteArrayList();
}
