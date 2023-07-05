package edu.alibaba.mpc4j.common.tool.filter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * abstract Bloom Filter.
 *
 * @author Weiran Liu
 * @date 2023/5/7
 */
abstract class AbstractBloomFilter<T> implements BloomFilter<T> {
    /**
     * Bloom Filter type
     */
    private final FilterType bloomFilterType;
    /**
     * max number of elements
     */
    private final int maxSize;
    /**
     * storage bit length
     */
    protected final int m;
    /**
     * offset
     */
    private final int offset;
    /**
     * storage
     */
    private final byte[] storage;
    /**
     * hashes
     */
    protected final Prf[] hashes;
    /**
     * number of inserted elements
     */
    private int size;
    /**
     * item byte length, used for computing compress radio
     */
    private int itemByteLength;

    AbstractBloomFilter(FilterType bloomFilterType, EnvType envType, int maxSize, int m, byte[][] keys,
                        int size, byte[] storage, int itemByteLength) {
        this.bloomFilterType = bloomFilterType;
        MathPreconditions.checkPositive("maxSize", maxSize);
        this.maxSize = maxSize;
        this.m = m;
        int byteM = CommonUtils.getByteLength(m);
        offset = byteM * Byte.SIZE - m;
        hashes = Arrays.stream(keys)
            .map(key -> {
                Prf hash = PrfFactory.createInstance(envType, Integer.BYTES);
                hash.setKey(key);
                return hash;
            })
            .toArray(Prf[]::new);
        MathPreconditions.checkNonNegative("size", size);
        this.size = size;
        MathPreconditions.checkEqual("storage.length", "byteM", storage.length, byteM);
        this.storage = storage;
        MathPreconditions.checkNonNegative("itemByteLength", itemByteLength);
        this.itemByteLength = itemByteLength;
    }

    @Override
    public List<byte[]> toByteArrayList() {
        List<byte[]> byteArrayList = new LinkedList<>();
        // type
        byteArrayList.add(IntUtils.intToByteArray(bloomFilterType.ordinal()));
        // max size
        byteArrayList.add(IntUtils.intToByteArray(maxSize));
        // size
        byteArrayList.add(IntUtils.intToByteArray(size));
        // item byte length
        byteArrayList.add(IntUtils.intToByteArray(itemByteLength));
        // storage
        byteArrayList.add(BytesUtils.clone(storage));
        // keys
        for (Prf hash : hashes) {
            byteArrayList.add(BytesUtils.clone(hash.getKey()));
        }

        return byteArrayList;
    }

    @Override
    public FilterFactory.FilterType getFilterType() {
        return bloomFilterType;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int maxSize() {
        return maxSize;
    }

    @Override
    public byte[] getStorage() {
        return storage;
    }

    @Override
    public int getM() {
        return m;
    }

    @Override
    public boolean mightContain(T data) {
        // verify each position in the sparse indexes is true
        int[] hashIndexes = hashIndexes(data);
        for (int index : hashIndexes) {
            if (!BinaryUtils.getBoolean(storage, index + offset)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void put(T data) {
        MathPreconditions.checkLess("size", size, maxSize);
        if (mightContain(data)) {
            throw new IllegalArgumentException("Insert might duplicate item: " + data);
        }
        // mightContain has checked if there is any collision, here we can directly insert the item.
        int[] hashIndexes = hashIndexes(data);
        for (int index : hashIndexes) {
            if (!BinaryUtils.getBoolean(storage, index + offset)) {
                BinaryUtils.setBoolean(storage, index + offset, true);
            }
        }
        // update the item byte length
        byte[] dataBytes = ObjectUtils.objectToByteArray(data);
        itemByteLength += dataBytes.length;
        size++;
    }

    @Override
    public void merge(MergeFilter<T> other) {
        AbstractBloomFilter<T> that = (AbstractBloomFilter<T>) other;
        // max size should be the same
        MathPreconditions.checkEqual("this.maxSize", "that.maxSize", this.maxSize, that.maxSize);
        MathPreconditions.checkEqual("this.hashNum", "that.hashNum", this.hashes.length, that.hashes.length);
        int hashNum = hashes.length;
        IntStream.range(0, hashNum).forEach(hashIndex -> {
            // hash type should be equal
            Preconditions.checkArgument(this.hashes[hashIndex].getPrfType().equals(that.hashes[hashIndex].getPrfType()));
            // key should be equal
            Preconditions.checkArgument(Arrays.equals(hashes[hashIndex].getKey(), that.hashes[hashIndex].getKey()));
        });
        MathPreconditions.checkLessOrEqual("merge size", this.size + that.size, maxSize);
        // merge Bloom Filter
        BytesUtils.ori(storage, that.storage);
        size += that.size;
        itemByteLength += that.itemByteLength;
    }

    @Override
    public double ratio() {
        return (double) storage.length / itemByteLength;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractBloomFilter)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        //noinspection unchecked
        AbstractBloomFilter<T> that = (AbstractBloomFilter<T>) obj;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(this.maxSize, that.maxSize)
            .append(this.size, that.size)
            .append(this.itemByteLength, that.itemByteLength)
            .append(this.storage, that.storage)
            .append(this.hashes.length, that.hashes.length);
        int hashNum = hashes.length;
        IntStream.range(0, hashNum).forEach(hashIndex -> {
            equalsBuilder.append(hashes[hashIndex].getPrfType(), that.hashes[hashIndex].getPrfType());
            equalsBuilder.append(hashes[hashIndex].getKey(), that.hashes[hashIndex].getKey());
        });
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(maxSize)
            .append(size)
            .append(itemByteLength)
            .append(storage)
            .append(hashes.length);
        int hashNum = hashes.length;
        IntStream.range(0, hashNum).forEach(hashIndex -> {
            hashCodeBuilder.append(hashes[hashIndex].getPrfType());
            hashCodeBuilder.append(hashes[hashIndex].getKey());
        });
        return hashCodeBuilder.toHashCode();
    }
}
