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

import ai.h2o.algos.tree.INode;
import biz.k11i.xgboost.fvec.Fvec;

import java.io.Serializable;

/**
 * Abstract Regression Tree Node.
 *
 * @author Honza Sterba, Weiran Liu
 * @date 2021/10/08
 */
public abstract class AbstractRegTreeNode implements INode<Fvec>, Serializable {
    private static final long serialVersionUID = 7314487558338026230L;

    /**
     * Gets index of node's parent.
     *
     * @return Index of node's parent.
     */
    public abstract int getParentIndex();

    /**
     * Gets index of node's left child node.
     *
     * @return Index of node's left child node.
     */
    @Override
    public abstract int getLeftChildIndex();

    /**
     * Gets index of node's right child node.
     *
     * @return Index of node's right child node.
     */
    @Override
    public abstract int getRightChildIndex();

    /**
     * Gets split condition on the node.
     *
     * @return Split condition on the node, if the node is a split node. Leaf nodes have this value set to NaN.
     */
    public abstract float getSplitCondition();

    /**
     * Replaces split condition on the node.
     *
     * @param splitCondition split condition.
     */
    public abstract void replaceSplitCondition(float splitCondition);

    /**
     * Gets predicted value on the leaf node.
     *
     * @return Predicted value on the leaf node, if the node is leaf. Otherwise NaN.
     */
    @Override
    public abstract float getLeafValue();

    /**
     * Gets if default direction for unrecognized values is the LEFT child.
     *
     * @return True if default direction for unrecognized values is the LEFT child, otherwise false.
     */
    public abstract boolean defaultLeft();

    /**
     * Gets index of domain category used to split on the node.
     *
     * @return Index of domain category used to split on the node.
     */
    @Override
    public abstract int getSplitIndex();
}
