// This is a generated file. Not intended for manual editing.
package com.freemarkerplus.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface FtlOuterElement extends PsiElement {

  @Nullable
  FtlComment getComment();

  @Nullable
  FtlAssignDirective getAssignDirective();

  @Nullable
  FtlFunctionDirective getFunctionDirective();

  @Nullable
  FtlGenericDirective getGenericDirective();

  @Nullable
  FtlImportDirective getImportDirective();

  @Nullable
  FtlIncludeDirective getIncludeDirective();

  @Nullable
  FtlInterpolation getInterpolation();

  @Nullable
  FtlListDirective getListDirective();

  @Nullable
  FtlMacroCall getMacroCall();

  @Nullable
  FtlMacroDirective getMacroDirective();

  @Nullable
  PsiElement getTemplateData();

}
