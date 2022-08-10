/*******************************************************************************
 * Original Work: Copyright (c) 2010-2020 Haifeng Li. All rights reserved.
 * Modified Work: Copyright (c) 2021-2022 Weiran Liu.
 *
 * Smile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Smile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Smile.  If not, see <https://www.gnu.org/licenses/>.
 ******************************************************************************/

package edu.alibaba.mpc4j.sml.smile.base.cart;

import smile.data.Tuple;
import smile.data.type.StructField;
import smile.data.type.StructType;

/**
 * A node with a ordinal split variable (real-valued or ordinal categorical value).
 *
 * @author Haifeng Li
 */
public class OrdinalNode extends InternalNode {
    private static final long serialVersionUID = 6200239129592792884L;
    /**
     * The split value.
     */
    double value;
    /**
     * The left value for splitting.
     */
    private double leftValue;
    /**
     * The right value for splitting.
     */
    private double rightValue;

    /**
     * Constructor.
     */
    public OrdinalNode(int feature, double value, double leftValue, double rightValue,
                       double score, double deviance, Node trueChild, Node falseChild) {
        super(feature, score, deviance, trueChild, falseChild);
        this.value = value;
        this.leftValue = leftValue;
        this.rightValue = rightValue;
    }

    /**
     * return the left value for splitting.
     *
     * @return left value for splitting.
     */
    public double getLeftValue() {
        return leftValue;
    }

    /**
     * return the right value for splitting.
     *
     * @return right value for splitting.
     */
    public double getRightValue() {
        return rightValue;
    }

    /**
     * replace the splitting values.
     *
     * @param leftValue  the replaced left value for splitting.
     * @param rightValue the replaced right value for splitting.
     */
    public void replaceValue(double leftValue, double rightValue) {
        this.leftValue = leftValue;
        this.rightValue = rightValue;
        value = (leftValue + rightValue) / 2;
    }

    @Override
    public LeafNode predict(Tuple x) {
        return x.getDouble(feature) <= value ? trueChild.predict(x) : falseChild.predict(x);
    }

    @Override
    public boolean branch(Tuple x) {
        return x.getDouble(feature) <= value;
    }

    @Override
    public OrdinalNode replace(Node trueChild, Node falseChild) {
        return new OrdinalNode(feature, value, leftValue, rightValue, score, deviance, trueChild, falseChild);
    }

    @Override
    public String dot(StructType schema, StructField response, int id) {
        StructField field = schema.field(feature);
        return String.format(" %d [label=<%s &le; %s<br/>size = %d<br/>impurity reduction = %.4f>, fillcolor=\"#00000000\"];\n", id, field.name, field.toString(value), size(), score);
    }

    @Override
    public String toString(StructType schema, boolean trueBranch) {
        StructField field = schema.field(feature);
        String condition = trueBranch ? "<=" : ">";
        return String.format("%s%s%g", field.name, condition, value);
    }
}
