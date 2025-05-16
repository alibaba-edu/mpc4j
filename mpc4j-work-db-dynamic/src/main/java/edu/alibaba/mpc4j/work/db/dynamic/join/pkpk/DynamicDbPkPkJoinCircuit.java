package edu.alibaba.mpc4j.work.db.dynamic.join.pkpk;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.work.db.dynamic.structure.UpdateMessage;

import java.util.List;

/**
 * @author Feng Han
 * @date 2025/3/7
 */
public interface DynamicDbPkPkJoinCircuit {
    /**
     * update the order-by Materialized Table
     * if the operation can not be done, throw an MpcAbortException
     *
     * @param updateMessage  update message
     * @param orderByMt      order-by Materialized Table
     * @param updateFromLeft update from left table
     * @return the update message for the upper node in the directed acyclic graph
     */
    List<UpdateMessage> update(UpdateMessage updateMessage, PkPkJoinMt orderByMt, boolean updateFromLeft) throws MpcAbortException;
}
