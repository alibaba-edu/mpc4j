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
     * Shift left by padding zero in the end.
     *
     * @param n shift distance, in bits.
     * @return result.
     */
    @Override
    TripletZ2Vector padShiftLeft(int n);

    /**
     * Shift right by reducing number of bits in the bit vector.
     *
     * @param n shift distance, in bits.
     * @return result.
     */
    @Override
    TripletZ2Vector reduceShiftRight(int n);
}
