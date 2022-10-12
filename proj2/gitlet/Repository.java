package gitlet;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;


/** Represents a gitlet repository.
 *  does at a high level.
 *
 *  @author Hang
 */
public class Repository {
    /**
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */


    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /** The branches' directory. */
    public static final File BRANCH_DIR = join(GITLET_DIR, "branches");

    /** The master pointer, the default branch when the repo is created. */
    public static final File master = join(BRANCH_DIR, "master");

    /** The commits' directory. */
    public static final File COMMITS_DIR = join(GITLET_DIR, "commits");

    /** HEAD pointer, which is pointing to the head commit,
     * represents as the commit's sha-1 value. */
    public static File HEAD = join(GITLET_DIR, "HEAD");

    /** The blobs' directory. */
    public static final File BLOBS_DIR = join(GITLET_DIR, "blobs");

    /** The staging file */
    private static final File STAGING_DIR = join(GITLET_DIR, "staged");

    /** The file of staging for adding */
    private static final File added = join(STAGING_DIR, "added");

    /** The file of staging for removing */
    private static final File removed = join(STAGING_DIR, "removed");


    public static void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        } else {
            setupPersistence();
            setInitialCommit();
        }
    }
    /** set the initial commit helper func */
    private static void setInitialCommit() {
        Commit initial = new Commit("initial commit", null, null);
        String sha1Generator = initial.archive.toString() + initial.getParent() + initial.getTimeStamp().toString() + initial.getMessage();
        initial.CID = sha1(sha1Generator);
        File initialCommit = join(COMMITS_DIR, initial.CID);
        writeContents(master, initial.CID);
        writeContents(HEAD, "master");
        writeObject(initialCommit, initial);
        writeContents(master, initial.CID);
    }

    /** implement the add command */
    public static void add(String fileName) {
        Stage stageRead = readObject(removed, Stage.class);
        File toAdd = join(CWD, fileName);
        if (!toAdd.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        } else {
            if (isTracked(toAdd, getHeadCommit())) {
                unStage(stageRead, fileName, removed);
            } else {
                Blob blob = new Blob(toAdd);
                File blobFile = Utils.join(BLOBS_DIR, blob.BID);
                writeObject(blobFile, blob);
                stageFor(fileName, blob, added);
                unStage(stageRead, fileName, removed);
            }
        }
    }
    /** add to stage helper func */
    private static void stageFor(String fileName, Blob blob, File addOrRm) {
        Stage toMod = fromStaged(addOrRm);
        if (blob == null) {
            toMod.fbPair.put(fileName, null);
        } else {
            toMod.fbPair.put(fileName, blob.BID);
        }
        writeObject(addOrRm, toMod);
    }
    private static Stage fromStaged(File staged) {
        return readObject(staged, Stage.class);
    }

    /** implement the remove command */
    public static void remove(String fileName) {
        Set<String> directoryFiles = new HashSet<String>();
        if (plainFilenamesIn(CWD) != null) {
            directoryFiles.addAll(Objects.requireNonNull(plainFilenamesIn(CWD)));
        }
        Set<String> deleted = deletedWithoutStage(directoryFiles);
        File toRemove = join(CWD, fileName);
        String onlyRead = getActiveBranch();
        if (deleted.contains(fileName)) {
            stageFor(fileName, null, removed);
        } else if (!toRemove.exists()) {
            System.out.println("No such file to remove");
            System.exit(0);
        } else {
            removeHelper(fileName, onlyRead);
        }
    }


    private static void unStage(Stage toUnstage, String fileName, File addOrRm) {
        toUnstage.fbPair.remove(fileName);
        writeObject(addOrRm, toUnstage);
    }
    /** 1. If the file has not been staged for addition and not archived in current commit, then exit.
     * 2. If the file has been added but modified again without stage, then exit
     * 3. Unstage from the stage of addition.
     * */
    private static void removeHelper(String fileName, String CID) {
        Commit commitRead = getCommit(CID);
        Stage stageRead = readObject(added, Stage.class);
        File toRead = join(CWD, fileName);
        String toReadSha1 = sha1(toRead.getName() + readContentsAsString(toRead));
        String getContentSha1 = stageRead.fbPair.getOrDefault(fileName, "No such key");
            if (getContentSha1.equals("No such key")) {  //not staged for addition
                if (!isTracked(toRead, commitRead)) {   //not tracked
                    System.out.println("No reason ro remove the file.");
                    System.exit(0);
                } else {    //not staged for addition, but it's tracked by parent commit
                    String BID = commitRead.archive.get(fileName);
                    stageFor(fileName, getBlob(BID), removed);
                    restrictedDelete(toRead);
                }
            } else if (!getContentSha1.equals(toReadSha1)) {    //staged for addition, but the content has been changed.
                System.out.println("No reason to remove the file.");
                System.exit(0);
            } else {    //staged for addition, if it's tracked, delete it, and just unstage if not.
                if (isTracked(toRead, commitRead)) {
                    String BID = stageRead.fbPair.get(fileName);
                    stageFor(fileName, getBlob(BID), removed);
                    unStage(stageRead, fileName, added);
                    restrictedDelete(toRead);
                } else {
                    unStage(stageRead, fileName, added);
                }
            }
    }
    private static Blob getBlob(String BID) {
        File blobRead = join(BLOBS_DIR, BID);
        return readObject(blobRead, Blob.class);
    }

    private static boolean isTracked(File toRead, Commit commit) {
        String contentBlob = commit.archive.get(toRead.getName());
        if (contentBlob == null) {
            return false;
        } else {
            return getBlob(contentBlob).getContent().equals(readContentsAsString(toRead));
        }
    }
    /** helper func to get the head commit */
    public static Commit getHeadCommit() {
        String headCID = getActiveBranch();
        File headCommitFile = join(COMMITS_DIR, headCID);
        return readObject(headCommitFile, Commit.class);
    }

    private static String getActiveBranch() {
        return readContentsAsString(activeBranchFile());
    }


    private static File activeBranchFile() {
        return join(BRANCH_DIR, readContentsAsString(HEAD));
    }

    /** implement the commit command */
    public static void commit(String message, String parent2) {
        Commit current = new Commit(message, getActiveBranch(), parent2);
        current.setArchive(fromCommitFile(getActiveBranch()).archive);
        modifyArchive(current.archive);
        String sha1Generator = current.archive.toString() + current.getParent() + current.getTimeStamp().toString() + current.getMessage();
        current.CID = sha1(sha1Generator);
        writeContents(activeBranchFile(), current.CID);
        writeObject(current.getToFile(), current);
    }

    public static void log(Commit commit) {
        logContent(commit);
        if (commit.getParent() == null) {
            return;
        } else {
            log(getParentCommit(commit));
        }
    }

    public static void globalLog() {
        List<String> commitFiles = plainFilenamesIn(COMMITS_DIR);
        assert commitFiles != null;
        for (String fileName : commitFiles) {
            logContent(fromCommitFile(fileName));
        }
    }

    public static void find(String messageToFind) {
        List<String> commitFiles = plainFilenamesIn(COMMITS_DIR);
        boolean haveFound = false;
        assert commitFiles != null;
        for (String fileName : commitFiles) {
            if (messageToFind.equals(fromCommitFile(fileName).getMessage())) {
                System.out.println(fromCommitFile(fileName).CID);
                haveFound = true;
            }
        }
        if (!haveFound) {
            System.out.println("Found no commit with that message.");
        }
    }

    /**
     * (modified)
     * (modified)
     * (deleted)
     * */
    public static void status() {
        System.out.println("=== Branches ===");
        List<String> branchFiles = plainFilenamesIn(BRANCH_DIR);
        String activeBranchName = activeBranchFile().getName();
        assert branchFiles != null;
        for (String branch : branchFiles) {
            if (branch.equals(activeBranchName)) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.print("\n");
        Set<String> directoryFiles = new HashSet<String>();
        if (plainFilenamesIn(CWD) != null) {
            directoryFiles.addAll(Objects.requireNonNull(plainFilenamesIn(CWD)));
        }
        HashMap<String, String> addedFiles = readObject(added, Stage.class).fbPair;
        HashMap<String, String> removedFiles = readObject(removed, Stage.class).fbPair;
        System.out.println("=== Staged Files ===");
        for (String key : addedFiles.keySet()) {
            System.out.println(key);
        }
        System.out.print("\n");
        System.out.println("=== Removed Files ===");
        for (String key : removedFiles.keySet()) {
            System.out.println(key);
        }
        System.out.print("\n");
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String fileName : deletedWithoutStage(directoryFiles)) {
            System.out.println(fileName + " (deleted)");
        }
        for (String fileName : modifiedWithoutStage(directoryFiles)) {
            System.out.println(fileName + " (modified)");
        }

        System.out.print("\n");
        System.out.println("=== Untracked Files ===");
        for (String file : untrackedFiles(directoryFiles)) {
            System.out.println(file);
        }
        System.out.print("\n");
    }

    private static Set<String> untrackedFiles (Set<String> directoryFiles) {
        Commit commit = getHeadCommit();
        Set<String> unTracked = new HashSet<>();
        for (String fileName : directoryFiles) {
            //boolean isTracked = isTracked(join(CWD, fileName), commit);
            HashMap<String, String> map = commit.archive;
            if (fromStaged(removed).fbPair.containsKey(fileName) ||
                    (!fromStaged(added).fbPair.containsKey(fileName) && !map.containsKey(fileName))) {
                unTracked.add(fileName);
            }
        }
        return unTracked;
    }

    private static Set<String> modifiedWithoutStage(Set<String> directoryFiles) {
        Commit commit = getHeadCommit();
        HashMap<String, String> fileToCompare = new HashMap<String, String>();
        HashMap<String, String> addedFiles = fromStaged(added).fbPair;
        Set<String> toDisplay = new HashSet<String>();
        for (String fileName : directoryFiles) {
            File toCompare = join(CWD, fileName);
            fileToCompare.put(fileName, new Blob(toCompare).BID);
        }
        for (String key : addedFiles.keySet()) {
            if (fileToCompare.get(key) != null && !addedFiles.get(key).equals(fileToCompare.get(key))) {
                toDisplay.add(key);
            }
        }
        for (String key : commit.archive.keySet()) {
            if (fileToCompare.get(key) != null && !commit.archive.get(key).equals(fileToCompare.get(key))) {
                toDisplay.add(key);
            }
        }
        return toDisplay;
    }

    private static Set<String> deletedWithoutStage(Set<String> directoryFiles) {
        Commit commit = getHeadCommit();
        Set<String> addedFiles = new HashSet<String>(fromStaged(added).fbPair.keySet());
        Set<String> removedFiles = new HashSet<String>(fromStaged(removed).fbPair.keySet());
        Set<String> toDisplay = new HashSet<String>();
        Set<String> commitFiles = commit.archive.keySet();
        for (String fileName : addedFiles) {
            if (!directoryFiles.contains(fileName)) {
                toDisplay.add(fileName);
            }
        }
        commitFiles.removeAll(removedFiles);
        commitFiles.removeAll(directoryFiles);
        toDisplay.addAll(commitFiles);
        return toDisplay;
    }


    public static void branch(String branchName) {
        File newBranch = join(BRANCH_DIR, branchName);
        if (newBranch.exists()) {
            System.out.println("A branch with that name already exists.");
        } else {
            writeContents(newBranch, getActiveBranch());
        }
    }

    public static void rmBranch(String branchName) {
        boolean removed = false;
        if (readContentsAsString(HEAD).equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
        } else {
            for (String fileName : Objects.requireNonNull(plainFilenamesIn(BRANCH_DIR))) {
                if (fileName.equals(branchName)) {
                    join(BRANCH_DIR, fileName).delete();
                    removed = true;
                }
            }
            if (!removed) {
                System.out.println("A branch with that name does not exist.");
            }
        }
    }

    private static void logContent(Commit commit) {
        System.out.println("===");
        System.out.println("commit " + commit.CID);
        if (commit.getParent2() != null) {
            System.out.println("Merge: " + commit.getParent().substring(0, 8) + " " + commit.getParent2().substring(0, 8));
        }
        System.out.println(
                "Date: " + new SimpleDateFormat("E MMM dd HH:mm:ss yyyy Z",
                        new Locale("en")).format(commit.getTimeStamp())
        );
        System.out.println(commit.getMessage() + "\n");
    }

    private static Commit getParentCommit(Commit current) {
        if (current.getParent() == null) {
            return null;
        } else {
            File parentCommit = join(COMMITS_DIR, current.getParent());
            return readObject(parentCommit, Commit.class);
        }
    }

    /** helper func to update the commit's archive */
    private static void modifyArchive(HashMap<String, String> archive) {
        Stage refAdd = fromStaged(added);
        Stage refRm = fromStaged(removed);
        if (refRm.fbPair.isEmpty() && refAdd.fbPair.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        } else {
            pairFromStage(refAdd, archive, true);
            pairFromStage(refRm, archive, false);
            writeObject(added, refAdd);
            writeObject(removed, refRm);
            clearStage(refAdd, added);
            clearStage(refRm, removed);
        }
    }
    /** helper func to get the fbPair from stage */
    private static void pairFromStage(Stage toGet, HashMap<String, String> toMod, boolean isAdd) {
        for (String fileName : toGet.fbPair.keySet()) {
            if (isAdd) {
                toMod.put(fileName, toGet.fbPair.get(fileName));
            } else {
                toMod.remove(fileName, toGet.fbPair.get(fileName));
            }
        }
    }
    /** helper func to clear the staging area after a commit */
    private static void clearStage(Stage toClear, File file) {
        toClear.fbPair.clear();
        writeObject(file, toClear);
    }

    private static Commit fromCommitFile(String CID) {
        File commitFile = join(COMMITS_DIR, CID);
        if (!commitFile.exists()) {
            System.out.println("the repository structure has been destroyed.");
            System.exit(0);
            return null;
        } else {
            return readObject(commitFile, Commit.class);
        }
    }

    public static void reSet(String CID6) {
        boolean foundCommit = false;
        for (String CID : Objects.requireNonNull(plainFilenamesIn(COMMITS_DIR))) {
            if (CID.contains(CID6)) {
                clearStage(readObject(added, Stage.class), added);
                clearStage(readObject(removed, Stage.class), removed);
                writeContents(join(BRANCH_DIR, readContentsAsString(HEAD)), CID);
                checkoutCommit(CID);
                foundCommit = true;
                break;
            }
        }
        if (!foundCommit) {
            System.out.println("No commit with that id exists.");
        }
    }

    public static void merge(String branchName) {
        String branchToMerge = readContentsAsString(join(BRANCH_DIR, branchName));
        if (!fromStaged(added).fbPair.isEmpty() || !fromStaged(removed).fbPair.isEmpty()) {
            System.out.println("You have uncommitted changes.");
        } else if (branchToMerge.equals(getActiveBranch())) {
          System.out.println("Cannot merge a branch with itself.");
        } else if (!Objects.requireNonNull(plainFilenamesIn(BRANCH_DIR)).contains(branchName)) {
            System.out.println("A branch with that name does not exists.");
        } else {
            String splitPointCID = splitPoint(activeBranchFile().getName(), branchName);
            mergeCondition(getActiveBranch(), branchToMerge, splitPointCID);
            System.out.println();
            commit("Merged " + branchName + "into " + readContentsAsString(HEAD) + ".", branchToMerge);
        }
    }

    private static void mergeCondition(String headCID, String branchToMerge, String splitPoint) {
        Commit head = getCommit(headCID);
        Commit other = getCommit(branchToMerge);
        Commit split = getCommit(splitPoint);
        boolean isConflicted = false;
        Set<String> inter = new HashSet<String>(head.archive.keySet());
        inter.retainAll(other.archive.keySet());
        Set<String> diffOther = new HashSet<String>(other.archive.keySet());
        diffOther.removeAll(head.archive.keySet());
        Set<String> diffHead = new HashSet<String>(head.archive.keySet());
        diffHead.removeAll(other.archive.keySet());
        for (String fileName : inter) {     //indicates that all BID below are not null
            String headFile = head.archive.get(fileName);
            String otherFile = other.archive.get(fileName);
            String splitFile = split.archive.get(fileName);
            if (!splitFile.equals(headFile)) {  //  modified in head
                if (splitFile.equals(otherFile)) {     // 2
                    continue;
                } else {
                    if (!headFile.equals(otherFile)) {     // 3.2(1) mod in both other and head
                        conflictCon(fileName, headCID, branchToMerge);
                        add(fileName);
                        isConflicted = true;
                    } else {    // 3.1
                        continue;
                    }
                }
            } else {    // unmodified in head
                 if (!headFile.equals(otherFile)) {     // 1 add the file in other
                    checkoutFile(fileName, branchToMerge);
                    stageFor(fileName, getBlob(otherFile), added);
                } else {    // unchanged all the time
                    continue;
                }
            }
        }
        for (String fileName : diffHead) {  // in head not in other
            String splitFile = split.archive.get(fileName);
            String headFile = head.archive.get(fileName);
            if (splitFile != null) {
                if (!splitFile.equals(headFile)) {  // 3.2(2) mod in head and del in other
                    conflictCon(fileName, headCID, null);
                    add(fileName);
                    isConflicted = true;
                } else {    // 6 remove the file not in other
                    stageFor(fileName, getBlob(headFile), removed);
                    restrictedDelete(fileName);
                }
            } else {    // 4
                continue;
            }
        }
        for (String fileName : diffOther) {
            String splitFile = split.archive.get(fileName);
            String otherFile = other.archive.get(fileName);
            if (splitFile != null) {
                if (splitFile.equals(otherFile)) {  // 7
                    continue;
                } else {    // 3.2(3) mod in other del in head
                    conflictCon(fileName, null, branchToMerge);
                    add(fileName);
                    isConflicted = true;
                }
            } else {    // 5 add the file in other
                checkoutFile(fileName, branchToMerge);
                stageFor(fileName, getBlob(otherFile), added);
            }
        }
        if (isConflicted) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    private static void conflictCon(String fileName, String headCID, String branchToMerge) {
        File conFile = join(CWD, fileName);
        if (headCID == null) {
        String otherContent = getBlob(getCommit(branchToMerge).archive.get(fileName)).getContent();
        writeContents(conFile, "<<<<<<< HEAD\n" + "\n=======\n" + otherContent + ">>>>>>>");
        } else if (branchToMerge == null) {
            String headContent = getBlob(getCommit(headCID).archive.get(fileName)).getContent();
            writeContents(conFile, "<<<<<<< HEAD\n" + headContent + "\n=======\n" + ">>>>>>>");
        } else {
            String otherContent = getBlob(getCommit(branchToMerge).archive.get(fileName)).getContent();
            String headContent = getBlob(getCommit(headCID).archive.get(fileName)).getContent();
            writeContents(conFile, "<<<<<<< HEAD\n" + headContent + "\n=======\n" + otherContent + ">>>>>>>");
        }
    }

    private static int depth(String CID, int number) {
        if (getCommit(CID).getParent() == null) {
            return number;
        } else {
            return depth(getCommit(CID).getParent(), number + 1);
        }
    }
    private static String splitPoint(String headBranch, String branchToMerge) {
        String headCID = getBranch(headBranch);
        String mergeCID = getBranch(branchToMerge);
        int margin = depth(headCID, 1) - depth(mergeCID, 1);
        if (margin > 0) {
            String balanced = getAncestor(headCID, margin);
            if (balanced.equals(mergeCID)) {
                System.out.println("Given branch is an ancestor of the current branch.");
                System.exit(0);
            }
            return LCParent(balanced, mergeCID);
        } else {
            String balanced = getAncestor(mergeCID, margin);
            if (balanced.equals(headCID)) {
                checkoutBranch(branchToMerge);
                System.out.println("Current branch fast-forwarded.");
                System.exit(0);
            }
            return LCParent(balanced, headCID);
        }
    }

    private static String getAncestor(String CID, int number) {
        if (number == 0) {
            return CID;
        } else {
            return getAncestor(parentOf(CID), number - 1);
        }
    }
    private static String LCParent(String CID1, String CID2) {
        if (parentOf(CID1) == null || parentOf(CID2) == null) {
            return null;
        } else if (parentOf(CID1).equals(parentOf(CID2))) {
            return parentOf(CID1);
        } else {
            return LCParent(parentOf(CID1), parentOf(CID2));
        }
    }
    private static String parentOf(String CID) {
        return getCommit(CID).getParent();
    }
    private static String getBranch(String branchName) {
        File branchFile = join(BRANCH_DIR, branchName);
        return readContentsAsString(branchFile);
    }
    public static void checkoutFile(String fileName) {
        checkoutFile(fileName, getActiveBranch());
    }
    private static void checkoutFile(String fileName, String CID) {
        Commit commit = getCommit(CID);
        if (commit.archive.containsKey(fileName)) {
            File toCheckout = join(CWD, fileName);
            String fileBID = commit.archive.get(fileName);
            writeContents(toCheckout, getBlob(fileBID).getContent());
        } else {
            System.out.println("File does not exist in that commit.");
        }
    }

    public static void checkoutCommitFile(String CID6, String fileName) {
        if (getActiveBranch().contains(CID6)) {
            //System.out.println("No need to checkout current branch.");
            checkoutFile(fileName, getActiveBranch());
            System.exit(0);
        }
        boolean checked = false;
        for (String CID : commitFiles()) {
            if (CID.contains(CID6)) {
                checkoutFile(fileName, CID);
                checked = true;
            }
        }
        if (!checked) {
            System.out.println("No commit with that id exists.");
        }
    }

    public static void checkoutBranch(String branchName) {
        Set<String> directoryFiles = new HashSet<>();
        if (plainFilenamesIn(CWD) != null) {
            directoryFiles.addAll(Objects.requireNonNull(plainFilenamesIn(CWD)));
        }
        Set<String> untracked = untrackedFiles(directoryFiles);
        File branchFile = join(BRANCH_DIR, branchName);
        if (branchFile.exists()) {
            if (readContentsAsString(HEAD).equals(branchName)) {    //the checkout branch is current branch
                System.out.println("No need to checkout the current branch.");
            } else {    //
                Commit commit = getCommit(getActiveBranch());
                if (!untracked.isEmpty()) {
                    for (String fileName : untracked) {
                        if (!commit.archive.containsKey(fileName)) {
                            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                            System.exit(0);
                        }
                    }
                }
                clearStage(readObject(added, Stage.class), added);
                clearStage(readObject(removed, Stage.class), removed);
                writeContents(HEAD, branchName);
                checkoutCommit(getActiveBranch());
            }
        } else {
            System.out.println("No such branch exists.");
        }
    }

    private static void checkoutCommit(String CID) {
        Commit commit = getCommit(CID);
        //List<String> directoryFiles = plainFilenamesIn(CWD);
        if (plainFilenamesIn(CWD) != null) {
            for (String fileName : Objects.requireNonNull(plainFilenamesIn(CWD))) {
                if (!commit.archive.containsKey(fileName)) {
                    restrictedDelete(fileName);
                }
            }
        }
        for (String fileName : commit.archive.keySet()) {
            File file = join(CWD, fileName);
            writeContents(file, getBlob(commit.archive.get(fileName)).getContent());
        }
    }

    private static Commit getCommit(String CID) {
        return readObject(join(COMMITS_DIR, CID), Commit.class);
    }

    private static List<String> commitFiles() {
        return plainFilenamesIn(COMMITS_DIR);
    }


    private static void setupPersistence() {
        GITLET_DIR.mkdir();
        COMMITS_DIR.mkdir();
        BLOBS_DIR.mkdir();
        STAGING_DIR.mkdir();
        BRANCH_DIR.mkdir();
        Stage initStage = new Stage(new HashMap<String, String>());
        writeObject(added, initStage);
        writeObject(removed, initStage);
    }
}
