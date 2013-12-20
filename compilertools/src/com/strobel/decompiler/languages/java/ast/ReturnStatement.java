/*
 * ReturnStatement.java
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

import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;

public class ReturnStatement extends Statement {
    public final static TokenRole RETURN_KEYWORD_ROLE = new TokenRole("return", TokenRole.FLAG_KEYWORD);

    public ReturnStatement(final int offset) {
        super(offset);
    }

    public ReturnStatement(final int offset, final Expression returnValue) {
        super(offset);
        setExpression(returnValue);
    }

    public final JavaTokenNode getReturnToken() {
        return getChildByRole(RETURN_KEYWORD_ROLE);
    }

    public final Expression getExpression() {
        return getChildByRole(Roles.EXPRESSION);
    }

    public final void setExpression(final Expression value) {
        setChildByRole(Roles.EXPRESSION, value);
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitReturnStatement(this, data);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        return other instanceof ReturnStatement &&
               !other.isNull() &&
               getExpression().matches(((ReturnStatement) other).getExpression(), match);
    }
}