package gitlet;

// TODO: any imports you need here

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;

import static gitlet.Repository.STAGING_AREA;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    Map<String, String> fileToContent; // mapping file name to SHA of its content
    Map<String, String> fileToCommit; // mapping file to id of the commit that the file is staged in
    Commit parent;
    Commit secondParent;
    String message;
    Date time;
    List<Object> objectsToHash;

    public Commit(Commit parent, String message, Date time) {
        setFields(parent, message, time);
        if (parent != null) {
            copyFromParent(parent);
            applyStageToCommit();
        }
    }

    public Commit(Commit parent, Commit secondParent, String message, Date time) {
        setFields(parent, message, time);
        this.secondParent = secondParent;
        copyFromParent(parent);
        copyFromParent(secondParent);
        applyStageToCommit();
    }

    private void setFields(Commit parent, String message, Date time) {
        this.parent = parent;
        this.message = message;
        this.time = time;
        this.objectsToHash = new ArrayList<>();

        this.fileToCommit = new HashMap<>();
        this.fileToContent = new HashMap<>();
    }

    private void copyFromParent(Commit parent) {
        for (String fileName: parent.fileToCommit.keySet())
            fileToCommit.put(fileName, parent.fileToCommit.get(fileName));
        for (String fileName: parent.fileToContent.keySet())
            fileToContent.put(fileName, parent.fileToContent.get(fileName));
    }

    private void applyStageToCommit() {
        // update entries in last commit to create current one
        Stage stage = Utils.readObject(STAGING_AREA, Stage.class);
        Map<String, String> stagedFileToContent = stage.fileNameToContent;
        Set<String> removalFileSet = stage.removalFileSet;
        if (stagedFileToContent.isEmpty() && removalFileSet.isEmpty())
            GitletException.handleException("No changes added to the commit.");

        for (String fileName: stagedFileToContent.keySet())
            fileToContent.put(fileName, stagedFileToContent.get(fileName));
        for (String fileName: removalFileSet) {
            fileToCommit.remove(fileName);
            fileToContent.remove(fileName);
        }

        String SHAOfCurrent = getSHAHash();
        for (String fileName: stagedFileToContent.keySet())
            fileToCommit.put(fileName, SHAOfCurrent);
    }

    public String getSHAHash() {
        if (objectsToHash.size() == 0) {
            if (parent != null) objectsToHash.add(parent.toString());
            objectsToHash.add(message);
            objectsToHash.add(time.toString());
            objectsToHash.add(fileToContent.toString());
        }
        return Utils.sha1(objectsToHash);
    }

    /**
     * test if given file is in last commit, return true if it's not
     * If so, test if the content is modified, return true if it is
     */
    public boolean isFileModified(String fileName, String SHA) {
        if (!fileToContent.containsKey(fileName)) return true;
        return !fileToContent.get(fileName).equals(SHA);
    }

    /**
     * Check if the given file is tracked
     */
    public boolean containsFile(String fileName) {
        return fileToCommit.containsKey(fileName);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) return false;
        if (object.getClass() != Commit.class) return false;
        Commit commit = (Commit) object;
        if (this.getSHAHash().equals(commit.getSHAHash())) return true;
        else return false;
    }

    @Override
    public int hashCode() {
        String SHA = getSHAHash();
        return SHA.charAt(0) + SHA.charAt(1);
    }

    /**
     * Return SHA of the given file's content
     */
    public String getFileContent(String fileName) {
        return fileToContent.get(fileName);
    }

    /**
     * Return SHA of the commit, whose backup repository is where the given file is stored
     */
    public String getCommitSHAOfFile(String fileName) {return fileToCommit.get(fileName);}

    @Override
    public String toString() {
        return getSHAHash();
    }
}
