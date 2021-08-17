package co.whale.context.entry;

import lombok.Builder;
import lombok.Data;

import java.io.File;
import java.io.Serializable;

@Data
@Builder
public class WhaleFile implements Serializable {

    private String filename;
    private String sha1;
    private File file;

    private long size;
    private long lastModified;

}
