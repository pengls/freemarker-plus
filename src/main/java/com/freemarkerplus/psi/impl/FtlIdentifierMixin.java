package com.freemarkerplus.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.ContributedReferenceHost;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Grammar-Kit mixin for {@code identifier}. Implements {@link ContributedReferenceHost}
 * so that reference providers registered through {@code psi.referenceContributor}
 * (see {@code com.freemarkerplus.reference.FtlReferenceContributor}) are consulted for
 * identifiers — specifically the name in a macro call ({@code <@name>}), which resolves
 * to the same-file {@code <#macro name>}/{@code <#function name>} declaration.
 */
public abstract class FtlIdentifierMixin extends ASTWrapperPsiElement implements ContributedReferenceHost {

  public FtlIdentifierMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public PsiReference @NotNull [] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this);
  }
}
