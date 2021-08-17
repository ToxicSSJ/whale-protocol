package co.whale.context.entry;

import lombok.Builder;
import lombok.Data;

import java.io.File;
import java.io.Serializable;

@Data
@Builder
public class WhaleSpace implements Serializable {

    private String whaleId;
    private String whaleHostname;
    private int whalePort;

    private long size;
    private long maxSize;

}
