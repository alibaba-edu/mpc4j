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
 * The original pendigits (Pen-Based Recognition of Handwritten Digits) dataset from UCI machine learning repository
 * is a multiclass classification dataset having 16 integer attributes and 10 classes (0 … 9). The digit database is
 * created by collecting 250 samples from 44 writers. The samples written by 30 writers are used for training,
 * cross-validation and writer dependent testing, and the digits written by the other 14 are used for writer
 * independent testing.
 * <p>
 * In this dataset, all classes have equal frequencies.
 * So the number of objects in one class (corresponding to the digit “0”) is reduced by a factor of 10%.
 * </p>
 * Download from:
 * <p>
 * https://github.com/haifengl/smile/blob/master/shell/src/universal/data/classification/pendigits.txt
 * </p>
 *
 * @author Haifeng, Weiran Liu
 * @date 2020/11/11
 */
public class PenDigits {
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
            // V1 / continuous
            new StructField("V1", DataTypes.DoubleType),
            // V2 / continuous
            new StructField("V2", DataTypes.DoubleType),
            // V3 / continuous
            new StructField("V3", DataTypes.DoubleType),
            // V4 / continuous
            new StructField("V4", DataTypes.DoubleType),
            // V5 / continuous
            new StructField("V5", DataTypes.DoubleType),
            // V6 / continuous
            new StructField("V6", DataTypes.DoubleType),
            // V7 / continuous
            new StructField("V7", DataTypes.DoubleType),
            // V8 / continuous
            new StructField("V8", DataTypes.DoubleType),
            // V9 / continuous
            new StructField("V9", DataTypes.DoubleType),
            // V10 / continuous
            new StructField("V10", DataTypes.DoubleType),
            // V11 / continuous
            new StructField("V11", DataTypes.DoubleType),
            // V12 / continuous
            new StructField("V12", DataTypes.DoubleType),
            // V13 / continuous
            new StructField("V13", DataTypes.DoubleType),
            // V14 / continuous
            new StructField("V14", DataTypes.DoubleType),
            // V15 / continuous
            new StructField("V15", DataTypes.DoubleType),
            // V16 / continuous
            new StructField("V16", DataTypes.DoubleType),
            // class / nominal [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
            new StructField(
                "class", DataTypes.ByteType,
                new NominalScale("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
            )
        );
        try {
            data = Read.csv(
                DatasetManager.pathPrefix + "/classification/pendigits/pendigits.csv",
                DatasetManager.DEFAULT_CSV_FORMAT, schema
            );
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalStateException("Failed to load '" + PenDigits.class.getSimpleName() + "'");
        }
    }
}
