package edu.alibaba.mpc4j.common.tool.okve.okvs;

/**
 * 二进制不经意键值对存储器（OKVS）。最大的特点是键值（key）可以是任意类型的数据，且映射值（value）的比特长度可以与键值对应的比特长度不相等。
 *
 * @author Weiran Liu
 * @date 2021/09/13
 */
public interface BinaryOkvs<T> extends Okvs<T> {

}
