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
package edu.alibaba.mpc4j.common.data.regression;

import edu.alibaba.mpc4j.common.data.DatasetManager;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.measure.NominalScale;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;
import smile.io.Read;

/**
 * Predicting the age of abalone from physical measurements. The age of abalone is determined by cutting the shell
 * through the cone, staining it, and counting the number of rings through a microscope -- a boring and time-consuming
 * task. Other measurements, which are easier to obtain, are used to predict the age. Further information, such as
 * weather patterns and location (hence food availability) may be required to solve the problem.
 * <p>
 * http://archive.ics.uci.edu/ml/datasets/Abalone
 * </p>
 * <p>
 * n = 4177, 1 nominal feature, 7 numeric features, numeric label, regression or classification.
 * </p>
 * Download from:
 * <p>
 * https://github.com/haifengl/smile/blob/master/shell/src/universal/data/regression/abalone-train.data
 * </p>
 * <p>
 * https://github.com/haifengl/smile/blob/master/shell/src/universal/data/regression/abalone-test.data
 * </p>
 *
 * @author Haifeng, Weiran Liu
 * @date 2020/11/10
 */
public class Abalone {
    /**
     * 样本集
     */
    public static DataFrame train;
    /**
     * 测试集
     */
    public static DataFrame test;
    /**
     * 预测标签为rings
     */
    public static Formula formula = Formula.lhs("rings");

    static {
        StructType schema = DataTypes.struct(
            // Sex / nominal [F, M, I]
            new StructField("sex_F", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("sex_M", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("sex_I", DataTypes.ByteType, new NominalScale("0", "1")),
            // Length / continuous
            new StructField("length", DataTypes.DoubleType),
            // Diameter / continuous
            new StructField("diameter", DataTypes.DoubleType),
            // Height / continuous
            new StructField("height", DataTypes.DoubleType),
            // Whole weight / continuous
            new StructField("whole weight", DataTypes.DoubleType),
            // Shucked weight / continuous
            new StructField("shucked weight", DataTypes.DoubleType),
            // Viscera weight / continuous
            new StructField("viscera weight", DataTypes.DoubleType),
            // Shell weight / continuous
            new StructField("shell weight", DataTypes.DoubleType),
            // Rings / integer
            new StructField("rings", DataTypes.DoubleType)
        );
        try {
            train = Read.csv(
                DatasetManager.pathPrefix +  "/regression/abalone/abalone-train.csv",
                DatasetManager.DEFAULT_CSV_FORMAT, schema
            );
            test = Read.csv(
                DatasetManager.pathPrefix +  "/regression/abalone/abalone-test.csv",
                DatasetManager.DEFAULT_CSV_FORMAT, schema
            );
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalStateException("Failed to load '" + Abalone.class.getSimpleName() + "'");
        }
    }
}
