package io.github.discusser;

import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public record GsonHelper(HashMap<String, ?> json) {
    public static LinkedTreeMap<String, ?> asMap(Map<String, ?> json, String key) {
        return (LinkedTreeMap<String, ?>) json.get(key);
    }

    public Clazz clazz() {
        return new Clazz(asMap(this.json, "class"));
    }

    public Fields fields() {
        return new Fields(asMap(this.json, "fields"));
    }

    public Methods methods() {
        return new Methods(asMap(this.json, "methods"));
    }

    public interface IGsonObject {
        LinkedTreeMap<String, ?> getMap();
    }

    public interface HasAnnotations extends IGsonObject {
        default Annotations annotations() {
            return new Annotations(asMap(getMap(), "annotations"));
        }
    }

    public interface HasModifiers extends IGsonObject {
        default List<String> modifiers() {
            return (List<String>) getMap().get("modifiers");
        }
    }

    public static class GsonObject implements IGsonObject {
        public final LinkedTreeMap<String, ?> map;

        public GsonObject(LinkedTreeMap<String, ?> map) {
            this.map = map;
        }

        @Override
        public LinkedTreeMap<String, ?> getMap() {
            return this.map;
        }
    }

    public static class GsonArray {
        public List<?> list;

        public GsonArray(List<?> list) {
            this.list = list;
        }
    }

    public static class Clazz extends GsonObject implements HasAnnotations, HasModifiers {
        public Clazz(LinkedTreeMap<String, ?> map) {
            super(map);
        }

        public String name() {
            return (String) this.map.get("name");
        }

        public String type() {
            return (String) this.map.get("type");
        }

        public String extends0() {
            return (String) this.map.get("extends");
        }

        public List<String> implements0() {
            return (List<String>) this.map.get("implements");
        }

        public Imports imports() {
            return new Imports((List<?>)this.map.get("imports"));
        }
    }

    public static class Annotations extends GsonObject {
        public Annotations(LinkedTreeMap<String, ?> map) {
            super(map);
        }

        public Annotation0 get(String key) {
            return new Annotation0(asMap(this.map, key));
        }
    }

    public static class Annotation0 extends GsonObject {
        public Annotation0(LinkedTreeMap<String, ?> map) {
            super(map);
        }

        public LinkedTreeMap<String, ?> args() {
            return asMap(this.map, "args");
        }
    }

    public static class Imports extends GsonArray {
        public Imports(List<?> list) {
            super(list);

            // dont mind this
            List<Import> l = new ArrayList<>();
            list.forEach(o -> {
                if (o instanceof String) l.add(new Import(false, (String)o));
                else {
                    LinkedTreeMap<String, ?> import_ = (LinkedTreeMap<String, ?>) o;
                    l.add(new Import((boolean) import_.get("static"), (String) import_.get("name")));
                }
            });

            this.list = l;
        }

        public List<Import> imports() { return (List<Import>) this.list; }
    }

    public record Import(boolean static_, String name) {}

    public static class Fields extends GsonObject {
        public Fields(LinkedTreeMap<String, ?> map) {
            super(map);
        }

        public Field0 get(String key) {
            return new Field0(asMap(this.map, key));
        }
    }

    public static class Field0 extends GsonObject implements HasAnnotations, HasModifiers {
        public Field0(LinkedTreeMap<String, ?> map) {
            super(map);
        }

        public String type() {
            return (String) this.map.get("type");
        }

        public String value() {
            return (String) this.map.get("value");
        }
    }

    public static class Methods extends GsonObject {
        public Methods(LinkedTreeMap<String, ?> map) {
            super(map);
        }

        public Method0 get(String key) {
            return new Method0(asMap(this.map, key));
        }
    }

    public static class Method0 extends GsonObject implements HasAnnotations, HasModifiers {
        public Method0(LinkedTreeMap<String, ?> map) {
            super(map);
        }

        public String returnType() {
            return (String) this.map.get("returnType");
        }

        public Fields args() {
            return new Fields(asMap(this.map, "args"));
        }

        public List<String> throws0() {
            return (List<String>) this.map.get("throws");
        }

        public List<String> contents() {
            return (List<String>) this.map.get("contents");
        }
    }
//    public static class MethodContents extends GsonObject {
//        public MethodContents(LinkedTreeMap<String, ?> map) {
//            super(map);
//        }
//
//        public String line(int line) {
//            String line0 = (String) this.map.get("line" + line);
//            return !line0.endsWith(";") ? line0 + ";" : line0;
//        }
//
//        public String return0() {
//            String return0 = (String) this.map.get("return");
//            if (return0 == null) return "null;";
//            return !return0.endsWith(";") ? return0 + ";" : return0;
//        }
//    }
}
