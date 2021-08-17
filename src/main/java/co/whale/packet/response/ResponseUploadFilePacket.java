package co.whale.packet.response;

import co.whale.packet.Packet;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@Builder
@ToString
public class ResponseUploadFilePacket extends Packet implements Serializable {

}
