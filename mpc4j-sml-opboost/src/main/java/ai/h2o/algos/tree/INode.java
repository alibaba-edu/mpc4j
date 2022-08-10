/*
 * Original Work Copyright 2018 H2O.ai.
 * Modified by Weiran Liu. Adjust the code based on Alibaba Java Code Guidelines.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.h2o.algos.tree;

/**
 * Interface of INode.
 *
 * @author Michal Kurka, Weiran Liu
 * @date 2021/10/08
 */
public interface INode<T> {
    /**
     * whether it is a leaf node.
     *
     * @return if it is a leaf node.
     */
    boolean isLeaf();

    /**
     * Gets leaf node value.
     *
     * @return leaf node value.
     */
    float getLeafValue();

    /**
     * Gets split feature index.
     *
     * @return split feature index.
     */
    int getSplitIndex();

    /**
     * Gets next node index.
     *
     * @param value input value.
     * @return next node index.
     */
    int next(T value);

    /**
     * Gets left child index.
     *
     * @return left child index.
     */
    int getLeftChildIndex();

    /**
     * Gets right child index.
     *
     * @return right child index.
     */
    int getRightChildIndex();

}