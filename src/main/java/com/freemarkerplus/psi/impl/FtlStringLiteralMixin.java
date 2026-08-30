package com.freemarkerplus.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.ContributedReferenceHost;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Grammar-Kit mixin for {@code string_literal}. Implements {@link ContributedReferenceHost}
 * so that reference providers registered through {@code psi.referenceContributor}
 * (see {@code com.freemarkerplus.reference.FtlReferenceContributor}) are consulted for
 * string literals in {@code <#include>}/{@code <#import>} directives.
 */
public abstract class FtlStringLiteralMixin extends ASTWrapperPsiElement implements ContributedReferenceHost {

  public FtlStringLiteralMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public PsiReference @NotNull [] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this);
  }
}
