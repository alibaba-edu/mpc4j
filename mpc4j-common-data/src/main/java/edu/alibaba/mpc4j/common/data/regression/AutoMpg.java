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
 * This dataset was taken from the StatLib library which is maintained at Carnegie Mellon University. The dataset was
 * used in the 1983 American Statistical Association Exposition.
 * <p>
 * n = 398, 3 nominal features, 4 numeric features, multi-class classification.
 * </p>
 * Download from:
 * <p>
 * https://github.com/haifengl/smile/blob/master/shell/src/universal/data/weka/regression/autoMpg.arff
 * </p>
 *
 * @author Haifeng, Weiran Liu
 * @date 2020/11/10
 */
public class AutoMpg {
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
            // cylinders / nominal [8, 4, 6, 3, 5]
            new StructField("cylinders_8", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("cylinders_4", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("cylinders_6", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("cylinders_3", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("cylinders_5", DataTypes.ByteType, new NominalScale("0", "1")),
            // displacement / continuous
            new StructField("displacement", DataTypes.FloatType),
            // horsepower / continuous
            new StructField("horsepower", DataTypes.FloatType),
            // weight / continuous
            new StructField("weight", DataTypes.FloatType),
            // acceleration / continuous
            new StructField("acceleration", DataTypes.FloatType),
            // model / nominal [70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82]
            new StructField("cylinders_70", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("cylinders_71", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("cylinders_72", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("cylinders_73", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("cylinders_74", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("cylinders_75", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("cylinders_76", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("cylinders_77", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("cylinders_78", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("cylinders_79", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("cylinders_80", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("cylinders_81", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("cylinders_82", DataTypes.ByteType, new NominalScale("0", "1")),
            // origin / nominal [1, 3, 2]
            new StructField("origin_1", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("origin_3", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("origin_2", DataTypes.ByteType, new NominalScale("0", "1")),
            // class / float
            new StructField("class", DataTypes.FloatType)
        );
        try {
            data = Read.csv(
                DatasetManager.pathPrefix + "/regression/autompg/autoMpg.csv",
                DatasetManager.DEFAULT_CSV_FORMAT, schema
            );
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalStateException("Failed to load '" + AutoMpg.class.getSimpleName() + "'");
        }
    }
}
