package gitlet;

// TODO: any imports you need here

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;

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
    String message;
    Date time;
    List<Object> objectsToHash;

    public Commit(Commit parent, String message, Date time) {
        this.parent = parent;
        this.message = message;
        this.time = time;
        this.objectsToHash = new ArrayList<>();

        this.fileToCommit = new HashMap<>();
        this.fileToContent = new HashMap<>();
        if (parent != null) copyFromParent();
    }

    private void copyFromParent() {
        for (String fileName: parent.fileToCommit.keySet())
            fileToCommit.put(fileName, parent.fileToCommit.get(fileName));
        for (String fileName: parent.fileToContent.keySet())
            fileToContent.put(fileName, parent.fileToContent.get(fileName));
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
     * Update the content of the given file in fileToContent
     */
    public void updateContentEntry(String fileName, String content) {
        fileToContent.put(fileName, content);
    }

    /**
     * Update the commit SHA of the given file in fileToCommit
     */
    public void updateCommitEntry(String fileName, String SHAOfCommit) {
        fileToCommit.put(fileName, SHAOfCommit);
    }

    /**
     * Remove a certain file staged for removal in fileToContent & fileToCommit
     */
    public void removeUntrackedFile(String fileName) {
        fileToCommit.remove(fileName);
        fileToContent.remove(fileName);
    }

    /**
     * Check if the given file is tracked
     */
    public boolean containsFile(String fileName) {
        return fileToCommit.containsKey(fileName);
    }

    @Override
    public boolean equals(Object object) {
        if (object.getClass() != Commit.class) return false;
        Commit commit = (Commit) object;
        if (this.getSHAHash().equals(commit.getSHAHash())) return true;
        else return false;
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
