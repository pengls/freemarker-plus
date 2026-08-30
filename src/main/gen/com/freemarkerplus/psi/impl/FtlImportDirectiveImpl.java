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

public class FtlImportDirectiveImpl extends ASTWrapperPsiElement implements FtlImportDirective {

  public FtlImportDirectiveImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull FtlVisitor visitor) {
    visitor.visitImportDirective(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof FtlVisitor) accept((FtlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public FtlIdentifier getIdentifier() {
    return findNotNullChildByClass(FtlIdentifier.class);
  }

  @Override
  @NotNull
  public FtlStringLiteral getStringLiteral() {
    return findNotNullChildByClass(FtlStringLiteral.class);
  }

}
