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
package biz.k11i.xgboost.tree;

import biz.k11i.xgboost.fvec.Fvec;
import biz.k11i.xgboost.util.ModelReader;

import java.io.IOException;
import java.io.Serializable;

/**
 * Regression tree.
 *
 * @author Michal Kurka, Honza Sterba, Weiran Liu
 * @date 2021/10/08
 */
public class RegTreeImpl implements RegTree {
    private static final long serialVersionUID = 5774052681523354958L;
    /**
     * tree nodes
     */
    private RegTreeNode[] regTreeNodes;
    /**
     * tree node states
     */
    private RegTreeNodeState[] states;

    /**
     * Loads model from stream.
     *
     * @param reader input stream.
     * @throws IOException If an I/O error occurs.
     */
    public void loadModel(ModelReader reader) throws IOException {
        ModelParam modelParam = new ModelParam(reader);

        regTreeNodes = new RegTreeNode[modelParam.num_nodes];
        for (int i = 0; i < modelParam.num_nodes; i++) {
            regTreeNodes[i] = new RegTreeNode(reader);
        }

        states = new RegTreeNodeState[modelParam.num_nodes];
        for (int i = 0; i < modelParam.num_nodes; i++) {
            states[i] = new RegTreeNodeState(reader);
        }
    }

    /**
     * Retrieves nodes from root to leaf and returns leaf index.
     *
     * @param featureVector feature vector
     * @return leaf index
     */
    @Override
    public int getLeafIndex(Fvec featureVector) {
        int id = 0;
        RegTreeNode n;
        while (!(n = regTreeNodes[id]).leaf) {
            id = n.next(featureVector);
        }
        return id;
    }

    /**
     * Retrieves nodes from root to leaf and returns path to leaf.
     *
     * @param featureVector feature vector
     * @param stringBuilder output param, will write path path to leaf into this buffer
     */
    @Override
    public void getLeafPath(Fvec featureVector, StringBuilder stringBuilder) {
        int id = 0;
        RegTreeNode n;
        while (!(n = regTreeNodes[id]).leaf) {
            id = n.next(featureVector);
            stringBuilder.append(id == n.leftChild ? "L" : "R");
        }
    }

    /**
     * Retrieves nodes from root to leaf and returns leaf value.
     *
     * @param featureVector feature vector
     * @param rootId        starting root index
     * @return leaf value
     */
    @Override
    public float getLeafValue(Fvec featureVector, int rootId) {
        RegTreeNode n = regTreeNodes[rootId];
        while (!n.leaf) {
            n = regTreeNodes[n.next(featureVector)];
        }

        return n.leafValue;
    }

    @Override
    public RegTreeNode[] getRegTreeNodes() {
        return regTreeNodes;
    }

    @Override
    public RegTreeNodeState[] getStates() {
        return states;
    }

    /**
     * Parameters.
     */
    static class ModelParam implements Serializable {
        private static final long serialVersionUID = -165824520418851150L;
        /**
         * number of start root
         */
        final int num_roots;
        /**
         * total number of nodes
         */
        final int num_nodes;
        /**
         * number of deleted nodes
         */
        final int num_deleted;
        /**
         * maximum depth, this is a statistics of the tree
         */
        final int max_depth;
        /**
         * number of features used for tree construction
         */
        final int num_feature;
        /**
         * leaf vector size, used for vector tree, used to store more than one dimensional information in tree
         */
        final int size_leaf_vector;
        /**
         * reserved part
         */
        final int[] reserved;

        ModelParam(ModelReader reader) throws IOException {
            num_roots = reader.readInt();
            num_nodes = reader.readInt();
            num_deleted = reader.readInt();
            max_depth = reader.readInt();
            num_feature = reader.readInt();

            size_leaf_vector = reader.readInt();
            reserved = reader.readIntArray(31);
        }
    }
}
