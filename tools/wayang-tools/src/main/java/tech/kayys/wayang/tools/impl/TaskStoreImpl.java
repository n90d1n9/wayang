package tech.kayys.wayang.tools.impl;

import tech.kayys.wayang.sdk.gollek.tools.TaskStoreIface;
import tech.kayys.wayang.sdk.gollek.model.Task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TaskStoreImpl implements TaskStoreIface {
    private final Path projectDir;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules().enable(SerializationFeature.INDENT_OUTPUT);

    public TaskStoreImpl(Path projectDir) { this.projectDir = projectDir; }

    private Path tasksFile() { return projectDir.resolve("tasks.json"); }

    @Override
    public List<Task> listTasks() throws IOException {
        Path f = tasksFile();
        if (!Files.exists(f)) return List.of();
        Task[] arr = mapper.readValue(f.toFile(), Task[].class);
        List<Task> out = new ArrayList<>();
        if (arr != null) for (Task t : arr) out.add(t);
        return out;
    }

    @Override
    public Task addTask(String description) throws IOException {
        String id = UUID.randomUUID().toString();
        Task t = new Task(id, description);
        List<Task> tasks = new ArrayList<>(listTasks());
        tasks.add(t);
        Files.createDirectories(projectDir);
        mapper.writeValue(tasksFile().toFile(), tasks);
        return t;
    }

    @Override
    public void updateTask(Task task) throws IOException {
        List<Task> tasks = new ArrayList<>(listTasks());
        tasks = tasks.stream().map(t -> t.id().equals(task.id()) ? task : t).collect(Collectors.toList());
        mapper.writeValue(tasksFile().toFile(), tasks);
    }

    @Override
    public boolean removeTask(String id) throws IOException {
        List<Task> tasks = new ArrayList<>(listTasks());
        boolean removed = tasks.removeIf(t -> t.id().equals(id));
        mapper.writeValue(tasksFile().toFile(), tasks);
        return removed;
    }
}
