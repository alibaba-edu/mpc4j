package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * ECC interface.
 *
 * @author Weiran Liu
 * @date 2021/05/23
 */
public interface Ecc {
    /**
     * Returns the EC domain parameters.
     *
     * @return the EC domain parameters.
     */
    ECDomainParameters getEcDomainParameters();

    /**
     * Returns the scalar order.
     *
     * @return the scalar order.
     */
    default BigInteger getN() {
        return getEcDomainParameters().getN();
    }

    /**
     * Returns the cofactor.
     *
     * @return the cofactor.
     */
    default BigInteger getCofactor() {
        return getEcDomainParameters().getCurve().getCofactor();
    }

    /**
     * Returns a random scalar.
     *
     * @param secureRandom the random state.
     * @return a random scalar.
     */
    default BigInteger randomZn(SecureRandom secureRandom) {
        return BigIntegerUtils.randomPositive(getN(), secureRandom);
    }

    /**
     * Returns the infinity point.
     *
     * @return the infinity point.
     */
    default ECPoint getInfinity() {
        return getEcDomainParameters().getCurve().getInfinity();
    }

    /**
     * Returns the generator point.
     *
     * @return the generator point.
     */
    default ECPoint getG() {
        return getEcDomainParameters().getG();
    }

    /**
     * Returns a random EC point.
     *
     * @param secureRandom the random state.
     * @return a random EC point.
     */
    ECPoint randomPoint(SecureRandom secureRandom);

    /**
     * Hashes the data to the EC point.
     *
     * @param data the data.
     * @return the hashed EC point.
     */
    ECPoint hashToCurve(byte[] data);

    /**
     * Encodes the EC point.
     *
     * @param p          the EC point p.
     * @param compressed compress encoding or not.
     * @return the encoded point.
     */
    default byte[] encode(ECPoint p, boolean compressed) {
        return p.getEncoded(compressed);
    }

    /**
     * Decodes the EC point.
     *
     * @param encoded the encoded point.
     * @return the decoded point.
     */
    default ECPoint decode(byte[] encoded) {
        return getEcDomainParameters().getCurve().decodePoint(encoded);
    }

    /**
     * Precomputes the EC point for multiplication.
     *
     * @param p the EC point p.
     */
    void precompute(ECPoint p);

    /**
     * Destroys the precomputed EC point.
     *
     * @param p the EC point p.
     */
    void destroyPrecompute(ECPoint p);

    /**
     * Computes r · P.
     *
     * @param p the EC point p.
     * @param r the scalar r.
     * @return r · P.
     */
    ECPoint multiply(ECPoint p, BigInteger r);

    /**
     * Adds two EC points, i.e., p + q.
     *
     * @param p the EC point p.
     * @param q the EC point q.
     * @return p + q.
     */
    default ECPoint add(ECPoint p, ECPoint q) {
        return p.add(q);
    }

    /**
     * Gets the negative of the EC point, i.e., -p.
     *
     * @param p the EC point p.
     * @return -a.
     */
    default ECPoint negate(ECPoint p) {
        return p.negate();
    }

    /**
     * Subtracts two EC points, i.e., p - q.
     *
     * @param p the EC point p.
     * @param q the EC point q.
     * @return p - q.
     */
    default ECPoint subtract(ECPoint p, ECPoint q) {
        return p.subtract(q);
    }

    /**
     * Computes the inner-product of the binary array with the EC point array.
     *
     * @param ps     the EC point array.
     * @param binary the binary array.
     * @return the inner product result.
     */
    default ECPoint innerProduct(ECPoint[] ps, boolean[] binary) {
        assert binary.length > 0 && ps.length > 0;
        assert binary.length == ps.length;
        ECPoint innerProduct = getInfinity();
        for (int index = 0; index < ps.length; index++) {
            if (binary[index]) {
                innerProduct = innerProduct.add(ps[index]);
            }
        }
        return innerProduct;
    }

    /**
     * Computes the inner-product of zp vector and positions.
     *
     * @param ps        the EC point vector.
     * @param positions positions.
     * @return the inner product.
     */
    default ECPoint innerProduct(ECPoint[] ps, int[] positions) {
        ECPoint value = getInfinity();
        for (int position : positions) {
            value = add(value, ps[position]);
        }
        return value;
    }

    /**
     * Gets the EC type.
     *
     * @return the EC type.
     */
    EccFactory.EccType getEccType();
}
