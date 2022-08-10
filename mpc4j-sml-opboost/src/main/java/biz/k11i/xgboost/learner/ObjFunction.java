/*
 * Original Work Copyright 2018 H2O.ai.
 * Modified Work Copyright 2021 Weiran Liu.
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
package biz.k11i.xgboost.learner;

import java.io.Serializable;

/**
 * Objective function implementations.
 *
 * @author Honza Sterba, Weiran Liu
 * @date 2021/10/08
 */
public class ObjFunction implements Serializable {
    private static final long serialVersionUID = 5506101120411849190L;

    /**
     * Transforms prediction values.
     *
     * @param preds prediction
     * @return transformed values
     */
    public float[] predTransform(float[] preds) {
        // do nothing
        return preds;
    }

    /**
     * Transforms a prediction value.
     *
     * @param pred prediction
     * @return transformed value
     */
    public float predTransform(float pred) {
        // do nothing
        return pred;
    }

    public float probToMargin(float prob) {
        // do nothing
        return prob;
    }

    /**
     * Regression.
     */
    static class RegObjFunction extends ObjFunction {
        private static final long serialVersionUID = -5634586375235472048L;

        @Override
        public float[] predTransform(float[] preds) {
            if (preds.length != 1) {
                throw new IllegalStateException(
                    "Regression problem is supposed to have just a single predicted value, got " + preds.length
                        + " instead."
                );
            }
            preds[0] = (float) Math.exp(preds[0]);
            return preds;
        }

        @Override
        public float predTransform(float pred) {
            return (float) Math.exp(pred);
        }

        @Override
        public float probToMargin(float prob) {
            return (float) Math.log(prob);
        }
    }

    /**
     * Logistic regression.
     */
    static class RegLossObjLogistic extends ObjFunction {
        private static final long serialVersionUID = -1491115945988157708L;

        @Override
        public float[] predTransform(float[] preds) {
            for (int i = 0; i < preds.length; i++) {
                preds[i] = sigmoid(preds[i]);
            }
            return preds;
        }

        @Override
        public float predTransform(float pred) {
            return sigmoid(pred);
        }

        float sigmoid(float x) {
            return (1f / (1f + (float) Math.exp(-x)));
        }

        @Override
        public float probToMargin(float prob) {
            return (float) -Math.log(1.0f / prob - 1.0f);
        }
    }

    /**
     * Multiclass classification.
     */
    static class SoftmaxMultiClassObjClassify extends ObjFunction {
        private static final long serialVersionUID = 5566066195239633690L;

        @Override
        public float[] predTransform(float[] preds) {
            int maxIndex = 0;
            float max = preds[0];
            for (int i = 1; i < preds.length; i++) {
                if (max < preds[i]) {
                    maxIndex = i;
                    max = preds[i];
                }
            }

            return new float[]{maxIndex};
        }

        @Override
        public float predTransform(float pred) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Multiclass classification (predicted probability).
     */
    static class SoftmaxMultiClassObjProb extends ObjFunction {
        private static final long serialVersionUID = -5245726657318525029L;

        @Override
        public float[] predTransform(float[] preds) {
            float max = preds[0];
            for (int i = 1; i < preds.length; i++) {
                max = Math.max(preds[i], max);
            }

            double sum = 0;
            for (int i = 0; i < preds.length; i++) {
                preds[i] = exp(preds[i] - max);
                sum += preds[i];
            }

            for (int i = 0; i < preds.length; i++) {
                preds[i] /= (float) sum;
            }

            return preds;
        }

        @Override
        public float predTransform(float pred) {
            throw new UnsupportedOperationException();
        }

        float exp(float x) {
            return (float) Math.exp(x);
        }
    }
}
