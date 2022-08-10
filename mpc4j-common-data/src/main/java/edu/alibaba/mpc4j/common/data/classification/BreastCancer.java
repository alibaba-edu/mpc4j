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
 * BreastCancer dataset (https://www.kaggle.com/uciml/breast-cancer-wisconsin-data),
 * <p>
 * Download from:
 * <p>
 * https://github.com/haifengl/smile/blob/master/shell/src/universal/data/classification/breastcancer.csv
 * </p>
 *
 * @author Haifeng, Weiran Liu
 * @date 2020/11/10
 */
public class BreastCancer {
    /**
     * 样本集
     */
    public static DataFrame data;
    /**
     * 预测标签：diagnosis
     */
    public static Formula formula = Formula.lhs("diagnosis");

    static {
        StructType schema = DataTypes.struct(
            // diagnosis / nominal [M, B]
            new StructField("diagnosis", DataTypes.ByteType, new NominalScale("M", "B")),
            new StructField("radius_mean", DataTypes.DoubleType),
            new StructField("texture_mean", DataTypes.DoubleType),
            new StructField("perimeter_mean", DataTypes.DoubleType),
            new StructField("area_mean", DataTypes.DoubleType),
            new StructField("smoothness_mean", DataTypes.DoubleType),
            new StructField("compactness_mean", DataTypes.DoubleType),
            new StructField("concavity_mean", DataTypes.DoubleType),
            new StructField("concave points_mean", DataTypes.DoubleType),
            new StructField("symmetry_mean", DataTypes.DoubleType),
            new StructField("fractal_dimension_mean", DataTypes.DoubleType),
            new StructField("radius_se", DataTypes.DoubleType),
            new StructField("texture_se", DataTypes.DoubleType),
            new StructField("perimeter_se", DataTypes.DoubleType),
            new StructField("area_se", DataTypes.DoubleType),
            new StructField("smoothness_se", DataTypes.DoubleType),
            new StructField("compactness_se", DataTypes.DoubleType),
            new StructField("concavity_se", DataTypes.DoubleType),
            new StructField("concave points_se", DataTypes.DoubleType),
            new StructField("symmetry_se", DataTypes.DoubleType),
            new StructField("fractal_dimension_se", DataTypes.DoubleType),
            new StructField("radius_worst", DataTypes.DoubleType),
            new StructField("texture_worst", DataTypes.DoubleType),
            new StructField("perimeter_worst", DataTypes.DoubleType),
            new StructField("area_worst", DataTypes.DoubleType),
            new StructField("smoothness_worst", DataTypes.DoubleType),
            new StructField("compactness_worst", DataTypes.DoubleType),
            new StructField("concavity_worst", DataTypes.DoubleType),
            new StructField("concave points_worst", DataTypes.DoubleType),
            new StructField("symmetry_worst", DataTypes.DoubleType),
            new StructField("fractal_dimension_worst", DataTypes.DoubleType)
        );
        try {
            data = Read.csv(
                DatasetManager.pathPrefix + "/classification/breast_cancer/breastcancer.csv",
                DatasetManager.DEFAULT_CSV_FORMAT, schema
            );
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalStateException("Failed to load '" + BreastCancer.class.getSimpleName() + "'");
        }
    }
}
