package edu.alibaba.mpc4j.work.db.dynamic.select;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.work.db.dynamic.structure.UpdateMessage;

import java.util.List;

/**
 * @author Feng Han
 * @date 2025/3/10
 */
public interface DynamicDbSelectCircuit {
    /**
     * update the order-by Materialized Table
     * if the operation can not be done, throw an MpcAbortException
     *
     * @param updateMessage update message
     * @return the update message for the upper node in the directed acyclic graph
     */
    List<UpdateMessage> update(UpdateMessage updateMessage, SelectMt selectMt) throws MpcAbortException;
}
