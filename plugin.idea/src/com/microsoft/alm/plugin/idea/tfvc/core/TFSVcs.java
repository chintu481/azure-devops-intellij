// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.openapi.vcs.VcsShowSettingOption;
import com.intellij.openapi.vcs.changes.ChangeProvider;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.rollback.RollbackEnvironment;
import com.intellij.openapi.vcs.update.UpdateEnvironment;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsRevisionNumber;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAuthorizedException;

/**
 * Class that sets up the TFS version control extension.
 * <p/>
 * TODO: comment back in code as more features are added
 */
public class TFSVcs extends AbstractVcs {
    public static final Logger logger = LoggerFactory.getLogger(TFSVcs.class);

    @NonNls
    public static final String TFVC_NAME = "TFVC";
    private static final VcsKey ourKey = createKey(TFVC_NAME);

    //  TODO: private VcsVFSListener myFileListener;
    private final VcsShowConfirmationOption myAddConfirmation;
    private final VcsShowConfirmationOption myDeleteConfirmation;
    private final VcsShowSettingOption myCheckoutOptions;

    private TFSCheckinEnvironment myCheckinEnvironment;
    private UpdateEnvironment myUpdateEnvironment;

    public TFSVcs(@NotNull Project project) {
        super(project, TFVC_NAME);
        final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
        myAddConfirmation = vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.ADD, this);
        myDeleteConfirmation = vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.REMOVE, this);
        myCheckoutOptions = vcsManager.getStandardOption(VcsConfiguration.StandardOption.CHECKOUT, this);
    }

    public static TFSVcs getInstance(Project project) {
        return (TFSVcs) ProjectLevelVcsManager.getInstance(project).findVcsByName(TFVC_NAME);
    }

    @NonNls
    public String getDisplayName() {
        return TFVC_NAME;
    }

    public Configurable getConfigurable() {
        //  TODO: return new TFSProjectConfigurable(myProject);
        return null;
    }


    @Override
    public void activate() {
//    TODO: myFileListener = new TFSFileListener(getProject(), this);
//    TODO: TfsSdkManager.activate();
    }

    @Override
    public void deactivate() {
//    TODO: Disposer.dispose(myFileListener);
    }

    public VcsShowConfirmationOption getAddConfirmation() {
        return myAddConfirmation;
    }

    public VcsShowConfirmationOption getDeleteConfirmation() {
        return myDeleteConfirmation;
    }

    public VcsShowSettingOption getCheckoutOptions() {
        return myCheckoutOptions;
    }

    public ChangeProvider getChangeProvider() {
        return new TFSChangeProvider(myProject);
    }

    @NotNull
    public TFSCheckinEnvironment createCheckinEnvironment() {
        if (myCheckinEnvironment == null) {
            myCheckinEnvironment = new TFSCheckinEnvironment(this);
        }
        return myCheckinEnvironment;
    }

    @NotNull
    public UpdateEnvironment createUpdateEnvironment() {
        if (myUpdateEnvironment == null) {
            myUpdateEnvironment = new TFSUpdateEnvironment(this);
        }
        return myUpdateEnvironment;
    }

    public RollbackEnvironment createRollbackEnvironment() {
        return new TFSRollbackEnvironment(myProject);
    }

    public boolean fileIsUnderVcs(final FilePath filePath) {
        return isVersionedDirectory(filePath.getVirtualFile());
    }

    /*
    TODO:
    public boolean isVersionedDirectory(final VirtualFile dir) {
        if (dir == null) {
            return false;
        }
        return (!Workstation.getInstance().findWorkspacesCached(TfsFileUtil.getFilePath(dir), false).isEmpty());
    }


    public EditFileProvider getEditFileProvider() {
        return new TFSEditFileProvider(myProject);
    }

    @NotNull
    public CommittedChangesProvider<TFSChangeList, ChangeBrowserSettings> getCommittedChangesProvider() {
        if (myCommittedChangesProvider == null) {
            myCommittedChangesProvider = new TFSCommittedChangesProvider(myProject);
        }
        return null; //myCommittedChangesProvider;
    }
     */

    @Nullable
    public VcsRevisionNumber parseRevisionNumber(final String revisionNumberString) {
        return TfsRevisionNumber.tryParse(revisionNumberString);
    }

    @Nullable
    public String getRevisionPattern() {
        return ourIntegerPattern;
    }


    public static VcsKey getKey() {
        return ourKey;
    }

    public static boolean isUnderTFS(FilePath path, Project project) {
        AbstractVcs vcs = VcsUtil.getVcsFor(project, path);
        return vcs != null && TFVC_NAME.equals(vcs.getName());
    }

    @Override
    public CheckoutProvider getCheckoutProvider() {
        return null; ///TODO: new TFSCheckoutProvider();
    }


    /**
     * This method is used by the environment classes to get the ServerContext.
     * We do not cache it here because it should already be cached in the ServerContextManager.
     */
    public ServerContext getServerContext(boolean throwIfNotFound) {
        final RepositoryContext repositoryContext = VcsHelper.getRepositoryContext(getProject());
        final ServerContext serverContext = repositoryContext != null ?
                ServerContextManager.getInstance().createContextFromTfvcServerUrl(
                        repositoryContext.getUrl(), repositoryContext.getTeamProjectName(), true)
                : null;

        if (serverContext == null && throwIfNotFound) {
            throw new NotAuthorizedException(repositoryContext != null ? repositoryContext.getUrl() : "");
        }
        return serverContext;
    }
}