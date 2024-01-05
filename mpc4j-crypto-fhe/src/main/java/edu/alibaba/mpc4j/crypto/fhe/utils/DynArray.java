package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealCloneable;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealVersion;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.io.*;
import java.util.Arrays;

/**
 * The DynArray class is mainly intended for internal use and provides the underlying data structure for Plaintext and
 * Ciphertext classes.
 * <p></p>
 * The implementation is from: https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/dynarray.h
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/31
 */
public class DynArray implements SealCloneable {
    /**
     * the capacity of the array, the implementation ensures that capacity >= size
     */
    private int capacity = 0;
    /**
     * the size of the array
     */
    private int size = 0;
    /**
     * data
     */
    private long[] data;

    /**
     * Creates a new DynArray. No memory is allocated by this constructor.
     */
    public DynArray() {
        data = new long[0];
    }

    /**
     * Creates a new DynArray with given size.
     *
     * @param size the size of the array.
     */
    public DynArray(int size) {
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
    public DynArray(int capacity, int size) {
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
    public DynArray(long[] data, int capacity) {
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
    public DynArray(long[] data) {
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
    public DynArray(long[] data, int capacity, int size, boolean fillZero) {
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
    public DynArray(DynArray copy) {
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
     * Releases any allocated memory to the memory pool and sets the size
     * and capacity of the array to zero.
     */
    public void release() {
        capacity = 0;
        size = 0;
        data = new long[0];
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DynArray)) {
            return false;
        }
        DynArray that = (DynArray) o;
        if (this.size != that.size) {
            return false;
        }
        // data[0, size) is equal
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        for (int i = 0; i < size; i++) {
            equalsBuilder.append(this.data[i], that.data[i]);
        }
        return equalsBuilder.isEquals();
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, new MultilineRecursiveToStringStyle()).toString();
    }

    @Override
    public void saveMembers(OutputStream outputStream) throws IOException {
        DataOutputStream stream = new DataOutputStream(outputStream);
        stream.writeInt(size);
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                stream.writeLong(data[i]);
            }
        }
        stream.close();
    }

    @Override
    public void loadMembers(SealContext context, InputStream inputStream, SealVersion version) throws IOException {
        DataInputStream stream = new DataInputStream(inputStream);
        int readSize = stream.readInt();
        // Set new size; this is potentially unsafe if size64 was not checked against expected_size
        resize(readSize);
        // Read data
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                data[i] = stream.readLong();
            }
        }
        stream.close();
    }

    @Override
    public int load(SealContext context, InputStream inputStream) throws IOException {
        // there is no valid check in DynArray
        return unsafeLoad(context, inputStream);
    }

    @Override
    public void load(SealContext context, byte[] in) throws IOException {
        // there is no valid chekc in DynArray
        unsafeLoad(context, in);
    }
}
