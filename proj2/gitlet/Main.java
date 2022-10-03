package gitlet;

import java.util.Objects;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        if (args.length == 0) {
            System.out.println("Please at least type in some arguments.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                Repository.init();
                break;
            case "add":
                String tobeAdded = args[1];
                Repository.add(tobeAdded);
                break;
            case "rm":
                String tobeRemoved = args[1];
                Repository.remove(tobeRemoved);
                break;
            case "commit":
                String commitMessage = args[1];
                if (commitMessage == null) {
                    throw new GitletException("Need a commit message.");
                }
                Repository.commit(commitMessage);
                break;
            case "log":
                Repository.log(Repository.getHeadCommit());
                break;
            case "status":
                Repository.status();
                break;
            case "global-log":
                Repository.globalLog();
                break;
            case "find":
                String messageToFind = args[1];
                Repository.find(messageToFind);
                break;
            case "checkout":
                if (args.length == 3) {
                    if (Objects.equals(args[1], "--")) {
                        Repository.checkoutFile(args[2]);
                    } else {
                        System.out.println("Invalid args.");
                    }
                }else if (args.length == 4) {
                    if (Objects.equals(args[2], "--")) {
                        Repository.checkoutCommitFile(args[1], args[3]);
                    } else {
                        System.out.println("Invalid args.");
                    }
                } else if (args.length == 2){
                    Repository.checkoutBranch(args[1]);
                } else {
                    System.out.println("Invalid args.");
                }
                break;
            case "branch":
                String branchName = args[1];
                Repository.branch(branchName);
                break;
            case "rm-branch":
                String branchNameToRm = args[1];
                Repository.rmBranch(branchNameToRm);
                break;
            case "reset":
                String CID = args[1];
                Repository.reSet(CID);
                break;
            case "merge":
                break;
            default:
                throw new GitletException(firstArg + "is not a valid argument");
            }
        }
    }
