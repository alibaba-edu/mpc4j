package edu.alibaba.mpc4j.s3pc.abb3.basic.conversion;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.ThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvOperations.ConvOp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;

/**
 * Interface for three-party type conversion
 *
 * @author Feng Han
 * @date 2024/01/17
 */
public interface ConvParty extends ThreePartyPto {
    /**
     * inits the protocol.
     */
    void init();

    /**
     * get the required number of tuples according to operation, the first is z2 tuples, the second is long tuples
     *
     * @param op           the required operation
     * @param inputDataNum size of input data in each dimension
     * @param dataDim      input data dimension
     * @param bitLen       required bit length or bit index
     */
    long[] getTupleNum(ConvOp op, int inputDataNum, int dataDim, int bitLen);

    /**
     * convert arithmetic sharing into binary sharing
     *
     * @param data   data to be converted
     * @param bitNum the valid bit length (how many bits should be appeared in the result)
     * @throws MpcAbortException if the protocol is abort.
     */
    TripletZ2Vector[] a2b(MpcLongVector data, int bitNum) throws MpcAbortException;

    /**
     * convert arithmetic sharing into binary sharing
     *
     * @param data   data to be converted
     * @param bitNum the valid bit length (how many bits should be appeared in the result)
     * @throws MpcAbortException if the protocol is abort.
     */
    TripletZ2Vector[][] a2b(MpcLongVector[] data, int bitNum) throws MpcAbortException;

    /**
     * convert binary sharing into arithmetic sharing
     *
     * @param data data to be converted
     * @throws MpcAbortException if the protocol is abort.
     */
    TripletLongVector b2a(MpcZ2Vector[] data) throws MpcAbortException;

    /**
     * convert binary sharing into arithmetic sharing
     *
     * @param data data to be converted
     * @throws MpcAbortException if the protocol is abort.
     */
    TripletLongVector[] b2a(MpcZ2Vector[][] data) throws MpcAbortException;

    /**
     * convert one-bit binary sharing into arithmetic sharing
     *
     * @param data data to be converted
     * @throws MpcAbortException if the protocol is abort.
     */
    TripletLongVector bit2a(MpcZ2Vector data) throws MpcAbortException;

    /**
     * convert one-bit binary sharing into arithmetic sharing
     *
     * @param data data to be converted
     * @throws MpcAbortException if the protocol is abort.
     */
    TripletLongVector[] bit2a(MpcZ2Vector[] data) throws MpcAbortException;

    /**
     * compute [a]^A · [b]^B = [ab]^A
     *
     * @param a arithmetic sharing
     * @param b binary sharing
     * @throws MpcAbortException if the protocol is abort.
     */
    TripletLongVector aMulB(MpcLongVector a, MpcZ2Vector b) throws MpcAbortException;

    /**
     * compute [a]^A · [b]^B = [ab]^A
     *
     * @param a arithmetic sharing
     * @param b binary sharing
     * @throws MpcAbortException if the protocol is abort.
     */
    TripletLongVector[] aMulB(MpcLongVector[] a, MpcZ2Vector[] b) throws MpcAbortException;

    /**
     * get the binary representation of a[index], index = 0 for the highest sign bit, and index = 63 for the lowest bit of long value
     *
     * @param a arithmetic sharing
     * @param index which bit should be extracted
     * @throws MpcAbortException if the protocol is abort.
     */
    TripletZ2Vector bitExtraction(MpcLongVector a, int index) throws MpcAbortException;

    /**
     * get the binary representation of a[index], index = 0 for the highest sign bit, and index = 63 for the lowest bit of long value
     *
     * @param a arithmetic sharing
     * @param index which bit should be extracted
     * @throws MpcAbortException if the protocol is abort.
     */
    TripletZ2Vector[] bitExtraction(MpcLongVector[] a, int index) throws MpcAbortException;
}
