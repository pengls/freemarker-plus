package com.freemarkerplus.psi.impl;

import com.freemarkerplus.psi.FtlIdentifier;
import com.freemarkerplus.psi.FtlPsiUtil;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.ContributedReferenceHost;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * Grammar-Kit mixin for {@code identifier}.
 *
 * <p>Implements {@link ContributedReferenceHost} so that reference providers registered
 * through {@code psi.referenceContributor} (see
 * {@code com.freemarkerplus.reference.FtlReferenceContributor}) are consulted for
 * identifiers — the name in a macro call ({@code <@name>}), expression root segments, etc.
 *
 * <p>Implements {@link PsiNameIdentifierOwner} so that an identifier is a renameable name
 * target. In particular the name of a {@code <#macro>}/{@code <#function>}/{@code <#assign>}/
 * {@code <#list>} declaration is reachable through
 * {@code com.intellij.codeInsight.TargetElementUtil} (either directly under the caret or via a
 * usage reference resolving to it), and the platform rename refactoring (Shift+F6) rewrites it
 * through {@link #setName(String)}.
 */
public abstract class FtlIdentifierMixin extends ASTWrapperPsiElement
    implements ContributedReferenceHost, PsiNameIdentifierOwner {

  public FtlIdentifierMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public PsiReference @NotNull [] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this);
  }

  @Override
  public @NotNull String getName() {
    return getText();
  }

  @Override
  public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
    FtlIdentifier replacement = FtlPsiUtil.INSTANCE.createIdentifier(getProject(), name);
    return replacement != null ? replace(replacement) : this;
  }

  @Override
  public PsiElement getNameIdentifier() {
    return this;
  }
}
