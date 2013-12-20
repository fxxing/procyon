/*
 * BootstrapMethodsAttribute.java
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

package com.strobel.assembler.ir.attributes;

import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;

import java.util.Collections;
import java.util.List;

public final class BootstrapMethodsAttribute extends SourceAttribute {
    private final List<BootstrapMethodsTableEntry> _bootstrapMethods;

    public BootstrapMethodsAttribute(final List<BootstrapMethodsTableEntry> bootstrapMethods) {
        this(
            VerifyArgument.notNull(bootstrapMethods, "bootstrapMethods")
                          .toArray(new BootstrapMethodsTableEntry[bootstrapMethods.size()])
        );
    }

    public BootstrapMethodsAttribute(final BootstrapMethodsTableEntry... bootstrapMethods) {
        super(AttributeNames.BootstrapMethods, computeSize(bootstrapMethods));
        _bootstrapMethods = ArrayUtilities.isNullOrEmpty(bootstrapMethods) ? Collections.<BootstrapMethodsTableEntry>emptyList()
                                                                           : ArrayUtilities.asUnmodifiableList(bootstrapMethods);
    }

    public final List<BootstrapMethodsTableEntry> getBootstrapMethods() {
        return _bootstrapMethods;
    }

    private static int computeSize(final BootstrapMethodsTableEntry[] bootstrapMethods) {
        int size = 2;

        if (bootstrapMethods == null) {
            return size;
        }

        for (final BootstrapMethodsTableEntry bootstrapMethod : bootstrapMethods) {
            size += (2 + (2 * bootstrapMethod.getArguments().size()));
        }

        return size;
    }
}
