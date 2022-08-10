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
 * Interface of gradient boosting model.
 *
 * @author KOMIYA Atsushi, Michal Kurka, Michal Kurka, Honza Sterba, Clement de Groc, Weiran Liu
 * @date 2021/10/08
 */
public interface GradBoostModel extends Serializable {

    class Factory {
        /**
         * GBDT模型名称
         */
        private static final String NAME_GB_TREE = "gbtree";
        /**
         * 线性模型名称
         */
        private static final String NAME_GB_LINEAR = "gblinear";
        /**
         * 决策树模型名称
         */
        private static final String NAME_DART = "dart";

        /**
         * Creates a gradient booster from given name.
         *
         * @param name name of gradient booster
         * @return created gradient booster
         */
        public static GradBoostModel createGradBooster(String name) {
            if (NAME_GB_TREE.equals(name)) {
                return new GradBoostTree();
            } else if (NAME_GB_LINEAR.equals(name)) {
                return new GardBoostLinear();
            } else if (NAME_DART.equals(name)) {
                return new GardBoostDart();
            }

            throw new IllegalArgumentException(name + " is not supported model.");
        }
    }

    /**
     * 设置分类数量。
     *
     * @param numClass 分类数量。
     */
    void setNumClass(int numClass);

    /**
     * 设置特征数量。
     *
     * @param numFeature 特征数量。
     */
    void setNumFeature(int numFeature);

    /**
     * Loads model from stream.
     *
     * @param config  predictor configuration.
     * @param reader  input stream.
     * @param pBuffer whether the incoming data contains pBuffer.
     * @throws IOException If an I/O error occurs.
     */
    void loadModel(PredictorConfiguration config, ModelReader reader, boolean pBuffer) throws IOException;

    /**
     * Generates predictions for given feature vector.
     *
     * @param featureVector feature vector.
     * @param numTreeLimit  limit the number of trees used in prediction.
     * @return prediction result.
     */
    float[] predict(Fvec featureVector, int numTreeLimit);

    /**
     * Generates a prediction for given feature vector.
     * <p>
     * This method only works when the model outputs single value.
     * </p>
     *
     * @param featureVector feature vector.
     * @param numTreeLimit  limit the number of trees used in prediction.
     * @return prediction result.
     */
    float predictSingle(Fvec featureVector, int numTreeLimit);

    /**
     * Predicts the leaf index of each tree. This is only valid in gbtree predictor.
     *
     * @param featureVector feature vector.
     * @param numTreeLimit  limit the number of trees used in prediction.
     * @return predicted leaf indexes.
     */
    int[] predictLeaf(Fvec featureVector, int numTreeLimit);

    /**
     * Predicts the path to leaf of each tree. This is only valid in gbtree predictor.
     *
     * @param featureVector feature vector.
     * @param numTreeLimit  limit the number of trees used in prediction.
     * @return predicted path to leaves.
     */
    String[] predictLeafPath(Fvec featureVector, int numTreeLimit);

    /**
     * Gets the number of features.
     *
     * @return number of features.
     */
    int getNumFeature();

    /**
     * Gets the number of output groups.
     *
     * @return number of output groups.
     */
    int getNumOutputGroup();
}