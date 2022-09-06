package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;

/**
 * 椭圆曲线窗口优化技术。完整计算过程参考MCL的窗口优化技术实现，参见：
 * <p>
 * https://github.com/herumi/mcl/blob/master/include/mcl/window_method.hpp
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/8/21
 */
public class WindowMethod {
    /**
     * 椭圆曲线运算接口
     */
    private final Ecc ecc;
    /**
     * 定点
     */
    private final ECPoint x;
    /**
     * 窗口大小
     */
    private final int windowSize;
    /**
     * 窗口遮蔽值
     */
    private final BigInteger windowsMask;
    /**
     * 表大小
     */
    private final int tableNum;
    /**
     * 每个查找表的大小
     */
    private final int r;
    /**
     * 查找表总大小
     */
    private final int totalTableSize;
    /**
     * 查找表
     */
    private ECPoint[] lookupTable;
    /**
     * 是否完成初始化
     */
    private boolean init;

    /**
     * 创建窗口优化方法。
     *
     * @param ecc 椭圆曲线接口。
     * @param x 定点。
     * @param windowSize 窗口大小。
     */
    public WindowMethod(Ecc ecc, ECPoint x, int windowSize) {
        this.ecc = ecc;
        this.x = x;
        int bitSize = ecc.getN().bitLength();
        assert windowSize > 0 && windowSize < Integer.SIZE - 1
            : "Window Size must be in range [0: " + (Integer.SIZE - 1) + "): " + windowSize;
        this.windowSize = windowSize;
        windowsMask = BigInteger.ONE.shiftLeft(windowSize).subtract(BigInteger.ONE);
        // const size_t tblNum = (bitSize + winSize - 1) / winSize
        tableNum = (bitSize + windowSize - 1) / windowSize;
        // const size_t r = size_t(1) << winSize
        r = 1 << windowSize;
        long longTableSize = (long)tableNum * bitSize;
        assert longTableSize > 0 && longTableSize < Integer.MAX_VALUE
            : "Table Size must be in range [0, " + Integer.MAX_VALUE + "): " + longTableSize;
        totalTableSize = tableNum * r;
    }

    /**
     * 初始化查找表。
     */
    public void init() {
        if (init) {
            return;
        }
        // *pb = tbl_.resize(tblNum * r)
        lookupTable = new ECPoint[totalTableSize];
        // Ec t(x)
        ECPoint t = x;
        // for (size_t i = 0; i < tblNum; i++)
        for (int tableIndex = 0; tableIndex < tableNum; tableIndex++) {
            // Ec* w = &tbl_[i * r]
            int startIndex = tableIndex << windowSize;
            // w[0].clear()
            lookupTable[startIndex] = ecc.getInfinity();
            // for (size_t d = 1; d < r; d *= 2)
            for (int d = 1; d < r; d = (d << 1)) {
                // for (size_t j = 0; j < d; j++)
                for (int j = 0; j < d; j++) {
                    lookupTable[startIndex + j + d] = lookupTable[startIndex + j].add(t);
                }
                // Ec::dbl(t, t)
                t = t.twice();
            }
        }
        // w[j].normalize()
        ECCurve ecCurve = ecc.getEcDomainParameters().getCurve();
        ecCurve.normalizeAll(lookupTable);
        init = true;
    }

    public ECPoint multiply(BigInteger r) {
        assert init : "Please init before using Window Method";
        // z.clear()
        ECPoint result = ecc.getInfinity();
        // BitIterator<Unit> ai(y, n);
        BigInteger modR = r.mod(ecc.getN());
        if (modR.equals(BigInteger.ZERO)) {
            return result;
        }
        for (int tableIndex = 0; tableIndex < tableNum; tableIndex++) {
            // Unit v = ai.getNext(winSize_)
            int v = modR.and(windowsMask).intValue();
            // if (v)
            if (v > 0) {
                // Ec::add(z, z, tbl_[(i << winSize_) + v])
                result = result.add(lookupTable[(tableIndex << windowSize) + v]);
            }
            modR = modR.shiftRight(windowSize);
            if (modR.equals(BigInteger.ZERO)) {
                break;
            }
        }
        return result;
    }
}
