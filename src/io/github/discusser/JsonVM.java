package io.github.discusser;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class JsonVM {
    protected static final Logger LOGGER = new Logger(java.util.logging.Logger.getLogger(JsonVM.class.getName()));
    private static final List<Project> PROJECTS = new ArrayList<>();
    public static final Path PROJECTS_DIR = Paths.get("src/projects");
    public static final Path SRC_DIR = Paths.get("src/generated");

    public static void main(String[] args) throws IOException {
//        Project myProject = registerProject("myProject");
        generateProjects();
    }

//    public static Project registerProject(String name) {
//        Project proj = new Project(new Gson(), name);
//        PROJECTS.add(proj);
//        return proj;
//    }

    private static void generateProjects() throws IOException {
        if (Files.notExists(SRC_DIR)) Files.createDirectory(SRC_DIR);
        File[] dirs = new File(PROJECTS_DIR.toString()).listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) {
                PROJECTS.add(new Project(new Gson(), dir.getName()));
            }
        }
        PROJECTS.forEach(project -> {
            try {
                Path path = Path.of(project.path.toString().replace("projects", "generated"));
                // recreate project
                // if you have something other than the java files in the generated directory say bye bye
                FileUtils.deleteDirectory(path.toFile());
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.severe("Could not delete or create directory at " + project.path);
            }
            project.getProjectFiles().forEach(file -> {
                // its hard to come up with variable names
                String oldFilePath = file.getPath();
                String fileStr = oldFilePath.replaceFirst("projects", "generated");
                // replace .json by .java
                fileStr = fileStr.substring(0, fileStr.length() - ".json".length()) + ".java";
                // remove file name from the path
                String dirStr = fileStr.substring(0, fileStr.length() - file.getName().length());
                Path filePath = Paths.get(fileStr);
                File newFile = filePath.toFile();

                if (newFile.exists()) newFile.delete();
                try {
                    Files.createDirectories(Paths.get(dirStr));
                    Files.createFile(filePath);

                    try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                        jsonToJava(project, file.toPath()).forEach(s -> {
                            try {
                                writer.write(s);
                            } catch (IOException e) {
                                e.printStackTrace();
                                LOGGER.severe("There was an error writing the java code to the file at " + filePath +
                                        " from " + oldFilePath);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    LOGGER.severe("Something went wrong when creating the java file at " + filePath +
                            " from " + oldFilePath);
                }
            });
        });
    }

    /**
     * An example of what a class should look like:
     * <pre>
     * {@code
     * {
     *     "class": {
     *         "annotations": {
     *             "SuppressWarnings": {
     *                 "args": { // If args is empty or not defined, annotation will not be followed by anything
     *                     "value": "{\"Foo\", \"Bar\"}" // No default value, it is better to use the annotation's default value
     *                 }
     *             }
     *         },
     *         "modifiers": ["public"], // Defaults to public
     *         "type": "class", // Defaults to "class", currently only classes and interfaces are supported
     *         "name": "Main", // Defaults to file name (useful for generics, eg: Main<E>)
     *         "extends": "SomeClass", // Defaults to nothing
     *         "implements": ["MyInterface", "AutoCloseable"], // Defaults to nothing
     *         "imports": ["java.lang.Math", {"static": true, "name": "java.io.BufferedWriter"}] // Defaults to nothing
     *     },
     *     "fields": {
     *         "myVariable": {
     *             "annotations": {
     *                 // annotations go here, see class->annotations->SuppressWarnings
     *             },
     *             "modifiers": ["public", "static"], // Defaults to nothing
     *             "type": "String", // Defaults to Object
     *             "value": "\"Hello World!\"" // Defaults to non initialized variable
     *         }
     *     },
     *     "methods": {
     *         "myObjectAsString": {
     *             "annotations": {
     *                 // annotations go here, see class->annotations->SuppressWarnings
     *             },
     *             "modifiers": [], // see fields->myVariable->modifiers
     *             "returnType": "String", // Defaults to void
     *             "args": {
     *                 // args go here, see fields->myVariable, note that any illegal syntax will be shown in logs.
     *             },
     *             "throws": ["IOException"], // Defaults to nothing
     *             "contents": [ // Defaults to an empty method
     *                 "this.myVariable = \"Goodbye!\";", // Semicolon is optional
     *                 "this.myVariable" // Defaults to null, if the method returns void, default to nothing
     *             ]
     *         }
     *     }
     * }
     * }
     * </pre>
     * @param project The file's project
     * @param file The file's path
     * @return A list containing the lines of the java code
     */
    public static List<String> jsonToJava(Project project, Path file) throws FileNotFoundException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file.toString()))) {
            if (reader.readLine() == null) return new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.severe("Could not read the file at " + file);
        }
        List<String> lines = new ArrayList<>();
        GsonHelper helper = new GsonHelper(project.readFile(file));

        GsonHelper.Clazz clazz = helper.clazz();
        String separator = System.getProperty("file.separator");
        String path = file.toString();
        path = path
                .replace("src" + separator, "")
                .replace(separator + file.getFileName(), "")
                .replace("projects", "generated")
                .replace(separator, ".") + ";";
        lines.add("package " + path);
        lines.add("\n\n");

        if (clazz.getMap().get("imports") != null) {
            GsonHelper.Imports imports = clazz.imports();
            clazz.imports().imports().forEach(i -> {
                lines.add("import " + (i.static_() ? "static " : "") + i.name() + ";");
                lines.add("\n");
            });
            lines.add("\n");
        }
        String annotations = getAnnotations(clazz.annotations());
        lines.add((!annotations.equals("") ? annotations + "\n" : ""));

        lines.add(
                (clazz.modifiers() != null ? String.join(" ", clazz.modifiers()) + " " : "public ")
                + (clazz.type() != null ? clazz.type() + " " : "class ")
                + (clazz.name() != null ? clazz.name() : file.getFileName().toString().replace(".json", ""))
                + (clazz.extends0() != null ? " extends " + clazz.extends0() : "")
                + (clazz.implements0() != null ? " implements " + String.join(", ", clazz.implements0()): "")
                + " {");
        lines.add("\n");

        GsonHelper.Fields fields = helper.fields();
        if (fields.map != null) {
            fields.map.keySet().forEach(key -> {
                GsonHelper.Field0 field = fields.get(key);
                String annotations0 = getAnnotations(field.annotations());
                lines.add((!annotations0.equals("") ? annotations0 + "\n" : ""));
                lines.add(
                        (field.modifiers() != null ? String.join(" ", field.modifiers()) + " " : "")
                                + (field.type() != null ? field.type() + " " : "Object ")
                                + key
                                + (field.value() != null ? " = " + field.value() + ";" : ";")
                                + "\n"
                );
            });
            lines.add("\n");
        }


        GsonHelper.Methods methods = helper.methods();
        methods.map.keySet().forEach(key -> {
            GsonHelper.Method0 method = methods.get(key);
            StringBuilder signature = new StringBuilder
                    ((method.modifiers() != null ? String.join(" ", method.modifiers()) + " " : ""))
                    .append(method.returnType() != null ? method.returnType() + " " : "void ")
                    .append(key)
                    .append("(");
            GsonHelper.Fields args = method.args();
            if (args.map != null) {
                args.map.keySet().forEach(key0 -> signature.append(args.get(key0).type()).append(" ").append(key0));
            }
            boolean flag = (!clazz.type().equals("interface")
                    || (clazz.type().equals("interface") &&
                    (method.modifiers() != null && method.modifiers().contains("default"))));
            signature
                    .append(")")
                    .append((method.throws0() != null ? " throws " + String.join(", ", method.throws0()) + " " : ""))
                    .append(flag ? " {\n" : "");
            if (flag) method.contents().forEach(s -> signature.append(s).append(!s.endsWith(";") ? ";" : "").append("\n"));

//            if (flag) {
//                GsonHelper.MethodContents contents = method.contents();
//                for (int i = 0; i < contents.map.size() - 1; i++) {
//                    signature.append(contents.line(i)).append("\n");
//                }
//                if (!method.returnType().equals("void")) {
//                    signature
//                            .append("return ")
//                            .append(contents.return0())
//                            .append("\n");
//                }
//            }
            signature.append(flag ? "}\n" : ";\n");
            lines.add(signature.toString());
        });

        lines.add("}");

        return lines;
    }

    public static String getAnnotations(GsonHelper.Annotations annotations) {
        StringBuilder str = new StringBuilder();
        if (annotations == null || annotations.map == null) return "";

        annotations.map.forEach((key, value) -> {
            // @SuppressWarnings(value = {"Foo", "Bar"})
            str.append("@").append(key);
            GsonHelper.Annotation0 annot = annotations.get(key);
            LinkedTreeMap<String, ?> args = annot.args();
            if (args != null) {
                // (value = {"Foo", "Bar"})
                // (value = {"Foo", "Bar"}, doThing = false)
                str.append("(");
                AtomicInteger i = new AtomicInteger(0);
                args.forEach((key0, value0) -> {
                    i.addAndGet(1);
                    str.append(key0).append(" = ").append(value0);
                    if (i.intValue() != args.size()) str.append(", ");
                });
                str.append(") ");
            }
        });

        return str.toString();
    }

    // low effort semi-decent logger
    public static class Logger {
        private final java.util.logging.Logger logger;

        public Logger(java.util.logging.Logger logger) {
            this.logger = logger;
        }

        public String getPrependText() {
            Calendar cal = Calendar.getInstance();
            String date = new Formatter().format(
                    "%02d:%02d:%02d",
                    cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND)
            ).toString();
            Class<?> caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
            return "[" + date + "]"
                    + "[" + caller.getName()
                    + "@" + caller.hashCode() + "]";
        }

        public void log(Level level, String msg) {
            this.logger.log(level, this.getPrependText() + " " + msg);
        }

        public void warning(String msg) {
            this.logger.warning(this.getPrependText() + " " + msg);
        }

        public void severe(String msg) {
            this.logger.severe(this.getPrependText() + " " + msg);
        }

        public void info(String msg) {
            this.logger.info(this.getPrependText() + " " + msg);
        }

        // thanks intellij
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Logger logger1 = (Logger) o;
            return Objects.equals(logger, logger1.logger);
        }

        @Override
        public int hashCode() {
            return Objects.hash(logger);
        }
    }
}
