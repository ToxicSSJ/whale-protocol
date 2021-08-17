package co.whale.config;

import co.whale.Main;
import co.whale.config.entry.ConfigServer;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.hjson.Stringify;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Configuration {

    private File file;

    @SneakyThrows
    public Configuration(String path) {

        File temp = new File(path);

        if (!temp.isFile() && !temp.exists()) {
            temp.createNewFile();
            FileUtils.copyToFile(Main.getResource("configuration.hjson"), temp);
        }

        this.file = temp;

        if(getServerSetting("type").asString().equalsIgnoreCase("PRIVATE")) {

            if(getServerSetting("private-key").isNull() ||
               getServerSetting("private-key").asString().isEmpty())
                setServerSetting("private-key", JsonValue.valueOf(UUID.randomUUID().toString().substring(0, 16) + "=="));

        }

    }

    public void addServerNode(ConfigServer configServer) {
        addServerSetting("nodes", configServer.toJsonValue());
    }

    public void removeServerNode(ConfigServer configServer) {
        JsonObject json = getServer();
        int index = 0;
        for(JsonValue jsonValue : json.get("nodes").asArray()) {
            JsonObject jsonObject = jsonValue.asObject();
            if(jsonObject.getString("hostname", "").equals(configServer.getHostname()) &&
               jsonObject.getInt("port", -1) == configServer.getPort()) {
                removeServerSetting("nodes", index);
            }
            index++;
        }
    }

    public List<ConfigServer> getServerNodes() {
        List<ConfigServer> result = new ArrayList<>();
        for(JsonValue jsonValue : getServerSetting("nodes").asArray().values()) {
            JsonObject jsonObject = jsonValue.asObject();
            result.add(ConfigServer.builder()
                    .key(jsonObject.getString("key", "none"))
                    .hostname(jsonObject.getString("hostname", "unknow"))
                    .port(jsonObject.getInt("port", 7070))
                    .build());
        }
        return result;
    }

    public JsonValue getServerSetting(String key) {
        return getServer().get(key);
    }

    public JsonValue getClientSetting(String key) {
        return getServer().get(key);
    }

    public void removeServerSetting(String key, int index) {
        JsonObject json = getServer();
        json.get(key).asArray().remove(index);
        saveServer(json);
    }

    public void removeClientSetting(String key, int index) {
        JsonObject json = getClient();
        json.get(key).asArray().add(index);
        saveClient(json);
    }

    public void addServerSetting(String key, JsonValue value) {
        JsonObject json = getServer();
        json.get(key).asArray().add(value);
        saveServer(json);
    }

    public void addClientSetting(String key, JsonValue value) {
        JsonObject json = getClient();
        json.get(key).asArray().add(value);
        saveClient(json);
    }

    public void setServerSetting(String key, JsonValue value) {
        saveServer(getServer().set(key, value));
    }

    public void setClientSetting(String key, JsonValue value) {
        saveClient(getClient().set(key, value));
    }

    public JsonObject getServer() {
        return getConfiguration().get("server").asObject();
    }

    public JsonObject getClient() {
        return getConfiguration().get("client").asObject();
    }

    @SneakyThrows
    public void saveClient(JsonObject object) {
        JsonObject current = getConfiguration();
        current.set("client", object);
        saveFile(current);
    }

    @SneakyThrows
    public void saveServer(JsonObject object) {
        JsonObject current = getConfiguration();
        current.set("server", object);
        saveFile(current);
    }

    private void saveFile(JsonObject jsonObject) {
        saveFile(jsonObject.toString(Stringify.FORMATTED));
    }

    @SneakyThrows
    private void saveFile(String content) {
        FileUtils.write(file, content, Charset.defaultCharset(), false);
    }

    @SneakyThrows
    private JsonObject getConfiguration() {
        return JsonValue.readHjson(new FileReader(file)).asObject();
    }

}
