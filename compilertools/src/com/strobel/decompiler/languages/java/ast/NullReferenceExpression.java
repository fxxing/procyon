/*
 * NullReferenceExpression.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.languages.java.ast;

import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.languages.TextLocation;
import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;

public final class NullReferenceExpression extends Expression {
    private final static String NULL_TEXT = "null";

    private TextLocation _startLocation;
    private TextLocation _endLocation;

    public NullReferenceExpression( final int offset) {
        this( offset, TextLocation.EMPTY);
    }

    public NullReferenceExpression( final int offset, final TextLocation startLocation) {
        super( offset);
        _startLocation = VerifyArgument.notNull(startLocation, "startLocation");
        _endLocation = new TextLocation(startLocation.line(), startLocation.column() + NULL_TEXT.length());
    }

    @Override
    public TextLocation getStartLocation() {
        return _startLocation;
    }

    @Override
    public TextLocation getEndLocation() {
        return _endLocation;
    }

    public void setStartLocation(final TextLocation startLocation) {
        _startLocation = VerifyArgument.notNull(startLocation, "startLocation");
        _endLocation = new TextLocation(startLocation.line(), startLocation.column() + NULL_TEXT.length());
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitNullReferenceExpression(this, data);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        return other instanceof NullReferenceExpression;
    }
}
