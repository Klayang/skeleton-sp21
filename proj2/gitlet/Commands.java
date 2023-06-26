package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.*;
import static gitlet.Repository.*;

public class Commands {
    // Helpers /////////////////////////////////////////////////////////////////
    /**
     * Helper method to serialize the given commit
     */
    private static File createNecessaryFile(File first, String... others) throws IOException {
        if (!first.exists()) first.mkdir();
        File filePath = first;
        for (int i = 0; i < others.length; ++i) {
            filePath = Utils.join(filePath, others[i]);
            if (filePath.exists()) continue;
            if (i == others.length - 1) filePath.createNewFile();
            else filePath.mkdir();
        }
        return filePath;
    }

    private static void writeCommit(Commit commit) throws IOException {
        String commitID = commit.getSHAHash();
        String folderName = commitID.substring(0, 2), commitFileName = commitID.substring(2);
        File commitObjectFile = createNecessaryFile(Repository.GITLET_DIR, "objects", folderName, commitFileName);
        Utils.writeObject(commitObjectFile, commit);
    }

    /**
     * Helper method to check if the given file exists in the working directory
     */
    private static void checkFileExists(String fileName) {
        File fileToBeAdded = Utils.join(fileName);
        if (!fileToBeAdded.exists()) GitletException.handleException("File does not exist.");
    }

    /**
     * Helper method to return the head commit
     */
    private static Commit getHeadCommit() {
        File commitTreeFile = Utils.join(Repository.GITLET_DIR, "commit_tree");
        CommitTree commitTree = Utils.readObject(commitTreeFile, CommitTree.class);
        return commitTree.head;
    }

    /**
     * Helper method to return SHA hash of a file's content
     */
    private static String getSHAOfFile(String fileName) {
        File file = Utils.join(fileName);
        String content = Utils.readContentsAsString(file);
        return Utils.sha1(content);
    }

    /**
     * Helper method to back up modified file in history storage
     */
    private static void backupModifiedFile(Set<String> modifiedFiles, String SHA) throws IOException {
        for (String fileName: modifiedFiles) {
            createNecessaryFile(BACKUP_DIR, SHA, fileName);
            File fileToStoreInHistory = Utils.join(BACKUP_DIR, SHA, fileName);
            File fileInWorkingDirectory = Utils.join(fileName);
            Utils.writeContents(fileToStoreInHistory, Utils.readContents(fileInWorkingDirectory));
        }
    }

    /**
     * Helper method to return staging area object
     */
    private static Stage getStagingArea() {
        return Utils.readObject(STAGING_AREA, Stage.class);
    }

    /**
     * Helper method to read commit tree
     */
    private static CommitTree getCommitTree() {
        return Utils.readObject(COMMIT_TREE, CommitTree.class);
    }

    /**
     * Helper method to write commit tree
     */
    private static void writeCommitTree(CommitTree commitTree) {
        Utils.writeObject(COMMIT_TREE, commitTree);
    }

    /**
     * Helper method to serialize staging area
     */
    private static void writeStagingArea(Stage stage) {
        Utils.writeObject(STAGING_AREA, stage);
    }

    /**
     * Helper method to get a string that represents offset between current timezone and UTC
     */
     private static String getOffsetBetweenTimezones() {
        String[] date = new Date(0).toString().split(" ");
        int year = Integer.parseInt(date[5]), hour = Integer.parseInt(date[3].split(":")[0]);
        String num, sign;
        if (year == 1970) {
            sign = "+";
            num = hour + "00";
        }
        else {
            sign = "-";
            num = String.valueOf(24 - hour);
        }
        if (num.length() == 3) num = "0" + num;
        return sign + num;
    }

    /**
     * Helper method to get a string that represents commit time
     */
    private static String getCommitTime(Date commitTime) {
        String[] components = commitTime.toString().split(" ");
        components[4] = components[5];
        components[5] = getOffsetBetweenTimezones();
        return components[0] + " " + components[1] + " " + components[2] + " " + components[3] + " "
                + components[4] + " " + components[5];
    }

    /**
     * Helper method called by command log, to print info of a commit
     */
    private static void printCommitLog(Commit commit) {
        Date commitTime = commit.time;
        String SHA = commit.getSHAHash();
        System.out.println("===");
        System.out.printf("commit %s\n", SHA);
        System.out.printf("Date: %s\n", getCommitTime(commitTime));
        System.out.println(commit.message);
        System.out.println();
    }

    /**
     * Helper method to collect all commits in history
     *  1. Find all subdirectories under object directory
     *  2. For each subdirectory, fetch all files within it
     *  3. Deserialize the file into commit object and put it into the result list
     */
    private static List<Commit> getAllCommits() {
        List<Commit> commits = new ArrayList<>();
        List<String> directoryNames = Utils.directoriesNamesIn(Repository.OBJECTS_DIR);
        for (String directoryName: directoryNames) {
            File directory = Utils.join(Repository.OBJECTS_DIR, directoryName);
            List<String> fileNames = Utils.plainFilenamesIn(directory);
            for (String fileName: fileNames) {
                File commitFile = Utils.join(directory, fileName);
                Commit commit = Utils.readObject(commitFile, Commit.class);
                commits.add(commit);
            }
        }
        return commits;
    }

//    /**
//     * Helper method to handle the exception that the given commit doesn't exist
//     */
//    private static void checkCommitExists(Commit commit) {
//        File commitBackup = Utils.join(Repository.BACKUP_DIR, commit.getSHAHash());
//        if (!commitBackup.exists()) GitletException.handleNoSpecifiedCommit();
//    }

    /**
     * Helper method to check out file in the given commit
     */
    private static void checkoutFile(String commitSHAHash, String fileName) throws IOException {
        overwrite(fileName, commitSHAHash);
        Stage stage = getStagingArea();
        stage.unstageFileIfAdded(fileName);
        Utils.writeObject(STAGING_AREA, stage);
    }

    /**
     * Helper method to return the commit with given id
     */
    private static Commit getCommit(String id) {
        String folderName = id.substring(0, 2), commitFileName = id.substring(2);
        File commitFile = Utils.join(Repository.OBJECTS_DIR, folderName, commitFileName);
        if (!commitFile.exists()) GitletException.handleException("File does not exist.");
        return Utils.readObject(commitFile, Commit.class);
    }

    /**
     * Helper method called by checkout & reset, restore the snapshot of the given commit
     *  1. Restore files in the working directory to be the image of the given commit
     *  2. Delete files in the working directory not in the image of the given commit
     *  3. Clear staging area
     */
    private static void checkoutCommit(Commit commit) throws IOException {
        Map<String, String> fileToCommit = commit.fileToCommit;
        for (String fileName: fileToCommit.keySet())
            checkoutFile(fileToCommit.get(fileName), fileName);
        List<String> filesInWorkingDirectory = Utils.plainFilenamesIn(Repository.CWD);
        for (String fileName: filesInWorkingDirectory)
            if (!fileToCommit.containsKey(fileName) && !isNecessaryFile(fileName)) Utils.restrictedDelete(fileName);

        // clear staging area
        Utils.writeObject(Utils.join(Repository.GITLET_DIR, "staging_area"), new Stage());
    }

    /**
     * Helper method called by merge, to find the first common ancestor of 2 commits
     *  See Leetcode offer52 for detail of solution
     */
    private static Commit findFirstCommonAncestor(Commit commitA, Commit commitB) {
        Commit h1 = commitA, h2 = commitB;
        while (h1 != null || h2 != null) {
            if (h1.equals(h2)) break;
            if (h1 == null) h1 = commitB;
            else h1 = h1.parent;
            if (h2 == null) h2 = commitA;
            else h2 = h2.parent;
        }
        return h1;
    }

    /**
     * Helper method called by merge to handle the 3rd case: divergence
     */
    private static void handleCoreMerge (Commit head, Commit branch, Commit split) throws IOException {
        Set<String> files = getFileSet(head, branch, split);
        for (String fileName: files) {
            String contentInSplit = split.getFileContent(fileName), contentInHead = head.getFileContent(fileName),
                    contentInBranch = branch.getFileContent(fileName);

            if (!split.containsFile(fileName)) {
                if (branch.containsFile(fileName) && !head.containsFile(fileName)) checkoutFile(branch.getSHAHash(), fileName);
                if (branch.containsFile(fileName) && head.containsFile(fileName) &&
                        !contentInBranch.equals(contentInHead)) resolveConflict(fileName, head, branch);
            }
            else if (!head.containsFile(fileName)) {
                if (branch.containsFile(fileName) && !contentInBranch.equals(contentInSplit))
                    resolveConflict(fileName, head, branch);
            }
            else if (!branch.containsFile(fileName)) {
                if (contentInHead.equals(contentInSplit)) rm(new String[]{"rm", fileName});
                else resolveConflict(fileName, head, branch);
            }
            else {
                if (contentInSplit.equals(contentInHead) && !contentInSplit.equals(contentInBranch))
                    checkoutFile(branch.getSHAHash(), fileName);
                else if (!contentInSplit.equals(contentInHead) && !contentInSplit.equals(contentInBranch) &&
                        !contentInHead.equals(contentInBranch)) resolveConflict(fileName, head, branch);
            }

        }
    }

    /**
     * Helper method called by handleCoreMerge
     *  to overwrite a file in working directory with the version in a commit
     */
    private static void overwrite(String fileName, String commitSHAHash) throws IOException {
        File commitBackup = Utils.join(Repository.BACKUP_DIR, commitSHAHash, fileName);
        byte[] contentOfBackup = Utils.readContents(commitBackup);
        File fileInWorkingDirectory = new File(fileName);
        if (!fileInWorkingDirectory.exists()) fileInWorkingDirectory.createNewFile();
        Utils.writeContents(fileInWorkingDirectory, contentOfBackup);
    }

    /**
     * Helper method called by handleCoreMerge:
     *  Given 3 commits, return union set of the files they track
     */
    private static Set<String> getFileSet(Commit head, Commit branch, Commit split) {
        Set<String> files = new HashSet<>();
        for (String fileName: head.fileToContent.keySet()) files.add(fileName);
        for (String fileName: branch.fileToContent.keySet()) files.add(fileName);
        for (String fileName: split.fileToContent.keySet()) files.add(fileName);
        return files;
    }

    /**
     * Helper method to resolves conflicts in files in 2 different branches
     */
    private static void resolveConflict(String fileName, Commit head, Commit branch) throws IOException {
        String headContent = "", branchContent = "";
        if (head.containsFile(fileName)) {
            File headFile = Utils.join(Repository.BACKUP_DIR, head.getSHAHash(), fileName);
            headContent = Utils.readContentsAsString(headFile);
        }
        if (branch.containsFile(fileName)) {
            File branchFile = Utils.join(Repository.BACKUP_DIR, branch.getSHAHash(), fileName);
            branchContent = Utils.readContentsAsString(branchFile);
        }

        File currentFile = Utils.join(Repository.CWD, fileName);
        if (!currentFile.exists()) currentFile.createNewFile();
        String newContent = "<<<<<<< HEAD\n";
        newContent += headContent;
        newContent += "=======\n";
        newContent += branchContent;
        newContent += ">>>>>>>";
        Utils.writeContents(currentFile, newContent);
    }

    /**
     * Helper method to assemble file contents in stage and previous commit (i.e., head)
     * This will be called by command status, to show files modified but not staged, and untracked
     */
    private static Map<String, String> getTrackedFileToContent(Map<String, String> fileToContentOnStage,
                                                               Map<String, String> fileToContentInHead) {
        Map<String, String> res = new HashMap<>();
        for (String fileName: fileToContentInHead.keySet())
            res.put(fileName, fileToContentInHead.get(fileName));
        for (String fileName: fileToContentOnStage.keySet())
            res.put(fileName, fileToContentOnStage.get(fileName));
        return res;
    }

    /**
     * Tell if the given file is the necessary configuration file for the project
     * If so we cannot delete it
     */
    private static boolean isNecessaryFile(String fileName) {
        return fileName.equals("gitlet1.iml") || fileName.equals("Makefile") || fileName.equals("pom.xml");
    }

    // Commands /////////////////////////////////////////////////////////////////


    /**
     * Execute init command:
     *  1. create a .gitlet directory
     *  2. start the initial commit
     */
    static void init(String[] args) throws IOException {
        // create metadata directory
        if (args.length != 1) GitletException.handleException("Incorrect operands.");
        File metadataFolder = Utils.join(".gitlet");
        if (metadataFolder.exists())
            GitletException.handleException("A Gitlet version-control system already exists in the current directory.");
        else metadataFolder.mkdir();

        // create the initial commit
        Commit initialCommit = Utils.initialCommit();
        writeCommit(initialCommit);

        // create commit tree and write the initial commit to the commit tree
        CommitTree commitTree = new CommitTree(initialCommit);
        File commitTreeFile = Utils.join(Repository.GITLET_DIR, "commit_tree");
        if (!commitTreeFile.exists()) commitTreeFile.createNewFile();
        Utils.writeObject(commitTreeFile, commitTree);

        // create staging area so that we could perform any operation later (e.g., add, rm)
        STAGING_AREA.createNewFile();
        Utils.writeObject(STAGING_AREA, new Stage());
    }

    /**
     * Execute add command:
     *  1. If the file is not staged:
     *      (1) if it's not modified since last commit, do not stage it
     *      (2) if it's modified, stage it
     *  2. If the file is staged:
     *      (1) if it's different from the version in last commit, overwrite the one on the stage
     *      (2) if it's identical to the version in last commit, unstage it
     */
    static void add(String[] args) throws IOException {
        // check possible failure cases
        if (args.length != 2) GitletException.handleException("Incorrect operands.");
        checkFileExists(args[1]);

        // read staging area from file
        Stage stage;
        if (!STAGING_AREA.exists()) {
            STAGING_AREA.createNewFile();
            stage = new Stage();
        } else stage = Utils.readObject(STAGING_AREA, Stage.class);

        // fetch info about last commit
        Commit head = getHeadCommit();

        // add given file to the staging area if it's modified since last commit
        String fileName = args[1];
        String SHAOfContent = getSHAOfFile(fileName);
        if (head.isFileModified(fileName, SHAOfContent))
            stage.addFile(fileName, SHAOfContent);
        else stage.unstageFileIfAdded(fileName);

        // get file out of removal set, this could happen when we remove a file, then add it back
        if (stage.removalFileSet.contains(fileName)) stage.unremoveFile(fileName);

        // write the updated staging area back to file
        Utils.writeObject(STAGING_AREA, stage);
    }

    /**
     * Execute commit command:
     *  1. Exception handling:
     *      (1) abort if no files staged
     *      (2) abort if no commit message
     *  2. Read info about last commit and staging area
     *  3. Update entries in last commit with content in staging area to create new commit
     *  4. Write new commit into commit tree and commit directory
     *  5. store modified file to corresponding history storage
     *  6. Clear the staging area
     */
    static void commit(String[] args) throws IOException {
        // check message of the commit exists
        if (args.length == 1 || args[1].length() == 0) GitletException.handleException("Please enter a commit message.");
        if (args.length > 2) GitletException.handleException("Incorrect operands.");

        // read info about last commit and staging area
        CommitTree commitTree = Utils.readObject(COMMIT_TREE, CommitTree.class);
        Commit head = commitTree.head;
        Commit current = new Commit(head, args[1], new Date());

        Stage stage = Utils.readObject(STAGING_AREA, Stage.class);

        // update entries in last commit to create current one
        Map<String, String> stagedFileToContent = stage.fileNameToContent;
        Set<String> removalFileSet = stage.removalFileSet;
        if (stagedFileToContent.isEmpty() && removalFileSet.isEmpty())
            GitletException.handleException("No changes added to the commit.");

        for (String fileName: stagedFileToContent.keySet())
            current.updateContentEntry(fileName, stagedFileToContent.get(fileName));
        for (String fileName: removalFileSet)
            current.removeUntrackedFile(fileName);

        String SHAOfCurrent = current.getSHAHash();
        for (String fileName: stagedFileToContent.keySet())
            current.updateCommitEntry(fileName, SHAOfCurrent);

        // write new head to commit tree and its underlying file
        commitTree.updateHead(current);
        Utils.writeObject(COMMIT_TREE, commitTree);
        writeCommit(current);

        // write each modified file to history storage
        backupModifiedFile(stagedFileToContent.keySet(), SHAOfCurrent);

        // clear the staging area
        Utils.writeObject(STAGING_AREA, new Stage());
    }

    /**
     * Execute rm command:
     *  1. Exception handling: abort if the given file is neither staged nor tracked
     *  2. If the file is staged, unstage it; Otherwise, stage it for removal & delete it in the working directory
     *  3. Write staging area back to disk
     */
    static void rm(String[] args) {
        // exception handling
        if (args.length != 2) GitletException.handleException("Incorrect operands.");
        String fileName = args[1];
        Commit head = getHeadCommit();
        Stage stage = getStagingArea();
        if (!head.containsFile(fileName) && !stage.hasFile(fileName))
            GitletException.handleException("No reason to remove the file.");

        // remove file from staging area and the working directory
        if (stage.hasFile(fileName)) stage.unstageFileIfAdded(fileName);
        if (head.containsFile(fileName)) stage.addFileToRemove(fileName);
        Utils.restrictedDelete(fileName);

        // write staging area back to disk
        writeStagingArea(stage);
    }

    /**
     * Execute log command:
     *  1. Find the head commit
     *  2. Follow the parent pointer backwards to print info of commits in the history
     */
    static void log(String[] args) {
        if (args.length != 1) GitletException.handleException("Incorrect operands.");
        Commit commit = getHeadCommit();
        while (commit != null) {
            printCommitLog(commit);
            commit = commit.parent;
        }
    }

    /**
     * Execute global-log command:
     *  1. Get all commits in the history
     *  2. For each of them, print its info as log
     */
    static void globalLog(String[] args) {
        if (args.length != 1) GitletException.handleException("Incorrect operands.");

        List<Commit> commits = getAllCommits();
        for (Commit commit: commits)
            printCommitLog(commit);
    }

    /**
     * Execute find command:
     *  1. Get all commits in the history
     *  2. For each of them, if it's message matches the given one, print its SHA id
     */
    static void find(String[] args) {
        if (args.length != 2) GitletException.handleException("Incorrect operands.");
        List<Commit> commits = getAllCommits();
        boolean hasFound = false;

        for (Commit commit: commits)
            if (commit.message.equals(args[1])) {
                System.out.println(commit.getSHAHash());
                hasFound = true;
            }
        if (!hasFound) GitletException.handleException("Found no commit with that message.");
    }

    /**
     * Execute status command:
     *  1. Display info of possible branches, and mark the current one with a *
     *  2. Display files staged for addition
     *  3. Display files staged for removal
     *  4. Display files modified but not staged
     *  5. Display files untracked
     */
    static void status(String[] args) {
        // Exception handling
        if (args.length != 1) GitletException.handleException("Incorrect operands.");
        if (!GITLET_DIR.exists()) GitletException.handleException("Not in an initialized Gitlet directory.");
        Stage stage = getStagingArea();
        CommitTree commitTree = Utils.readObject(COMMIT_TREE, CommitTree.class);
        Map<String, String> trackedFileToContent = getTrackedFileToContent(stage.fileNameToContent,
                                                                    commitTree.head.fileToContent);

        // Display info of possible branches
        System.out.println("=== Branches ===");
        for (String branchName: commitTree.branches.keySet()) {
            if (branchName.equals(commitTree.currentBranchName)) System.out.print("*");
            System.out.println(branchName);
        }

        // Display files staged for addition
        System.out.println("=== Staged Files ===");
        for (String fileName: stage.fileNameToContent.keySet())
            System.out.println(fileName);
        System.out.println();

        // Display files staged for removal
        System.out.println("=== Removed Files ===");
        for (String fileName: stage.removalFileSet)
            System.out.println(fileName);
        System.out.println();

        // Display files modified but not staged
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String fileName: trackedFileToContent.keySet()) {
            File file = new File(fileName);
            if (!file.exists()) {
                if (!stage.removalFileSet.contains(fileName)) System.out.printf("%s (deleted)\n", fileName);
            }
            else {
                String SHAOfCurrentContent = Utils.sha1(Utils.readContents(Utils.join(fileName)));
                if (!SHAOfCurrentContent.equals(trackedFileToContent.get(fileName)))
                    System.out.printf("%s (modified)\n", fileName);
            }
        }
        System.out.println();

        // Display files untracked
        System.out.println("=== Untracked Files ===");
        List<String> fileNames = Utils.plainFilenamesIn(Repository.CWD);
        for (String fileName: fileNames)
            if (!trackedFileToContent.containsKey(fileName)) System.out.println(fileName);
        System.out.println();
    }

    /**
     * Execute checkout command:
     *  1. checkout -- [file name], replace the file in working directory with the version in head commit
     *     and unstage the file
     *  2. checkout [commit id] -- [file name], replace the file in working directory with the version in
     *     given commit and unstage the file
     *  3. checkout [branch name], replace files in working directory with ones in the given branch
     */
    static void checkout(String[] args) throws IOException {
        if (args.length == 1 || args.length > 4) GitletException.handleException("Incorrect operands.");

        if (args.length == 2) {
            CommitTree commitTree = Utils.readObject(COMMIT_TREE, CommitTree.class);
            String branchName = args[1];
            String commitID = commitTree.getCommitOfBranch(branchName).getSHAHash();
            Commit headCommitOfBranch = getCommit(commitID);
            checkoutCommit(headCommitOfBranch);

            // update the current branch
            commitTree.updateBranch(branchName);
            Utils.writeObject(COMMIT_TREE, commitTree);
        }
        else if (args.length == 3) {
            if (!args[1].equals("--")) GitletException.handleException("No command with that name exists.");
            String fileName = args[2];
            Commit head = getHeadCommit();
            if (!head.containsFile(fileName)) GitletException.handleException("File does not exist in that commit.");
            checkoutFile(head.getCommitSHAOfFile(fileName), fileName);
        }
        else {
            if (!args[2].equals("--")) GitletException.handleException("No command with that name exists.");
            String commitID = args[1], fileName = args[3];
            Commit commit = getCommit(commitID);
            if (!commit.containsFile(fileName)) GitletException.handleException("No commit with that id exists.");
            checkoutFile(commit.getCommitSHAOfFile(fileName), fileName);
        }
    }

    /**
     * Execute branch command:
     *  1. Add an entry in commit-tree's mapping from branch names to commit id
     */
    static void branch(String[] args) {
        // Exception handling
        if (args.length != 2) GitletException.handleException("Incorrect operands.");
        String branchName = args[1];
        CommitTree commitTree = Utils.readObject(COMMIT_TREE, CommitTree.class);
        if (commitTree.hasBranch(branchName)) GitletException.handleException("A branch with that name already exists.");

        // Add an entry in commit tree's branch info
        Commit current = getHeadCommit();
        commitTree.addBranch(branchName, current);

        // Write the commit tree object back to its file
        Utils.writeObject(COMMIT_TREE, commitTree);
    }

    /**
     * Execute rm-branch command:
     *  1. Remove the given branch entry in commit tree's branch mappings
     */
    static void rmBranch(String[] args) {
        if (args.length != 2) GitletException.handleException("Incorrect operands.");
        String branchName = args[1];
        CommitTree commitTree = getCommitTree();
        if (!commitTree.hasBranch(branchName)) GitletException.handleException("A branch with that name does not exist.");
        // how to tell if we're trying to remove the branch we're currently on?
        if (commitTree.isCurrentBranch(branchName)) GitletException.handleException("Cannot remove the current branch.");

        // Remove the branch entry in commit tree and write the object back to the file
        commitTree.removeBranch(branchName);
        writeCommitTree(commitTree);
    }

    /**
     * Execute reset command:
     *  1. checkout each file in the given commit
     */
    static void reset(String[] args) throws IOException {
        if (args.length != 2) GitletException.handleException("No command with that name exists.");
        String commitID = args[1];
        Commit commit = getCommit(commitID);
        checkoutCommit(commit);
    }

    /**
     * Execute merge command:
     *  1. If HEAD is the split point, fast-forward to the other branch
     *  2. If the other branch is the split point, do nothing
     *  3. Otherwise, create a new commit with 2 parents pointing to 2 branches
     */
    static void merge(String[] args) throws IOException {
        if (args.length != 2) GitletException.handleException("Incorrect operands.");
        Stage stage = getStagingArea();
        if (!stage.removalFileSet.isEmpty() || !stage.fileNameToContent.isEmpty())
            GitletException.handleException("You have uncommitted changes.");
        CommitTree commitTree = getCommitTree();
        String branchName = args[1];
        if (!commitTree.hasBranch(branchName))
            GitletException.handleException("A branch with that name does not exist.");
        Commit head = getHeadCommit();
        if (commitTree.getCommitOfBranch(branchName).equals(head.getSHAHash()))
            GitletException.handleException("Cannot merge a branch with itself.");

        // Get the split point of 2 branches
        Commit branchCommit = getCommit(commitTree.getCommitOfBranch(branchName).getSHAHash());
        Commit splitCommit = findFirstCommonAncestor(head, branchCommit);

        if (head.equals(splitCommit)) {
            checkoutCommit(branchCommit);
            System.out.println("Current branch fast-forwarded.");
        }
        else if (branchCommit.equals(splitCommit))
            System.out.println("Given branch is an ancestor of the current branch.");
        else handleCoreMerge(head, branchCommit, splitCommit);
    }
}
