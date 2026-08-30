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

public class FtlOuterElementImpl extends ASTWrapperPsiElement implements FtlOuterElement {

  public FtlOuterElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull FtlVisitor visitor) {
    visitor.visitOuterElement(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof FtlVisitor) accept((FtlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public FtlComment getComment() {
    return findChildByClass(FtlComment.class);
  }

  @Override
  @Nullable
  public FtlAssignDirective getAssignDirective() {
    return findChildByClass(FtlAssignDirective.class);
  }

  @Override
  @Nullable
  public FtlFunctionDirective getFunctionDirective() {
    return findChildByClass(FtlFunctionDirective.class);
  }

  @Override
  @Nullable
  public FtlGenericDirective getGenericDirective() {
    return findChildByClass(FtlGenericDirective.class);
  }

  @Override
  @Nullable
  public FtlImportDirective getImportDirective() {
    return findChildByClass(FtlImportDirective.class);
  }

  @Override
  @Nullable
  public FtlIncludeDirective getIncludeDirective() {
    return findChildByClass(FtlIncludeDirective.class);
  }

  @Override
  @Nullable
  public FtlInterpolation getInterpolation() {
    return findChildByClass(FtlInterpolation.class);
  }

  @Override
  @Nullable
  public FtlListDirective getListDirective() {
    return findChildByClass(FtlListDirective.class);
  }

  @Override
  @Nullable
  public FtlMacroCall getMacroCall() {
    return findChildByClass(FtlMacroCall.class);
  }

  @Override
  @Nullable
  public FtlMacroDirective getMacroDirective() {
    return findChildByClass(FtlMacroDirective.class);
  }

  @Override
  @Nullable
  public PsiElement getTemplateData() {
    return findChildByType(TEMPLATE_DATA);
  }

}
