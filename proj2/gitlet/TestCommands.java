package gitlet;
import org.junit.Test;
import static gitlet.Commands.*;
import static gitlet.Utils.*;
import static gitlet.Repository.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Contents in the following commits:
 *  1. --
 *  2. hello.txt: hello world. I'm Klay
 *     work.txt
 *  3. work.txt
 */
public class TestCommands {
    @Test
    public void deleteNecessaryFiles() {
        if (GITLET_DIR.exists()) deleteDirectory(GITLET_DIR);
        if (TEST_FILE_HELLO.exists()) TEST_FILE_HELLO.delete();
        if (TEST_FILE_WORK.exists()) TEST_FILE_WORK.delete();
        if (TEST_FILE_BELGIUM.exists()) TEST_FILE_BELGIUM.delete();
    }
    @Test
    /**
     * [test01] work flow before this test:
     *  1. delete .gitlet directory
     */
    public void testInit() throws IOException {
        deleteNecessaryFiles();

        String[] args = new String[]{"init"};
        init(args);
        File commitFile = join(OBJECTS_DIR, "29",
                "f06e3e5dc29c16ba9188804c697a5069815f53");
        assertTrue(commitFile.exists());
        Commit initialCommit = readObject(commitFile, Commit.class);
        CommitTree commitTree = readObject(COMMIT_TREE, CommitTree.class);
        assertTrue(commitTree.head.equals(initialCommit));
        assertTrue(commitTree.branches.get("master").equals(initialCommit));
        System.out.println(initialCommit.time);
    }

    @Test
    /**
     * [test02] work flow before this test:
     *  1. pass [test01]
     *  2. create a file hello.txt, type "hello world" in it
     */
    public void simpleTestAdd() throws IOException {
        testInit();
        if (!TEST_FILE_HELLO.exists()) TEST_FILE_HELLO.createNewFile();
        writeContents(TEST_FILE_HELLO, "hello world");

        String[] args = new String[]{"add", "hello.txt"};
        add(args);
        Stage stage = readObject(STAGING_AREA, Stage.class);
        assertEquals(stage.fileNameToContent.size(), 1);
        assertTrue(stage.fileNameToContent.containsKey("hello.txt"));
        assertTrue(stage.removalFileSet.isEmpty());
        System.out.println(stage.fileNameToContent.get("hello.txt"));
    }

    @Test
    /**
     * [test03] work flow before this test:
     *  1. pass [test02]
     *  2. append "I'm Klay" to the next line of hello.txt
     *  3. create a blank file work.txt
     */
    public void moreTestAdd1() throws IOException {
        simpleTestAdd();
        appendContents(TEST_FILE_HELLO, "\nI'm Klay");
        if (!TEST_FILE_WORK.exists()) TEST_FILE_WORK.createNewFile();

        String[] args = new String[]{"add", "work.txt"};
        add(args);
        Stage stage = readObject(STAGING_AREA, Stage.class);
        assertEquals(stage.fileNameToContent.size(), 2);

        args[1] = "hello.txt";
        add(args);
        stage = readObject(STAGING_AREA, Stage.class);
        assertFalse(stage.fileNameToContent.get("hello.txt").
                equals("2aae6c35c94fcfb415dbe95f408b9ce91ee846ed"));

        System.out.println("hello.txt: " + stage.fileNameToContent.get("hello.txt"));
        System.out.println("work.txt: " + stage.fileNameToContent.get("work.txt"));
    }

    @Test
    /**
     * [test04] work flow before this test:
     *  1. pass [test03]
     */
    public void simpleTestCommit() throws IOException {
        moreTestAdd1();

        String[] args = new String[]{"commit", "2nd commit"};
        commit(args);
        // test staging area is cleared
        Stage stage = readObject(STAGING_AREA, Stage.class);
        assertEquals(stage.fileNameToContent.size(), 0);
        assertEquals(stage.removalFileSet.size(), 0);

        // test the commit records 2 files
        CommitTree commitTree = readObject(COMMIT_TREE, CommitTree.class);
        Commit head = commitTree.head, master = commitTree.branches.get("master");
        assertTrue(head.equals(master));
        assertEquals(head.fileToCommit.size(), 2);
        for (String fileName: head.fileToCommit.keySet())
            assertTrue(head.fileToCommit.get(fileName).equals(head.getSHAHash()));
    }

    @Test
    /**
     * [test05] work flow before this test:
     *  1. pass [test04]
     *  2. write "Yang's work" to work.txt
     *  3. delete "Klay" in hello.txt
     */
    public void moreTestAdd2() throws IOException {
        simpleTestCommit();
        writeContents(TEST_FILE_WORK, "Yang's work");
        String strInHello = readContentsAsString(TEST_FILE_HELLO);
        String newStrInHello = strInHello.substring(0, strInHello.length() - 4);
        writeContents(TEST_FILE_HELLO, newStrInHello);

        String[] args = new String[]{"add", "work.txt"};
        add(args);
        Stage stage = readObject(STAGING_AREA, Stage.class);
        assertEquals(stage.fileNameToContent.size(), 1);

        args[1] = "hello.txt";
        add(args);
        stage = readObject(STAGING_AREA, Stage.class);
        assertEquals(stage.fileNameToContent.size(), 2);
    }

    @Test
    /**
     * [test06] work flow before this test:
     *  1. pass [test05]
     *  2. write "Klay" back to hello.txt
     */
    public void moreTestAdd3() throws IOException {
        moreTestAdd2();
        appendContents(TEST_FILE_HELLO, "Klay");

        Stage stage = readObject(STAGING_AREA, Stage.class);
        assertEquals(stage.fileNameToContent.size(), 2);

        String[] args = new String[]{"add", "hello.txt"};
        add(args);
        stage = readObject(STAGING_AREA, Stage.class);
        assertEquals(stage.fileNameToContent.size(), 1);
    }

    @Test
    /**
     * [test07] work flow before this test:
     *  1. pass [test06]
     */
    public void testRm() throws IOException {
        moreTestAdd3();

        String[] args = new String[]{"rm", "hello.txt"};
        rm(args);
        Stage stage = readObject(STAGING_AREA, Stage.class);
        assertEquals(stage.removalFileSet.size(), 1);

        args[1] = "work.txt";
        rm(args);
        stage = readObject(STAGING_AREA, Stage.class);
        assertTrue(stage.fileNameToContent.isEmpty());
    }

    @Test
    /**
     * [test08] work flow before this test:
     *  1. pass test07
     */
    public void moreTestCommit1() throws IOException {
        testRm();

        String[] args = new String[]{"commit", "3rd commit"};
        commit(args);

        CommitTree commitTree = readObject(COMMIT_TREE, CommitTree.class);
        Commit head = commitTree.head, master = commitTree.branches.get("master");
        assertTrue(head.equals(master));
        assertEquals(head.fileToCommit.size(), 1);
    }

    @Test
    /**
     * [test09] work flow before this test:
     *  1. pass test08
     */
    public void simpleTestLog() throws IOException {
        moreTestCommit1();

        String[] args = new String[]{"log"};
        log(args);
    }

    @Test
    /**
     * [Test10] work flow before this test:
     *  1. pass test09
     */
    public void simpleTestGlobalLog() throws IOException {
        simpleTestLog();

        String[] args = new String[]{"global-log"};
        globalLog(args);
    }

    @Test
    /**
     * [Test11] work flow before this test:
     *  1. pass test09
     */
    public void testFind() throws IOException {
        simpleTestLog();

        String[] args = new String[]{"find", "3rd commit"};
        find(args);
    }

    @Test
    /**
     * [Test12] work flow before this test:
     *  1. pass test09
     */
    public void testStatus() throws IOException {
        simpleTestLog();

        String[] args = new String[]{"status"};
        status(args);
    }

    @Test
    /**
     * [Test13] work flow before this test:
     *  1. pass test12
     */
    public void simpleTestBranch() throws IOException {
        testStatus();

        String[] args = new String[]{"branch", "1B"};
        branch(args);
        CommitTree commitTree = Utils.readObject(COMMIT_TREE, CommitTree.class);
        assertTrue(commitTree.currentBranchName.equals("master"));
        assertEquals(commitTree.branches.size(), 2);
    }

    @Test
    /**
     * [Test14] This test is prepared for upcoming tests for branch & checkout
     * work flow before this test:
     *  1. pass Test13
     *  2. create a file called Belgium.txt, type "Leuven"
     */
    public void testAddAndCommit() throws IOException {
        simpleTestBranch();
        if (!TEST_FILE_BELGIUM.exists()) TEST_FILE_BELGIUM.createNewFile();
        writeContents(TEST_FILE_BELGIUM, "Leuven");

        String[] args = new String[]{"add", "Belgium.txt"};
        add(args);
        args = new String[]{"add", "work.txt"};
        add(args);
        args = new String[]{"commit", "4th commit"};
        commit(args);
        CommitTree commitTree = Utils.readObject(COMMIT_TREE, CommitTree.class);
        assertTrue(commitTree.head.equals(commitTree.branches.get("master")));
        assertFalse(commitTree.head.equals(commitTree.branches.get("1B")));
        args = new String[]{"status"};
        status(args);
    }

    @Test
    /**
     * [Test15] work flow before this test:
     * 1. pass Test14
     */
    public void testCheckout1() throws IOException {
        testAddAndCommit();

        String[] args = new String[]{"checkout", "1B"};
        checkout(args); // after this command, only work.txt will be in the directory
        CommitTree commitTree = Utils.readObject(COMMIT_TREE, CommitTree.class);
        assertTrue(commitTree.currentBranchName.equals("1B"));
        assertTrue(commitTree.branches.get("1B").equals(commitTree.head));
    }
    @Test
    /**
     * [Test16] work flow before this test:
     * 1. pass Test15
     * 2. append "programming" to work.txt
     */
    public void testMoreAddAndCommit1() throws IOException {
        testCheckout1();
        appendContents(TEST_FILE_WORK, "programming");

        String[] args = new String[]{"add", "work.txt"};
        add(args);
        args = new String[]{"commit", "5th commit"};
        commit(args);
        CommitTree commitTree = Utils.readObject(COMMIT_TREE, CommitTree.class);
        assertFalse(commitTree.head.equals(commitTree.branches.get("master")));
        assertTrue(commitTree.head.equals(commitTree.branches.get("1B")));
        Commit head = commitTree.head;
        assertEquals(head.fileToCommit.size(), 1);
        assertTrue(head.fileToCommit.get("work.txt").equals(head.getSHAHash()));
    }

    @Test
    /**
     * [Test 17] work flow before this test:
     * 1. pass Test16
     */
    public void testCheckout2() throws IOException {
        testMoreAddAndCommit1();
        String[] args = new String[]{"checkout", "master"};
        checkout(args);

        CommitTree commitTree = Utils.readObject(COMMIT_TREE, CommitTree.class);
        assertTrue(commitTree.currentBranchName.equals("master"));
        assertTrue(commitTree.branches.get("master").equals(commitTree.head));
        Commit head = commitTree.head;
        assertEquals(head.fileToCommit.size(), 2);
    }

    @Test
    /**
     * [Test 18] work flow before this test:
     * 1. pass Test17
     */
    public void testRmBranch() throws IOException {
        testCheckout2();
        String[] args = new String[]{"rm-branch", "1B"};
        rmBranch(args);

        CommitTree commitTree = Utils.readObject(COMMIT_TREE, CommitTree.class);
        assertEquals(commitTree.branches.size(), 1);
    }

    @Test
    /**
     * [Test 19] work flow before this test:
     * 1. pass Test01
     */
    public void testBasicStatus() throws IOException {
        testInit();
        String[] args = new String[]{"status"};
        status(args);
    }

    @Test
    /**
     * [Test 20] work flow before this test:
     * 1. pass Test03
     */
    public void testRmStatus() throws IOException {
        moreTestAdd1();
        String[] args = new String[]{"rm", "hello.txt"};
        rm(args);
        assertFalse(TEST_FILE_HELLO.exists());
        args = new String[]{"status"};
        status(args);
    }

    @Test
    /**
     * [Test 21] work flow before this test
     * 1. pass Test04
     */
    public void testRmAndAdd() throws IOException {
        simpleTestCommit();
        String[] args = new String[]{"rm", "hello.txt"};
        rm(args);
        assertFalse(TEST_FILE_HELLO.exists());
        TEST_FILE_HELLO.createNewFile();
        writeContents(TEST_FILE_HELLO, "hello world\nI'm Klay");
        args = new String[]{"add", "hello.txt"};
        add(args);
        args = new String[]{"status"};
        status(args);
    }

    @Test
    /**
     * [Test 22] work flow before this test:
     * 1. pass Test03
     */
    public void testAddAndRm() throws IOException {
        moreTestAdd1();
        String[] args = new String[]{"rm", "hello.txt"};
        rm(args);
        assertFalse(TEST_FILE_HELLO.exists());
        args = new String[]{"status"};
        status(args);
    }

    @Test
    /**
     * [Test 23] work flow before this test:
     * 1. pass Test03
     */
    public void testEmptyCommit() throws IOException {
        moreTestAdd1();
        String[] args = new String[]{"commit", ""};
        commit(args);
    }

    @Test
    /**
     * [Test 24]
     */
    public void testCheckoutAfterAddAndRm() throws IOException {
        moreTestAdd1();
        String[] args = new String[]{"rm", "work.txt"};
        rm(args);
        args = new String[]{"status"};
        status(args);
    }

    @Test
    /**
     * [Test 25]
     */
    public void testMerge() throws IOException {
        testInit();
        branch(new String[]{"branch", "1B"});

        if (!TEST_FILE_HELLO.exists()) TEST_FILE_HELLO.createNewFile();
        writeContents(TEST_FILE_HELLO, "hello world\n");

        String[] args = new String[]{"add", "hello.txt"};
        add(args);
        commit(new String[]{"commit", "master branch"});
        Commit master = readObject(COMMIT_TREE, CommitTree.class).head;

        checkout(new String[]{"checkout", "1B"});
        if (!TEST_FILE_WORK.exists()) TEST_FILE_HELLO.createNewFile();
        writeContents(TEST_FILE_HELLO, "I'm Klay\n");
        args = new String[]{"add", "hello.txt"};
        add(args);
        commit(new String[]{"commit", "1B branch"});

        merge(new String[]{"merge", "master"});
        Commit head = readObject(COMMIT_TREE, CommitTree.class).head;
        assertTrue(master.equals(head.secondParent));
        log(new String[]{"log"});
    }

    @Test
    public void testSpecialMerge() throws IOException {
        simpleTestCommit();
        branch(new String[]{"branch", "b1"});
        appendContents(TEST_FILE_HELLO, "2nd");
        add(new String[]{"add", "hello.txt"});
        commit(new String[]{"commit", "2nd"});
        appendContents(TEST_FILE_HELLO, "3rd");
        add(new String[]{"add", "hello.txt"});
        commit(new String[]{"commit", "3rd"});
        merge(new String[]{"merge", "b1"});
    }

    @Test
    public void testMergeParent2() throws IOException {
        testInit();
        branch(new String[]{"branch", "B1"});
        branch(new String[]{"branch", "B2"});

        checkout(new String[]{"checkout", "B1"});
        writeContents(TEST_FILE_H, "h");
        add(new String[]{"add", "h.txt"});
        commit(new String[]{"commit", "Add h.txt"});

        checkout(new String[]{"checkout", "B2"});
        writeContents(TEST_FILE_F, "f");
        add(new String[]{"add", "f.txt"});
        commit(new String[]{"commit", "Add f.txt"});
        branch(new String[]{"branch", "C1"});

        writeContents(TEST_FILE_G, "g");
        add(new String[]{"add", "g.txt"});
        rm(new String[]{"rm", "f.txt"});
        commit(new String[]{"commit", "g.txt added, f.txt removed"});

        checkout(new String[]{"checkout", "B1"});
        merge(new String[]{"merge", "C1"});
        assertTrue(TEST_FILE_F.exists());
        assertTrue(TEST_FILE_H.exists());
        assertFalse(TEST_FILE_G.exists());

        merge(new String[]{"merge", "B2"});
        assertTrue(TEST_FILE_G.exists());
        assertTrue(TEST_FILE_H.exists());
        assertFalse(TEST_FILE_F.exists());
    }

    @Test
    public void testCommitHashcode() {
        Set<Commit> set = new HashSet<>();
        Commit commit1 = new Commit(null, "abc", new Date());
        Commit commit2 = new Commit(null, "abc", new Date());
        assertTrue(commit1.equals(commit2));
        assertTrue(Objects.equals(commit1, commit2));
        set.add(commit1);
        assertTrue(set.contains(commit2));
    }
}
