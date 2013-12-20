/*
 * Func2.java
 *
 * Copyright (c) 2012 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.core.delegates;

/**
 * @author Mike Strobel
 */
public interface Func2<T1, T2, R> {
    R apply(final T1 t1, final T2 t2);
}
