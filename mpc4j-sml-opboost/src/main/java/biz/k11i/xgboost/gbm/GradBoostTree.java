/*
 * Original Work Copyright 2018 H2O.ai.
 * Modified by Weiran Liu. Adjust the code based on Alibaba Java Code Guidelines.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package biz.k11i.xgboost.gbm;

import biz.k11i.xgboost.config.PredictorConfiguration;
import biz.k11i.xgboost.fvec.Fvec;
import biz.k11i.xgboost.tree.RegTree;
import biz.k11i.xgboost.util.ModelReader;

import java.io.IOException;
import java.io.Serializable;

/**
 * Gradient boosted tree implementation.
 *
 * @author KOMIYA Atsushi, Michal Kurka, Michal Kurka, Honza Sterba, Weiran Liu
 * @date 2021/10/08
 */
public class GradBoostTree extends AbstractGradBoostModel {
    private static final long serialVersionUID = -1800527777515088905L;
    /**
     * model parameter
     */
    protected ModelParam modelParam;
    /**
     * regression trees
     */
    protected RegTree[] trees;
    /**
     * group regression trees
     */
    protected RegTree[][] groupTrees;

    @Override
    public void loadModel(PredictorConfiguration config, ModelReader reader, boolean pBuffer) throws IOException {
        // load model parameter
        modelParam = new ModelParam(reader);
        // load regression trees
        trees = new RegTree[modelParam.treeNum];
        for (int i = 0; i < modelParam.treeNum; i++) {
            trees[i] = config.getRegTreeFactory().loadTree(reader);
        }
        // load tree information
        int[] treeInfo = modelParam.treeNum > 0 ? reader.readIntArray(modelParam.treeNum) : new int[0];

        if (modelParam.pBufferNum != 0 && pBuffer) {
            reader.skip(4 * predBufferSize());
            reader.skip(4 * predBufferSize());
        }

        groupTrees = new RegTree[numOutputGroup][];
        for (int i = 0; i < numOutputGroup; i++) {
            int treeCount = 0;
            for (int value : treeInfo) {
                if (value == i) {
                    treeCount++;
                }
            }

            groupTrees[i] = new RegTree[treeCount];
            treeCount = 0;

            for (int j = 0; j < treeInfo.length; j++) {
                if (treeInfo[j] == i) {
                    groupTrees[i][treeCount++] = trees[j];
                }
            }
        }
    }

    @Override
    public float[] predict(Fvec featureVector, int numTreeLimit) {
        float[] preds = new float[numOutputGroup];
        for (int gid = 0; gid < numOutputGroup; gid++) {
            preds[gid] = predictGroup(featureVector, gid, numTreeLimit);
        }
        return preds;
    }

    @Override
    public float predictSingle(Fvec featureVector, int numTreeLimit) {
        if (numOutputGroup != 1) {
            throw new IllegalStateException(
                "Can't invoke predictSingle() because this model outputs multiple values: "
                    + numOutputGroup);
        }
        return predictGroup(featureVector, 0, numTreeLimit);
    }

    protected float predictGroup(Fvec featureVector, int groupId, int numTreeLimit) {
        RegTree[] trees = groupTrees[groupId];
        int treeLeft = numTreeLimit == 0 ? trees.length : numTreeLimit;

        float pSum = 0;
        for (int i = 0; i < treeLeft; i++) {
            pSum += trees[i].getLeafValue(featureVector, 0);
        }

        return pSum;
    }

    @Override
    public int[] predictLeaf(Fvec featureVector, int numTreeLimit) {
        int treeLeft = numTreeLimit == 0 ? trees.length : numTreeLimit;
        int[] leafIndex = new int[treeLeft];
        for (int i = 0; i < treeLeft; i++) {
            leafIndex[i] = trees[i].getLeafIndex(featureVector);
        }
        return leafIndex;
    }

    @Override
    public String[] predictLeafPath(Fvec featureVector, int numTreeLimit) {
        int treeLeft = numTreeLimit == 0 ? trees.length : numTreeLimit;
        String[] leafPath = new String[treeLeft];
        StringBuilder sb = new StringBuilder(64);
        for (int i = 0; i < treeLeft; i++) {
            trees[i].getLeafPath(featureVector, sb);
            leafPath[i] = sb.toString();
            sb.setLength(0);
        }
        return leafPath;
    }

    private long predBufferSize() {
        return numOutputGroup * modelParam.pBufferNum * (modelParam.leafVectorSize + 1);
    }

    static class ModelParam implements Serializable {
        private static final long serialVersionUID = -5034090323764074216L;
        /**
         * number of trees
         */
        final int treeNum;
        /**
         * number of root: default 0, means single tree
         */
        final int rootNum;
        /**
         * size of prediction buffer allocated used for buffering
         */
        final long pBufferNum;
        /**
         * size of leaf vector needed in tree
         */
        final int leafVectorSize;
        /**
         * reserved space
         */
        final int[] reserved;

        ModelParam(ModelReader reader) throws IOException {
            treeNum = reader.readInt();
            rootNum = reader.readInt();
            // num_feature deprecated
            reader.readInt();
            // read padding
            reader.readInt();
            pBufferNum = reader.readLong();
            // num_output_group not used anymore
            reader.readInt();
            leafVectorSize = reader.readInt();
            reserved = reader.readIntArray(31);
            // read padding
            reader.readInt();
        }
    }

    /**
     * Gets grouped trees.
     *
     * @return A two-dim array, with trees grouped into classes.
     */
    public RegTree[][] getGroupedTrees() {
        return groupTrees;
    }
}
