package tech.kayys.wayang.tools;

import tech.kayys.wayang.client.gollek.model.Task;

import java.io.IOException;
import java.util.List;

public interface TaskStoreIface {
    List<Task> listTasks() throws IOException;
    Task addTask(String description) throws IOException;
    void updateTask(Task task) throws IOException;
    boolean removeTask(String id) throws IOException;
}
