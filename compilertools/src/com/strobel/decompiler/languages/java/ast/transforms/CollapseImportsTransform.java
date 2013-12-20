/*
 * CollapseImportsTransform.java
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

import com.strobel.assembler.metadata.PackageReference;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.AstNodeCollection;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import com.strobel.decompiler.languages.java.ast.Identifier;
import com.strobel.decompiler.languages.java.ast.ImportDeclaration;
import com.strobel.decompiler.languages.java.ast.Keys;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CollapseImportsTransform implements IAstTransform {
    private final DecompilerSettings _settings;

    public CollapseImportsTransform(final DecompilerContext context) {
        _settings = context.getSettings();
    }

    @Override
    public void run(final AstNode root) {
        if (!(root instanceof CompilationUnit) || _settings.getForceExplicitImports()) {
            return;
        }

        final CompilationUnit compilationUnit = (CompilationUnit) root;
        final AstNodeCollection<ImportDeclaration> imports = compilationUnit.getImports();

        if (imports.isEmpty())
            return;

        final Set<String> newImports = new LinkedHashSet<>();
        final List<ImportDeclaration> removedImports = new ArrayList<>();

        for (final ImportDeclaration oldImport : imports) {
            final Identifier importedType = oldImport.getImportIdentifier();

            if (importedType != null && !importedType.isNull()) {
                final TypeReference type = oldImport.getUserData(Keys.TYPE_REFERENCE);

                if (type != null) {
                    final String packageName = type.getPackageName();

                    if (!StringUtilities.isNullOrEmpty(packageName)) {
                        newImports.add(packageName);
                    }

                    removedImports.add(oldImport);
                }
            }
        }

        if (removedImports.isEmpty()) {
            return;
        }

        final ImportDeclaration lastRemoved = removedImports.get(removedImports.size() - 1);

        for (final String packageName : newImports) {
            compilationUnit.insertChildAfter(
                lastRemoved,
                new ImportDeclaration(PackageReference.parse(packageName)),
                CompilationUnit.IMPORT_ROLE
            );
        }

        for (final ImportDeclaration removedImport : removedImports) {
            removedImport.remove();
        }
    }
}
