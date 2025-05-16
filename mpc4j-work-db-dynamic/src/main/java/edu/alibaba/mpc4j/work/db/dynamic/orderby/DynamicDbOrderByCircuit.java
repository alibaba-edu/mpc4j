package edu.alibaba.mpc4j.work.db.dynamic.orderby;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.work.db.dynamic.structure.UpdateMessage;

import java.util.List;

/**
 * interface for dynamic db order by circuit
 *
 * @author Feng Han
 * @date 2025/3/6
 */
public interface DynamicDbOrderByCircuit {

    /**
     * update the order-by Materialized Table
     * if the operation can not be done, throw an MpcAbortException
     *
     * @param updateMessage update message
     * @param orderByMt     order-by Materialized Table
     * @return the update message for the upper node in the directed acyclic graph
     */
    List<UpdateMessage> update(UpdateMessage updateMessage, OrderByMt orderByMt) throws MpcAbortException;
}
