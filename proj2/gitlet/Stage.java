package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Stage implements Serializable {
    Map<String, String> fileNameToContent; // store mapping from file name to SHA of its content;
    Set<String> removalFileSet; // store files to remove in current commit

    public Stage() {
        fileNameToContent = new HashMap<>();
        removalFileSet = new HashSet<>();
    }

    boolean hasFile(String fileName) {
        return fileNameToContent.containsKey(fileName);
    }

    void addFile(String fileName, String SHAOfContent) {
        fileNameToContent.put(fileName, SHAOfContent);
    }

    void unstageFileIfAdded(String fileName) {
        if (fileNameToContent.containsKey(fileName)) fileNameToContent.remove(fileName);
    }

    /**
     * called by command rm to stage a file for removal
     */
    void addFileToRemove(String fileName) {
        removalFileSet.add(fileName);
    }

    void unremoveFile(String fileName) {removalFileSet.remove(fileName);}
}
