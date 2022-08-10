package cc.redberry.rings;

import cc.redberry.rings.bigint.BigInteger;
import cc.redberry.rings.util.RandomUtil;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.Iterator;

/**
 * JDK实现的Zp域运算，其中modInverse调用GMP实现。实现代码参考：
 * <p>
 * cc.redberry.rings.IntegersZp
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/8/9
 */
public class JdkIntegersZp extends AIntegers {
    private static final long serialVersionUID = 729718760221666559L;
    /**
     * The modulus.
     */
    private final BigInteger modulus;
    /**
     * The Java modulus.
     */
    private final java.math.BigInteger jModulus;

    /**
     * Creates Zp ring for specified modulus.
     *
     * @param modulus the modulus
     */
    public JdkIntegersZp(BigInteger modulus) {
        this.modulus = modulus;
        jModulus = new java.math.BigInteger(modulus.toByteArray());
    }

    @Override
    public boolean isField() {
        return true;
    }

    @Override
    public boolean isEuclideanRing() {
        return true;
    }

    @Override
    public BigInteger cardinality() {
        return modulus;
    }

    @Override
    public BigInteger characteristic() {
        return modulus;
    }

    @Override
    public boolean isUnit(BigInteger element) {
        return !element.isZero() && !modulus.divideAndRemainder(element)[1].isZero();
    }

    /**
     * Returns {@code val mod this.modulus}.
     *
     * @param val the integer.
     * @return {@code val mod this.modulus}.
     */
    public BigInteger modulus(BigInteger val) {
        return (val.signum() >= 0 && val.compareTo(modulus) < 0) ? val : val.mod(modulus);
    }

    @Override
    public BigInteger add(BigInteger a, BigInteger b) {
        a = valueOf(a);
        b = valueOf(b);
        BigInteger r = a.add(b);
        BigInteger rm = r.subtract(modulus);
        return rm.signum() >= 0 ? rm : r;
    }

    @Override
    public BigInteger subtract(BigInteger a, BigInteger b) {
        a = valueOf(a);
        b = valueOf(b);
        BigInteger r = a.subtract(b);
        return r.signum() < 0 ? r.add(modulus) : r;
    }

    @Override
    public BigInteger negate(BigInteger element) {
        return element.isZero() ? element : modulus.subtract(valueOf(element));
    }

    @Override
    public BigInteger multiply(BigInteger a, BigInteger b) {
        return modulus(a.multiply(b));
    }

    @Override
    public BigInteger[] divideAndRemainder(BigInteger a, BigInteger b) {
        return new BigInteger[]{divide(a, b), BigInteger.ZERO};
    }

    public BigInteger divide(BigInteger a, BigInteger b) {
        java.math.BigInteger jb = new java.math.BigInteger(b.toByteArray());
        java.math.BigInteger jc = BigIntegerUtils.modInverse(jb, jModulus);
        return multiply(a, new BigInteger(jc));
    }

    @Override
    public BigInteger remainder(BigInteger a, BigInteger b) {
        return getZero();
    }

    @Override
    public BigInteger reciprocal(BigInteger element) {
        java.math.BigInteger jElement = new java.math.BigInteger(element.toByteArray());
        java.math.BigInteger jInverse = BigIntegerUtils.modInverse(jElement, jModulus);
        return new BigInteger(jInverse);
    }

    @Override
    public FactorDecomposition<BigInteger> factorSquareFree(BigInteger element) {
        return factor(element);
    }

    @Override
    public FactorDecomposition<BigInteger> factor(BigInteger element) {
        return FactorDecomposition.of(this, element);
    }

    @Override
    public BigInteger valueOf(BigInteger val) {
        return modulus(val);
    }

    @Override
    public BigInteger valueOf(long val) {
        return valueOf(BigInteger.valueOf(val));
    }

    @Override
    public BigInteger randomElement(RandomGenerator rnd) {
        return RandomUtil.randomInt(modulus, rnd);
    }

    @Override
    public Iterator<BigInteger> iterator() {
        return new JdkIntegersZp.It();
    }

    private final class It implements Iterator<BigInteger> {
        private BigInteger val = BigInteger.ZERO;

        @Override
        public boolean hasNext() {
            return val.compareTo(modulus) < 0;
        }

        @Override
        public BigInteger next() {
            BigInteger r = val;
            val = val.increment();
            return r;
        }
    }

    @Override
    public String toString() {
        return "Z/" + modulus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JdkIntegersZp that = (JdkIntegersZp) o;

        return modulus.equals(that.modulus);
    }

    @Override
    public int hashCode() {
        return modulus.hashCode();
    }
}
