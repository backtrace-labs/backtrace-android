package backtraceio.library.common;

/**
 * Backtrace TypeHelper helps with common type comparision.
 */
public class TypeHelper {
    /**
     * Check if object type is primitive - for example: int or long.
     *
     * @param type object to check
     * @return true, if an object is primitive. Otherwise false.
     */
    public static boolean isPrimitiveOrPrimitiveWrapperOrString(Class type) {
        return (type.isPrimitive() && type != void.class) ||
                type == Double.class || type == Float.class || type == Long.class ||
                type == Integer.class || type == Short.class || type == Character.class ||
                type == Byte.class || type == Boolean.class || type == String.class || type.isEnum();
    }
}