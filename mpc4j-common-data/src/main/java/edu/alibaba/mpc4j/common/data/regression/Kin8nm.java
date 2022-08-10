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
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;
import smile.io.Read;

/**
 * This is data set is concerned with the forward kinematics of an 8 link robot arm. Among the existing variants of
 * this data set we have used the variant 8nm, which is known to be highly non-linear and medium noisy.
 * Original source: DELVE repository of data.
 * <p>
 * Source: collection of regression datasets by Luis Torgo (ltorgo@ncc.up.pt) at
 * http://www.ncc.up.pt/~ltorgo/Regression/DataSets.html
 * </p>
 * <p>
 * n = 8192, 9 numeric features, regression.
 * </p>
 * Download from:
 * <p>
 * https://github.com/haifengl/smile/blob/master/shell/src/universal/data/weka/regression/kin8nm.arff
 * </p>
 *
 * @author Haifeng, Weiran Liu
 * @date 2020/11/11
 */
public class Kin8nm {
    /**
     * 样本集
     */
    public static DataFrame data;
    /**
     * 预测标签：y
     */
    public static Formula formula = Formula.lhs("y");

    static {
        StructType schema = DataTypes.struct(
            // theta1 / continuous
            new StructField("theta1", DataTypes.DoubleType),
            // theta2 / continuous
            new StructField("theta2", DataTypes.DoubleType),
            // theta3 / continuous
            new StructField("theta3", DataTypes.DoubleType),
            // theta4 / continuous
            new StructField("theta4", DataTypes.DoubleType),
            // theta5 / continuous
            new StructField("theta5", DataTypes.DoubleType),
            // theta6 / continuous
            new StructField("theta6", DataTypes.DoubleType),
            // theta7 / continuous
            new StructField("theta7", DataTypes.DoubleType),
            // theta8 / continuous
            new StructField("theta8", DataTypes.DoubleType),
            // y / continuous
            new StructField("y", DataTypes.DoubleType)
        );
        try {
            data = Read.csv(
                DatasetManager.pathPrefix + "/regression/kin8nm/kin8nm.csv",
                DatasetManager.DEFAULT_CSV_FORMAT, schema
            );
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalStateException("Failed to load '" + Kin8nm.class.getSimpleName() + "'");
        }
    }
}
