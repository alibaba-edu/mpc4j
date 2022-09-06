package edu.alibaba.mpc4j.common.tool.crypto.ecc.openssl;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.AbstractNativeEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.NativeEcc;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

/**
 * OpenSSL椭圆曲线。
 *
 * @author Weiran Liu
 * @date 2022/8/24
 */
abstract class AbstractOpensslEcc extends AbstractNativeEcc {
    /**
     * 表示无穷远点的字符串
     */
    private static final String INFINITY_STRING = "00";

    AbstractOpensslEcc(NativeEcc nativeEcc, EccFactory.EccType eccType, String curveName) {
        super(nativeEcc, eccType, curveName);
    }

    @Override
    protected ECPoint nativePointStringToEcPoint(String pointString) {
        // 如果返回结果是0元，需要单独处理
        if (INFINITY_STRING.equals(pointString)) {
            return getInfinity();
        }
        return ecDomainParameters.getCurve().decodePoint(Hex.decode(pointString));
    }

    @Override
    protected String ecPointToNativePointString(ECPoint ecPoint) {
        return Hex.toHexString(ecPoint.getEncoded(false));
    }
}
