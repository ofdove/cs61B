package gitlet;

import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable {
    public final String BID;
    private final String content;

    public Blob(File f) {
        this.content = Utils.readContentsAsString(f);
        this.BID = Utils.sha1(f.getName() + content);
    }

    public String getContent() {
        return content;
    }
}
