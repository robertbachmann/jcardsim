/*
 * Copyright 2017 Robert Bachmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.licel.jcardsim.utils;

import java.util.Stack;

final class HexStringParser {
    private static final byte[] LOOKUP_TABLE = new byte['f' + 1];
    private static final ThreadLocal<Stack<Record>> STACK_THREAD_LOCAL =
            new ThreadLocal<Stack<Record>>();

    static {
        LOOKUP_TABLE['0'] = 0;
        LOOKUP_TABLE['1'] = 1;
        LOOKUP_TABLE['2'] = 2;
        LOOKUP_TABLE['3'] = 3;
        LOOKUP_TABLE['4'] = 4;
        LOOKUP_TABLE['5'] = 5;
        LOOKUP_TABLE['6'] = 6;
        LOOKUP_TABLE['7'] = 7;
        LOOKUP_TABLE['8'] = 8;
        LOOKUP_TABLE['9'] = 9;
        LOOKUP_TABLE['A'] = 0xA;
        LOOKUP_TABLE['a'] = 0xA;
        LOOKUP_TABLE['B'] = 0xB;
        LOOKUP_TABLE['b'] = 0xB;
        LOOKUP_TABLE['C'] = 0xC;
        LOOKUP_TABLE['c'] = 0xC;
        LOOKUP_TABLE['D'] = 0xD;
        LOOKUP_TABLE['d'] = 0xD;
        LOOKUP_TABLE['E'] = 0xE;
        LOOKUP_TABLE['e'] = 0xE;
        LOOKUP_TABLE['F'] = 0xF;
        LOOKUP_TABLE['f'] = 0xF;
    }

    private static class Record {
        public final int position;
        public final char end;

        Record(int position, char end) {
            this.position = position;
            this.end = end;
        }
    }

    private static Stack<Record> getStack() {
        Stack<Record> stack = STACK_THREAD_LOCAL.get();
        if (stack == null) {
            STACK_THREAD_LOCAL.set(new Stack<Record>());
            return STACK_THREAD_LOCAL.get();
        }
        stack.clear();
        return stack;
    }

    public static byte[] parse(final String hexString) {
        if (hexString == null) {
            throw new NullPointerException("hexArray");
        }

        final Stack<Record> stack = getStack();
        final byte[] byteBuffer = new byte[hexString.length()];

        int byteBufferPosition = 0;
        boolean secondNibble = false;
        boolean charModeEnabled = false;
        byte tmp = 0;
        int i;

        for (i = 0; i < hexString.length(); ++i) {
            char ch = hexString.charAt(i);

            switch (ch) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case 'A':
                case 'a':
                case 'B':
                case 'b':
                case 'C':
                case 'c':
                case 'D':
                case 'd':
                case 'E':
                case 'e':
                case 'F':
                case 'f':
                    if (charModeEnabled) {
                        byteBuffer[byteBufferPosition] = (byte) (ch & 0xFF);
                        ++byteBufferPosition;
                    } else if (secondNibble) {
                        secondNibble = false;
                        tmp |= LOOKUP_TABLE[ch];
                        byteBuffer[byteBufferPosition] = tmp;
                        ++byteBufferPosition;
                    } else {
                        secondNibble = true;
                        tmp = (byte) (LOOKUP_TABLE[ch] << 4);
                    }
                    break;
                case '|':
                    if (secondNibble) {
                        throw new IllegalArgumentException("Odd number of digits at position " + i);
                    }
                    charModeEnabled = !charModeEnabled;
                    break;
                case ' ':
                    if (charModeEnabled) {
                        byteBuffer[byteBufferPosition] = (byte) (ch & 0xFF);
                        ++byteBufferPosition;
                    }
                    break;
                case '#': {
                    if (i + 1 == hexString.length()) {
                        if (charModeEnabled) {
                            byteBuffer[byteBufferPosition] = (byte) '#';
                            ++byteBufferPosition;
                        } else {
                            throw new IllegalArgumentException("Can not parse input at " + (i + 1) + ": #");
                        }
                    } else {
                        final char next = hexString.charAt(i + 1);
                        byte b = 0;
                        switch (next) {
                            case '<':
                                ++i;
                                stack.push(new Record(byteBufferPosition, '>'));
                                break;
                            case '(':
                                ++i;
                                stack.push(new Record(byteBufferPosition, ')'));
                                break;
                            case '{':
                                ++i;
                                stack.push(new Record(byteBufferPosition, '}'));
                                break;
                            default:
                                if (charModeEnabled) {
                                    b = (byte) (ch & 0xFF);
                                } else {
                                    throw new IllegalArgumentException("Can not parse input at " + (i + 1) + ": " + next);
                                }
                        }
                        byteBuffer[byteBufferPosition] = b;
                        ++byteBufferPosition;
                    }
                    break;
                }
                case '>':
                case ')':
                case '}': {
                    final Record record = (stack.isEmpty() ? null : stack.peek());
                    if (record == null || record.end != ch) {
                        if (!charModeEnabled) {
                            throw new IllegalArgumentException("Can not parse input at " + (i + 1) + ": " + ch);
                        } else {
                            byteBuffer[byteBufferPosition] = (byte) (ch & 0xFF);
                            ++byteBufferPosition;
                        }
                    } else {
                        int len = byteBufferPosition - record.position - 1;
                        byteBuffer[record.position] = (byte) (len & 0xFF);
                        stack.pop();
                    }
                    break;
                }
                default:
                    if (charModeEnabled) {
                        byteBuffer[byteBufferPosition] = (byte) (ch & 0xFF);
                        ++byteBufferPosition;
                    } else {
                        throw new IllegalArgumentException("Can not parse input at " + (i + 1) + ": " + ch);
                    }
            }
        }

        if (!stack.isEmpty()) {
            final Record record = stack.pop();
            throw new IllegalArgumentException("Missing closing '" + record.end + "'");
        }

        if (!charModeEnabled && secondNibble) {
            throw new IllegalArgumentException("Odd number of digits at position " + i);
        }

        if (byteBufferPosition == 0) {
            return new byte[0];
        }

        byte[] result = new byte[byteBufferPosition];
        System.arraycopy(byteBuffer, 0, result, 0, result.length);
        return result;
    }
}
