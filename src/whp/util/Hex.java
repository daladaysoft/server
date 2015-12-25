package whp.util;

import java.text.ParseException;

/*
 * @(#)Hex.java    1.0  27 November 2004
 *
 * Copyright 2004
 * College of Computer and Information Science
 * Northeastern University
 * Boston, MA  02115
 *
 * The Java Power Tools software may be used for educational
 * purposes as long as this copyright notice is retained intact
 * at the top of all source files.
 *
 * To discuss possible commercial use of this software, 
 * contact Richard Rasala at Northeastern University, 
 * College of Computer and Information Science,
 * 617-373-2462 or rasala@ccs.neu.edu.
 *
 * The Java Power Tools software has been designed and built
 * in collaboration with Viera Proulx and Jeff Raab.
 *
 * Should this software be modified, the words "Modified from 
 * Original" must be included as a comment below this notice.
 *
 * All publication rights are retained.  This software or its 
 * documentation may not be published in any media either
 * in whole or in part without explicit permission.
 *
 * This software was created with support from Northeastern 
 * University and from NSF grant DUE-9950829.
 */

/**
 * <P>Class <CODE>Hex</CODE> provides static methods for the conversion
 * of numeric types to hexadecimal strings and vice versa.</P> 
 *
 * <P>Class <CODE>Hex</CODE> cannot be instantiated.</P>
 *
 * @author  Richard Rasala
 * @version 2.3.3
 * @since   2.3
 */
public class Hex {
    
    /** Private constructor to prevent instantiation. */
    private Hex() {}
    
    
    /**
     * Returns the 2-character hexadecimal <CODE>String</CODE>
     * for the given byte.
     *
     * @param b the byte to convert
     * @return its 2-character hexadecimal <CODE>String</CODE>
     */
    public static String byteToHex(byte b) {
        return longToHex(b, 2);
    }
    
    
    /**
     * Returns the 4-character hexadecimal <CODE>String</CODE>
     * for the given short.
     *
     * @param s the short to convert
     * @return its 4-character hexadecimal <CODE>String</CODE>
     */
    public static String shortToHex(short s) {
        return longToHex(s, 4);
    }
    
    
    /**
     * Returns the 8-character hexadecimal <CODE>String</CODE>
     * for the given int.
     *
     * @param i the int to convert
     * @return its 8-character hexadecimal <CODE>String</CODE>
     */
    public static String intToHex(int i) {
        return longToHex(i, 8);
    }
    
    
    /**
     * Returns the 16-character hexadecimal <CODE>String</CODE>
     * for the given long.
     *
     * @param a the long to convert
     * @return its 16-character hexadecimal <CODE>String</CODE>
     */
    public static String longToHex(long a) {
        return longToHex(a, 16);
    }
    
    
    /**
     * <P>Returns the 8-character hexadecimal <CODE>String</CODE>
     * for the given float.</P>
     *
     * <P>Uses <CODE>Float.floatToRawIntBits</CODE> internally.</P>
     *
     * @param f the float to convert
     * @return its 8-character hexadecimal <CODE>String</CODE>
     */
    public static String floatToHex(float f) {
        return intToHex(Float.floatToRawIntBits(f));
    }
    
    
    /**
     * <P>Returns the 16-character hexadecimal <CODE>String</CODE>
     * for the given double.</P>
     *
     * <P>Uses <CODE>Double.doubleToRawLongBits</CODE> internally.</P>
     *
     * @param d the double to convert
     * @return its 16-character hexadecimal <CODE>String</CODE>
     */
    public static String doubleToHex(double d) {
        return longToHex(Double.doubleToRawLongBits(d));
    }
    
    
    /**
     * <P>Returns a hexadecimal string for the given long that contains
     * the maxdigits trailing hexadecimal digits.</P>
     *
     * <P>This method is a helper method for the other methods in this class.</P>
     *
     * @param a the long to convert
     * @param maxdigits the number of trailing digits to provide
     * @return the trailing digit hexadecimal <CODE>String</CODE> with maxdigits digits
     */
    public static String longToHex(long a, int maxdigits) {
        if (maxdigits < 1)
            return "";
        
        String hex = "0123456789ABCDEF";
        
        char[] digits = new char[maxdigits];
        
        for (int k = (maxdigits - 1); k >= 0; k--) {
            digits[k] = hex.charAt((int) (a & 15));
            a = a >>> 4;
        }
        
        return new String(digits);
    }
    
    
    /**
     * <P>If the given <CODE>String s</CODE> is a hexadecimal <CODE>String</CODE>
     * and contains at most 2 digits,
     * then the <CODE>String</CODE> is converted to a byte and returned.</P>
     *
     * <P>Throws a <CODE>ParseException</CODE> if the <CODE>String</CODE> is not a
     * hexadecimal <CODE>String</CODE> or its length is greater than 2.</P>
     *
     * @param s the hexadecimal <CODE>String</CODE>
     * @return the byte value of s
     * @throws ParseException if s is not hexadecimal with at most 2 digits
     */
    public static byte hexToByte(String s)
        throws ParseException
    {
        return (byte) hexToLong(s, 2);
    }
    
    
    /**
     * <P>If the given <CODE>String s</CODE> is a hexadecimal <CODE>String</CODE>
     * and contains at most 4 digits,
     * then the <CODE>String</CODE> is converted to a short and returned.</P>
     *
     * <P>Throws a <CODE>ParseException</CODE> if the <CODE>String</CODE> is not a
     * hexadecimal <CODE>String</CODE> or its length is greater than 4.</P>
     *
     * @param s the hexadecimal <CODE>String</CODE>
     * @return the short value of s
     * @throws ParseException if s is not hexadecimal with at most 4 digits
     */
    public static short hexToShort(String s)
        throws ParseException
    {
        return (short) hexToLong(s, 4);
    }
    
    
    /**
     * <P>If the given <CODE>String s</CODE> is a hexadecimal <CODE>String</CODE>
     * and contains at most 8 digits,
     * then the <CODE>String</CODE> is converted to an int and returned.</P>
     *
     * <P>Throws a <CODE>ParseException</CODE> if the <CODE>String</CODE> is not a
     * hexadecimal <CODE>String</CODE> or its length is greater than 8.</P>
     *
     * @param s the hexadecimal <CODE>String</CODE>
     * @return the int value of s
     * @throws ParseException if s is not hexadecimal with at most 8 digits
     */
    public static int hexToInt(String s)
        throws ParseException
    {
        return (int) hexToLong(s, 8);
    }
    
    
    /**
     * <P>If the given <CODE>String s</CODE> is a hexadecimal <CODE>String</CODE>
     * and contains at most 16 digits,
     * then the <CODE>String</CODE> is converted to a long and returned.</P>
     *
     * <P>Throws a <CODE>ParseException</CODE> if the <CODE>String</CODE> is not a
     * hexadecimal <CODE>String</CODE> or its length is greater than 16.</P>
     *
     * @param s the hexadecimal <CODE>String</CODE>
     * @return the long value of s
     * @throws ParseException if s is not hexadecimal with at most 16 digits
     */
    public static long hexToLong(String s)
        throws ParseException
    {
        return hexToLong(s, 16);
    }
    
    
    /**
     * <P>If the given <CODE>String s</CODE> is a hexadecimal <CODE>String</CODE>
     * and contains exactly 8 digits,
     * then the <CODE>String</CODE> is converted to a float and returned.</P>
     *
     * <P>Uses <CODE>Float.intBitsToFloat</CODE> internally.</P>
     *
     * <P>Throws a <CODE>ParseException</CODE> if the <CODE>String</CODE> is not a
     * hexadecimal <CODE>String</CODE> or its length is not exactly 8.</P>
     *
     * @param s the hexadecimal <CODE>String</CODE>
     * @return the float value of s
     * @see #hexToInt(String)
     * @see Float#intBitsToFloat(int)
     * @throws ParseException if s is not hexadecimal with exactly 8 digits
     */
    public static float hexToFloat(String s)
        throws ParseException
    {
        if ((s != null) && (s.length() != 8))
            throw new ParseException
                ("Float hex string must have length 8", Math.min(s.length(), 8));
            
        return Float.intBitsToFloat(hexToInt(s));
    }
    
    
    /**
     * <P>If the given <CODE>String s</CODE> is a hexadecimal <CODE>String</CODE>
     * and contains exactly 16 digits,
     * then the <CODE>String</CODE> is converted to a double and returned.</P>
     *
     * <P>Uses <CODE>Double.longBitsToDouble</CODE> internally.</P>
     *
     * <P>Throws a <CODE>ParseException</CODE> if the <CODE>String</CODE> is not a
     * hexadecimal <CODE>String</CODE> or its length is not exactly 16.</P>
     *
     * @param s the hexadecimal <CODE>String</CODE>
     * @return the double value of s
     * @see #hexToLong(String)
     * @see Double#longBitsToDouble(long)
     * @throws ParseException if s is not hexadecimal with exactly 16 digits
     */
    public static double hexToDouble(String s)
        throws ParseException
    {
        if ((s != null) && (s.length() != 16))
            throw new ParseException
                ("Double hex string must have length 16", Math.min(s.length(), 16));
            
        return Double.longBitsToDouble(hexToLong(s));
    }
    
    
    /**
     * <P>If the given <CODE>String s</CODE> is a hexadecimal <CODE>String</CODE>
     * and contains at most maxdigits,
     * then the <CODE>String</CODE> is converted to a long and returned.</P>
     *
     * <P>Throws a <CODE>ParseException</CODE> if the given <CODE>String</CODE>
     * is <CODE>null</CODE> or is not a hexadecimal <CODE>String</CODE>,
     * or if its length is greater than maxdigits,
     * or if maxdigits is greater than 16.</P>
     *
     * <P>This method is a helper method for the other methods in this class.</P>
     *
     * @param s the hexadecimal <CODE>String</CODE>
     * @param maxdigits the limit on the length of s
     * @return the long value of s
     */
    public static long hexToLong(String s, int maxdigits)
        throws ParseException
    {
        if (s == null)
            throw new ParseException("String is null", 0);
        
        int length = s.length();
        int index  = 0;
        
        if (!isHexString(s)) {
            for (index = 0; index < length; index++) {
                if (!isHexDigit(s.charAt(index)))
                    throw new ParseException
                        ("String is not a hex string", index);
            }
        }
        
        if (length > maxdigits)
            throw new ParseException
                ("String is longer than " + maxdigits + "digits", maxdigits);
        
        if (maxdigits > 16)
            throw new ParseException
                ("Max digits is larger than 16", 0);
        
        long x = 0;
        long y;
        
        for (index = 0; index < length; index++) {
            x = x << 4;
            y = Character.digit(s.charAt(index), 16);
            x = x | y;
        }
        
        return x;
    }
    
    
    /**
     * <P>Returns whether or not the given <CODE>String</CODE> contains only
     * hexadecimal digits (0 to 9, A to F, a to f).</P>
     *
     * <P>Returns false if the given <CODE>String</CODE> is <CODE>null</CODE>.
     *
     * @param s the <CODE>String</CODE> to test
     * @return whether the given <CODE>String</CODE> contains only hexadecimal digits
     */
    public static boolean isHexString(String s) {
        if (s == null)
            return false;
        
        int length = s.length();
        
        for (int index = 0; index < length; index++)
            if (!isHexDigit(s.charAt(index)))
                return false;
        
        return true;
    }
    
    
    /**
     * Returns whether or not the given char is a hexadecimal digit
     * (0 to 9, A to F, a to f).
     *
     * @param c the char to test
     * @return whether the given char is a hexadecimal digit
     */
    public static boolean isHexDigit(char c) {
        return (('0' <= c) && (c <= '9'))
            || (('A' <= c) && (c <= 'F'))
            || (('a' <= c) && (c <= 'f'));
    }
    
    
//    private static final long low8bits = 0xFFL;
//    
//    private static final long low16bits = 0xFFFFL;
//    
//    private static final long low32bits = 0xFFFFFFFFL;
    
//    /**
//     * <p>Returns a Stringable filter that filters <code>XNumber</code>
//     * objects to be those that have zero bits where the given mask has
//     * one bits.</p>
//     *
//     * <p>If the object is of integral type then it is tested as a long
//     * but with the high order bits set to zero if necessary prior to
//     * the testing.</p>
//     *
//     * <p>If the object is of floating type then it is converted to raw
//     * bits with the high order bits set to zero if necessary prior to
//     * the testing.</p>
//     * 
//     * @param mask the mask that tests for zero bits in the data
//     */
//    public static StringableFilter maskFilter(final long mask) {
//        return new StringableFilter() {
//            public Stringable filterStringable(Stringable obj)
//                throws FilterException
//            {
//                if (! (obj instanceof XNumber))
//                    throw new FilterException
//                        (obj, "Filter must apply to objects that extend XNumber");
//                
//                XNumber number = (XNumber) obj;
//                
//                long data;
//                
//                
//                if (obj instanceof XFloat) {
//                    XFloat x = (XFloat) number;
//                    data = Float.floatToRawIntBits(x.floatValue());
//                    data &= low32bits;
//                }
//                else
//                if (obj instanceof XDouble) {
//                    XDouble x = (XDouble) number;
//                    data = Double.doubleToRawLongBits(x.doubleValue());
//                }
//                else {
//                    data = number.longValue();
//                    
//                    if (obj instanceof XByte)
//                        data &= low8bits;
//                    else
//                    if (obj instanceof XShort)
//                        data &= low16bits;
//                    else
//                    if (obj instanceof XInt)
//                        data &= low32bits;
//                }
//                
//                if ((data & mask) == 0)
//                    return obj;
//                
//                String s = Hex.longToHex(mask);
//                
//                throw new FilterException
//                    (obj, "Number must have zero bits where the mask " + s + " has one bits");
//            }
//        };
//    }
    
    
}