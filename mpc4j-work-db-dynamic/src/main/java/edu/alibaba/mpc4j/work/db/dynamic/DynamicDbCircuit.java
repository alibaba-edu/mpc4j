package edu.alibaba.mpc4j.work.db.dynamic;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.work.db.dynamic.agg.AggMt;
import edu.alibaba.mpc4j.work.db.dynamic.agg.DynamicDbAggCircuit;
import edu.alibaba.mpc4j.work.db.dynamic.agg.DynamicDbAggCircuitFactory;
import edu.alibaba.mpc4j.work.db.dynamic.group.DynamicDbGroupByCircuit;
import edu.alibaba.mpc4j.work.db.dynamic.group.DynamicDbGroupByCircuitFactory;
import edu.alibaba.mpc4j.work.db.dynamic.group.GroupByMt;
import edu.alibaba.mpc4j.work.db.dynamic.join.pkpk.DynamicDbPkPkJoinCircuit;
import edu.alibaba.mpc4j.work.db.dynamic.join.pkpk.DynamicDbPkPkJoinCircuitFactory;
import edu.alibaba.mpc4j.work.db.dynamic.join.pkpk.PkPkJoinMt;
import edu.alibaba.mpc4j.work.db.dynamic.orderby.DynamicDbOrderByCircuit;
import edu.alibaba.mpc4j.work.db.dynamic.orderby.DynamicDbOrderByCircuitFactory;
import edu.alibaba.mpc4j.work.db.dynamic.orderby.OrderByMt;
import edu.alibaba.mpc4j.work.db.dynamic.select.DynamicDbSelectCircuit;
import edu.alibaba.mpc4j.work.db.dynamic.select.DynamicDbSelectCircuitFactory;
import edu.alibaba.mpc4j.work.db.dynamic.select.SelectMt;
import edu.alibaba.mpc4j.work.db.dynamic.structure.MaterializedTable;
import edu.alibaba.mpc4j.work.db.dynamic.structure.UpdateMessage;

import java.util.List;

/**
 * dynamic db circuit.
 *
 * @author Feng Han
 * @date 2025/3/6
 */
public class DynamicDbCircuit extends AbstractDynamicDbCircuit {
    /**
     * order-by circuit
     */
    protected final DynamicDbCircuitConfig config;
    /**
     * order-by circuit
     */
    protected final DynamicDbOrderByCircuit orderByCircuit;
    /**
     * group-by circuit
     */
    protected final DynamicDbGroupByCircuit groupByCircuit;
    /**
     * join circuit
     */
    protected final DynamicDbPkPkJoinCircuit joinCircuit;
    /**
     * select circuit
     */
    protected final DynamicDbSelectCircuit selectCircuit;
    /**
     * aggregate circuit
     */
    protected final DynamicDbAggCircuit aggCircuit;

    public DynamicDbCircuit(DynamicDbCircuitConfig config, MpcZ2cParty z2cParty) {
        super(config, z2cParty);
        this.config = config;
        orderByCircuit = DynamicDbOrderByCircuitFactory.createCircuit(config.getOrderByCircuitType(), circuit);
        groupByCircuit = DynamicDbGroupByCircuitFactory.createCircuit(config.getGroupByCircuitType(), circuit);
        joinCircuit = DynamicDbPkPkJoinCircuitFactory.createCircuit(config.getJoinCircuitType(), circuit);
        selectCircuit = DynamicDbSelectCircuitFactory.createCircuit(config.getSelectCircuitType(), circuit);
        aggCircuit = DynamicDbAggCircuitFactory.createCircuit(config.getAggCircuitType(), circuit);
    }

    public DynamicDbCircuit(MpcZ2cParty z2cParty) {
        this(new DynamicDbCircuitConfig.Builder().build(), z2cParty);
    }

    /**
     * update the Materialized Table
     * if the operation can not be done, throw an MpcAbortException
     *
     * @param updateMessage     update message
     * @param materializedTable Materialized Table
     * @return the update message for the upper node in the directed acyclic graph
     */
    public List<UpdateMessage> oneTabUpdate(UpdateMessage updateMessage, MaterializedTable materializedTable) throws MpcAbortException {
        return switch (materializedTable.getMaterializedTableType()) {
            case SELECT_MT -> selectCircuit.update(updateMessage, (SelectMt) materializedTable);
            case GROUP_BY_MT -> groupByCircuit.update(updateMessage, (GroupByMt) materializedTable);
            case ORDER_BY_MT -> orderByCircuit.update(updateMessage, (OrderByMt) materializedTable);
            case GLOBAL_AGG_MT -> aggCircuit.update(updateMessage, (AggMt) materializedTable);
            default ->
                throw new MpcAbortException("illegal materialized table type" + materializedTable.getMaterializedTableType());
        };
    }

    /**
     * update the Materialized Table
     * if the operation can not be done, throw an MpcAbortException
     *
     * @param updateMessage     update message
     * @param materializedTable Materialized Table
     * @return the update message for the upper node in the directed acyclic graph
     */
    public List<UpdateMessage> twoTabUpdate(UpdateMessage updateMessage, MaterializedTable materializedTable, boolean updateFromLeft) throws MpcAbortException {
        return switch (materializedTable.getMaterializedTableType()) {
            case PK_PK_JOIN_MT -> joinCircuit.update(updateMessage, (PkPkJoinMt) materializedTable, updateFromLeft);
            default ->
                throw new MpcAbortException("illegal materialized table type" + materializedTable.getMaterializedTableType());
        };
    }
}
