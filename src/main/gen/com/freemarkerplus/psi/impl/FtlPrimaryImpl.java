// This is a generated file. Not intended for manual editing.
package com.freemarkerplus.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.freemarkerplus.psi.FtlElementTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.freemarkerplus.psi.*;

public class FtlPrimaryImpl extends ASTWrapperPsiElement implements FtlPrimary {

  public FtlPrimaryImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull FtlVisitor visitor) {
    visitor.visitPrimary(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof FtlVisitor) accept((FtlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public FtlExpression getExpression() {
    return findChildByClass(FtlExpression.class);
  }

  @Override
  @Nullable
  public FtlIdentifier getIdentifier() {
    return findChildByClass(FtlIdentifier.class);
  }

  @Override
  @Nullable
  public FtlStringLiteral getStringLiteral() {
    return findChildByClass(FtlStringLiteral.class);
  }

  @Override
  @Nullable
  public PsiElement getNumber() {
    return findChildByType(NUMBER);
  }

}
