package tech.kayys.wayang.sdk.gollek.persistence;

import tech.kayys.wayang.sdk.gollek.model.Project;
import java.nio.file.Path;
import java.util.List;

public interface PersistenceStrategy {
    List<Project> listProjects() throws Exception;
    void saveProject(Project project) throws Exception;
    void removeProject(String projectId) throws Exception;
    Path exportProject(String projectId, Path destFile) throws Exception;
    Project importProject(Path srcFile) throws Exception;
    void setCurrentProjectId(String projectId) throws Exception;
    String getCurrentProjectId() throws Exception;
}
