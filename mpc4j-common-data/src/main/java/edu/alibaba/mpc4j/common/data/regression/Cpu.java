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
 * Numeric prediction using instance-based learning with encoding length selection.
 * <p>
 * n= 309, 6 numeric features, classification.
 * </p>
 * Download from:
 * <p>
 * https://github.com/haifengl/smile/blob/master/shell/src/universal/data/weka/cpu.arff
 * </p>
 *
 * @author Haifeng, Weiran Liu
 * @date 2020/11/10
 */
public class Cpu {
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
            // MYCT / continuous
            new StructField("MYCT", DataTypes.FloatType),
            // MMIN / continuous
            new StructField("MMIN", DataTypes.FloatType),
            // MMAX / continuous
            new StructField("MMAX", DataTypes.FloatType),
            // CACH / continuous
            new StructField("CACH", DataTypes.FloatType),
            // CHMIN / continuous
            new StructField("CHMIN", DataTypes.FloatType),
            // CHMAX/ continuous
            new StructField("CHMAX", DataTypes.FloatType),
            // class / continuous
            new StructField("class", DataTypes.FloatType)
        );
        try {
            data = Read.csv(
                DatasetManager.pathPrefix + "/regression/cpu/cpu.csv",
                DatasetManager.DEFAULT_CSV_FORMAT, schema
            );
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalStateException("Failed to load '" + Cpu.class.getSimpleName() + "'");
        }
    }
}
