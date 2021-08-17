package co.whale.packet.response;

import co.whale.packet.Packet;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@Builder
@ToString
public class ResponseDownloadFilePacket extends Packet implements Serializable {

    // private String localPath;

    private byte[] fileBytes;

}
