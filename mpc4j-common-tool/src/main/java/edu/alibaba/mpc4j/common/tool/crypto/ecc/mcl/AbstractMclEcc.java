package edu.alibaba.mpc4j.common.tool.crypto.ecc.mcl;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.AbstractNativeEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.NativeEcc;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;

/**
 * MCL椭圆曲线。
 *
 * @author Weiran Liu
 * @date 2022/8/24
 */
abstract class AbstractMclEcc extends AbstractNativeEcc {
    /**
     * 表示无穷远点的字符串
     */
    private static final String INFINITY_STRING = "0";

    AbstractMclEcc(NativeEcc nativeEcc, EccFactory.EccType eccType, String bcCurveName) {
        super(nativeEcc, eccType, bcCurveName);
    }

    @Override
    protected ECPoint nativePointStringToEcPoint(String mclString) {
        // 如果返回结果是0元，需要单独处理
        if (INFINITY_STRING.equals(mclString)) {
            return getInfinity();
        }
        // Bouncy Castle中的ECPoint不支持设置点的参数Z，因此C++层所有计算结果都需要normalize
        String[] mclStrings = mclString.split(" ");
        assert mclStrings.length == 3;
        return ecDomainParameters.getCurve().createPoint(
            new BigInteger(mclStrings[1], RADIX), new BigInteger(mclStrings[2], RADIX)
        );
    }

    @Override
    protected String ecPointToNativePointString(ECPoint ecPoint) {
        // Bouncy Castle中的ECPoint可以得到点的参数Z，MCL椭圆曲线库的格式为"Z X Y"，必须要先归一化再完成计算
        ECPoint normalizedEcPoint = ecPoint.normalize();
        return normalizedEcPoint.getZCoord(0)
            + " " + normalizedEcPoint.getXCoord()
            + " " + normalizedEcPoint.getYCoord();
    }
}
