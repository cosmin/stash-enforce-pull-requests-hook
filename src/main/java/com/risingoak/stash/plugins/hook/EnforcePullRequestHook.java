package com.risingoak.stash.plugins.hook;

import com.atlassian.stash.content.Changeset;
import com.atlassian.stash.history.HistoryService;
import com.atlassian.stash.hook.HookResponse;
import com.atlassian.stash.hook.repository.PreReceiveRepositoryHook;
import com.atlassian.stash.hook.repository.RepositoryHookContext;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.RefChangeType;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.repository.RepositoryMetadataService;
import com.atlassian.stash.util.Page;
import com.atlassian.stash.util.PageRequestImpl;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;

public class EnforcePullRequestHook implements PreReceiveRepositoryHook {
    private final RepositoryMetadataService repositoryMetadataService;
    private final com.atlassian.stash.history.HistoryService historyService;

    public EnforcePullRequestHook(RepositoryMetadataService repositoryMetadataService, HistoryService historyService) {
        this.repositoryMetadataService = repositoryMetadataService;
        this.historyService = historyService;
    }

    @Override
    public boolean onReceive(@Nonnull RepositoryHookContext context, @Nonnull Collection<RefChange> refChanges, @Nonnull HookResponse hookResponse) {
        Repository repository = context.getRepository();
        String defaultBranchRefId = repositoryMetadataService.getDefaultBranch(repository).getId();
        for(RefChange refChange : refChanges) {
            if (refChange.getType().equals(RefChangeType.UPDATE) && refChange.getRefId().equals(defaultBranchRefId)) {
                String fromHash = refChange.getFromHash();
                String toHash = refChange.getToHash();
                PageRequestImpl UP_TO_10 = new PageRequestImpl(0, 10);
                Page<Changeset> changes = historyService.getChangesetsBetween(repository, fromHash, toHash, UP_TO_10);
                for (Changeset changeset : getChangesets(changes)) {
                    if (!changeset.getMessage().contains("This reverts commit ")) {
                        hookResponse.err().format("\n*** ERROR ***\n\nDirect pushes to %s are not allowed (except for reverts). Please use pull requests.\n\n", defaultBranchRefId);
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static ArrayList<Changeset> getChangesets(Page<Changeset> changesetPage) {
        ArrayList<Changeset> changesets = new ArrayList<Changeset>();
        for (Changeset changeset : changesetPage.getValues()) {
            changesets.add(changeset);
        }
        return changesets;
    }
}
