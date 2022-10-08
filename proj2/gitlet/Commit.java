package gitlet;
import java.io.File;
import java.util.Date;
import java.io.Serializable;
import java.util.HashMap;
import static gitlet.Repository.*;
import static gitlet.Utils.*;
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



    /** The UID of the commit. */
    public String CID;
    /** The message of this Commit. */
    private final String message;
    /** The timeStamp when the commit is created */
    private final Date timeStamp;
    /** The commit has a parent commit, represented as the parent commit's UID.*/
    private final String parent;

    /** The other parent */
    private final String parent2;

    /** a HashMap archives the commit's state(filename and BID) */
    public HashMap<String, String> archive;

    public Commit(String message, String parent, String parent2) {
        this.message = message;
        this.parent = parent;
        this.parent2 = parent2;
        // Initial commit has the UNIX time 0 stamp
        if (this.parent == null) {
            this.timeStamp = new Date(0);
            this.setArchive(new HashMap<String, String>());
        } else {
            this.timeStamp = new Date();
        }
    }

    public String getMessage() {
        return this.message;
    }

    public String getParent2() {
        return this.parent2;
    }

    public void setArchive(HashMap<String, String> archive) {
        this.archive = archive;
    }

    public String getParent() {
        return this.parent;
    }

    public Date getTimeStamp() {
        return this.timeStamp;
    }

    public String getCID() {
        return this.CID;
    }

    public File getToFile() {
        return join(COMMITS_DIR, this.CID);
    }



    /* TODO: fill in the rest of this class. */
}
