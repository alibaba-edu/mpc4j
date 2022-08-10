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

import biz.k11i.xgboost.learner.ObjFunction.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Objective function manager.
 *
 * @author KOMIYA Atsushi, Michal Kurka, Stanislav Levental, jsleight, Clement de Groc, Weiran Liu
 * @date 2021/10/08
 */
public class ObjFunctionManager {
    /**
     * Objective function map
     */
    private static final Map<String, ObjFunction> FUNCTIONS = new HashMap<>();

    static {
        register("rank:pairwise", new ObjFunction());
        register("rank:ndcg", new ObjFunction());
        register("binary:logistic", new RegLossObjLogistic());
        register("reg:logistic", new RegLossObjLogistic());
        register("binary:logitraw", new ObjFunction());
        register("multi:softmax", new SoftmaxMultiClassObjClassify());
        register("multi:softprob", new SoftmaxMultiClassObjProb());
        register("reg:linear", new ObjFunction());
        register("reg:squarederror", new ObjFunction());
        register("reg:gamma", new RegObjFunction());
        register("reg:tweedie", new RegObjFunction());
        register("count:poisson", new RegObjFunction());
    }

    private ObjFunctionManager() {
        // empty
    }

    /**
     * Gets {@link ObjFunction} from given name.
     *
     * @param name name of objective function
     * @return objective function
     */
    public static ObjFunction fromName(String name) {
        ObjFunction result = FUNCTIONS.get(name);
        if (result == null) {
            throw new IllegalArgumentException(name + " is not supported objective function.");
        }
        return result;
    }

    /**
     * Register an {@link ObjFunction} for a given name.
     *
     * @param name        name of objective function
     * @param objFunction objective function
     */
    private static void register(String name, ObjFunction objFunction) {
        FUNCTIONS.put(name, objFunction);
    }
}
