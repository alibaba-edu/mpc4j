package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyCore;
import edu.alibaba.mpc4j.crypto.fhe.serialization.ComprModeType;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealCloneable;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealVersion;
import edu.alibaba.mpc4j.crypto.fhe.utils.DynArray;
import edu.alibaba.mpc4j.crypto.fhe.utils.ValCheck;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.*;

/**
 * Class to store a plaintext element. The data for the plaintext is a polynomial
 * with coefficients modulo the plaintext modulus. The degree of the plaintext
 * polynomial must be one less than the degree of the polynomial modulus. The
 * backing array always allocates one 64-bit word per each coefficient of the
 * polynomial.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/plaintext.h
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/2
 */
public class Plaintext implements SealCloneable {
    /**
     * parms_id
     */
    private ParmsId parmsId = ParmsId.parmsIdZero();
    /**
     * the number of coefficients in the plaintext polynomial
     */
    private int coeffCount = 0;
    /**
     * scale, only needed when using the CKKS encryption scheme
     */
    private double scale = 1.0;
    /**
     * data
     */
    private DynArray data;

    public Plaintext() {
        data = new DynArray();
    }

    /**
     * Constructs a plaintext representing a constant polynomial 0. The coefficient
     * count of the polynomial is set to the given value. The capacity is set to
     * the same value.
     *
     * @param coeffCount the number of (zeroed) coefficients in the plaintext.
     */
    public Plaintext(int coeffCount) {
        this.coeffCount = coeffCount;
        this.data = new DynArray(coeffCount);
    }

    /**
     * Constructs a plaintext representing a constant polynomial 0. The coefficient
     * count of the polynomial and the capacity are set to the given values.
     *
     * @param capacity   the capacity.
     * @param coeffCount the number of (zeroed) coefficients in the plaintext.
     */
    public Plaintext(int capacity, int coeffCount) {
        this.coeffCount = coeffCount;
        this.data = new DynArray(capacity, coeffCount);
    }

    /**
     * Constructs a plaintext representing a polynomial with given coefficient
     * values. The coefficient count of the polynomial is set to the number of
     * coefficient values provided, and the capacity is set to the given value.
     *
     * @param coeffs   desired values of the plaintext coefficients.
     * @param capacity the capacity.
     */
    public Plaintext(long[] coeffs, int capacity) {
        this.coeffCount = coeffs.length;
        this.data = new DynArray(coeffs, capacity);
    }

    /**
     * Constructs a plaintext representing a polynomial with given coefficient
     * values. The coefficient count of the polynomial is set to the number of
     * coefficient values provided, and the capacity is set to the same value.
     *
     * @param coeffs desired values of the plaintext coefficients.
     */
    public Plaintext(long[] coeffs) {
        this.coeffCount = coeffs.length;
        this.data = new DynArray(coeffs);
    }

    /**
     * Constructs a plaintext from a given hexadecimal string describing the
     * plaintext polynomial.
     * <p>
     * The string description of the polynomial must adhere to the format returned
     * by to_string(),
     * which is of the form "7FFx^3 + 1x^1 + 3" and summarized by the following
     * rules:
     * 1. Terms are listed in order of strictly decreasing exponent
     * 2. Coefficient values are non-negative and in hexadecimal format (upper
     * and lower case letters are both supported)
     * 3. Exponents are positive and in decimal format
     * 4. Zero coefficient terms (including the constant term) may be (but do
     * not have to be) omitted
     * 5. Term with the exponent value of one must be exactly written as x^1
     * 6. Term with the exponent value of zero (the constant term) must be written
     * as just a hexadecimal number without exponent
     * 7. Terms must be separated by exactly <space>+<space> and minus is not
     * allowed
     * 8. Other than the +, no other terms should have whitespace
     *
     * @param hexPoly a poly in hex string.
     */
    public Plaintext(String hexPoly) {
        // first call new Plaintext()
        this();
        fromHexPoly(hexPoly);
    }

    /**
     * Copies a given plaintext to the current one.
     *
     * @param assign the plaintext to copy from.
     */
    public void copyFrom(Plaintext assign) {
        this.coeffCount = assign.coeffCount;
        this.parmsId = new ParmsId(assign.parmsId);
        this.scale = assign.scale;
        this.data = new DynArray(assign.data);
    }

    /**
     * Creates a new plaintext from a given hexadecimal string describing the plaintext polynomial.
     *
     * @param hexPoly the formatted polynomial string specifying the plaintext polynomial.
     */
    private void fromHexPoly(String hexPoly) {
        if (isNttForm()) {
            throw new RuntimeException("cannot set an NTT transformed Plaintext");
        }
        if (Common.unsignedGt(hexPoly.length(), Integer.MAX_VALUE)) {
            throw new IllegalArgumentException("hex_poly too long");
        }
        int length = hexPoly.length();
        // Determine size needed to store string coefficient.
        int assignCoeffCount = 0;
        int assignCoeffBitCount = 0;
        int pos = 0;
        int lastPower = Math.min(data.maxSize(), Integer.MAX_VALUE);
        while (pos < length) {
            // Determine length of coefficient starting at pos.
            int coeffLength = getCoeffLength(hexPoly, pos);
            if (coeffLength == 0) {
                throw new IllegalArgumentException("unable to parse hex poly, please check the format of the hex poly");
            }

            // Determine bit length of coefficient.
            int coeffBitCount = Common.getHexStringBitCount(hexPoly, pos, coeffLength);
            if (coeffBitCount > assignCoeffBitCount) {
                assignCoeffBitCount = coeffBitCount;
            }
            pos += coeffLength;
            // Extract power-term.
            int[] powerLength = new int[1];
            int power = getCoeffPower(hexPoly, pos, powerLength);
            if (power == -1 || power >= lastPower) {
                throw new IllegalArgumentException("unable to parse hex poly");
            }
            if (assignCoeffCount == 0) {
                assignCoeffCount = power + 1;
            }
            pos += powerLength[0];
            lastPower = power;

            // Extract plus (unless it is the end).
            int plusLength = getPlus(hexPoly, pos);
            if (plusLength == -1) {
                throw new IllegalArgumentException("unable to parse hex poly");
            }
            pos += plusLength;
        }

        // If string is empty, then done.
        if (assignCoeffCount == 0 || assignCoeffBitCount == 0) {
            setZero();
            return;
        }

        // Resize polynomial.
        if (assignCoeffBitCount > Common.BITS_PER_UINT64) {
            throw new IllegalArgumentException("hex poly has too large coefficients");
        }
        resize(assignCoeffCount);

        // Populate polynomial from string.
        pos = 0;
        lastPower = coeffCount();
        while (pos < length) {
            // Determine length of coefficient starting at pos.
            int coeffPos = pos;
            int coeffLength = getCoeffLength(hexPoly, pos);
            pos += coeffLength;

            // Extract power-term.
            int[] powerLength = new int[1];
            int power = getCoeffPower(hexPoly, pos, powerLength);
            pos += powerLength[0];

            // Extract plus (unless it is the end).
            int plusLength = getPlus(hexPoly, pos);
            pos += plusLength;

            // Zero coefficients not set by string.
            for (int zeroPower = lastPower - 1; zeroPower > power; --zeroPower) {
                data.set(zeroPower, 0);
            }

            // Populate coefficient.
            UintCore.hexStringToUint(hexPoly, coeffPos, coeffLength, 1, power, getData());
            lastPower = power;
        }

        // Zero coefficients not set by string.
        for (int zeroPower = lastPower - 1; zeroPower >= 0; --zeroPower) {
            data.set(zeroPower, 0);
        }
    }

    private boolean isDecChar(char c) {
        return c >= '0' && c <= '9';
    }

    private int getDecValue(char c) {
        return c - '0';
    }

    /**
     * Gets the coefficient values length of the polynomial.
     *
     * @param poly       the polynomial.
     * @param startIndex start index.
     * @return the coefficient values length.
     */
    private int getCoeffLength(String poly, int startIndex) {
        int length = 0;
        int charIndex = startIndex;
        // here we also need to check the length
        while (charIndex < poly.length() && Common.isHexChar(poly.charAt(charIndex))) {
            length++;
            charIndex++;
        }
        return length;
    }

    /**
     * Gets the coefficient power.
     *
     * @param poly        the polynomial.
     * @param startIndex  start index.
     * @param powerLength the power length.
     * @return the coefficient power.
     */
    private int getCoeffPower(String poly, int startIndex, int[] powerLength) {
        int length = 0;
        int polyIndex = startIndex;
        if (poly.length() == startIndex) {
            powerLength[0] = 0;
            return 0;
        }
        if (poly.charAt(polyIndex) != 'x') {
            return -1;
        }
        polyIndex++;
        length++;
        if (poly.charAt(polyIndex) != '^') {
            return -1;
        }
        polyIndex++;
        length++;
        int power = 0;
        while (polyIndex < poly.length() && isDecChar(poly.charAt(polyIndex))) {
            power *= 10;
            power += getDecValue(poly.charAt(polyIndex));
            polyIndex++;
            length++;
        }
        powerLength[0] = length;
        return power;
    }

    /**
     * Gets "+" symbol length.
     *
     * @param poly       the polynomial.
     * @param startIndex start index.
     * @return "+" symbol length.
     */
    private int getPlus(String poly, int startIndex) {
        int polyIndex = startIndex;
        if (poly.length() == startIndex) {
            return 0;
        }

        if (poly.charAt(polyIndex++) != ' ') {
            return -1;
        }

        if (poly.charAt(polyIndex++) != '+') {
            return -1;
        }
        if (poly.charAt(polyIndex) != ' ') {
            return -1;
        }
        return 3;
    }

    /**
     * Allocates enough memory to accommodate the backing array of a plaintext with given capacity.
     *
     * @param capacity the capacity.
     */
    public void reserve(int capacity) {
        if (isNttForm()) {
            throw new RuntimeException("cannot reserve for an NTT transformed Plaintext");
        }
        data.reserve(capacity);
        coeffCount = data.size();
    }

    /**
     * Reallocates the data so that its capacity exactly matches its size.
     */
    public void shrinkToFit() {
        data.shrinkToFit();
    }

    /**
     * Resizes the plaintext to have a given coefficient count. The plaintext
     * is automatically reallocated if the new coefficient count does not fit in
     * the current capacity.
     *
     * @param coeffCount the number of coefficients in the plaintext polynomial.
     */
    public void resize(int coeffCount) {
        if (isNttForm()) {
            throw new RuntimeException("cannot resize for an NTT transformed Plaintext");
        }
        data.resize(coeffCount);
        this.coeffCount = coeffCount;
    }

    /**
     * Sets a coefficient of the polynomial with the given value.
     *
     * @param index the index of the coefficient to set.
     * @param coeff the given coefficient value.
     */
    public void set(int index, long coeff) {
        data.set(index, coeff);
    }

    /**
     * Sets the value of the current plaintext to a given constant polynomial and
     * sets the parms_id to parms_id_zero, effectively marking the plaintext as
     * not NTT transformed. The coefficient count is set to one.
     *
     * @param constCoeff the constant coefficient.
     */
    public void set(long constCoeff) {
        data.resize(1);
        data.set(0, constCoeff);
        coeffCount = 1;
        parmsId = ParmsId.parmsIdZero();
    }

    /**
     * Sets the coefficients of the current plaintext to given values and sets
     * the parms_id to parms_id_zero, effectively marking the plaintext as not
     * NTT transformed.
     *
     * @param coeffs desired values of the plaintext coefficients.
     */
    public void set(long[] coeffs) {
        data = new DynArray(coeffs);
        coeffCount = coeffs.length;
        parmsId = ParmsId.parmsIdZero();
    }

    /**
     * Returns the value of a given coefficient in the plaintext polynomial.
     *
     * @param index the index of the coefficient in the plaintext polynomial.
     * @return the value of a given coefficient in the plaintext polynomial.
     */
    public long get(int index) {
        return data.at(index);
    }

    /**
     * Returns the value of a given coefficient in the plaintext polynomial.
     *
     * @param index the index of the coefficient in the plaintext polynomial.
     * @return the value of a given coefficient in the plaintext polynomial.
     */
    public long at(int index) {
        return data.at(index);
    }

    /**
     * Returns the value of a given coefficient in the plaintext polynomial.
     *
     * @param index the index of the coefficient in the plaintext polynomial.
     * @return the value of a given coefficient in the plaintext polynomial.
     */
    public long getValue(int index) {
        return data.at(index);
    }

    /**
     * Returns the scale of the plaintext.
     *
     * @return the scale of the plaintext.
     */
    public double getScale() {
        return scale;
    }

    /**
     * Sets a given range of coefficients of a plaintext polynomial to zero; does
     * nothing if length is zero.
     *
     * @param startCoeff the index of the first coefficient to set to zero.
     * @param length     the number of coefficients to set to zero.
     */
    public void setZero(int startCoeff, int length) {
        if (length <= 0) {
            return;
        }
        if (startCoeff + length - 1 >= coeffCount) {
            throw new IndexOutOfBoundsException("length must be non-negative and start_coeff + length - 1 must be within [0, coeff_count)");
        }
        data.setZero(startCoeff, length);
    }

    /**
     * Sets the plaintext polynomial coefficients to zero starting at a given index.
     *
     * @param startCoeff the index of the first coefficient to set to zero.
     */
    public void setZero(int startCoeff) {
        if (startCoeff >= coeffCount) {
            throw new IndexOutOfBoundsException("start_coeff must be within [0, coeff_count)");
        }
        data.setZero(startCoeff);
    }

    /**
     * Sets the plaintext polynomial to zero.
     */
    public void setZero() {
        data.setZero();
    }

    /**
     * Gets the DynArray object of the plaintext.
     *
     * @return the data of the plaintext.
     */
    public DynArray getDynArray() {
        return data;
    }

    /**
     * Gets the data of the plaintext.
     *
     * @return the data of the plaintext.
     */
    public long[] getData() {
        return data.data();
    }

    /**
     * Returns the value of a given coefficient in the plaintext polynomial.
     *
     * @param coeffIndex the index of the coefficient in the plaintext polynomial.
     * @return the value of a given coefficient in the plaintext polynomial.
     */
    public long getData(int coeffIndex) {
        if (coeffCount == 0) {
            throw new RuntimeException();
        }
        if (coeffIndex >= coeffCount) {
            throw new IndexOutOfBoundsException("coeff_index must be within [0, coeff_count)");
        }
        return data.at(coeffIndex);
    }

    /**
     * Returns whether the current plaintext polynomial has all zero coefficients.
     *
     * @return whether the current plaintext polynomial has all zero coefficients.
     */
    public boolean isZero() {
        return (coeffCount == 0) || data.isZero();
    }

    /**
     * Returns the capacity of the current allocation.
     *
     * @return the capacity of the current allocation.
     */
    public int capacity() {
        return data.capacity();
    }

    /**
     * Returns the coefficient count of the current plaintext polynomial.
     *
     * @return the coefficient count of the current plaintext polynomial.
     */
    public int coeffCount() {
        return coeffCount;
    }

    /**
     * Returns the significant coefficient count of the current plaintext polynomial.
     *
     * @return the significant coefficient count of the current plaintext polynomial.
     */
    public int significantCoeffCount() {
        if (coeffCount == 0) {
            return 0;
        }
        return UintCore.getSignificantUint64CountUint(data.data(), coeffCount);
    }

    /**
     * Returns the non-zero coefficient count of the current plaintext polynomial.
     *
     * @return the non-zero coefficient count of the current plaintext polynomial.
     */
    public int nonZeroCoeffCount() {
        if (coeffCount == 0) {
            return 0;
        }
        return UintCore.getNonZeroUint64CountUint(data.data(), coeffCount);
    }

    /**
     * Returns the parms_id. The parms_id must remain zero unless the plaintext polynomial is in NTT form.
     *
     * @return the parms_id.
     */
    public ParmsId parmsId() {
        return parmsId;
    }

    /**
     * Sets the given parms_id to current plaintext.
     *
     * @param parmsId the given parms_id.
     */
    public void setParmsId(ParmsId parmsId) {
        this.parmsId = parmsId;
    }

    /**
     * Returns the scale of the plaintext.
     *
     * @return the scale of the plaintext.
     */
    public double scale() {
        return scale;
    }

    /**
     * Returns whether the current plaintext polynomial has all zero coefficients.
     *
     * @return true if the current plaintext polynomial has all zero coefficients; false otherwise.
     */
    public boolean isNttForm() {
        return !parmsId.isZero();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Plaintext)) {
            return false;
        }
        Plaintext that = (Plaintext) o;
        int sigCoeffCount = this.significantCoeffCount();
        int sigCoeffCountCompare = that.significantCoeffCount();
        if (sigCoeffCount != sigCoeffCountCompare) {
            return false;
        }
        // if both is NTT form, then compare parms_id
        boolean parmsIdCompare = (isNttForm() && that.isNttForm() && (parmsId.equals(that.parmsId))) || (
            !isNttForm() && !that.isNttForm());
        if (!parmsIdCompare) {
            return false;
        }
        long[] thisData = this.data.data();
        long[] thatData = that.data.data();
        // [0, sigCoeffCount) should be equal
        for (int i = 0; i < sigCoeffCount; i++) {
            if (thisData[i] != thatData[i]) {
                return false;
            }
        }
        // [sigCoeffCount, ..) should be zero
        for (int i = sigCoeffCount; i < thisData.length; i++) {
            if (thisData[i] != 0) {
                return false;
            }
        }
        for (int i = sigCoeffCount; i < thatData.length; i++) {
            if (thatData[i] != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(parmsId)
            .append(coeffCount)
            .append(scale)
            .append(data)
            .toHashCode();
    }

    @Override
    public String toString() {
        if (isNttForm()) {
            throw new IllegalArgumentException("cannot convert NTT transformed plaintext to string");
        }
        return PolyCore.polyToHexString(data.data(), coeffCount, 1);
    }

    @Override
    public void saveMembers(OutputStream outputStream) throws IOException {
        DataOutputStream stream = new DataOutputStream(outputStream);
        parmsId.saveMembers(stream);
        stream.writeLong(coeffCount);
        stream.writeDouble(scale);
        data.save(outputStream, ComprModeType.NONE);
        stream.close();
    }

    @Override
    public void loadMembers(SealContext context, InputStream inputStream, SealVersion version) throws IOException {
        // Verify parameters
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }
        DataInputStream stream = new DataInputStream(inputStream);

        // Set the metadata
        parmsId.loadMembers(stream);
        coeffCount = (int) stream.readLong();
        scale = stream.readDouble();

        // Checking the validity of loaded metadata
        // Note: We allow pure key levels here! This is to allow load_members
        // to be used also when loading derived objects like SecretKey. This
        // further means that functions reading in Plaintext objects must check
        // that for those use-cases the Plaintext truly is at the data level
        // if it is supposed to be. In other words, one cannot assume simply
        // based on load_members succeeding that the Plaintext is valid for
        // computations.
        if (!ValCheck.isMetaDataValidFor(this, context, true)) {
            throw new IllegalArgumentException("plaintext data is invalid");
        }

        // Reserve memory now that the metadata is checked for validity.
        data.reserve(coeffCount);

        // Load the data. Note that we are supplying also the expected maximum
        // size of the loaded DynArray. This is an important security measure to
        // prevent a malformed DynArray from causing arbitrarily large memory
        // allocations.
        data.load(context, inputStream);

        // Verify that the buffer is correct
        if (!ValCheck.isBufferValid(this)) {
            throw new IllegalArgumentException("plaintext data is invalid");
        }
        stream.close();
    }

    @Override
    public int load(SealContext context, InputStream inputStream) throws IOException {
        int inSize = unsafeLoad(context, inputStream);
        if (!ValCheck.isValidFor(this, context)) {
            throw new IllegalArgumentException("Plaintext data is invalid");
        }
        return inSize;
    }

    @Override
    public void load(SealContext context, byte[] in) throws IOException {
        unsafeLoad(context, in);
        if (!ValCheck.isValidFor(this, context)) {
            throw new IllegalArgumentException("Plaintext data is invalid");
        }
    }
}
