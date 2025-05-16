package edu.alibaba.mpc4j.work.db.dynamic.agg;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.work.db.dynamic.structure.UpdateMessage;

import java.util.List;

/**
 * interface for dynamic db agg circuit
 *
 * @author Feng Han
 * @date 2025/3/10
 */
public interface DynamicDbAggCircuit {
    /**
     * update the agg Materialized Table
     * if the operation can not be done, throw an MpcAbortException
     *
     * @param updateMessage update message
     * @param aggMt         agg Materialized Table
     * @return the update message for the upper node in the directed acyclic graph
     */
    List<UpdateMessage> update(UpdateMessage updateMessage, AggMt aggMt) throws MpcAbortException;
}
