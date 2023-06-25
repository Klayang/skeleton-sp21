package gitlet;

import java.util.Date;

/** General exception indicating a Gitlet error.  For fatal errors, the
 *  result of .getMessage() is the error message to be printed.
 *  @author P. N. Hilfinger
 */
class GitletException extends RuntimeException {


    /** A GitletException with no message. */
    GitletException() {
        super();
    }

    /** A GitletException MSG as its message. */
    GitletException(String msg) {
        super(msg);
    }

    static void handleException(String message) {
        System.out.println(message);
        System.exit(0);
    }

//    static void handleNoArguments() {
//        System.out.println("Please enter a command.");
//        System.exit(0);
//    }
//
//    static void handleIncorrectCommand() {
//        System.out.println("No command with that name exists.");
//        System.exit(0);
//    }
//
//    static void handleIncorrectOperands() {
//        System.out.println("Incorrect operands.");
//        System.exit(0);
//    }
//
//    static void handlePreInitializedCommand() {
//        System.out.println("Not in an initialized Gitlet directory.");
//        System.exit(0);
//        Date date = new Date();
//    }
//
//    static void handleNotExistedFile() {
//        System.out.println("File does not exist.");
//        System.exit(0);
//    }
//
//    static void handleDuplicateInitialization() {
//        System.out.println("A Gitlet version-control system already exists in the current directory.");
//        System.exit(0);
//    }
//
//    static void handleNoChangesToCommit() {
//        System.out.println("No changes added to the commit.");
//        System.exit(0);
//    }
//
//    static void handleNoCommitMessage() {
//        System.out.println("Please enter a commit message.");
//        System.exit(0);
//    }
//
//    static void handleInvalidRemoval() {
//        System.out.println("No reason to remove the file.");
//        System.exit(0);
//    }
//
//    static void handleNoSpecifiedMessage() {
//        System.out.println("Found no commit with that message.");
//        System.exit(0);
//    }
//
//    static void handleFileNotExistInTheCommit() {
//        System.out.println("File does not exist in that commit.");
//        System.exit(0);
//    }
//
//    static void handleNoSpecifiedCommit() {
//        System.out.println("No commit with that id exists.");
//        System.exit(0);
//    }
//
//    static void handleNoSpecifiedBranch() {
//        System.out.println("No such branch exists.");
//        System.exit(0);
//    }
//
//    static void handleNoSpecifiedBranchToRemove() {
//        System.out.println("A branch with that name does not exist.");
//        System.exit(0);
//    }
//
//    static void handleDuplicateCheckout() {
//        System.out.println("No need to checkout the current branch.");
//        System.exit(0);
//    }
//
//    static void handleCheckoutUntrackedFile() {
//        System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
//        System.exit(0);
//    }
//
//    static void handleExistedBranch() {
//        System.out.println("A branch with that name already exists.");
//        System.exit(0);
//    }
//
//    static void handleOccupiedStageInMerge() {
//        System.out.println("You have uncommitted changes.");
//        System.exit(0);
//    }
//
//    static void handleNoSpecifiedBranchInMerge() {
//        System.out.println("A branch with that name does not exist.");
//        System.exit(0);
//    }
//
//    static void handleMergeItself() {
//        System.out.println("Cannot merge a branch with itself.");
//        System.exit(0);
//    }

}
