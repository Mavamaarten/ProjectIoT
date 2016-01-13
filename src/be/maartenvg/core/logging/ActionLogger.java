package be.maartenvg.core.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActionLogger {
    public final List<LogItem> logs = new ArrayList<>();
    private static ActionLogger actionLoggerInstance;

    private ActionLogger(){}

    public static synchronized ActionLogger getInstance(){
        if(actionLoggerInstance == null) actionLoggerInstance = new ActionLogger();
        return actionLoggerInstance;
    }

    public void log(LogItem logItem){
        logs.add(logItem);
    }

    public void log(LogAction action){
        logs.add(new LogItem(action));
    }

    public void log(LogAction action, String description){
        logs.add(new LogItem(action, description));
    }

    public List<LogItem> getLogs() {
        return Collections.unmodifiableList(logs);
    }
}
