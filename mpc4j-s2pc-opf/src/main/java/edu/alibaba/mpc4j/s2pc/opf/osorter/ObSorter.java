package edu.alibaba.mpc4j.s2pc.opf.osorter;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.util.Arrays;

/**
 * @author Feng Han
 * @date 2024/9/26
 */
public interface ObSorter extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Sorts in the specified order.
     *
     * @param xiArray         xi array, in column form.
     * @param needPermutation whether the permutation is needed or not
     * @param needStable      whether stable sorting or not
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector[] unSignSort(SquareZ2Vector[] xiArray, boolean needPermutation, boolean needStable) throws MpcAbortException;

    /**
     * Sorts in the specified order. the xiArray and payloads will be in-place changed.
     *
     * @param xiArray         xi array, in column form.
     * @param payloads        payload
     * @param needPermutation whether the permutation is needed or not
     * @param needStable      whether stable sorting or not
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector[] unSignSort(SquareZ2Vector[] xiArray, SquareZ2Vector[] payloads, boolean needPermutation, boolean needStable) throws MpcAbortException;

    /**
     * Sorts in the specified order. the xiArray and payloads will be in-place changed.
     *
     * @param xiArray         xi array, in column form.
     * @param payloads        payload
     * @param needPermutation whether the permutation is needed or not
     * @param needStable      whether stable sorting or not
     * @throws MpcAbortException the protocol failure aborts.
     */
    default SquareZ2Vector[] unSignSort(SquareZ2Vector[] xiArray, SquareZ2Vector[][] payloads, boolean needPermutation, boolean needStable) throws MpcAbortException{
        if(payloads != null && payloads.length > 0){
            SquareZ2Vector[] flatPayload = Arrays.stream(payloads).flatMap(Arrays::stream).toArray(SquareZ2Vector[]::new);
            SquareZ2Vector[] perm = unSignSort(xiArray, flatPayload, needPermutation, needStable);
            for(int i = 0, start = 0; i < payloads.length; i++){
                payloads[i] = Arrays.copyOfRange(flatPayload, start, payloads[i].length + start);
                start += payloads[i].length;
            }
            return perm;
        }else{
            return unSignSort(xiArray, needPermutation, needStable);
        }
    }
}