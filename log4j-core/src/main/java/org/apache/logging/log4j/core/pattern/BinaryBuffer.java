/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package org.apache.logging.log4j.core.pattern;

import java.nio.ByteBuffer;

import org.apache.logging.log4j.core.util.Assert;

/**
 * Buffer implementation that tracks data as bytes.
 */
public class BinaryBuffer implements Buffer {

    private ByteBuffer buffer;

    public BinaryBuffer() {
        this(ByteBuffer.allocateDirect(512 * 1024)); // FIXME
    }

    public BinaryBuffer(ByteBuffer buffer) {
        this.buffer = Assert.requireNonNull(buffer, "buffer");
    }

    @Override
    public BinaryBuffer append(Object object) {
        return append(String.valueOf(object));
    }

    @Override
    public BinaryBuffer append(String text) {
        buffer.put(text.getBytes());
        return this;
    }

    @Override
    public BinaryBuffer append(char ch) {
        if (ch > 126) { // not in ISO-8859-1
            append(new String(new char[] { ch }));
        } else {
            buffer.put((byte) ch);
        }
        return this;
    }

    @Override
    public BinaryBuffer append(int i) {
        if (i == Integer.MIN_VALUE) {
            append("-2147483648");
            return this;
        }
        int appendedLength = (i < 0) ? stringSizeOfInt(-i) + 1 : stringSizeOfInt(i);
        int spaceNeeded = buffer.position() + appendedLength;
        if (spaceNeeded > buffer.capacity()) { // TODO use buffer.remaining()
            // expandCapacity(spaceNeeded); // FIXME make expandable
        }
        getChars(i, spaceNeeded, buffer);
        buffer.position(spaceNeeded);
        return this;
    }

    @Override
    public BinaryBuffer append(long l) {
        if (l == Long.MIN_VALUE) {
            append("-9223372036854775808");
            return this;
        }
        int appendedLength = (l < 0) ? stringSize(-l) + 1 : stringSize(l);
        int spaceNeeded = buffer.position() + appendedLength;
        // ensureCapacityInternal(spaceNeeded); // FIXME
        getChars(l, spaceNeeded, buffer);
        buffer.position(spaceNeeded);
        return this;
    }

    final static int[] sizeTable = { 9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE };

    // Requires positive x
    static int stringSizeOfInt(int x) {
        for (int i = 0;; i++)
            if (x <= sizeTable[i])
                return i + 1;
    }

    // Requires positive x
    static int stringSize(long x) {
        long p = 10;
        for (int i = 1; i < 19; i++) {
            if (x < p)
                return i;
            p = 10 * p;
        }
        return 19;
    }

    /**
     * Places characters representing the integer i into the ByteBuffer buf. The characters are placed into the buffer
     * backwards starting with the least significant digit at the specified index (exclusive), and working backwards
     * from there.
     *
     * Will fail if i == Integer.MIN_VALUE
     */
    static void getChars(int i, int index, ByteBuffer buf) {
        int q, r;
        int charPos = index;
        char sign = 0;

        if (i < 0) {
            sign = '-';
            i = -i;
        }

        // Generate two digits per iteration
        while (i >= 65536) {
            q = i / 100;
            // really: r = i - (q * 100);
            r = i - ((q << 6) + (q << 5) + (q << 2));
            i = q;
            buf.put(--charPos, (byte) DigitOnes[r]);
            buf.put(--charPos, (byte) DigitTens[r]);
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i <= 65536, i);
        for (;;) {
            q = (i * 52429) >>> (16 + 3);
            r = i - ((q << 3) + (q << 1)); // r = i-(q*10) ...
            buf.put(--charPos, (byte) digits[r]);
            i = q;
            if (i == 0)
                break;
        }
        if (sign != 0) {
            buf.put(--charPos, (byte) sign);
        }
    }

    /**
     * Places characters representing the long i into the ByteBuffer buf. The characters are placed into the buffer
     * backwards starting with the least significant digit at the specified index (exclusive), and working backwards
     * from there.
     *
     * Will fail if i == Long.MIN_VALUE
     */
    static void getChars(long i, int index, ByteBuffer buf) {
        long q;
        int r;
        int charPos = index;
        char sign = 0;

        if (i < 0) {
            sign = '-';
            i = -i;
        }

        // Get 2 digits/iteration using longs until quotient fits into an int
        while (i > Integer.MAX_VALUE) {
            q = i / 100;
            // really: r = i - (q * 100);
            r = (int) (i - ((q << 6) + (q << 5) + (q << 2)));
            i = q;
            buf.put(--charPos, (byte) DigitOnes[r]);
            buf.put(--charPos, (byte) DigitTens[r]);
        }

        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int) i;
        while (i2 >= 65536) {
            q2 = i2 / 100;
            // really: r = i2 - (q * 100);
            r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
            i2 = q2;
            buf.put(--charPos, (byte) DigitOnes[r]);
            buf.put(--charPos, (byte) DigitTens[r]);
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i2 <= 65536, i2);
        for (;;) {
            q2 = (i2 * 52429) >>> (16 + 3);
            r = i2 - ((q2 << 3) + (q2 << 1)); // r = i2-(q2*10) ...
            buf.put(--charPos, (byte) digits[r]);
            i2 = q2;
            if (i2 == 0)
                break;
        }
        if (sign != 0) {
            buf.put(--charPos, (byte) sign);
        }
    }

    /**
     * All possible chars for representing a number as a String
     */
    final static char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
            'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

    final static char[] DigitTens = { '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '1', '1', '1', '1', '1', '1',
            '1', '1', '1', '1', '2', '2', '2', '2', '2', '2', '2', '2', '2', '2', '3', '3', '3', '3', '3', '3', '3',
            '3', '3', '3', '4', '4', '4', '4', '4', '4', '4', '4', '4', '4', '5', '5', '5', '5', '5', '5', '5', '5',
            '5', '5', '6', '6', '6', '6', '6', '6', '6', '6', '6', '6', '7', '7', '7', '7', '7', '7', '7', '7', '7',
            '7', '8', '8', '8', '8', '8', '8', '8', '8', '8', '8', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9', };

    final static char[] DigitOnes = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', };

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.logging.log4j.core.pattern.Buffer#append(byte[])
     */
    @Override
    public BinaryBuffer append(byte[] data) {
        buffer.put(data);
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.logging.log4j.core.pattern.Buffer#length()
     */
    @Override
    public int length() {
        return buffer.position();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.logging.log4j.core.pattern.Buffer#setLength(int)
     */
    @Override
    public void setLength(int length) {
        buffer.position(length);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.logging.log4j.core.pattern.Buffer#hasTrailingWhitespace()
     */
    @Override
    public boolean hasTrailingWhitespace() {
        final int len = buffer.position();
        return len > 0 && !Character.isWhitespace((char) buffer.get(len - 1));
    }
}