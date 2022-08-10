/*
 * Original Work: Copyright (c) 2010-2021 Haifeng Li. All rights reserved.
 * Modified Work: Copyright 2021-2022 Weiran Liu.
 *
 * Smile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Smile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Smile.  If not, see <https://www.gnu.org/licenses/>.
 */
package edu.alibaba.mpc4j.common.data.classification;

import edu.alibaba.mpc4j.common.data.DatasetManager;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.measure.NominalScale;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;
import smile.io.Read;

/**
 * Iris Plants Database.
 * <p>
 * n = 150, 4 numeric features, 3-class classification.
 * </p>
 * Download from:
 * <p>
 * https://github.com/haifengl/smile/blob/master/shell/src/universal/data/weka/iris.arff
 * </p>
 *
 * @author Haifeng, Weiran Liu
 * @date 2020/11/11
 */
public class Iris {
    /**
     * 样本集
     */
    public static DataFrame data;
    /**
     * 预测标签：class
     */
    public static Formula formula = Formula.lhs("class");

    static {
        StructType schema = DataTypes.struct(
            // sepallength / continuous
            new StructField("sepallength", DataTypes.FloatType),
            // sepalwidth / continuous
            new StructField("sepalwidth", DataTypes.FloatType),
            // petallength / continuous
            new StructField("petallength", DataTypes.FloatType),
            // petalwidth / continuous
            new StructField("petalwidth", DataTypes.FloatType),
            // class / nominal Iris-setosa, Iris-versicolor, Iris-virginica]
            new StructField(
                "class", DataTypes.ByteType,
                new NominalScale("Iris-setosa", "Iris-versicolor", "Iris-virginica")
            )
        );
        try {
            data = Read.csv(
                DatasetManager.pathPrefix + "/classification/iris/iris.csv",
                DatasetManager.DEFAULT_CSV_FORMAT, schema
            );
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalStateException("Failed to load '" + Iris.class.getSimpleName() + "'");
        }
    }
}
