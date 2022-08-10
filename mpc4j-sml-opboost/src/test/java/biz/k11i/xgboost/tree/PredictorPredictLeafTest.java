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
package biz.k11i.xgboost.tree;

import biz.k11i.xgboost.Predictor;
import biz.k11i.xgboost.fvec.Fvec;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;

/**
 * Predictor predict path test.
 *
 * @author Honza Sterba, Weiran Liu
 * @date 2021/10/08
 */
public class PredictorPredictLeafTest {

    @Test
    public void shouldPredictLeafIds() throws IOException {
        Predictor p = new Predictor(getClass().getResourceAsStream("/boosterBytes.bin"));
        float[] input = new float[]{10, 20, 30, 5, 7, 10};
        Fvec vec = Fvec.Transformer.fromArray(input, false);
        // 预测值
        int[] predicts = p.predictLeaf(vec);
        // 期望值
        int[] expects = new int[]{33, 47, 41, 41, 49};
        assertArrayEquals(expects, predicts);
    }

    @Test
    public void shouldPredictLeafPaths() throws IOException {
        Predictor p = new Predictor(getClass().getResourceAsStream("/boosterBytes.bin"));
        float[] input = new float[]{10, 20, 30, 5, 7, 10};
        Fvec vec = Fvec.Transformer.fromArray(input, false);
        // 实际预测路径
        String[] predicts = p.predictLeafPath(vec);
        // 期望预测路径
        String[] expects = new String[]{"LRRLL", "LLRRLL", "LLRRLL", "LLRRLL", "LLRRLL"};
        assertArrayEquals(expects, predicts);
    }

}
