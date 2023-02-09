package edu.alibaba.mpc4j.s2pc.pir.index;

/**
 * 索引PIR协议参数。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public abstract class AbstractIndexPirParams {

    /**
     * 返回多项式包含的元素数量。
     *
     * @param elementByteLength 元素字节长度。
     * @param polyModulusDegree 多项式阶。
     * @param coeffBitLength    系数比特长度。
     * @return 多项式包含的元素数量。
     */
    protected int elementSizeOfPlaintext(int elementByteLength, int polyModulusDegree, int coeffBitLength) {
        int coeffSizeOfElement = coeffSizeOfElement(elementByteLength, coeffBitLength);
        int elementSizeOfPlaintext = polyModulusDegree / coeffSizeOfElement;
        assert elementSizeOfPlaintext > 0 :
            "N should be larger than the of coefficients needed to represent a database element";
        return elementSizeOfPlaintext;
    }

    /**
     * 返回表示单个元素所需的系数个数。
     *
     * @param elementByteLength 元素字节长度。
     * @param coeffBitLength    系数比特长度。
     * @return 表示单个元素所需的系数个数。
     */
    protected int coeffSizeOfElement(int elementByteLength, int coeffBitLength) {
        return (int) Math.ceil(Byte.SIZE * elementByteLength / (double) coeffBitLength);
    }
}
