// This is a generated file. Not intended for manual editing.
package com.freemarkerplus.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface FtlExpression extends PsiElement {

  @NotNull
  List<FtlExpression> getExpressionList();

  @NotNull
  List<FtlIdentifier> getIdentifierList();

  @NotNull
  List<FtlPrimary> getPrimaryList();

}
