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
import org.apache.commons.math3.util.Precision;

import java.io.IOException;
import java.io.Serializable;

/**
 * Regression Tree Node.
 *
 * @author Michal Kurka, Weiran Liu
 * @date 2021/10/08
 */
public class RegTreeNode extends AbstractRegTreeNode implements Serializable {
    private static final long serialVersionUID = 4784744702996114477L;
    /**
     * pointer to parent, highest bit is used to indicate whether it's a left child or not.
     */
    final int unsignedParentNode;
    /**
     * pointer to left
     */
    final int leftChild;
    /**
     * pointer to right
     */
    final int rightChild;
    /**
     * split feature index, left split or right split depends on the highest bit
     */
    final int unsignedSplitIndex;
    /**
     * leaf value, only used if the current node is a leaf node, otherwise Float.NaN
     */
    final float leafValue;
    /**
     * split condition, only available if the current node is a internal node, otherwise Float.NaN
     */
    float splitCondition;
    /**
     * default child
     */
    private final int defaultChild;
    /**
     * split feature index without highest indicate bit
     */
    private final int splitIndex;
    /**
     * whether the current node is a leaf node
     */
    final boolean leaf;

    RegTreeNode(ModelReader reader) throws IOException {
        unsignedParentNode = reader.readInt();
        leftChild = reader.readInt();
        rightChild = reader.readInt();
        unsignedSplitIndex = reader.readInt();

        if (isLeaf()) {
            leafValue = reader.readFloat();
            splitCondition = Float.NaN;
        } else {
            splitCondition = reader.readFloat();
            leafValue = Float.NaN;
        }
        defaultChild = cdefault();
        splitIndex = getSplitIndex();
        leaf = isLeaf();
    }

    @Override
    public boolean isLeaf() {
        return leftChild == -1;
    }

    @Override
    public int getSplitIndex() {
        return (int) (unsignedSplitIndex & ((1L << 31) - 1L));
    }

    public int cdefault() {
        return defaultLeft() ? leftChild : rightChild;
    }

    @Override
    public boolean defaultLeft() {
        return (unsignedSplitIndex >>> 31) != 0;
    }

    @Override
    public int next(Fvec featureVector) {
        float value = featureVector.featureValue(splitIndex);
        if (!Precision.equals(value, value, Double.MIN_VALUE)) {
            // is NaN?
            return defaultChild;
        }
        return (value < splitCondition) ? leftChild : rightChild;
    }

    @Override
    public int getParentIndex() {
        return unsignedParentNode;
    }

    @Override
    public int getLeftChildIndex() {
        return leftChild;
    }

    @Override
    public int getRightChildIndex() {
        return rightChild;
    }

    @Override
    public float getSplitCondition() {
        return splitCondition;
    }

    @Override
    public void replaceSplitCondition(float splitCondition) {
        this.splitCondition = splitCondition;
    }

    @Override
    public float getLeafValue() {
        return leafValue;
    }
}
