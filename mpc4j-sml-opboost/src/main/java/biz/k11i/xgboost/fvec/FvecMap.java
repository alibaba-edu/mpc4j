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

import java.util.Map;

/**
 * Feature Vector Map.
 *
 * @author KOMIYA Atsushi, Michal Kurka, Weiran Liu
 * @date 2021/10/08
 */
class FvecMap implements Fvec {
    private static final long serialVersionUID = 4538474534313444423L;
    /**
     * feature (index, value)-map
     */
    private final Map<Integer, ? extends Number> values;

    FvecMap(Map<Integer, ? extends Number> values) {
        this.values = values;
    }

    @Override
    public float featureValue(int index) {
        Number number = values.get(index);
        if (number == null) {
            return Float.NaN;
        }

        return number.floatValue();
    }
}
