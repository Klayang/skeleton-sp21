package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CommitTree implements Serializable {
    Commit head;
    String currentBranchName;
    Map<String, Commit> branches; // store mapping of a branch's name to the commit it points to
    public CommitTree(Commit initialCommit) {
        this.head = initialCommit;
        this.currentBranchName = "master";
        branches = new HashMap<>();
        branches.put("master", initialCommit);
    }
    public void updateHead(Commit newHead) {
        this.head = newHead;
        branches.put(currentBranchName, newHead);
    }
    public boolean hasBranch(String branchName) {
        return branches.containsKey(branchName);
    }
    public void addBranch(String branchName, Commit commit) {
        branches.put(branchName, commit);
    }
    public void removeBranch(String branchName) {
        branches.remove(branchName);
    }
    public Commit getCommitOfBranch(String branchName) {
        return branches.get(branchName);
    }
    public void updateBranch(String branchName) {
        currentBranchName = branchName;
        head = branches.get(currentBranchName);
    }
    public boolean isCurrentBranch(String branchName) {
        return currentBranchName.equals(branchName);
    }
}
