package edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.ThreePartyOpfPto;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteFnParam;

/**
 * Interface for three-party permutation
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public interface PermuteParty extends ThreePartyOpfPto {
    /**
     * set up the usage of this function
     *
     * @param params the parameters indicating the function and parameters used on one invocation
     */
    long[] setUsage(PermuteFnParam... params);

    /**
     * get rho=sigma◦pai = pai·sigma
     *
     * @param sigma first permutation
     * @param pai   second permutation
     * @return rho=sigma◦pai = pai·sigma
     */
    TripletLongVector[] composePermutation(TripletLongVector pai, TripletLongVector... sigma) throws MpcAbortException;

    /**
     * get rho=sigma◦pai = pai·sigma
     *
     * @param sigma first permutation
     * @param pai   second permutation
     * @return rho=sigma◦pai = pai·sigma
     */
    TripletZ2Vector[] composePermutation(TripletZ2Vector[] pai, TripletZ2Vector[] sigma) throws MpcAbortException;

    /**
     * compute y=(pai^-1)·x
     *
     * @param pai the input permutation
     * @param x   the data to be permuted
     */
    TripletZ2Vector[] applyInvPermutation(TripletZ2Vector[] pai, TripletZ2Vector[] x) throws MpcAbortException;

    /**
     * compute y=(pai^-1)·x
     *
     * @param pai the input permutation
     * @param x   the data to be permuted
     */
    TripletLongVector[] applyInvPermutation(TripletLongVector pai, TripletLongVector... x) throws MpcAbortException;

    /**
     * compute y=(pai^-1)·x
     *
     * @param pai the input permutation
     * @param x   the data to be permuted
     */
    TripletZ2Vector[] applyInvPermutation(TripletLongVector pai, TripletZ2Vector[] x) throws MpcAbortException;
}
