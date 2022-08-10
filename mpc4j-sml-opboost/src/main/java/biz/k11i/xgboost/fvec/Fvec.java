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
package biz.k11i.xgboost.fvec;

import java.io.Serializable;
import java.util.Map;

/**
 * Interface of feature vector.
 *
 * @author KOMIYA Atsushi, Michal Kurka, Weiran Liu
 * @date 2021/10/08
 */
public interface Fvec extends Serializable {
    /**
     * Gets index-th value.
     *
     * @param index index.
     * @return value.
     */
    float featureValue(int index);

    class Transformer {

        private Transformer() {
            // do nothing
        }

        /**
         * Builds FVec from dense vector.
         *
         * @param values         float values.
         * @param treatsZeroAsNa treat zero as N/A if true.
         * @return FVec.
         */
        public static Fvec fromArray(float[] values, boolean treatsZeroAsNa) {
            return new FvecFloatArray(values, treatsZeroAsNa);
        }

        /**
         * Builds FVec from dense vector.
         *
         * @param values         double values.
         * @param treatsZeroAsNa treat zero as N/A if true.
         * @return FVec.
         */
        public static Fvec fromArray(double[] values, boolean treatsZeroAsNa) {
            return new FvecDoubleArray(values, treatsZeroAsNa);
        }

        /**
         * Builds FVec from map.
         *
         * @param map map containing non-zero values.
         * @return FVec.
         */
        public static Fvec fromMap(Map<Integer, ? extends Number> map) {
            return new FvecMap(map);
        }
    }
}