package co.whale.config.entry;

import lombok.Builder;
import lombok.Data;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

@Data
@Builder
public class ConfigServer {

    private String hostname;
    private int port;

    private String key = "none";

    public JsonValue toJsonValue() {
        return new JsonObject()
                .set("hostname", hostname)
                .set("port", port)
                .set("key", key)
                .asObject();
    }

}
