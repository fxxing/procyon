/*
 * NullType.java
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

package com.strobel.reflection;

import javax.lang.model.type.TypeKind;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("unchecked")
final class NullType extends Type {
    @Override
    public TypeKind getKind() {
        return TypeKind.NULL;
    }

    @Override
    public final Class getErasedClass() {
        return null;
    }

    @Override
    public boolean isInstance(final Object o) {
        return o == null;
    }

    @Override
    public Object accept(final TypeVisitor visitor, final Object parameter) {
        return visitor.visitType(this, parameter);
    }

    @Override
    public final MemberType getMemberType() {
        return MemberType.TypeInfo;
    }

    @Override
    public final Type getDeclaringType() {
        return null;
    }

    @Override
    public final int getModifiers() {
        return Flags.PUBLIC;
    }

    @Override
    protected final StringBuilder _appendClassName(final StringBuilder sb, final boolean fullName, final boolean dottedName) {
        return sb.append("<nulltype>");
    }

    @Override
    protected final StringBuilder _appendClassDescription(final StringBuilder sb) {
        return sb.append("<nulltype>");
    }

    @Override
    public final StringBuilder appendBriefDescription(final StringBuilder sb) {
        return sb.append("<nulltype>");
    }

    @Override
    public final StringBuilder appendSimpleDescription(final StringBuilder sb) {
        return sb.append("<nulltype>");
    }
}

