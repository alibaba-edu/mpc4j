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
 * Concerns housing values in suburbs of Boston.
 * <p>
 * n = 506, 1 nominal feature, 12 numeric features, regression.
 * </p>
 * Download from:
 * <p>
 * https://github.com/haifengl/smile/blob/master/shell/src/universal/data/weka/regression/housing.arff
 * </p>
 *
 * @author ray, Weiran Liu
 * @date 2020/11/10
 */
public class BostonHousing {
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
            // CRIM / continuous
            new StructField("CRIM", DataTypes.FloatType),
            // ZN / continuous
            new StructField("ZN", DataTypes.FloatType),
            // INDUS / continuous
            new StructField("INDUS", DataTypes.FloatType),
            // CHAS / nominal [0, 1]
            new StructField("CHAS", DataTypes.ByteType, new NominalScale("0", "1")),
            // NOX / continuous
            new StructField("NOX", DataTypes.FloatType),
            // RM / continuous
            new StructField("RM", DataTypes.FloatType),
            // AGE / continuous
            new StructField("AGE", DataTypes.FloatType),
            // DIS / continuous
            new StructField("DIS", DataTypes.FloatType),
            // RAD / continuous
            new StructField("RAD", DataTypes.FloatType),
            // TAX / continuous
            new StructField("TAX", DataTypes.FloatType),
            // PTRATIO / continuous
            new StructField("PTRATIO", DataTypes.FloatType),
            // B / continuous
            new StructField("B", DataTypes.FloatType),
            // LSTAT / continuous
            new StructField("LSTAT", DataTypes.FloatType),
            // class / continuous
            new StructField("class", DataTypes.FloatType)
        );
        try {
            data = Read.csv(
                DatasetManager.pathPrefix + "/regression/boston_housing/boston_housing.csv",
                DatasetManager.DEFAULT_CSV_FORMAT, schema
            );
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalStateException("Failed to load '" + BostonHousing.class.getSimpleName() + "'");
        }
    }
}
