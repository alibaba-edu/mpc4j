package edu.alibaba.mpc4j.crypto.fhe.seal.utils;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;

/**
 * The DynArray class is mainly intended for internal use and provides the underlying data structure for Plaintext and
 * Ciphertext classes.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/dynarray.h">dynarray.h</a>.
 *
 * @author Weiran Liu
 * @date 2025/4/20
 */
public abstract class AbstractDynArray {
    /**
     * the capacity of the array, the implementation ensures that capacity >= size
     */
    private int capacity = 0;
    /**
     * the size of the array
     */
    protected int size = 0;
    /**
     * data
     */
    protected long[] data;

    /**
     * Creates a new DynArray. No memory is allocated by this constructor.
     */
    public AbstractDynArray() {
        data = new long[0];
    }

    /**
     * Creates a new DynArray with given size.
     *
     * @param size the size of the array.
     */
    public AbstractDynArray(int size) {
        data = new long[0];
        // Reserve memory, resize, and set to zero
        resize(size);
    }

    /**
     * Creates a new DynArray with given capacity and size.
     *
     * @param capacity the capacity of the array.
     * @param size     the size of the array.
     */
    public AbstractDynArray(int capacity, int size) {
        if (capacity < size) {
            throw new IllegalArgumentException("capacity cannot be smaller than size");
        }
        data = new long[0];
        // Reserve memory, resize, and set to zero
        reserve(capacity);
        resize(size);
    }

    /**
     * Creates a new DynArray with given capacity, initialized with data from
     * a given buffer.
     *
     * @param data     desired contents of the array.
     * @param capacity the capacity of the array.
     */
    public AbstractDynArray(long[] data, int capacity) {
        this.capacity = capacity;
        this.size = data.length;
        this.data = new long[capacity];
        // Copy over value
        System.arraycopy(data, 0, this.data, 0, size);
    }

    /**
     * Creates a new DynArray initialized with data from a given buffer.
     *
     * @param data desired contents of the array.
     */
    public AbstractDynArray(long[] data) {
        this.capacity = data.length;
        this.size = data.length;
        this.data = new long[capacity];
        // Copy over value
        System.arraycopy(data, 0, this.data, 0, size);
    }

    /**
     * Creates a new DynArray with given size wrapping a given pointer. This
     * constructor allocates no memory. If the DynArray goes out of scope, the
     * Pointer object given here is destroyed. On resizing the DynArray to larger
     * size, the data will be copied over to a new allocation from the memory pool
     * pointer to by the given MemoryPoolHandle and the Pointer object given here
     * will subsequently be destroyed. Unlike the other constructors, this one
     * exposes the option of not automatically zero-filling the allocated memory.
     *
     * @param data     desired contents of the array.
     * @param capacity the capacity of the array.
     * @param size     the size of the array.
     * @param fillZero if true, fills data with zeros.
     */
    public AbstractDynArray(long[] data, int capacity, int size, boolean fillZero) {
        if (capacity < size) {
            throw new IllegalArgumentException("capacity cannot be smaller than size");
        }
        // Grab the given Pointer
        this.data = data;
        // Resize, and optionally set to zero
        resize(size, fillZero);
    }

    /**
     * Creates a new DynArray by copying a given one.
     *
     * @param copy the DynArray to copy from.
     */
    public AbstractDynArray(AbstractDynArray copy) {
        this.capacity = copy.capacity;
        this.size = copy.size;
        this.data = new long[size];
        System.arraycopy(copy.data, 0, this.data, 0, size);
    }

    /**
     * Returns a constant reference to the array element at a given index.
     * This function performs bounds checking and will throw an error if
     * the index is out of range.
     *
     * @param index the index of the array element.
     * @return a constant reference to the array element at a given index.
     */
    public long at(int index) {
        return data[index];
    }

    /**
     * Sets the array element at a given index to a given value. This
     * function performs bounds checking and will throw an error if
     * the index is out of range.
     *
     * @param index the index of the array element.
     * @param value the value to be set.
     * @return the set value.
     */
    public long set(int index, long value) {
        return data[index] = value;
    }

    /**
     * Returns whether the array has size zero.
     *
     * @return true if the array has size zero.
     */
    public boolean empty() {
        return size == 0;
    }

    /**
     * Reallocates the array so that its capacity exactly matches its size.
     */
    public void shrinkToFit() {
        reserve(size);
    }

    /**
     * Allocates enough memory for storing a given number of elements without
     * changing the size of the array. If the given capacity is smaller than
     * the current size, the size is automatically set to equal the new capacity.
     *
     * @param capacity the capacity of the array.
     */
    public void reserve(int capacity) {
        int copySize = Math.min(capacity, size);
        // Create new allocation and copy over value
        long[] newData = new long[capacity];
        System.arraycopy(data, 0, newData, 0, copySize);
        this.data = newData;
        // Set the coeff_count and capacity
        this.capacity = capacity;
        size = copySize;
    }

    /**
     * Resizes the array to given size. When resizing to larger size the data
     * in the array remains unchanged and any new space is initialized to zero;
     * when resizing to smaller size the last elements of the array are dropped. I
     * f the capacity is not already large enough to hold the new size, the array
     * is also reallocated.
     *
     * @param size the size of the array.
     */
    public void resize(int size) {
        resize(size, true);
    }

    /**
     * Resizes the array to given size. When resizing to larger size the data
     * in the array remains unchanged and any new space is initialized to zero
     * if fill_zero is set to true; when resizing to smaller size the last
     * elements of the array are dropped. If the capacity is not already large
     * enough to hold the new size, the array is also reallocated.
     *
     * @param size     the size of the array.
     * @param fillZero If true, fills expanded space with zeros.
     */
    public void resize(int size, boolean fillZero) {
        if (size <= capacity) {
            // Are we changing size to bigger within current capacity?
            // If so, need to set top terms to zero
            if (size > this.size && fillZero) {
                Arrays.fill(data, this.size, size, 0);
            }
            // set size
            this.size = size;
            return;
        }
        // At this point we know for sure that size_ <= capacity_ < size so need
        // to reallocate to bigger
        long[] newData = new long[size];
        // copy original data
        System.arraycopy(data, 0, newData, 0, this.size);
        if (fillZero) {
            Arrays.fill(newData, this.size, size, 0);
        }
        this.data = newData;
        // Set the coeff_count and capacity
        capacity = size;
        this.size = size;
    }

    /**
     * Sets the data to the given data.
     *
     * @param data the given data.
     */
    public void setData(long[] data) {
        assert data.length == size;
        System.arraycopy(data, 0, this.data, 0, size);
    }

    /**
     * Sets data[startIndex, startIndex + length) to zero.
     *
     * @param startIndex the start index.
     * @param length     the length.
     */
    public void setZero(int startIndex, int length) {
        Arrays.fill(data, startIndex, startIndex + length, 0);
    }

    /**
     * Sets data[startIndex, size) to zero.
     *
     * @param startIndex the start index.
     */
    public void setZero(int startIndex) {
        Arrays.fill(data, startIndex, size, 0);
    }

    /**
     * Sets data[0, size) to zero.
     */
    public void setZero() {
        Arrays.fill(data, 0, size, 0);
    }

    /**
     * Sets the size of the array to zero. The capacity is not changed.
     */
    public void clear() {
        size = 0;
    }

    /**
     * Returns if the data is all zero.
     *
     * @return true if the data is all zero.
     */
    public boolean isZero() {
        return Arrays.stream(data).allMatch(n -> n == 0);
    }

    /**
     * Gets the whole data.
     *
     * @return the whole data.
     */
    public long[] data() {
        return data;
    }

    /**
     * Gets data[startIndex, endIndex).
     *
     * @param startIndex the start index.
     * @param endIndex   the end index.
     * @return data[startIndex, endIndex).
     */
    public long[] data(int startIndex, int endIndex) {
        assert startIndex < endIndex;
        assert endIndex <= size;
        long[] result = new long[endIndex - startIndex + 1];
        System.arraycopy(data, startIndex, result, 0, endIndex - startIndex + 1);
        return result;
    }

    /**
     * Returns the capacity of the array.
     *
     * @return the capacity of the array.
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Returns the size of the array.
     *
     * @return the size of the array.
     */
    public int size() {
        return size;
    }

    /**
     * Returns the largest possible array size.
     *
     * @return the largest possible array size.
     */
    public int maxSize() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int hashCode() {
        // data[0, size) is equal
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        for (int i = 0; i < size; i++) {
            hashCodeBuilder.append(data);
        }
        return hashCodeBuilder.hashCode();
    }
}
