package com.intellij.psi;

import com.intellij.codeInsight.multiverse.CodeInsightContextUtil;
import com.intellij.codeInsight.multiverse.CodeInsightContexts;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.lang.ASTNode;
import com.intellij.lang.FileASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.NonPhysicalFileSystem;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.psi.impl.FreeThreadedFileViewProvider;
import com.intellij.psi.impl.PsiDocumentManagerEx;
import com.intellij.psi.impl.PsiFileEx;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.PsiTreeChangeEventImpl;
import com.intellij.psi.impl.file.PsiBinaryFileImpl;
import com.intellij.psi.impl.file.PsiLargeBinaryFileImpl;
import com.intellij.psi.impl.file.PsiLargeTextFileImpl;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.psi.impl.file.impl.PossibleInvalidationKt;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.PsiPlainTextFileImpl;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.reference.SoftReference;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.LocalTimeCounter;
import com.intellij.util.containers.CollectionFactory;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBTreeTraverser;
import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class AbstractFileViewProvider extends UserDataHolderBase implements FileViewProvider {
   private static final Logger LOG = Logger.getInstance(AbstractFileViewProvider.class);
   public static final Key<Object> FREE_THREADED = Key.create("FREE_THREADED");
   private static final Key<Set<AbstractFileViewProvider>> KNOWN_COPIES = Key.create("KNOWN_COPIES");
   private final @NotNull PsiManagerEx myManager;
   private final @NotNull VirtualFile myVirtualFile;
   private final boolean myEventSystemEnabled;
   private final boolean myPhysical;
   private volatile Content myContent;
   private volatile Reference<Document> myDocument;
   private final PsiLock myPsiLock = new PsiLock();

   protected AbstractFileViewProvider(@NotNull PsiManager manager, @NotNull VirtualFile virtualFile, boolean eventSystemEnabled) {
      this.myManager = (PsiManagerEx)manager;
      this.myVirtualFile = virtualFile;
      this.myEventSystemEnabled = eventSystemEnabled;
      this.setContent(new VirtualFileContent(this, (1)null));
      this.myPhysical = eventSystemEnabled && !(virtualFile instanceof LightVirtualFile) && !(virtualFile.getFileSystem() instanceof NonPhysicalFileSystem);
      virtualFile.putUserData(FREE_THREADED, isFreeThreaded(this));
      if (virtualFile instanceof VirtualFileWindow && !(this instanceof FreeThreadedFileViewProvider) && !isFreeThreaded(this)) {
         throw new IllegalArgumentException("Must not create " + this.getClass() + " for injected file " + virtualFile + "; InjectedFileViewProvider must be used instead");
      }
   }

   protected boolean shouldCreatePsi() {
      if (this.isIgnored()) {
         return false;
      } else {
         VirtualFile vFile = this.getVirtualFile();
         if (this.isPhysical() && vFile.isInLocalFileSystem()) {
            VirtualFile parent = vFile.getParent();
            if (parent == null) {
               return false;
            }

            PsiDirectory psiDir = this.getManager().findDirectory(parent);
            if (psiDir == null) {
               FileIndexFacade indexFacade = FileIndexFacade.getInstance(this.getManager().getProject());
               if (!indexFacade.isInLibrarySource(vFile) && !indexFacade.isInLibraryClasses(vFile)) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   public static boolean isFreeThreaded(@NotNull FileViewProvider provider) {
      return provider.getVirtualFile() instanceof LightVirtualFile && !provider.isEventSystemEnabled();
   }

   public @NotNull PsiLock getFilePsiLock() {
      return this.myPsiLock;
   }

   protected final boolean isIgnored() {
      VirtualFile file = this.getVirtualFile();
      return !(file instanceof LightVirtualFile) && FileTypeRegistry.getInstance().isFileIgnored(file);
   }

   protected @Nullable PsiFile createFile(@NotNull Project project, @NotNull VirtualFile file, @NotNull FileType fileType) {
      if (fileType == null) {
         $$$reportNull$$$0(6);
      }

      return this.createFile(file, fileType, this.getBaseLanguage());
   }

   protected @NotNull PsiFile createFile(@NotNull VirtualFile file, @NotNull FileType fileType, @NotNull Language language) {
      if (language == null) {
         $$$reportNull$$$0(9);
      }

      if (!fileType.isBinary() && !file.is(VFileProperty.SPECIAL)) {
         if (!SingleRootFileViewProvider.isTooLargeForIntelligence(file)) {
            PsiFile psiFile = this.createFile(language);
            if (psiFile != null) {
               return psiFile;
            }
         }

         return (PsiFile)(SingleRootFileViewProvider.isTooLargeForContentLoading(file) ? new PsiLargeTextFileImpl(this) : new PsiPlainTextFileImpl(this));
      } else {
         return (PsiFile)(SingleRootFileViewProvider.isTooLargeForContentLoading(file) ? new PsiLargeBinaryFileImpl(this.getManager(), this) : new PsiBinaryFileImpl(this.getManager(), this));
      }
   }

   protected @Nullable PsiFile createFile(@NotNull Language lang) {
      if (lang != this.getBaseLanguage()) {
         return null;
      } else {
         ParserDefinition parserDefinition = (ParserDefinition)LanguageParserDefinitions.INSTANCE.forLanguage(lang);
         return parserDefinition != null ? parserDefinition.createFile(this) : null;
      }
   }

   public final @NotNull PsiManagerEx getManager() {
      return this.myManager;
   }

   public @NotNull CharSequence getContents() {
      return this.getContent().getText();
   }

   public @NotNull VirtualFile getVirtualFile() {
      return this.myVirtualFile;
   }

   @Nullable Document getCachedDocument() {
      Document document = (Document)SoftReference.dereference(this.myDocument);
      return document != null ? document : FileDocumentManager.getInstance().getCachedDocument(this.getVirtualFile());
   }

   public Document getDocument() {
      Document document = (Document)SoftReference.dereference(this.myDocument);
      if (document == null) {
         VirtualFile file = this.getVirtualFile();
         document = FileDocumentManager.getInstance().getDocument(file, this.myManager.getProject());
         this.myDocument = document == null ? null : new java.lang.ref.SoftReference(document);
      }

      return document;
   }

   public final @Nullable PsiFile getPsi(@NotNull Language target) {
      if (!this.isPhysical()) {
         FileManager fileManager = this.getManager().getFileManager();
         VirtualFile virtualFile = this.getVirtualFile();
         if (fileManager.findCachedViewProvider(virtualFile) == null && this.getCachedPsiFiles().isEmpty()) {
            fileManager.setViewProvider(virtualFile, this);
         }
      }

      return this.getPsiInner(target);
   }

   protected abstract @Nullable PsiFile getPsiInner(@NotNull Language var1);

   public FileViewProvider clone() {
      VirtualFile origFile = this.getVirtualFile();
      LightVirtualFile copy = new LightVirtualFile(origFile.getName(), origFile.getFileType(), this.getContents(), origFile.getCharset(), this.getModificationStamp());
      origFile.copyCopyableDataTo(copy);
      copy.setOriginalFile(origFile);
      UndoUtil.disableUndoFor(copy);
      copy.setCharset(origFile.getCharset());
      return this.createCopy(copy);
   }

   public PsiElement findElementAt(int offset, @NotNull Language language) {
      PsiFile psiFile = this.getPsi(language);
      return psiFile != null ? findElementAt(psiFile, offset) : null;
   }

   public @Nullable PsiReference findReferenceAt(int offset, @NotNull Language language) {
      PsiFile psiFile = this.getPsi(language);
      return psiFile != null ? findReferenceAt(psiFile, offset) : null;
   }

   protected static @Nullable PsiReference findReferenceAt(@Nullable PsiFile psiFile, int offset) {
      if (psiFile == null) {
         return null;
      } else {
         int offsetInElement = offset;

         for(PsiElement child = psiFile.getFirstChild(); child != null; child = child.getNextSibling()) {
            int length = child.getTextLength();
            if (length > offsetInElement) {
               return child.findReferenceAt(offsetInElement);
            }

            offsetInElement -= length;
         }

         return null;
      }
   }

   public static @Nullable PsiElement findElementAt(@Nullable PsiElement psiFile, int offset) {
      ASTNode node = psiFile == null ? null : psiFile.getNode();
      return node == null ? null : SourceTreeToPsiMap.treeElementToPsi(node.findLeafElementAt(offset));
   }

   public void beforeContentsSynchronized() {
   }

   public void contentsSynchronized() {
      if (this.myContent instanceof PsiFileContent) {
         this.setContent(new VirtualFileContent(this, (1)null));
      }

      this.checkLengthConsistency();
   }

   public final void onContentReload() {
      List<PsiFile> psiFiles = this.getCachedPsiFiles();
      List<PsiTreeChangeEventImpl> events = new ArrayList(psiFiles.size());
      List<PsiTreeChangeEventImpl> genericEvents = new ArrayList(psiFiles.size());

      for(PsiFile psiFile : psiFiles) {
         genericEvents.add(this.createChildrenChangeEvent(psiFile, true));
         events.add(this.createChildrenChangeEvent(psiFile, false));
      }

      this.beforeContentsSynchronized();

      for(PsiTreeChangeEventImpl event : genericEvents) {
         this.getManager().beforeChildrenChange(event);
      }

      for(PsiTreeChangeEventImpl event : events) {
         this.getManager().beforeChildrenChange(event);
      }

      for(PsiFile psiFile : psiFiles) {
         if (psiFile instanceof PsiFileEx) {
            ((PsiFileEx)psiFile).onContentReload();
         }
      }

      this.contentsSynchronized();

      for(PsiTreeChangeEventImpl event : events) {
         this.getManager().childrenChanged(event);
      }

      for(PsiTreeChangeEventImpl event : genericEvents) {
         this.getManager().childrenChanged(event);
      }

   }

   private @NotNull PsiTreeChangeEventImpl createChildrenChangeEvent(@NotNull PsiFile psiFile, boolean generic) {
      PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(this.myManager);
      event.setParent(psiFile);
      event.setFile(psiFile);
      event.setGenericChange(generic);
      if (psiFile instanceof PsiFileImpl && ((PsiFileImpl)psiFile).isContentsLoaded()) {
         event.setOffset(0);
         event.setOldLength(psiFile.getTextLength());
      }

      return event;
   }

   public void rootChanged(@NotNull PsiFile psiFile) {
      if (psiFile instanceof PsiFileImpl && ((PsiFileImpl)psiFile).isContentsLoaded() && psiFile.isValid()) {
         this.setContent(new PsiFileContent(((PsiFileImpl)psiFile).calcTreeElement(), LocalTimeCounter.currentTime()));
      }

   }

   public boolean isEventSystemEnabled() {
      return this.myEventSystemEnabled;
   }

   public boolean isPhysical() {
      return this.myPhysical;
   }

   public long getModificationStamp() {
      return this.getContent().getModificationStamp();
   }

   public boolean supportsIncrementalReparse(@NotNull Language rootLanguage) {
      return true;
   }

   private @NotNull Content getContent() {
      return this.myContent;
   }

   private void setContent(@NotNull Content content) {
      this.myContent = content;
   }

   private void checkLengthConsistency() {
      Document document = this.getCachedDocument();
      if (!(document instanceof DocumentWindow)) {
         if (document == null || !((PsiDocumentManagerEx)PsiDocumentManager.getInstance(this.myManager.getProject())).getSynchronizer().isInSynchronization(document)) {
            List<FileASTNode> knownTreeRoots = this.getKnownTreeRoots();
            if (!knownTreeRoots.isEmpty()) {
               int fileLength = this.myContent.getTextLength();

               for(FileASTNode fileElement : knownTreeRoots) {
                  int nodeLength = fileElement.getTextLength();
                  if (!this.isDocumentConsistentWithPsi(fileLength, fileElement, nodeLength)) {
                     PsiUtilCore.ensureValid(fileElement.getPsi());
                     Attachment vfContent = new Attachment(this.myVirtualFile.getName(), this.myContent.getText().toString());
                     Attachment astContent = new Attachment(this.myVirtualFile.getNameWithoutExtension() + ".tree.txt", fileElement.getText());
                     Attachment[] attachments = document == null ? new Attachment[]{vfContent, astContent} : new Attachment[]{vfContent, astContent, new Attachment(this.myVirtualFile.getNameWithoutExtension() + ".document.txt", document.getText())};
                     String message = "Inconsistent " + fileElement.getElementType() + " tree in " + this + "; nodeLength=" + nodeLength + "; fileLength=" + fileLength;
                     if (CodeInsightContexts.isSharedSourceSupportEnabled(this.getManager().getProject())) {
                        message = message + "; context: " + CodeInsightContextUtil.getCodeInsightContext(this);
                        FileManager fileManager = PsiManagerEx.getInstanceEx(this.getManager().getProject()).getFileManager();
                        List<FileViewProvider> providers = fileManager.findCachedViewProviders(this.myVirtualFile);
                        message = message + "; known view providers: " + providers.size();
                     }

                     LOG.error(message, attachments);
                  }
               }

            }
         }
      }
   }

   private boolean isDocumentConsistentWithPsi(int fileLength, @NotNull FileASTNode fileElement, int nodeLength) {
      if (nodeLength != fileLength) {
         return false;
      } else {
         return ApplicationManager.getApplication().isUnitTestMode() && !ApplicationManagerEx.isInStressTest() ? fileElement.getPsi().textMatches(this.myContent.getText()) : true;
      }
   }

   public @NonNls String toString() {
      return this.getClass().getName() + "{vFile=" + this.myVirtualFile + (this.myVirtualFile instanceof VirtualFileWithId ? ", vFileId=" + ((VirtualFileWithId)this.myVirtualFile).getId() : "") + ", content=" + this.getContent() + ", eventSystemEnabled=" + this.isEventSystemEnabled() + '}';
   }

   public abstract @Nullable PsiFile getCachedPsi(@NotNull Language var1);

   public abstract @Unmodifiable @NotNull List<PsiFile> getCachedPsiFiles();

   public abstract @Unmodifiable @NotNull List<FileASTNode> getKnownTreeRoots();

   public final void markInvalidated() {
      this.invalidateCachedPsi();

      for(AbstractFileViewProvider copy : this.getKnownCopies()) {
         this.myManager.getFileManager().setViewProvider(copy.getVirtualFile(), (FileViewProvider)null);
      }

   }

   @Internal
   public final void markPossiblyInvalidated() {
      this.invalidateCachedPsi();

      for(AbstractFileViewProvider copy : this.getKnownCopies()) {
         PossibleInvalidationKt.markPossiblyInvalidated(copy);
      }

   }

   private void invalidateCachedPsi() {
      for(PsiFile file : this.getCachedPsiFiles()) {
         if (file instanceof PsiFileEx) {
            ((PsiFileEx)file).markInvalidated();
         }
      }

   }

   private @NotNull @Unmodifiable Iterable<AbstractFileViewProvider> getKnownCopies() {
      Set<AbstractFileViewProvider> copies = (Set)this.getUserData(KNOWN_COPIES);
      return (Iterable<AbstractFileViewProvider>)(copies != null ? ContainerUtil.filter(copies, (copy) -> ContainerUtil.exists(copy.getCachedPsiFiles(), (f) -> f.getOriginalFile().getViewProvider() == this)) : Collections.emptySet());
   }

   public final void registerAsCopy(@NotNull AbstractFileViewProvider copy) {
      if (copy instanceof FreeThreadedFileViewProvider) {
         LOG.assertTrue(this instanceof FreeThreadedFileViewProvider, "Injected file can't have non-injected original file");
      }

      Set<AbstractFileViewProvider> copies = (Set)ConcurrencyUtil.computeIfAbsent(this, KNOWN_COPIES, () -> Collections.newSetFromMap(CollectionFactory.createConcurrentWeakMap()));
      if (copy.getUserData(KNOWN_COPIES) != null) {
         List<AbstractFileViewProvider> derivations = ((JBTreeTraverser)JBTreeTraverser.from(AbstractFileViewProvider::getKnownCopies).withRoot(copy)).toList();
         if (derivations.contains(this)) {
            throw new IllegalStateException("An attempted cycle in view provider copy graph involving " + this + " and " + copy);
         }
      }

      copies.add(copy);
   }

   private @NotNull CharSequence getLastCommittedText(@NotNull Document document) {
      return PsiDocumentManager.getInstance(this.myManager.getProject()).getLastCommittedText(document);
   }

   private long getLastCommittedStamp(@NotNull Document document) {
      return PsiDocumentManager.getInstance(this.myManager.getProject()).getLastCommittedStamp(document);
   }

   public @NotNull PsiFile getStubBindingRoot() {
      PsiFile psi = this.getPsi(this.getBaseLanguage());

      assert psi != null;

      return psi;
   }

   public final @NotNull FileType getFileType() {
      return this.myVirtualFile.getFileType();
   }

   // $FF: synthetic method
   static CharSequence access$100(AbstractFileViewProvider x0, Document x1) {
      return x0.getLastCommittedText(x1);
   }

   // $FF: synthetic method
   static long access$200(AbstractFileViewProvider x0, Document x1) {
      return x0.getLastCommittedStamp(x1);
   }

   // $FF: synthetic method
   private static void $$$reportNull$$$0(int var0) {
      String var10000;
      switch (var0) {
         case 0:
         case 1:
         case 2:
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         case 9:
         case 11:
         case 15:
         case 16:
         case 17:
         case 18:
         case 20:
         case 21:
         case 23:
         case 24:
         case 27:
         case 28:
         case 30:
         default:
            var10000 = "Argument for @NotNull parameter '%s' of %s.%s must not be null";
            break;
         case 3:
         case 10:
         case 12:
         case 13:
         case 14:
         case 19:
         case 22:
         case 25:
         case 26:
         case 29:
         case 31:
         case 32:
            var10000 = "@NotNull method %s.%s must not return null";
      }

      byte var10001;
      switch (var0) {
         case 0:
         case 1:
         case 2:
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         case 9:
         case 11:
         case 15:
         case 16:
         case 17:
         case 18:
         case 20:
         case 21:
         case 23:
         case 24:
         case 27:
         case 28:
         case 30:
         default:
            var10001 = 3;
            break;
         case 3:
         case 10:
         case 12:
         case 13:
         case 14:
         case 19:
         case 22:
         case 25:
         case 26:
         case 29:
         case 31:
         case 32:
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
            ((Object[])var3)[0] = "provider";
            break;
         case 3:
         case 10:
         case 12:
         case 13:
         case 14:
         case 19:
         case 22:
         case 25:
         case 26:
         case 29:
         case 31:
         case 32:
            ((Object[])var3)[0] = "com/intellij/psi/AbstractFileViewProvider";
            break;
         case 4:
            ((Object[])var3)[0] = "project";
            break;
         case 5:
         case 7:
            ((Object[])var3)[0] = "file";
            break;
         case 6:
         case 8:
            ((Object[])var3)[0] = "fileType";
            break;
         case 9:
         case 16:
         case 17:
            ((Object[])var3)[0] = "language";
            break;
         case 11:
            ((Object[])var3)[0] = "lang";
            break;
         case 15:
            ((Object[])var3)[0] = "target";
            break;
         case 18:
         case 20:
            ((Object[])var3)[0] = "psiFile";
            break;
         case 21:
            ((Object[])var3)[0] = "rootLanguage";
            break;
         case 23:
            ((Object[])var3)[0] = "content";
            break;
         case 24:
            ((Object[])var3)[0] = "fileElement";
            break;
         case 27:
            ((Object[])var3)[0] = "copy";
            break;
         case 28:
         case 30:
            ((Object[])var3)[0] = "document";
      }

      switch (var0) {
         case 0:
         case 1:
         case 2:
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         case 9:
         case 11:
         case 15:
         case 16:
         case 17:
         case 18:
         case 20:
         case 21:
         case 23:
         case 24:
         case 27:
         case 28:
         case 30:
         default:
            ((Object[])var3)[1] = "com/intellij/psi/AbstractFileViewProvider";
            break;
         case 3:
            ((Object[])var3)[1] = "getFilePsiLock";
            break;
         case 10:
            ((Object[])var3)[1] = "createFile";
            break;
         case 12:
            ((Object[])var3)[1] = "getManager";
            break;
         case 13:
            ((Object[])var3)[1] = "getContents";
            break;
         case 14:
            ((Object[])var3)[1] = "getVirtualFile";
            break;
         case 19:
            ((Object[])var3)[1] = "createChildrenChangeEvent";
            break;
         case 22:
            ((Object[])var3)[1] = "getContent";
            break;
         case 25:
         case 26:
            ((Object[])var3)[1] = "getKnownCopies";
            break;
         case 29:
            ((Object[])var3)[1] = "getLastCommittedText";
            break;
         case 31:
            ((Object[])var3)[1] = "getStubBindingRoot";
            break;
         case 32:
            ((Object[])var3)[1] = "getFileType";
      }

      switch (var0) {
         case 0:
         case 1:
         default:
            ((Object[])var3)[2] = "<init>";
            break;
         case 2:
            ((Object[])var3)[2] = "isFreeThreaded";
         case 3:
         case 10:
         case 12:
         case 13:
         case 14:
         case 19:
         case 22:
         case 25:
         case 26:
         case 29:
         case 31:
         case 32:
            break;
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         case 9:
         case 11:
            ((Object[])var3)[2] = "createFile";
            break;
         case 15:
            ((Object[])var3)[2] = "getPsi";
            break;
         case 16:
            ((Object[])var3)[2] = "findElementAt";
            break;
         case 17:
            ((Object[])var3)[2] = "findReferenceAt";
            break;
         case 18:
            ((Object[])var3)[2] = "createChildrenChangeEvent";
            break;
         case 20:
            ((Object[])var3)[2] = "rootChanged";
            break;
         case 21:
            ((Object[])var3)[2] = "supportsIncrementalReparse";
            break;
         case 23:
            ((Object[])var3)[2] = "setContent";
            break;
         case 24:
            ((Object[])var3)[2] = "isDocumentConsistentWithPsi";
            break;
         case 27:
            ((Object[])var3)[2] = "registerAsCopy";
            break;
         case 28:
            ((Object[])var3)[2] = "getLastCommittedText";
            break;
         case 30:
            ((Object[])var3)[2] = "getLastCommittedStamp";
      }

      var10000 = String.format(var10000, var3);
      Object var2;
      switch (var0) {
         case 0:
         case 1:
         case 2:
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         case 9:
         case 11:
         case 15:
         case 16:
         case 17:
         case 18:
         case 20:
         case 21:
         case 23:
         case 24:
         case 27:
         case 28:
         case 30:
         default:
            IllegalArgumentException var6 = new IllegalArgumentException;
            var3 = var10000;
            var2 = var6;
            var6.<init>(var3);
            break;
         case 3:
         case 10:
         case 12:
         case 13:
         case 14:
         case 19:
         case 22:
         case 25:
         case 26:
         case 29:
         case 31:
         case 32:
            IllegalStateException var10002 = new IllegalStateException;
            var3 = var10000;
            var2 = var10002;
            var10002.<init>(var3);
      }

      throw var2;
   }
}
