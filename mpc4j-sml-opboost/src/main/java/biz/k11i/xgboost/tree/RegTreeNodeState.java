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

import ai.h2o.algos.tree.INodeStat;
import biz.k11i.xgboost.util.ModelReader;

import java.io.IOException;
import java.io.Serializable;

/**
 * Statistics for node in tree.
 *
 * @author Honza Sterba, Weiran Liu
 * @date 2021/10/08
 */
public class RegTreeNodeState implements INodeStat, Serializable {
    private static final long serialVersionUID = 7781857320427509894L;
    /**
     * sum of gain values
     */
    private final float lossChg;
    /**
     * sum of hessian values
     */
    private final float sumHess;
    /**
     * weight of current node
     */
    private final float baseWeight;
    /**
     * number of child that is leaf node known up to now
     */
    private final int leafCount;

    RegTreeNodeState(ModelReader reader) throws IOException {
        lossChg = reader.readFloat();
        sumHess = reader.readFloat();
        baseWeight = reader.readFloat();
        leafCount = reader.readInt();
    }

    @Override
    public float getWeight() {
        return getCover();
    }

    /**
     * Gets loss chg caused by current split.
     *
     * @return loss chg caused by current split.
     */
    public float getGain() {
        return lossChg;
    }

    /**
     * Gets sum of hessian values.
     *
     * @return sum of hessian values, used to measure coverage of data.
     */
    public float getCover() {
        return sumHess;
    }

    /**
     * Gets weight of current node.
     *
     * @return weight of current node.
     */
    public float getBaseWeight() {
        return baseWeight;
    }

    /**
     * Gets number of child that is leaf node known up to now.
     *
     * @return number of child that is leaf node known up to now.
     */
    public int getLeafCount() {
        return leafCount;
    }
}
