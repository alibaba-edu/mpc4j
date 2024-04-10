package edu.alibaba.mpc4j.s3pc.abb3.structure.z2;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * Basic data structure for three-party secret sharing
 *
 * @author Feng Han
 * @date 2023/12/15
 */
public interface TripletZ2Vector extends MpcZ2Vector {

    /**
     * Get the inner bit vector.
     *
     * @return the inner bit vector.
     */
    @Override
    default BitVector getBitVector() {
        throw new RuntimeException("should not invoke this method");
    }

    /**
     * Get the inner bit vectors, there may be multiple vectors in three party sharing
     *
     * @return the inner bit vectors.
     */
    @Override
    BitVector[] getBitVectors();

    /**
     * pad zeros in the front of bits to make the valid bit length = targetBitLength
     *
     * @param targetBitLength the target bit length
     */
    void extendLength(int targetBitLength);

    /**
     * Shift left by padding zero in the end.
     *
     * @param n shift distance, in bits.
     * @return result.
     */
    TripletZ2Vector padShiftLeft(int n);

    /**
     * Inner shift left by fixing number of bits in the bit vector.
     *
     * @param n shift distance, in bits.
     */
    void fixShiftLefti(int n);

    /**
     * Shift right by reducing number of bits in the bit vector.
     *
     * @param n shift distance, in bits.
     * @return result.
     */
    TripletZ2Vector reduceShiftRight(int n);

    /**
     * Inner shift right by reducing number of bits in the bit vector.
     *
     * @param n shift distance, in bits.
     */
    void reduceShiftRighti(int n);

    /**
     * Inner shift right by fixing number of bits in the bit vector.
     *
     * @param n shift distance, in bits.
     */
    void fixShiftRighti(int n);
}
