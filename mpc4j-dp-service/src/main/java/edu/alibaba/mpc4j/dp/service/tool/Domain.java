package edu.alibaba.mpc4j.dp.service.tool;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * domain.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class Domain {
    /**
     * the domain set
     */
    private final Set<String> domainSet;
    /**
     * the index-domain map
     */
    private final ArrayList<String> indexDomainMap;
    /**
     * the domain-index map
     */
    private final Map<String, Integer> domainIndexMap;
    /**
     * the domain size d, i.e., |Ω|
     */
    private final int d;

    public Domain(Set<String> domainSet) {
        d = domainSet.size();
        MathPreconditions.checkGreater("|Ω|", d, 1);
        this.domainSet = domainSet;
        indexDomainMap = new ArrayList<>(domainSet);
        domainIndexMap = IntStream.range(0, d)
            .boxed()
            .collect(Collectors.toMap(indexDomainMap::get, index -> index));
    }

    /**
     * Gets the domain set.
     *
     * @return the domain set.
     */
    public Set<String> getDomainSet() {
        return domainSet;
    }

    /**
     * Returns if the domain contains the given item.
     *
     * @param item the item.
     * @return true if the domain contains the given item.
     */
    public boolean contains(String item) {
        return domainSet.contains(item);
    }

    /**
     * Gets the index of the item.
     *
     * @param item the item.
     * @return the index.
     */
    public int getItemIndex(String item) {
        Preconditions.checkArgument(domainIndexMap.containsKey(item));
        return domainIndexMap.get(item);
    }

    /**
     * Gets the item of the index.
     *
     * @param index the index.
     * @return the item.
     */
    public String getIndexItem(int index) {
        MathPreconditions.checkNonNegativeInRange("index", index, d);
        return indexDomainMap.get(index);
    }

    /**
     * Gets the domain size d, i.e., |Ω|.
     *
     * @return the domain size.
     */
    public int getD() {
        return d;
    }
}
