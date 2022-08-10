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

/**
 * Gradient boosted DART tree implementation.
 *
 * @author Jarosław Bojar, Michal Kurka, Honza Sterba, Weiran Liu
 * @date 2021/10/08
 */
public class GardBoostDart extends GradBoostTree {
    private static final long serialVersionUID = 4430224981013721834L;
    /**
     * dropped weight
     */
    private float[] weightDrop;

    GardBoostDart() {
        // do nothing
    }

    @Override
    public void loadModel(PredictorConfiguration config, ModelReader reader, boolean pBuffer) throws IOException {
        super.loadModel(config, reader, pBuffer);
        if (modelParam.treeNum != 0) {
            long size = reader.readLong();
            weightDrop = reader.readFloatArray((int)size);
        }
    }

    @Override
    protected float predictGroup(Fvec feat, int groupId, int numTreeLimit) {
        RegTree[] trees = groupTrees[groupId];
        int treeLeft = numTreeLimit == 0 ? trees.length : numTreeLimit;

        float pSum = 0;
        for (int i = 0; i < treeLeft; i++) {
            pSum += weightDrop[i] * trees[i].getLeafValue(feat, 0);
        }

        return pSum;
    }

    public float weight(int tidx) {
        return weightDrop[tidx];
    }

}
