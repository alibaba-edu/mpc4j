package edu.alibaba.mpc4j.sml.opboost.grad;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.sml.opboost.AbstractOpBoostHost;
import edu.alibaba.mpc4j.sml.smile.base.cart.LeafNode;
import edu.alibaba.mpc4j.sml.smile.base.cart.Node;
import edu.alibaba.mpc4j.sml.smile.base.cart.NominalNode;
import edu.alibaba.mpc4j.sml.smile.base.cart.OrdinalNode;
import edu.alibaba.mpc4j.sml.smile.classification.GradientTreeBoost;
import edu.alibaba.mpc4j.sml.smile.regression.RegressionTree;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.type.StructType;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 分类OpGradBoost主机。
 *
 * @author Weiran Liu
 * @date 2021/09/26
 */
public class ClsOpGradBoostHost extends AbstractOpBoostHost {
    /**
     * 主机配置参数
     */
    private ClsOpGradBoostHostConfig hostConfig;
    /**
     * 训练模型
     */
    private GradientTreeBoost gradientTreeBoost;

    public ClsOpGradBoostHost(Rpc hostRpc, Party... slaveParties) {
        super(hostRpc, slaveParties);
    }

    public GradientTreeBoost fit(Formula formula, DataFrame hostDataFrame, ClsOpGradBoostHostConfig hostConfig)
        throws MpcAbortException {
        setPtoInput(formula, hostDataFrame, hostConfig);
        this.hostConfig = hostConfig;
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        slaveSchemaStep();
        stopWatch.stop();
        long slaveSchemaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, slaveSchemaTime);

        stopWatch.start();
        ldpDataFrameStep();
        stopWatch.stop();
        long ldpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, ldpTime);

        stopWatch.start();
        slaveDataStep();
        stopWatch.stop();
        long slaveDataTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, slaveDataTime);

        stopWatch.start();
        trainModel();
        stopWatch.stop();
        long trainTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 5, trainTime);

        stopWatch.start();
        traverseSplits();
        traverseTreeModel();
        updateSplitStep();
        replaceSplits();
        stopWatch.stop();
        long splitNodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, splitNodeTime);

        logPhaseInfo(PtoState.PTO_END);
        return gradientTreeBoost;
    }

    @Override
    protected void trainModel() {
        gradientTreeBoost = GradientTreeBoost.fit(formula, wholeDataFrame, hostConfig.getSmileProperties());
    }

    @Override
    protected void traverseTreeModel() {
        // 遍历所有树，找到所有需要的切分点
        for (RegressionTree regressionTree : gradientTreeBoost.trees()) {
            Node rootNode = regressionTree.root();
            StructType schema = regressionTree.schema();
            preOrderTraverse(rootNode, schema);
        }
    }

    private void preOrderTraverse(Node node, StructType schema) {
        // 如果节点为空或节点已经是叶子节点，则不需要修改，直接返回
        if (node == null || node instanceof LeafNode) {
            return;
        }
        // 如果节点为枚举类型节点，不需要替换，只需要左右继续遍历
        if (node instanceof NominalNode) {
            NominalNode nominalNode = (NominalNode) node;
            preOrderTraverse(nominalNode.trueChild(), schema);
            preOrderTraverse(nominalNode.falseChild(), schema);
        }
        // 如果节点为数值类型节点，需要把排序值替换为真实切分值，同时左右继续遍历
        if (node instanceof OrdinalNode) {
            OrdinalNode ordinalNode = (OrdinalNode) node;
            int featureIndex = ordinalNode.feature();
            String columnName = schema.fieldName(featureIndex);
            Party slaveParty = columnSlaveMap.get(columnName);
            if (slaveParty != null) {
                // 这意味着此列属于从机，需要替换
                Map<String, Set<Integer>> slaveSplitSetMap = slavesSplitSetMap.get(slaveParty);
                Set<Integer> slaveSplitSet = slaveSplitSetMap.get(columnName);
                slaveSplitSet.add((int) Math.round(ordinalNode.getLeftValue()));
                slaveSplitSet.add((int) Math.round(ordinalNode.getRightValue()));
            }
            // 继续遍历
            preOrderTraverse(ordinalNode.trueChild(), schema);
            preOrderTraverse(ordinalNode.falseChild(), schema);
        }
    }

    @Override
    protected void replaceSplits() {
        // 遍历所有树，找到所有需要的切分点
        for (RegressionTree regressionTree : gradientTreeBoost.trees()) {
            Node rootNode = regressionTree.root();
            StructType schema = regressionTree.schema();
            preOrderReplace(rootNode, schema);
        }
    }

    private void preOrderReplace(Node node, StructType schema) {
        // 如果节点为空或节点已经是叶子节点，则不需要修改，直接返回
        if (node == null || node instanceof LeafNode) {
            return;
        }
        // 如果节点为枚举类型节点，不需要替换，只需要左右继续遍历
        if (node instanceof NominalNode) {
            NominalNode nominalNode = (NominalNode) node;
            preOrderReplace(nominalNode.trueChild(), schema);
            preOrderReplace(nominalNode.falseChild(), schema);
        }
        // 如果节点为数值类型节点，需要把排序值替换为真实切分值，同时左右继续遍历
        if (node instanceof OrdinalNode) {
            OrdinalNode ordinalNode = (OrdinalNode) node;
            int columnIndex = ordinalNode.feature();
            String columnName = schema.fieldName(columnIndex);
            Party slaveParty = columnSlaveMap.get(columnName);
            if (slaveParty != null) {
                // 这意味着此列属于从机，需要替换
                Map<String, Map<Integer, Double>> slaveSplitValueMap = slavesSplitValueMap.get(slaveParty);
                Map<Integer, Double> slaveColumnSplitMap = slaveSplitValueMap.get(columnName);
                double leftValue = slaveColumnSplitMap.get((int) Math.round(ordinalNode.getLeftValue()));
                double rightValue = slaveColumnSplitMap.get((int) Math.round(ordinalNode.getRightValue()));
                ordinalNode.replaceValue(leftValue, rightValue);
            }
            // 继续遍历
            preOrderReplace(ordinalNode.trueChild(), schema);
            preOrderReplace(ordinalNode.falseChild(), schema);
        }
    }
}
