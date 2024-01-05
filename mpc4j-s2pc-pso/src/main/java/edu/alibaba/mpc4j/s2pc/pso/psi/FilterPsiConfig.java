package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;

/**
 * filter-supported PSI config.
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
public interface FilterPsiConfig extends PsiConfig {
    /**
     * Gets filter type.
     *
     * @return filter type.
     */
    FilterType getFilterType();
}
