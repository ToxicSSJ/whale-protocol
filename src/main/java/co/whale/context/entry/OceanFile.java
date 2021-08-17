package co.whale.context.entry;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.File;
import java.io.Serializable;

@Data
@Builder
@ToString
public class OceanFile implements Serializable {

    private String whaleId;
    private String whaleHostname;
    private int whalePort;

    private String filename;
    private String sha1;

    private long size;
    private long lastModified;

}
