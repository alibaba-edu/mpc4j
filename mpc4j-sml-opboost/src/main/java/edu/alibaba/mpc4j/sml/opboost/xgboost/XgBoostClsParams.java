package edu.alibaba.mpc4j.sml.opboost.xgboost;

import java.util.HashMap;
import java.util.Map;

/**
 * XGBoost分类参数。
 *
 * @author Weiran Liu
 * @date 2021/10/07
 */
public class XgBoostClsParams implements XgBoostParams {
    /**
     * 目前支持的参数数量，需要增加一个verbosity和一个tree_method，对于多方分类还需要增加一个num_class
     */
    private static final int PARAMS_NUM = 7;
    /**
     * XGBoost回归参数映射
     */
    private final Map<String, Object> params;
    /**
     * XGBoost回归树数量
     */
    private final int treeNum;

    private XgBoostClsParams(Builder builder) {
        params = new HashMap<>(PARAMS_NUM);
        params.put("eta", builder.shrinkage);
        params.put("max_depth", builder.maxDepth);
        params.put("subsample", builder.subSample);
        params.put("verbosity", 0);
        if (builder.numClass == 2) {
            // 二分类
            params.put("objective", "binary:logistic");
        } else {
            // 多分类，还需要设置num_class
            params.put("objective", "multi:softmax");
            params.put("num_class", builder.numClass);
        }
        switch (builder.treeMethodType) {
            case AUTO:
                params.put("tree_method", "auto");
                break;
            case EXACT:
                params.put("tree_method", "exact");
                break;
            case ARRPOX:
                params.put("tree_method", "approx");
                break;
            default:
                throw new IllegalArgumentException("Invalid TreeMethodType: " + builder.treeMethodType.name());
        }
        // 构建树数量
        treeNum = builder.treeNum;
    }

    @Override
    public Map<String, Object> getParams() {
        return params;
    }

    @Override
    public int getTreeNum() {
        return treeNum;
    }

    /**
     * XGBoost回归参数构建器
     */
    public static class Builder implements org.apache.commons.lang3.builder.Builder<XgBoostClsParams> {
        /**
         * number of classes
         */
        private final int numClass;
        /**
         * n_estimators [default = 1, alias: rounds]
         * – Number of gradient boosted trees. Equivalent to number of boosting rounds.
         * - range: (0, ∞)
         */
        private int treeNum;
        /**
         * eta [default = 0.3, alias: learning_rate]
         * - Step size shrinkage used in update to prevents overfitting. After each boosting step, we can directly get
         *   the weights of new features, and eta shrinks the feature weights to make the boosting process more
         *   conservative.
         * - range: (0, 1)
         */
        private double shrinkage;
        /**
         * max_depth [default=6]
         * - Maximum depth of a tree. Increasing this value will make the model more complex and more likely to overfit.
         *   0 is only accepted in lossguide growing policy when tree_method is set as hist or gpu_hist and it indicates
         *   no limit on depth. Beware that XGBoost aggressively consumes memory when training a deep tree.
         * - range: [0, ∞) (0 is only accepted in lossguide growing policy when tree_method is set as hist or gpu_hist)
         */
        private int maxDepth;
        /**
         * subsample [default=1]
         * - Subsample ratio of the training instances. Setting it to 0.5 means that XGBoost would randomly sample
         *   half of the training data prior to growing trees. and this will prevent overfitting. Subsampling will
         *   occur once in every boosting iteration.
         * - range: (0, 1]
         */
        private double subSample;
        /**
         * tree_method string [default= auto]
         * - The tree construction algorithm used in XGBoost.
         * - XGBoost supports approx, hist and gpu_hist for distributed training.
         * - Choices: auto, exact, approx, hist, gpu_hist. Here we only allow auto, exact and approx
         */
        private OpXgBoostUtils.TreeMethodType treeMethodType;

        public Builder(int numClass) {
            assert numClass > 1 : "# class must be greater than 1";
            this.numClass = numClass;
            treeNum = 1;
            shrinkage = 0.3;
            maxDepth = 6;
            subSample = 1.0;
            treeMethodType = OpXgBoostUtils.TreeMethodType.EXACT;
        }

        public Builder setTreeNum(int treeNum) {
            assert treeNum > 0 : "treeNum must be greater than 0";
            this.treeNum = treeNum;
            return this;
        }

        public Builder setShrinkage(double shrinkage) {
            assert shrinkage > 0 && shrinkage < 1 : "shrinkage must be in range (0, 1)";
            this.shrinkage = shrinkage;
            return this;
        }

        public Builder setMaxDepth(int maxDepth) {
            assert maxDepth > 0 : "maxDepth must be greater than 0";
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder setSubSample(double subSample) {
            assert subSample > 0 && subSample <= 1 : "subSample must be in range (0, 1]";
            this.subSample = subSample;
            return this;
        }

        public Builder setTreeMethodType(OpXgBoostUtils.TreeMethodType treeMethodType) {
            this.treeMethodType = treeMethodType;
            return this;
        }

        @Override
        public XgBoostClsParams build() {
            return new XgBoostClsParams(this);
        }
    }
}
