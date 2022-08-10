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

import java.io.Serializable;

/**
 * Regression tree.
 *
 * @author Michal Kurka, Michal Kurka, Honza Sterba, Weiran Liu
 * @date 2021/10/08
 */
public interface RegTree extends Serializable {

    /**
     * Retrieves nodes from root to leaf and returns leaf index.
     *
     * @param featureVector feature vector.
     * @return leaf index.
     */
    int getLeafIndex(Fvec featureVector);

    /**
     * Retrieves nodes from root to leaf and returns path to leaf.
     *
     * @param featureVector feature vector.
     * @param stringBuilder output param, will write path path to leaf into this buffer.
     */
    void getLeafPath(Fvec featureVector, StringBuilder stringBuilder);

    /**
     * Retrieves nodes from root to leaf and returns leaf value.
     *
     * @param featureVector feature vector.
     * @param rootId        starting root index.
     * @return leaf value.
     */
    float getLeafValue(Fvec featureVector, int rootId);

    /**
     * Gets tree's nodes.
     *
     * @return Tree's nodes.
     */
    AbstractRegTreeNode[] getRegTreeNodes();

    /**
     * Gets tree's nodes states.
     *
     * @return Tree's nodes states.
     */
    RegTreeNodeState[] getStates();

}
