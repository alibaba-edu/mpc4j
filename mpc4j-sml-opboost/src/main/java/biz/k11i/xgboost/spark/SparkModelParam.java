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
package biz.k11i.xgboost.spark;

import biz.k11i.xgboost.util.ModelReader;

import java.io.IOException;
import java.io.Serializable;

/**
 * Spark model parameters.
 *
 * @author KOMIYA Atsushi, Weiran Liu
 * @date 2021/10/08
 */
public class SparkModelParam implements Serializable {
    private static final long serialVersionUID = -2414980077804503124L;
    public static final String MODEL_TYPE_CLS = "_cls_";
    public static final String MODEL_TYPE_REG = "_reg_";

    final String modelType;
    final String featureCol;

    final String labelCol;
    final String predictionCol;

    /**
     * classification model only
     */
    final String rawPredictionCol;
    final double[] thresholds;

    public SparkModelParam(String modelType, String featureCol, ModelReader reader) throws IOException {
        this.modelType = modelType;
        this.featureCol = featureCol;
        this.labelCol = reader.readUtf();
        this.predictionCol = reader.readUtf();

        if (MODEL_TYPE_CLS.equals(modelType)) {
            this.rawPredictionCol = reader.readUtf();
            int thresholdLength = reader.readIntBigEndian();
            this.thresholds = thresholdLength > 0 ? reader.readDoubleArrayBigEndian(thresholdLength) : null;

        } else if (MODEL_TYPE_REG.equals(modelType)) {
            this.rawPredictionCol = null;
            this.thresholds = null;

        } else {
            throw new UnsupportedOperationException("Unknown modelType: " + modelType);
        }
    }

    public String getModelType() {
        return modelType;
    }

    public String getFeatureCol() {
        return featureCol;
    }

    public String getLabelCol() {
        return labelCol;
    }

    public String getPredictionCol() {
        return predictionCol;
    }

    public String getRawPredictionCol() {
        return rawPredictionCol;
    }

    public double[] getThresholds() {
        return thresholds;
    }
}
