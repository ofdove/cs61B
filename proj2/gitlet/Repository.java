package gitlet;


import javax.management.remote.JMXServerErrorException;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

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

    /* TODO: fill in the rest of this class. */

    /**
     * TODO: create persistence area for the repo (commits, blobs)
     * */
    public static void init() {
        if (GITLET_DIR.exists()) {
            throw new RuntimeException("This directory has already been initialized as a repo");
        } else {
            setupPersistence();
            setInitialCommit();
        }
    }
    /** set the initial commit helper func */
    private static void setInitialCommit() {
        Commit initial = new Commit("initial commit", null);
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
            System.out.println("The file to add is not exist");
            System.exit(0);
        } else {
            Blob blob = new Blob(toAdd);
            File blobFile = Utils.join(BLOBS_DIR, blob.BID);
            if (blobFile.exists()) {
                return;
            } else {
                writeObject(blobFile, blob);
                stageFor(fileName, blob, added);
                unStage(stageRead, fileName, removed);
            }
        }
    }
    /** add to stage helper func */
    private static void stageFor(String fileName,Blob blob, File addOrRm) {
        Stage toMod = fromStaged(addOrRm);
        toMod.fbPair.put(fileName, blob.BID);
        writeObject(addOrRm, toMod);
    }
    private static Stage fromStaged(File staged) {
        return readObject(staged, Stage.class);
    }

    /** implement the remove command
     * TODO: unstage the file if it's staged for addition
     * TODO: if the file is tracked by current commit, stage it for removal and remove the file
     * TODO: DO NOT remove a file which is neither staged for addition nor tracked by current commit.
     */
    public static void remove(String fileName) {
        File toRemove = join(CWD, fileName);
        String onlyRead = getActiveBranch();
        if (!toRemove.exists()) {
            System.out.println("No such file to remove");
            System.exit(0);
        } else {
            removeHelper(fileName, onlyRead);
            restrictedDelete(toRemove);
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
                    System.out.println("This file is not added to the repo, or has been modified since last commit, please add it anyway.");
                    System.exit(0);
                } else {    //not staged for addition, but it's tracked by parent commit
                    String BID = commitRead.archive.get(fileName);
                    stageFor(fileName, getBlob(BID), removed);
                }
            } else if (!getContentSha1.equals(toReadSha1)) {    //staged for addition, but the content has been changed.
                System.out.println("This file has been modified since last add, please add it again");
                System.exit(0);
            } else {    //staged for addition, no matter if it's tracked
                String BID = stageRead.fbPair.get(fileName);
                stageFor(fileName, getBlob(BID), removed);
                unStage(stageRead, fileName, added);
            }
    }
    private static Blob getBlob(String BID) {
        File blobRead = join(BLOBS_DIR, BID);
        return readObject(blobRead, Blob.class);
    }

    private static boolean isTracked(File toRead, Commit commit) {
        return getBlob(commit.archive.get(toRead.getName())).getContent().equals(readContentsAsString(toRead));
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

    /** implement the commit command
     * TODO: create a new commit instance, whose archive is its parent's
     * TODO: base on the stage for addition and stage for removal, modify the archive
     * TODO: HEAD pointer points to the newest commit
     * TODO: CID takes in the commit's the file (blob) references of its files, parent reference, message, and commit time.
     * */
    public static void commit(String message) {
        Commit current = new Commit(message, getActiveBranch());
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
                System.out.println(fromCommitFile(fileName).CID + "\n");
                haveFound = true;
            }
        }
        if (!haveFound) {
            System.out.println("Commit with such message doesn't exist.\n");
        } else {
            System.out.println("Commits with such message are listed above." + "\n");
        }
    }

    /**
     * TODO: Tracked in the current commit, changed in the working directory, but not staged;
     * (modified)
     * TODO: Staged for addition, but with different contents than in the working directory;
     * (modified)
     * TODO: Staged for addition, but deleted in the working directory;
     * (deleted)
     * TODO: Not staged for removal, but tracked in the current commit and deleted from the working directory.
     * */
    public static void status() {
        System.out.println("=== Branches ===");
        List<String> branchFiles = plainFilenamesIn(BRANCH_DIR);
        String activeBranchName = activeBranchFile().getName();
        assert branchFiles != null;
        for (String branch : branchFiles) {
            if (branch.equals(activeBranchName)) {
                System.out.println("* " + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println("\n");
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
        System.out.println("\n");
        System.out.println("=== Removed Files ===");
        for (String key : removedFiles.keySet()) {
            System.out.println(key);
        }
        System.out.println("\n");

        System.out.println("=== Modifications Not Staged For Commit ===");
        deletedWithoutStage(directoryFiles);
        modifiedWithoutStage(directoryFiles);

        System.out.println("\n");

        System.out.println("=== Untracked Files ===");
        for (String file : untrackedFiles(directoryFiles)) {
            System.out.println(file);
        }
        System.out.println("\n");
    }

    private static Set<String> untrackedFiles (Set<String> directoryFiles) {
        Commit commit = getHeadCommit();
        Set<String> unTracked = new HashSet<>();
        for (String fileName : directoryFiles) {
            //boolean isTracked = isTracked(join(CWD, fileName), commit);
            HashMap<String, String> map = commit.archive;
            if (fromStaged(removed).fbPair.containsKey(fileName) ||
                    (!fromStaged(added).fbPair.containsKey(fileName) && !commit.archive.containsKey(fileName))) {
                unTracked.add(fileName);
            }
        }
        return unTracked;
    }

    private static void modifiedWithoutStage(Set<String> directoryFiles) {
        Commit commit = getHeadCommit();
        HashMap<String, String> fileToCompare = new HashMap<String, String>();
        HashMap<String, String> addedFiles = fromStaged(added).fbPair;
        Set<String> hasDisplayed = new HashSet<String>();
        for (String fileName : directoryFiles) {
            File toCompare = join(CWD, fileName);
            fileToCompare.put(fileName, new Blob(toCompare).BID);
        }
        for (String key : addedFiles.keySet()) {
            if (fileToCompare.get(key) != null && !addedFiles.get(key).equals(fileToCompare.get(key))) {
                System.out.println(key + " (modified)");
                hasDisplayed.add(key);
            }
        }
        for (String key : commit.archive.keySet()) {
            if (fileToCompare.get(key) != null && !commit.archive.get(key).equals(fileToCompare.get(key))) {
                if (!hasDisplayed.contains(key)) {
                    System.out.println(key + " (modified)");
                }
            }
        }
    }

    private static void deletedWithoutStage(Set<String> directoryFiles) {
        Commit commit = getHeadCommit();
        Set<String> addedFiles = new HashSet<String>(fromStaged(added).fbPair.keySet());
        Set<String> removedFiles = new HashSet<String>(fromStaged(removed).fbPair.keySet());
        Set<String> hasDisplayed = new HashSet<String>();
        Set<String> commitFiles = commit.archive.keySet();
        for (String fileName : addedFiles) {
            if (!directoryFiles.contains(fileName)) {
                System.out.println(fileName + " (deleted)");
                hasDisplayed.add(fileName);
            }
        }
        commitFiles.removeAll(removedFiles);
        commitFiles.removeAll(directoryFiles);
        commitFiles.removeAll(hasDisplayed);
        for (String fileName : commitFiles) {
            System.out.println(fileName + " (deleted)");
        }
    }


    public static void branch(String branchName) {
        File newBranch = join(BRANCH_DIR, branchName);
        writeContents(HEAD, branchName);
        writeContents(newBranch, getActiveBranch());
    }

    public static void rmBranch(String branchName) {
        boolean removed = false;
        if (readContentsAsString(HEAD).equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
        } else {
            for (String fileName : Objects.requireNonNull(plainFilenamesIn(BRANCH_DIR))) {
                if (fileName.equals(branchName)) {
                    restrictedDelete(join(BRANCH_DIR, fileName));
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
        pairFromStage(refAdd, archive, true);
        Stage refRm = fromStaged(removed);
        pairFromStage(refRm, archive, false);
        writeObject(added, refAdd);
        writeObject(removed, refRm);
        clearStage(refAdd, added);
        clearStage(refRm, removed);
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

    public  static void checkoutFile(String fileName) {
        checkoutFile(fileName, getActiveBranch());
    }
    private static void checkoutFile(String fileName, String CID) {
        Commit commit = getCommit(CID);
        if (commit.archive.containsKey(fileName)) {
            File toCheckout = join(CWD, fileName);
            String fileBID = commit.archive.get(fileName);
            writeContents(toCheckout, getBlob(fileBID).getContent());
        } else {
            System.out.println("This file is not tracked by this commit.");
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
        Set<String> untracked = untrackedFiles(directoryFiles);
        if (plainFilenamesIn(CWD) != null) {
            directoryFiles.addAll(Objects.requireNonNull(plainFilenamesIn(CWD)));
        }
        File branchFile = join(BRANCH_DIR, branchName);
        if (branchFile.exists()) {
            if (readContentsAsString(HEAD).equals(branchName)) {    //the checkout branch is current branch
                System.out.println("The check-out branch is current branch, nothing changed.");
            } else {    //
                Commit commit = getCommit(readContentsAsString(branchFile));
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
            System.out.println("No such branch.");
        }
    }

    private static void checkoutCommit(String CID) {
        Commit commit = getCommit(CID);
        //List<String> directoryFiles = plainFilenamesIn(CWD);
        if (plainFilenamesIn(CWD) != null) {
            for (String fileName : Objects.requireNonNull(plainFilenamesIn(CWD))) {
                if (commit.archive.containsKey(fileName)) {
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


    /**
     * TODO: create GITLET_DIR
     * TODO: create COMMITS_DIR
     * TODO: create BLOBS_DIR
     * TODO: create STAGING_DIR
     * TODO: set the HEAD file to initialCommit
     * TODO: create a stage for add
     * TODO: create a stage for remove
     * */
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
