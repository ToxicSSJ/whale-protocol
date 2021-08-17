package co.whale.util;

import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class Net {

    @SneakyThrows
    public static String getIp() {

        URL whatismyip = new URL("http://checkip.amazonaws.com");
        BufferedReader in = null;

        try {

            in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            String ip = in.readLine();
            return ip;

        } finally {

            if (in != null)
                in.close();

        }

    }

}
