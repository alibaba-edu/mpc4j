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
 * Weather Nominal data.
 * <p>
 * 4 nominal features, 2-class classification.
 * </p>
 * Download from:
 * <p>
 * https://github.com/haifengl/smile/blob/master/shell/src/universal/data/weka/weather.nominal.arff
 * </p>
 *
 * @author Haifeng, Weiran Liu
 * @date 2020/11/17
 */
public class Weather {
    /**
     * 样本集
     */
    public static DataFrame data;
    /**
     * 样本标签：play
     */
    public static Formula formula = Formula.lhs("play");

    static {
        StructType schema = DataTypes.struct(
            // outlook / nominal [sunny, overcast, rainy]
            new StructField("outlook_sunny", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("outlook_overcast", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("outlook_rainy", DataTypes.ByteType, new NominalScale("0", "1")),
            // temperature / nominal [hot, mild, cool]
            new StructField("temperature_hot", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("temperature_mild", DataTypes.ByteType, new NominalScale("0", "1")),
            new StructField("temperature_cool", DataTypes.ByteType, new NominalScale("0", "1")),
            // humidity / nominal [high, normal]
            new StructField("humidity", DataTypes.ByteType, new NominalScale("0", "1")),
            // windy / nominal [TRUE, FALSE]
            new StructField("windy", DataTypes.ByteType, new NominalScale("0", "1")),
            // play / nominal [yes, no]
            new StructField("play", DataTypes.ByteType, new NominalScale("yes", "no"))
        );
        try {
            data = Read.csv(
                DatasetManager.pathPrefix + "/classification/weather/weather.csv",
                DatasetManager.DEFAULT_CSV_FORMAT, schema
            );
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalStateException("Failed to load '" + Weather.class.getSimpleName() + "'");
        }
    }
}
