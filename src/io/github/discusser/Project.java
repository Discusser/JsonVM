package io.github.discusser;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

public class Project {
    public final Gson gson;
    public final String name;
    public final Path path;

    public Project(Gson gson, String name) {
        this.gson = gson;
        this.name = name;
        this.path = JsonVM.PROJECTS_DIR.resolve(name);
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, ?> readFile(Path path) {
        try {
            Reader reader = Files.newBufferedReader(path);
            HashMap<String, ?> map = gson.fromJson(reader, HashMap.class);
            reader.close();
            return map;
        } catch (Exception e) {
            JsonVM.LOGGER.severe("An error occured reading the JSON file at " + path);
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public Collection<File> getProjectFiles() {
        return FileUtils.listFiles(this.path.toFile(), new String[]{"json"}, true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return Objects.equals(gson, project.gson) && Objects.equals(path, project.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gson, path);
    }
}
