package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.*;
import static gitlet.Commands.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        try {
            if (args.length == 0) GitletException.handleException("Please enter a command.");
            String firstArg = args[0];
            switch (firstArg) {
                case "init":
                    init(args);
                    break;
                case "add":
                    add(args);
                    break;
                case "commit":
                    commit(args);
                    break;
                case "rm":
                    rm(args);
                    break;
                case "log":
                    log(args);
                    break;
                case "global-log":
                    globalLog(args);
                    break;
                case "find":
                    find(args);
                    break;
                case "status":
                    status(args);
                    break;
                case "checkout":
                    checkout(args);
                    break;
                case "branch":
                    branch(args);
                    break;

                case "rm-branch":
                    rmBranch(args);
                    break;
//            case "reset":
//                reset(args);
//                break;
//            case "merge":
//                merge(args);
//                break;
                default:
                    GitletException.handleException("No command with that name exists.");
            }
        }
        catch(IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
