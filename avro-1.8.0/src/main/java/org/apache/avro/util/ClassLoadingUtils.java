/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.avro.util;

public class ClassLoadingUtils {

    private ClassLoadingUtils() {
        //Utility Class
    }

    /**
     * Loads a class using the class loader.
     * 1. The class loader of the current class is being used.
     * 2. The thread context class loader is being used.
     * If both approaches fail, returns null.
     *
     * @param className The name of the class to load.
     * @return The class or null if no class loader could load the class.
     */
    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        return ClassLoadingUtils.loadClass(ClassLoadingUtils.class, className);
    }

    /**
     * Loads a class using the class loader.
     * 1. The class loader of the context class is being used.
     * 2. The thread context class loader is being used.
     * If both approaches fail, returns null.
     *
     * @param contextClass The name of a context class to use.
     * @param className    The name of the class to load
     * @return The class or null if no class loader could load the class.
     */
    public static Class<?> loadClass(Class<?> contextClass, String className) throws ClassNotFoundException {
        Class<?> clazz = null;
        if (contextClass.getClassLoader() != null) {
            clazz = loadClassFromClassLoader(contextClass.getClassLoader(), className);
        }
        if (clazz == null && Thread.currentThread().getContextClassLoader() != null) {
            clazz = loadClassFromClassLoader(Thread.currentThread().getContextClassLoader(), className);
        }
        if (clazz == null) {
            throw new ClassNotFoundException("Failed to load class" + className);
        }
        return clazz;
    }

        /**
     * Loads a class using the class loader.
     * 1. The class loader of the context class is being used.
     * 2. The thread context class loader is being used.
     * If both approaches fail, returns null.
     *
     * @param classLoader The classloader to use.
     * @param className    The name of the class to load
     * @return The class or null if no class loader could load the class.
     */
    public static Class<?> loadClass(ClassLoader classLoader, String className) throws ClassNotFoundException {
        Class<?> clazz = null;
        if (classLoader != null) {
            clazz = loadClassFromClassLoader(classLoader, className);
        }
        if (clazz == null && Thread.currentThread().getContextClassLoader() != null) {
            clazz = loadClassFromClassLoader(Thread.currentThread().getContextClassLoader(),className);
        }
        if (clazz == null) {
            throw new ClassNotFoundException("Failed to load class" + className);
        }
        return clazz;
    }

    /**
     * Loads a {@link Class} from the specified {@link ClassLoader} without throwing {@ClassNotFoundException}.
     *
     * @param className
     * @param classLoader
     * @return
     */
    private static Class<?> loadClassFromClassLoader(ClassLoader classLoader, String className) {
        Class<?> clazz = null;
        if (classLoader != null && className != null) {
            try {
                clazz = classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                //Ignore and return null
            }
        }
        return clazz;
    }
}
