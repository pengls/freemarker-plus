package com.intellij.psi;

import com.intellij.diagnostic.PluginException;
import com.intellij.lang.FileASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.PsiDocumentManagerBase;
import com.intellij.psi.impl.SharedPsiElementImplUtil;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.annotations.Unmodifiable;

public abstract class MultiplePsiFilesPerDocumentFileViewProvider extends AbstractFileViewProvider {
   protected final ConcurrentMap<Language, PsiFileImpl> myRoots = new ConcurrentHashMap(1, 0.75F, 1);
   private MultiplePsiFilesPerDocumentFileViewProvider myOriginal;

   public MultiplePsiFilesPerDocumentFileViewProvider(@NotNull PsiManager manager, @NotNull VirtualFile virtualFile, boolean eventSystemEnabled) {
      super(manager, virtualFile, eventSystemEnabled);
   }

   public abstract @NotNull Language getBaseLanguage();

   @NotNull
   public List<PsiFile> getAllFiles() {
      List<PsiFile> roots = new ArrayList();

      for(Language language : this.getLanguages()) {
         PsiFile psi = this.getPsi(language);
         if (psi != null) {
            roots.add(psi);
         }
      }

      Language baseLanguage = this.getBaseLanguage();
      PsiFile base = this.getPsi(baseLanguage);
      if (!roots.isEmpty() && roots.get(0) != base && base != null) {
         roots.remove(base);
         roots.add(0, base);
      }

      return roots;
   }

   protected final void removeFile(@NotNull Language language) {
      PsiFileImpl file = (PsiFileImpl)this.myRoots.remove(language);
      if (file != null) {
         file.markInvalidated();
      }

   }

   protected PsiFile getPsiInner(@NotNull Language target) {
      PsiFileImpl file = (PsiFileImpl)this.myRoots.get(target);
      if (file == null) {
         if (!this.shouldCreatePsi()) {
            return null;
         }

         if (target != this.getBaseLanguage() && !this.getLanguages().contains(target)) {
            return null;
         }

         file = this.createPsiFileImpl(target);
         if (file == null) {
            return null;
         }

         if (file.getLanguage() != target) {
            throw PluginException.createByClass(new IllegalStateException("Inconsistent view provider implementation: " + this + " (" + this.getClass() + "). Its createPsiFileImpl('" + target + "') returned " + file + "(" + file.getClass() + ") with unexpected getLanguage()='" + file.getLanguage() + "'"), this.getClass());
         }

         if (this.myOriginal != null) {
            PsiFile originalFile = this.myOriginal.getPsi(target);
            if (originalFile != null) {
               file.setOriginalFile(originalFile);
            }
         }

         file = (PsiFileImpl)ConcurrencyUtil.cacheOrGet(this.myRoots, target, file);
      }

      return file;
   }

   protected @Nullable PsiFileImpl createPsiFileImpl(@NotNull Language target) {
      return (PsiFileImpl)this.createFile(target);
   }

   public final @Nullable PsiFile getCachedPsi(@NotNull Language target) {
      return (PsiFile)this.myRoots.get(target);
   }

   public final @Unmodifiable @NotNull List<PsiFile> getCachedPsiFiles() {
      return ContainerUtil.mapNotNull(this.myRoots.keySet(), this::getCachedPsi);
   }

   public final @NotNull List<FileASTNode> getKnownTreeRoots() {
      List<FileASTNode> files = new ArrayList(this.myRoots.size());

      for(PsiFileImpl file : this.myRoots.values()) {
         FileASTNode treeElement = file.getNodeIfLoaded();
         if (treeElement != null) {
            files.add(treeElement);
         }
      }

      return files;
   }

   @TestOnly
   public void checkAllTreesEqual() {
      Collection<PsiFileImpl> roots = this.myRoots.values();
      PsiDocumentManager documentManager = PsiDocumentManager.getInstance(this.getManager().getProject());
      documentManager.commitAllDocuments();

      for(PsiFile root : roots) {
         Document document = documentManager.getDocument(root);

         assert document != null;

         PsiDocumentManagerBase.checkConsistency(root, document);

         assert root.getText().equals(document.getText());
      }

   }

   public final @NotNull MultiplePsiFilesPerDocumentFileViewProvider createCopy(@NotNull VirtualFile fileCopy) {
      MultiplePsiFilesPerDocumentFileViewProvider copy = this.cloneInner(fileCopy);
      copy.myOriginal = this.myOriginal == null ? this : this.myOriginal;
      return copy;
   }

   protected abstract @NotNull MultiplePsiFilesPerDocumentFileViewProvider cloneInner(@NotNull VirtualFile var1);

   public @Nullable PsiElement findElementAt(int offset, @NotNull Class<? extends Language> lang) {
      PsiFile mainRoot = this.getPsi(this.getBaseLanguage());
      PsiElement ret = null;

      for(Language language : this.getLanguages()) {
         if (ReflectionUtil.isAssignable(lang, language.getClass()) && (!lang.equals(Language.class) || this.getLanguages().contains(language))) {
            PsiFile psiRoot = this.getPsi(language);
            PsiElement psiElement = findElementAt(psiRoot, offset);
            if (psiElement != null && !(psiElement instanceof OuterLanguageElement) && (ret == null || psiRoot != mainRoot)) {
               ret = psiElement;
            }
         }
      }

      return ret;
   }

   public @Nullable PsiElement findElementAt(int offset) {
      return this.findElementAt(offset, Language.class);
   }

   public @Nullable PsiReference findReferenceAt(int offset) {
      TextRange minRange = new TextRange(0, this.getContents().length());
      PsiReference ret = null;

      for(Language language : this.getLanguages()) {
         PsiElement psiRoot = this.getPsi(language);
         PsiReference reference = SharedPsiElementImplUtil.findReferenceAt(psiRoot, offset, language);
         if (reference != null) {
            TextRange textRange = reference.getRangeInElement().shiftRight(reference.getElement().getTextRange().getStartOffset());
            if (minRange.contains(textRange) && (!textRange.contains(minRange) || ret == null)) {
               minRange = textRange;
               ret = reference;
            }
         }
      }

      return ret;
   }

   public void contentsSynchronized() {
      Set<Language> languages = this.getLanguages();
      Iterator<Map.Entry<Language, PsiFileImpl>> iterator = this.myRoots.entrySet().iterator();

      while(iterator.hasNext()) {
         Map.Entry<Language, PsiFileImpl> entry = (Map.Entry)iterator.next();
         if (!languages.contains(entry.getKey())) {
            PsiFileImpl file = (PsiFileImpl)entry.getValue();
            iterator.remove();
            DebugUtil.performPsiModification(this.getClass().getName() + " root change", () -> file.markInvalidated());
         }
      }

      super.contentsSynchronized();
   }

   // $FF: synthetic method
   private static void $$$reportNull$$$0(int var0) {
      String var10000;
      switch (var0) {
         case 0:
         case 1:
         case 3:
         case 4:
         case 5:
         case 6:
         case 9:
         case 11:
         default:
            var10000 = "Argument for @NotNull parameter '%s' of %s.%s must not be null";
            break;
         case 2:
         case 7:
         case 8:
         case 10:
            var10000 = "@NotNull method %s.%s must not return null";
      }

      byte var10001;
      switch (var0) {
         case 0:
         case 1:
         case 3:
         case 4:
         case 5:
         case 6:
         case 9:
         case 11:
         default:
            var10001 = 3;
            break;
         case 2:
         case 7:
         case 8:
         case 10:
            var10001 = 2;
      }

      String var3 = new Object[var10001];
      switch (var0) {
         case 0:
         default:
            ((Object[])var3)[0] = "manager";
            break;
         case 1:
            ((Object[])var3)[0] = "virtualFile";
            break;
         case 2:
         case 7:
         case 8:
         case 10:
            ((Object[])var3)[0] = "com/intellij/psi/MultiplePsiFilesPerDocumentFileViewProvider";
            break;
         case 3:
            ((Object[])var3)[0] = "language";
            break;
         case 4:
         case 5:
         case 6:
            ((Object[])var3)[0] = "target";
            break;
         case 9:
            ((Object[])var3)[0] = "fileCopy";
            break;
         case 11:
            ((Object[])var3)[0] = "lang";
      }

      switch (var0) {
         case 0:
         case 1:
         case 3:
         case 4:
         case 5:
         case 6:
         case 9:
         case 11:
         default:
            ((Object[])var3)[1] = "com/intellij/psi/MultiplePsiFilesPerDocumentFileViewProvider";
            break;
         case 2:
            ((Object[])var3)[1] = "getAllFiles";
            break;
         case 7:
            ((Object[])var3)[1] = "getCachedPsiFiles";
            break;
         case 8:
            ((Object[])var3)[1] = "getKnownTreeRoots";
            break;
         case 10:
            ((Object[])var3)[1] = "createCopy";
      }

      switch (var0) {
         case 0:
         case 1:
         default:
            ((Object[])var3)[2] = "<init>";
         case 2:
         case 7:
         case 8:
         case 10:
            break;
         case 3:
            ((Object[])var3)[2] = "removeFile";
            break;
         case 4:
            ((Object[])var3)[2] = "getPsiInner";
            break;
         case 5:
            ((Object[])var3)[2] = "createPsiFileImpl";
            break;
         case 6:
            ((Object[])var3)[2] = "getCachedPsi";
            break;
         case 9:
            ((Object[])var3)[2] = "createCopy";
            break;
         case 11:
            ((Object[])var3)[2] = "findElementAt";
      }

      var10000 = String.format(var10000, var3);
      Object var2;
      switch (var0) {
         case 0:
         case 1:
         case 3:
         case 4:
         case 5:
         case 6:
         case 9:
         case 11:
         default:
            IllegalArgumentException var6 = new IllegalArgumentException;
            var3 = var10000;
            var2 = var6;
            var6.<init>(var3);
            break;
         case 2:
         case 7:
         case 8:
         case 10:
            IllegalStateException var10002 = new IllegalStateException;
            var3 = var10000;
            var2 = var10002;
            var10002.<init>(var3);
      }

      throw var2;
   }
}
