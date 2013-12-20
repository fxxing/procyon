/*
 * EliminateSyntheticAccessorsTransform.java
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

package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.assembler.metadata.*;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.*;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.strobel.core.CollectionUtilities.*;

public class EliminateSyntheticAccessorsTransform extends ContextTrackingVisitor<Void> {
    private final List<AstNode> _nodesToRemove;
    private final Map<String, MethodDeclaration> _accessMethodDeclarations;
    private final Set<String> _visitedTypes;

    public EliminateSyntheticAccessorsTransform(final DecompilerContext context) {
        super(context);

        _nodesToRemove = new ArrayList<>();
        _accessMethodDeclarations = new HashMap<>();
        _visitedTypes = new HashSet<>();
    }

    @Override
    public void run(final AstNode compilationUnit) {
        //
        // First run through and locate any outer class member access methods.
        //
        new PhaseOneVisitor().run(compilationUnit);

        super.run(compilationUnit);

        for (final AstNode node : _nodesToRemove) {
            node.remove();
        }
    }

    private static String makeMethodKey(final MethodReference method) {
        return method.getFullName() + ":" + method.getErasedSignature();
    }

    @Override
    public Void visitInvocationExpression(final InvocationExpression node, final Void data) {
        super.visitInvocationExpression(node, data);

        final Expression target = node.getTarget();
        final AstNodeCollection<Expression> arguments = node.getArguments();

        if (target instanceof MemberReferenceExpression) {
            final MemberReferenceExpression memberReference = (MemberReferenceExpression) target;

            MemberReference reference = memberReference.getUserData(Keys.MEMBER_REFERENCE);

            if (reference == null) {
                reference = node.getUserData(Keys.MEMBER_REFERENCE);
            }

            if (reference instanceof MethodReference) {
                final MethodReference method = (MethodReference) reference;
                final TypeReference declaringType = method.getDeclaringType();

                if (!MetadataResolver.areEquivalent(context.getCurrentType(), declaringType) &&
                    !MetadataHelper.isEnclosedBy(context.getCurrentType(), declaringType) &&
                    !_visitedTypes.contains(declaringType.getInternalName())) {

                    final MethodDefinition resolvedMethod = method.resolve();

                    if (resolvedMethod != null &&
                        resolvedMethod.isSynthetic()) {
                        final AstBuilder astBuilder = context.getUserData(Keys.AST_BUILDER);

                        if (astBuilder != null) {
                            final TypeDeclaration ownerTypeDeclaration = astBuilder.createType(resolvedMethod.getDeclaringType());

                            ownerTypeDeclaration.acceptVisitor(new PhaseOneVisitor(), data);
                        }
                    }
                }

                final String key = makeMethodKey(method);
                final MethodDeclaration declaration = _accessMethodDeclarations.get(key);

                if (declaration != null) {
                    final MethodDefinition definition = declaration.getUserData(Keys.METHOD_DEFINITION);
                    final List<ParameterDefinition> parameters = definition != null ? definition.getParameters() : null;

                    if (definition != null && parameters.size() == arguments.size()) {
                        final Map<ParameterDefinition, AstNode> parameterMap = new IdentityHashMap<>();

                        int i = 0;

                        for (final Expression argument : arguments) {
                            parameterMap.put(parameters.get(i++), argument);
                        }

                        final AstNode inlinedBody = InliningHelper.inlineMethod(declaration, parameterMap);

                        if (inlinedBody instanceof Expression) {
                            node.replaceWith(inlinedBody);
                        }
                        else if (inlinedBody instanceof BlockStatement) {
                            final BlockStatement block = (BlockStatement) inlinedBody;

                            if (block.getStatements().size() == 2) {
                                final Statement setStatement = block.getStatements().firstOrNullObject();

                                if (setStatement instanceof ExpressionStatement) {
                                    final Expression expression = ((ExpressionStatement) setStatement).getExpression();

                                    if (expression instanceof AssignmentExpression) {
                                        expression.remove();
                                        node.replaceWith(expression);
                                    }
                                }
                            }
                            else if (block.getStatements().size() == 3) {
                                final Statement tempAssignment = block.getStatements().firstOrNullObject();
                                final Statement setStatement = getOrDefault(block.getStatements(), 1);

                                if (tempAssignment instanceof VariableDeclarationStatement &&
                                    setStatement instanceof ExpressionStatement) {

                                    final Expression expression = ((ExpressionStatement) setStatement).getExpression();

                                    if (expression instanceof AssignmentExpression) {
                                        final VariableDeclarationStatement tempVariable = (VariableDeclarationStatement) tempAssignment;
                                        final Expression initializer = tempVariable.getVariables().firstOrNullObject().getInitializer();
                                        final AssignmentExpression assignment = (AssignmentExpression) expression;

                                        initializer.remove();
                                        assignment.setRight(initializer);
                                        expression.remove();
                                        node.replaceWith(expression);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    // <editor-fold defaultstate="collapsed" desc="PhaseOneVisitor Class">

    private final static MethodDeclaration SYNTHETIC_GET_ACCESSOR;
    private final static MethodDeclaration SYNTHETIC_SET_ACCESSOR;
    private final static MethodDeclaration SYNTHETIC_SET_ACCESSOR_ALT;
    private final static MethodDeclaration SYNTHETIC_STATIC_GET_ACCESSOR;
    private final static MethodDeclaration SYNTHETIC_STATIC_SET_ACCESSOR;
    private final static MethodDeclaration SYNTHETIC_STATIC_SET_ACCESSOR_ALT;

    static {
        final MethodDeclaration getAccessor = new MethodDeclaration();
        final MethodDeclaration setAccessor = new MethodDeclaration();

        getAccessor.setName(Pattern.ANY_STRING);
        getAccessor.getModifiers().add(new JavaModifierToken(Modifier.STATIC));
        getAccessor.setReturnType(new AnyNode("returnType").toType());

        setAccessor.setName(Pattern.ANY_STRING);
        setAccessor.getModifiers().add(new JavaModifierToken(Modifier.STATIC));
        setAccessor.setReturnType(new AnyNode("returnType").toType());

        final ParameterDeclaration getParameter = new ParameterDeclaration(
            Pattern.ANY_STRING,
            new AnyNode("targetType").toType()
        );

        getParameter.setAnyModifiers(true);
        getAccessor.getParameters().add(getParameter);

        final ParameterDeclaration setParameter1 = new ParameterDeclaration(
            Pattern.ANY_STRING,
            new AnyNode("targetType").toType()
        );

        final ParameterDeclaration setParameter2 = new ParameterDeclaration(
            Pattern.ANY_STRING,
            new BackReference("returnType").toType()
        );

        setParameter1.setAnyModifiers(true);
        setParameter2.setAnyModifiers(true);

        setAccessor.getParameters().add(setParameter1);
        setAccessor.getParameters().add(new OptionalNode(setParameter2).toParameterDeclaration());

        getAccessor.setBody(
            new BlockStatement(
                new ReturnStatement(
                    Expression.MYSTERY_OFFSET,
                    new SubtreeMatch(
                        new MemberReferenceTypeNode(
                            new MemberReferenceExpression(
                                Expression.MYSTERY_OFFSET,
                                new ParameterReferenceNode(0).toExpression(),
                                Pattern.ANY_STRING
                            ),
                            FieldReference.class
                        )
                    ).toExpression()
                )
            )
        );

        final MethodDeclaration altSetAccessor = (MethodDeclaration) setAccessor.clone();

        setAccessor.setBody(
            new Choice(
                new BlockStatement(
                    new ExpressionStatement(
                        new AssignmentExpression(
                            new MemberReferenceTypeNode(
                                new MemberReferenceExpression(
                                    Expression.MYSTERY_OFFSET,
                                    new ParameterReferenceNode(0).toExpression(),
                                    Pattern.ANY_STRING
                                ),
                                FieldReference.class
                            ).toExpression(),
                            AssignmentOperatorType.ANY,
                            new ParameterReferenceNode(1, "value").toExpression()
                        )
                    ),
                    new ReturnStatement(Expression.MYSTERY_OFFSET, new BackReference("value").toExpression())
                ),
                new BlockStatement(
                    new ReturnStatement(
                        Expression.MYSTERY_OFFSET,
                        new AssignmentExpression(
                            new MemberReferenceTypeNode(
                                new MemberReferenceExpression(
                                    Expression.MYSTERY_OFFSET,
                                    new ParameterReferenceNode(0).toExpression(),
                                    Pattern.ANY_STRING
                                ),
                                FieldReference.class
                            ).toExpression(),
                            AssignmentOperatorType.ANY,
                            new ParameterReferenceNode(1, "value").toExpression()
                        )
                    )
                )
            ).toBlockStatement()
        );

        final VariableDeclarationStatement tempVariable = new VariableDeclarationStatement(
            new AnyNode().toType(),
            Pattern.ANY_STRING,
            new AnyNode("value").toExpression()
        );

        tempVariable.addModifier(Modifier.FINAL);

        altSetAccessor.setBody(
            new BlockStatement(
                new NamedNode("tempVariable", tempVariable).toStatement(),
                new ExpressionStatement(
                    new AssignmentExpression(
                        new MemberReferenceTypeNode(
                            new MemberReferenceExpression(
                                Expression.MYSTERY_OFFSET,
                                new ParameterReferenceNode(0).toExpression(),
                                Pattern.ANY_STRING
                            ),
                            FieldReference.class
                        ).toExpression(),
                        AssignmentOperatorType.ANY,
                        new SubtreeMatch(new DeclaredVariableBackReference("tempVariable")).toExpression()
                    )
                ),
                new ReturnStatement(Expression.MYSTERY_OFFSET, new DeclaredVariableBackReference("tempVariable").toExpression())
            )
        );

        SYNTHETIC_GET_ACCESSOR = getAccessor;
        SYNTHETIC_SET_ACCESSOR = setAccessor;
        SYNTHETIC_SET_ACCESSOR_ALT = altSetAccessor;

        final MethodDeclaration staticGetAccessor = (MethodDeclaration) getAccessor.clone();
        final MethodDeclaration staticSetAccessor = (MethodDeclaration) setAccessor.clone();
        final MethodDeclaration altStaticSetAccessor = (MethodDeclaration) altSetAccessor.clone();

        staticGetAccessor.getParameters().clear();

        staticGetAccessor.setBody(
            new BlockStatement(
                new ReturnStatement(Expression.MYSTERY_OFFSET,
                    new SubtreeMatch(
                        new MemberReferenceTypeNode(
                            new MemberReferenceExpression(
                                Expression.MYSTERY_OFFSET,
                                new TypedNode(TypeReferenceExpression.class).toExpression(),
                                Pattern.ANY_STRING
                            ),
                            FieldReference.class
                        )
                    ).toExpression()
                )
            )
        );

        staticSetAccessor.getParameters().firstOrNullObject().remove();

        staticSetAccessor.setBody(
            new Choice(
                new BlockStatement(
                    new ExpressionStatement(
                        new AssignmentExpression(
                            new MemberReferenceTypeNode(
                                new MemberReferenceExpression(
                                    Expression.MYSTERY_OFFSET,
                                    new TypedNode(TypeReferenceExpression.class).toExpression(),
                                    Pattern.ANY_STRING
                                ),
                                FieldReference.class
                            ).toExpression(),
                            AssignmentOperatorType.ANY,
                            new NamedNode("value", new SubtreeMatch(new ParameterReferenceNode(0))).toExpression()
                        )
                    ),
                    new ReturnStatement(Expression.MYSTERY_OFFSET, new BackReference("value").toExpression())
                ),
                new BlockStatement(
                    new ReturnStatement(Expression.MYSTERY_OFFSET,
                        new AssignmentExpression(
                            new MemberReferenceTypeNode(
                                new MemberReferenceExpression(
                                    Expression.MYSTERY_OFFSET,
                                    new TypedNode(TypeReferenceExpression.class).toExpression(),
                                    Pattern.ANY_STRING
                                ),
                                FieldReference.class
                            ).toExpression(),
                            AssignmentOperatorType.ANY,
                            new NamedNode("value", new SubtreeMatch(new ParameterReferenceNode(0))).toExpression()
                        )
                    )
                )
            ).toBlockStatement()
        );

        altStaticSetAccessor.getParameters().firstOrNullObject().remove();

        altStaticSetAccessor.setBody(
            new BlockStatement(
                new NamedNode("tempVariable", tempVariable).toStatement(),
                new ExpressionStatement(
                    new AssignmentExpression(
                        new MemberReferenceTypeNode(
                            new MemberReferenceExpression(
                                Expression.MYSTERY_OFFSET,
                                new TypedNode(TypeReferenceExpression.class).toExpression(),
                                Pattern.ANY_STRING
                            ),
                            FieldReference.class
                        ).toExpression(),
                        AssignmentOperatorType.ANY,
                        new SubtreeMatch(new DeclaredVariableBackReference("tempVariable")).toExpression()
                    )
                ),
                new ReturnStatement(Expression.MYSTERY_OFFSET, new DeclaredVariableBackReference("tempVariable").toExpression())
            )
        );

        SYNTHETIC_STATIC_GET_ACCESSOR = staticGetAccessor;
        SYNTHETIC_STATIC_SET_ACCESSOR = staticSetAccessor;
        SYNTHETIC_STATIC_SET_ACCESSOR_ALT = altStaticSetAccessor;
    }

    private class PhaseOneVisitor extends ContextTrackingVisitor<Void> {
        private PhaseOneVisitor() {
            super(EliminateSyntheticAccessorsTransform.this.context);
        }

        @Override
        public Void visitTypeDeclaration(final TypeDeclaration node, final Void _) {
            final TypeDefinition type = node.getUserData(Keys.TYPE_DEFINITION);

            if (type != null) {
                if (!_visitedTypes.add(type.getInternalName())) {
                    return null;
                }
            }

            return super.visitTypeDeclaration(node, _);
        }

        @Override
        public Void visitMethodDeclaration(final MethodDeclaration node, final Void _) {
            final MethodDefinition method = node.getUserData(Keys.METHOD_DEFINITION);

            if (method != null) {
                if (method.isSynthetic() && method.isStatic()) {
                    if (tryMatchAccessor(node) || tryMatchCallWrapper(node)) {
                        _accessMethodDeclarations.put(makeMethodKey(method), node);
                    }
                }
            }

            return super.visitMethodDeclaration(node, _);
        }

        private boolean tryMatchAccessor(final MethodDeclaration node) {
            if (SYNTHETIC_GET_ACCESSOR.matches(node) ||
                SYNTHETIC_SET_ACCESSOR.matches(node) ||
                SYNTHETIC_SET_ACCESSOR_ALT.matches(node) ||
                SYNTHETIC_STATIC_GET_ACCESSOR.matches(node) ||
                SYNTHETIC_STATIC_SET_ACCESSOR.matches(node) ||
                SYNTHETIC_STATIC_SET_ACCESSOR_ALT.matches(node)) {

                return true;
            }

            return false;
        }

        private boolean tryMatchCallWrapper(final MethodDeclaration node) {
            final AstNodeCollection<Statement> statements = node.getBody().getStatements();

            if (!statements.hasSingleElement()) {
                return false;
            }

            final Statement s = statements.firstOrNullObject();
            final InvocationExpression invocation;

            if (s instanceof ExpressionStatement) {
                final ExpressionStatement e = (ExpressionStatement) s;

                invocation = e.getExpression() instanceof InvocationExpression ? (InvocationExpression) e.getExpression()
                                                                               : null;
            }
            else if (s instanceof ReturnStatement) {
                final ReturnStatement r = (ReturnStatement) s;

                invocation = r.getExpression() instanceof InvocationExpression ? (InvocationExpression) r.getExpression()
                                                                               : null;
            }
            else {
                invocation = null;
            }

            if (invocation == null) {
                return false;
            }

            final MethodReference targetMethod = (MethodReference) invocation.getUserData(Keys.MEMBER_REFERENCE);
            final MethodDefinition resolvedTarget = targetMethod != null ? targetMethod.resolve() : null;

            if (resolvedTarget == null) {
                return false;
            }

            final int parametersStart = resolvedTarget.isStatic() ? 0 : 1;
            final List<ParameterDeclaration> parameterList = toList(node.getParameters());
            final List<Expression> argumentList = toList(invocation.getArguments());

            if (argumentList.size() != parameterList.size() - parametersStart) {
                return false;
            }

            if (!resolvedTarget.isStatic()) {
                if (!(invocation.getTarget() instanceof MemberReferenceExpression)) {
                    return false;
                }

                final MemberReferenceExpression m = (MemberReferenceExpression) invocation.getTarget();
                final Expression target = m.getTarget();

                if (!target.matches(new IdentifierExpression( Expression.MYSTERY_OFFSET, parameterList.get(0).getName()))) {
                    return false;
                }
            }

            int i, j;

            for (i = parametersStart, j = 0;
                 i < parameterList.size() && j < argumentList.size();
                 i++, j++)

            {
                if (!argumentList.get(j).matches(new IdentifierExpression( Expression.MYSTERY_OFFSET, parameterList.get(i).getName()))) {
                    return false;
                }
            }

            return i == j + parametersStart;
        }
    }

    // </editor-fold>
}
