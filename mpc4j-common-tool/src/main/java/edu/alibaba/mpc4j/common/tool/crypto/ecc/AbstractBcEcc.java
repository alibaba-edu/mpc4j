package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.*;
import org.bouncycastle.math.raw.Nat;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Bouncy Castle椭圆曲线抽象类。其中，HashToPoint采用Google的private-join-and-compute方法实现，参见：
 * https://github.com/google/private-join-and-compute/blob/master/private_join_and_compute/crypto/ec_group.cc
 *
 * @author Weiran Liu
 * @date 2021/05/30
 */
public abstract class AbstractBcEcc implements Ecc {
    /**
     * 椭圆曲线类型
     */
    private final EccFactory.EccType eccType;
    /**
     * 椭圆曲线参数
     */
    protected final ECDomainParameters ecDomainParameters;
    /**
     * 预计算窗口映射
     */
    private final Map<ECPoint, FixedPointPreCompInfo> fixedPointPreCompInfoMap;

    /**
     * 构造Bouncy Castle椭圆曲线。
     *
     * @param eccType     椭圆曲线类型。
     * @param bcCurveName Bouncy Castle下椭圆曲线的名字。
     */
    AbstractBcEcc(EccFactory.EccType eccType, String bcCurveName) {
        X9ECParameters ecParameters = CustomNamedCurves.getByName(bcCurveName);
        ECParameterSpec ecParameterSpec = new ECParameterSpec(
            ecParameters.getCurve(), ecParameters.getG(), ecParameters.getN(),
            ecParameters.getH(), ecParameters.getSeed()
        );
        ecDomainParameters = new ECDomainParameters(
            ecParameterSpec.getCurve(), ecParameterSpec.getG(), ecParameterSpec.getN()
        );
        // 初始化窗口指针映射表
        fixedPointPreCompInfoMap = new HashMap<>();
        this.eccType = eccType;
    }

    @Override
    public ECDomainParameters getEcDomainParameters() {
        return ecDomainParameters;
    }

    @Override
    public ECPoint randomPoint(SecureRandom secureRandom) {
        while (true) {
            ECFieldElement x = ecDomainParameters.getCurve().randomFieldElement(secureRandom);
            ECFieldElement y = x.square()
                .add(ecDomainParameters.getCurve().getA()).multiply(x)
                .add(ecDomainParameters.getCurve().getB())
                .sqrt();
            if (y == null) {
                continue;
            }
            ECPoint ecPoint = ecDomainParameters.getCurve().createPoint(x.toBigInteger(), y.toBigInteger());
            // clearing the cofactor
            ecPoint = ecPoint.multiply(getCofactor());
            if (ecPoint == null || !ecPoint.isValid()) {
                continue;
            }
            return ecPoint;
        }
    }

    /**
     * 根据给定的哈希函数计算数据映射到椭圆曲线的结果。
     *
     * @param message 数据。
     * @param hash    哈希函数。
     * @return 数据映射到椭圆曲线的结果。
     */
    protected ECPoint hashToCurve(byte[] message, Hash hash) {
        // 计算输入消息的哈希值，尝试构建坐标x和坐标y，如果失败，则继续哈希
        byte[] messageHashBytes = hash.digestToBytes(message);
        while (true) {
            ECFieldElement x, y;
            // 哈希结果不需要模n，交给ECFieldElement判断结果是否合法
            BigInteger messageHash = BigIntegerUtils.byteArrayToNonNegBigInteger(messageHashBytes);
            try {
                x = ecDomainParameters.getCurve().fromBigInteger(messageHash);
            } catch (IllegalArgumentException e) {
                // 如果无法将哈希结果转换为坐标x，意味着哈希结果不是有效的椭圆曲线点，重新哈希
                messageHashBytes = hash.digestToBytes(messageHashBytes);
                continue;
            }
            y = x.square()
                .add(ecDomainParameters.getCurve().getA()).multiply(x)
                .add(ecDomainParameters.getCurve().getB())
                .sqrt();
            if (y == null) {
                // 如果y无解，重新哈希
                messageHashBytes = hash.digestToBytes(messageHashBytes);
                continue;
            }
            ECPoint ecPoint = ecDomainParameters.getCurve().createPoint(x.toBigInteger(), y.toBigInteger());
            // clearing the cofactor
            ecPoint = ecPoint.multiply(getCofactor());
            if (ecPoint == null || !ecPoint.isValid()) {
                messageHashBytes = hash.digestToBytes(messageHashBytes);
                continue;
            }
            return ecPoint;
        }
    }

    @Override
    public void precompute(ECPoint ecPoint) {
        if (!fixedPointPreCompInfoMap.containsKey(ecPoint)) {
            // 先判断给定点是否已经进行了预计算，如果没有，再执行预计算操作
            FixedPointPreCompInfo fixedPointPreCompInfo = FixedPointUtil.precompute(ecPoint);
            fixedPointPreCompInfoMap.put(ecPoint, fixedPointPreCompInfo);
        }
    }

    @Override
    public void destroyPrecompute(ECPoint point) {
        fixedPointPreCompInfoMap.remove(point);
    }

    @Override
    public ECPoint multiply(ECPoint ecPoint, BigInteger r) {
        if (fixedPointPreCompInfoMap.containsKey(ecPoint)) {
            // 先判断给定点是否已经进行了预计算，如果进行过预计算，则用预计算乘法处理
            FixedPointPreCompInfo info = fixedPointPreCompInfoMap.get(ecPoint);
            return fixedPointMultiply(info, r);
        } else {
            return ecPoint.multiply(r);
        }
    }

    @Override
    public ECPoint[] multiply(ECPoint ecPoint, BigInteger[] rs) {
        assert rs.length > 0;
        if (fixedPointPreCompInfoMap.containsKey(ecPoint)) {
            // 先判断给定点是否已经进行了预计算，如果进行过预计算，则用预计算乘法处理
            FixedPointPreCompInfo info = fixedPointPreCompInfoMap.get(ecPoint);
            return Arrays.stream(rs).map(r -> fixedPointMultiply(info, r)).toArray(ECPoint[]::new);
        } else {
            return Arrays.stream(rs).map(ecPoint::multiply).toArray(ECPoint[]::new);
        }
    }

    private ECPoint fixedPointMultiply(FixedPointPreCompInfo info, BigInteger r) {
        /*
         * 固定点乘法预计算，此部分代码来自于org.bouncycastle.math.ec.FixedPointCombMultiplier。原始代码中有个提示：
         * The comb works best when the scalars are less than the (possibly unknown) order.
         * Still, if we want to handle larger scalars, we could allow customization of the comb
         * size, or alternatively we could deal with the 'extra' bits either by running the comb
         * multiple times as necessary, or by using an alternative multiplier as prelude.
         */
        BigInteger n = this.ecDomainParameters.getN();
        BigInteger modR = r.mod(n);
        ECCurve c = this.ecDomainParameters.getCurve();
        int size = FixedPointUtil.getCombSize(c);
        ECLookupTable lookupTable = info.getLookupTable();
        int width = info.getWidth();
        int d = (size + width - 1) / width;
        ECPoint R = c.getInfinity();
        int fullComb = d * width;
        int[] K = Nat.fromBigInteger(fullComb, modR);
        int top = fullComb - 1;
        for (int i = 0; i < d; ++i) {
            int secretIndex = 0;
            for (int j = top - i; j >= 0; j -= d) {
                int secretBit = K[j >>> 5] >>> (j & 0x1F);
                secretIndex ^= secretBit >>> 1;
                secretIndex <<= 1;
                secretIndex ^= secretBit;
            }

            ECPoint add = lookupTable.lookup(secretIndex);
            R = R.twicePlus(add);
        }
        return R.add(info.getOffset());
    }

    @Override
    public EccFactory.EccType getEccType() {
        return eccType;
    }
}
