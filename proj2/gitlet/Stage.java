package gitlet;

import java.io.Serializable;
import java.util.HashMap;

public class Stage implements Serializable {
    public HashMap<String, String> fbPair;
    public Stage(HashMap<String, String> fbPair) {
        this.fbPair = fbPair;
    }
}
