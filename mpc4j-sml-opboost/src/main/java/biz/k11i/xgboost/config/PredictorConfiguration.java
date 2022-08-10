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
package biz.k11i.xgboost.config;

import biz.k11i.xgboost.learner.ObjFunction;
import biz.k11i.xgboost.tree.DefaultRegTreeFactory;
import biz.k11i.xgboost.tree.RegTreeFactory;

/**
 * 预测器配置参数。原始代码来自于：
 * <p>
 * https://github.com/h2oai/xgboost-predictor
 * </p>
 *
 * @author Michal Kurka, KOMIYA Atsushi, Weiran Liu
 * @date 2021/10/08
 */
public class PredictorConfiguration {
    /**
     * 目标函数
     */
    private ObjFunction objFunction;
    /**
     * 回归树工厂
     */
    private RegTreeFactory regTreeFactory;

    private PredictorConfiguration() {
        regTreeFactory = DefaultRegTreeFactory.INSTANCE;
    }

    public ObjFunction getObjFunction() {
        return objFunction;
    }

    public RegTreeFactory getRegTreeFactory() {
        return regTreeFactory;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 预测器配置参数构造器。
     */
    public static class Builder implements org.apache.commons.lang3.builder.Builder<PredictorConfiguration> {
        /**
         * 预测器配置参数
         */
        private PredictorConfiguration predictorConfiguration;

        Builder() {
            predictorConfiguration = new PredictorConfiguration();
        }

        public Builder objFunction(ObjFunction objFunction) {
            predictorConfiguration.objFunction = objFunction;
            return this;
        }

        public Builder regTreeFactory(RegTreeFactory regTreeFactory) {
            predictorConfiguration.regTreeFactory = regTreeFactory;
            return this;
        }

        @Override
        public PredictorConfiguration build() {
            PredictorConfiguration result = predictorConfiguration;
            predictorConfiguration = null;
            return result;
        }
    }

    public static final PredictorConfiguration DEFAULT = new PredictorConfiguration();
}
