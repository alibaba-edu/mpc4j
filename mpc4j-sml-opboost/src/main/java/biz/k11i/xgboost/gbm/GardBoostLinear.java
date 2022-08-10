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
import biz.k11i.xgboost.util.ModelReader;

import java.io.IOException;
import java.io.Serializable;

/**
 * Linear booster implementation.
 *
 * @author Michal Kurka, Michal Kurka, Honza Sterba, Clement de Groc, Weiran Liu
 * @date 2021/10/08
 */
public class GardBoostLinear extends AbstractGradBoostModel {
    private static final long serialVersionUID = -2872708415351829275L;
    /**
     * weights
     */
    private float[] weights;

    @Override
    public void loadModel(PredictorConfiguration config, ModelReader reader, boolean pBuffer) throws IOException {
        // ignore the parsed model parameter
        new ModelParam(reader);
        long len = reader.readLong();
        if (len == 0) {
            weights = new float[(numFeature + 1) * numOutputGroup];
        } else {
            weights = reader.readFloatArray((int)len);
        }
    }

    @Override
    public float[] predict(Fvec featureVector, int numTreeLimit) {
        float[] preds = new float[numOutputGroup];
        for (int gid = 0; gid < numOutputGroup; ++gid) {
            preds[gid] = predictGroup(featureVector, gid);
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
        return predictGroup(featureVector, 0);
    }

    /**
     * predict for the given group.
     *
     * @param featureVector feature vector.
     * @param groupId       group ID.
     * @return predict result.
     */
    private float predictGroup(Fvec featureVector, int groupId) {
        float psum = bias(groupId);
        float featValue;
        for (int fid = 0; fid < numFeature; ++fid) {
            featValue = featureVector.featureValue(fid);
            if (!Float.isNaN(featValue)) {
                psum += featValue * weight(fid, groupId);
            }
        }
        return psum;
    }

    @Override
    public int[] predictLeaf(Fvec featureVector, int numTreeLimit) {
        throw new UnsupportedOperationException("gblinear does not support predict leaf index");
    }

    @Override
    public String[] predictLeafPath(Fvec featureVector, int numTreeLimit) {
        throw new UnsupportedOperationException("gblinear does not support predict leaf path");
    }

    private float weight(int featureId, int groupId) {
        return weights[(featureId * numOutputGroup) + groupId];
    }

    private float bias(int groupId) {
        return weights[(numFeature * numOutputGroup) + groupId];
    }

    private static class ModelParam implements Serializable {
        private static final long serialVersionUID = 8807591557466884703L;
        /**
         * reserved space
         */
        final int[] reserved;

        ModelParam(ModelReader reader) throws IOException {
            // num_feature deprecated
            reader.readUnsignedInt();
            // num_output_group deprecated
            reader.readInt();
            reserved = reader.readIntArray(32);
        }
    }
}
