/*******************************************************************************
 * Copyright (c) 2010-2020 Haifeng Li. All rights reserved.
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

package edu.alibaba.mpc4j.sml.smile.feature;

import edu.alibaba.mpc4j.sml.smile.base.cart.Cart;
import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.formula.Formula;

import java.util.Objects;

/**
 * SHAP of ensemble tree methods. TreeSHAP is a fast and exact method to
 * estimate SHAP values for tree models and ensembles of trees, under
 * several different possible assumptions about feature dependence.
 *
 * @author Haifeng Li
 */
public interface TreeShap extends Shap<Tuple> {

    /**
     * Returns the classification/regression trees.
     *
     * @return the classification/regression trees.
     */
    Cart[] trees();

    /**
     * Returns the formula associated with the model.
     *
     * @return the formula associated with the model.
     */
    Formula formula();

    /**
     * Returns the SHAP value.
     *
     * @param x features.
     * @return the SHAP value.
     */
    @Override
    default double[] shap(Tuple x) {
        Cart[] forest = trees();
        Tuple xt = formula().x(x);

        double[] phi = null;
        for (Cart tree : forest) {
            double[] phii = tree.shap(xt);

            if (phi == null) {
                phi = phii;
            } else {
                for (int i = 0; i < phi.length; i++) {
                    phi[i] += phii[i];
                }
            }
        }

        for (int i = 0; i < Objects.requireNonNull(phi).length; i++) {
            phi[i] /= forest.length;
        }

        return phi;
    }

    /**
     * Returns the average of absolute SHAP values over a data frame.
     *
     * @param data data.
     * @return he average of absolute SHAP values over a data frame.
     */
    default double[] shap(DataFrame data) {
        // Binds the formula to the data frame's schema in case that
        // it is different from that of training data.
        formula().bind(data.schema());
        return shap(data.stream().parallel());
    }
}
