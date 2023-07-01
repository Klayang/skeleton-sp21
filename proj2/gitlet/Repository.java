package gitlet;

import java.io.File;
import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The .gitlet/objects directory, which stores commit info. */
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    /** The .gitlet/backup directory, which stores version of file in each commit. */
    public static final File BACKUP_DIR = join(GITLET_DIR, "backup");
    /** The .gitlet/commit_tree file, which stores head, master, and other branches */
    public static final File COMMIT_TREE = join(GITLET_DIR, "commit_tree");
    /** The .gitlet/staging_area file, which stores files staged for addition/removal */
    public static final File STAGING_AREA = join(".gitlet", "staging_area");

    /** 3 files used in test code **/
    public static final File TEST_FILE_HELLO = join(CWD, "hello.txt");
    public static final File TEST_FILE_WORK = join(CWD, "work.txt");
    public static final File TEST_FILE_BELGIUM = join(CWD, "Belgium.txt");

    public static final File TEST_FILE_F = join(CWD, "f.txt");
    public static final File TEST_FILE_H = join(CWD, "h.txt");
    public static final File TEST_FILE_G = join(CWD, "g.txt");
    /* TODO: fill in the rest of this class. */
}
