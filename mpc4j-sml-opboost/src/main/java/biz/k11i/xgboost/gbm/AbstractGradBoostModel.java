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

/**
 * Abstract Gradient booster.
 *
 * @author KOMIYA Atsushi, Michal Kurka, Michal Kurka, Honza Sterba, Clement de Groc, Weiran Liu
 * @date 2021/10/08
 */
abstract class AbstractGradBoostModel implements GradBoostModel {
    private static final long serialVersionUID = 4728130049417912562L;
    /**
     * number of classes
     */
    protected int numClass;
    /**
     * number of features
     */
    protected int numFeature;
    /**
     * number of output group
     */
    protected int numOutputGroup;

    @Override
    public void setNumClass(int numClass) {
        this.numClass = numClass;
        this.numOutputGroup = (this.numClass == 0) ? 1 : this.numClass;
    }

    @Override
    public void setNumFeature(int numFeature) {
        this.numFeature = numFeature;
    }

    @Override
    public int getNumFeature() {
        return numFeature;
    }

    @Override
    public int getNumOutputGroup() {
        return numOutputGroup;
    }
}
