package co.whale.logger;

import com.github.tomaslanger.chalk.Chalk;
import lombok.Data;
import lombok.SneakyThrows;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.SynchronousQueue;

@Data
public class VirtualLogger {

    private final DateFormat dateFormat = new SimpleDateFormat("[dd-MM-yyyy hh:mm:ss]");

    private String name;

    private StringBuffer buffer;
    private Queue<String> logs;

    private boolean observing;

    @SneakyThrows
    public VirtualLogger(String name) {

        this.name = name;

        this.buffer = new StringBuffer();
        this.logs = new ConcurrentLinkedDeque<>();

    }

    public void debug(String msg) {
        debug(msg, buffer);
    }

    public void net(String msg) {
        net(msg, buffer);
    }

    public void info(String msg) {
        info(msg, buffer);
    }

    public void warn(String msg) {
        warn(msg, buffer);
    }

    public void error(String msg) {
        error(msg, buffer);
    }

    public void net(String msg, StringBuffer stringBuffer) {
        send(Chalk.on("(NET)").green().bgWhite().toString(), msg, stringBuffer);
    }

    public void debug(String msg, StringBuffer stringBuffer) {
        send(Chalk.on("(DBG)").magenta().toString(), msg, stringBuffer);
    }

    public void info(String msg, StringBuffer stringBuffer) {
        send(Chalk.on("(INF)").cyan().toString(), msg, stringBuffer);
    }

    public void warn(String msg, StringBuffer stringBuffer) {
        send(Chalk.on("(WRN)").yellow().toString(), msg, stringBuffer);
    }

    public void error(String msg, StringBuffer stringBuffer) {
        send(Chalk.on("(ERR)").red().toString(), msg, stringBuffer);
    }

    private void send(String prefix, String message, StringBuffer stringBuffer) {

        String append = "| " + time() + " " + prefix + " :: " + message + "\n";

        if(isObserving())
            logs.add(append);

        stringBuffer.append(append);

    }

    private String time() {
        return dateFormat.format(now());
    }

    private Date now() {
        return new Date();
    }

}
