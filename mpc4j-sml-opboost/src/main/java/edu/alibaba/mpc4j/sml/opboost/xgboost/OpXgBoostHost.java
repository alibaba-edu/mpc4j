package edu.alibaba.mpc4j.sml.opboost.xgboost;

import biz.k11i.xgboost.Predictor;
import biz.k11i.xgboost.gbm.GradBoostModel;
import biz.k11i.xgboost.gbm.GradBoostTree;
import biz.k11i.xgboost.tree.AbstractRegTreeNode;
import biz.k11i.xgboost.tree.RegTree;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.sml.opboost.AbstractOpBoostHost;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.measure.NominalScale;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * OpXgBoost主机。
 *
 * @author Weiran Liu
 * @date 2022/6/29
 */
public class OpXgBoostHost extends AbstractOpBoostHost {
    /**
     * 主机配置项
     */
    private OpXgBoostHostConfig hostConfig;
    /**
     * 训练模型
     */
    private Predictor predictor;

    public OpXgBoostHost(Rpc hostRpc, Party... slaveParties) {
        super(hostRpc, slaveParties);
    }

    public Predictor fit(Formula formula, DataFrame hostDataFrame, OpXgBoostHostConfig hostConfig)
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
        // 遍历所有的切分点
        traverseSplits();
        traverseTreeModel();
        updateSplitStep();
        replaceSplits();
        stopWatch.stop();
        long splitNodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, splitNodeTime);

        logPhaseInfo(PtoState.PTO_END);
        return predictor;
    }

    @Override
    protected void trainModel() throws MpcAbortException {
        try {
            DMatrix trainDataMatrix = OpXgBoostUtils.dataFrameToDataMatrix(formula, wholeDataFrame);
            // 构建参数
            XgBoostParams xgBoostParams = hostConfig.getXgBoostParams();
            Map<String, Object> params = xgBoostParams.getParams();
            int treeNum = xgBoostParams.getTreeNum();
            // 训练并存储模型
            Booster booster = XGBoost.train(trainDataMatrix, params, treeNum, new HashMap<>(0), null, null);
            String modelName = encodeTaskId + "_" + extraInfo + ".deprecated";
            booster.saveModel(modelName);
            File modelFile = new File(modelName);
            FileInputStream fileInputStream = new FileInputStream(modelFile);
            predictor = new Predictor(fileInputStream);
            fileInputStream.close();
            // 删除模型
            modelFile.deleteOnExit();
        } catch (XGBoostError | IOException e) {
            e.printStackTrace();
            throw new MpcAbortException("Error when training the model...");
        }
    }

    @Override
    protected void traverseTreeModel() {
        // 遍历所有树，找到所有需要的切分点
        GradBoostModel gradBoostModel = predictor.getBooster();
        assert gradBoostModel instanceof GradBoostTree;
        GradBoostTree gradBoostTree = (GradBoostTree) gradBoostModel;
        RegTree[][] groupedRegTrees = gradBoostTree.getGroupedTrees();
        for (RegTree[] groupedRegTree : groupedRegTrees) {
            for (RegTree regTree : groupedRegTree) {
                AbstractRegTreeNode[] regTreeNodes = regTree.getRegTreeNodes();
                preOrderTraverse(regTreeNodes, 0);
            }
        }
    }

    private void preOrderTraverse(AbstractRegTreeNode[] regTreeNodes, int index) {
        //noinspection StatementWithEmptyBody
        if (regTreeNodes[index].isLeaf()) {
            // 如果已经是叶子节点，则直接返回
        } else if (wholeSchema.field(regTreeNodes[index].getSplitIndex()).measure instanceof NominalScale) {
            // 如果是枚举类型节点，则跳过，继续遍历
            preOrderTraverse(regTreeNodes, regTreeNodes[index].getLeftChildIndex());
            preOrderTraverse(regTreeNodes, regTreeNodes[index].getRightChildIndex());
        } else {
            // 如果是数值类型节点，需要把排序值替换为真实切分值，同时左右继续遍历
            int columnIndex = regTreeNodes[index].getSplitIndex();
            String columnName = wholeSchema.fieldName(columnIndex);
            Party slaveParty = columnSlaveMap.get(columnName);
            if (slaveParty != null) {
                // 这意味着此列属于从机，需要替换
                Map<String, Set<Integer>> slaveSplitSetMap = slavesSplitSetMap.get(slaveParty);
                Set<Integer> slaveSplitSet = slaveSplitSetMap.get(columnName);
                double orderSplit = regTreeNodes[index].getSplitCondition();
                slaveSplitSet.add((int) Math.floor(orderSplit));
                slaveSplitSet.add((int) Math.ceil(orderSplit));
            }
            // 继续遍历
            preOrderTraverse(regTreeNodes, regTreeNodes[index].getLeftChildIndex());
            preOrderTraverse(regTreeNodes, regTreeNodes[index].getRightChildIndex());
        }
    }

    @Override
    protected void replaceSplits() {
        // 遍历所有树，找到所有需要的切分点
        GradBoostModel gradBoostModel = predictor.getBooster();
        assert gradBoostModel instanceof GradBoostTree;
        GradBoostTree gradBoostTree = (GradBoostTree) gradBoostModel;
        RegTree[][] groupedRegTrees = gradBoostTree.getGroupedTrees();
        for (RegTree[] groupedRegTree : groupedRegTrees) {
            for (RegTree regTree : groupedRegTree) {
                AbstractRegTreeNode[] regTreeNodes = regTree.getRegTreeNodes();
                preOrderReplace(regTreeNodes, 0);
            }
        }
    }

    private void preOrderReplace(AbstractRegTreeNode[] regTreeNodes, int index) {
        //noinspection StatementWithEmptyBody
        if (regTreeNodes[index].isLeaf()) {
            // 如果已经是叶子节点，则直接返回
        } else if (wholeSchema.field(regTreeNodes[index].getSplitIndex()).measure instanceof NominalScale) {
            // 如果是枚举类型节点，则跳过，继续遍历
            preOrderReplace(regTreeNodes, regTreeNodes[index].getLeftChildIndex());
            preOrderReplace(regTreeNodes, regTreeNodes[index].getRightChildIndex());
        } else {
            // 如果节点为数值类型节点，需要把排序值替换为真实切分值，同时左右继续遍历
            int columnIndex = regTreeNodes[index].getSplitIndex();
            String columnName = wholeSchema.fieldName(columnIndex);
            Party slaveParty = columnSlaveMap.get(columnName);
            if (slaveParty != null) {
                // 这意味着此列属于从机，需要替换
                Map<String, Map<Integer, Double>> slaveSplitValueMap = slavesSplitValueMap.get(slaveParty);
                Map<Integer, Double> slaveColumnSplitMap = slaveSplitValueMap.get(columnName);
                double orderSplit = regTreeNodes[index].getSplitCondition();
                double lowerValue = slaveColumnSplitMap.get((int) Math.floor(orderSplit));
                double upperValue = slaveColumnSplitMap.get((int) Math.ceil(orderSplit));
                float splitValue = (float) (lowerValue + upperValue) / 2;
                regTreeNodes[index].replaceSplitCondition(splitValue);
            }
            // 继续遍历
            preOrderReplace(regTreeNodes, regTreeNodes[index].getLeftChildIndex());
            preOrderReplace(regTreeNodes, regTreeNodes[index].getRightChildIndex());
        }
    }
}
