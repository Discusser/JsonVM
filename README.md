very serious project to convert `json` files to `java` files

step 1. download `JsonVM.jar`\
step 2. place it in a folder\
step 3. create a directory called `src` in the same folder\
step 4. create a directory called `projects` in `src`\
step 5. create a directory in `projects` with your project's name\
step 6. add json files to that directory (more at the end)\
step 7. run `JsonVM.jar`\
step 8. your files will be in `src/generated/your-project-name/`

An example of what a class should look like:
<pre>
{
    "class": {
        "annotations": {
            "SuppressWarnings": {
                "args": { // If args is empty or not defined, annotation will not be followed by anything
                    "value": "{\"Foo\", \"Bar\"}" // No default value, it is better to use the annotation's default value
                }
            }
        },
        "modifiers": ["public"], // Defaults to public
        "type": "class", // Defaults to "class", currently only classes and interfaces are supported
        "name": "Main", // Defaults to file name (useful for generics, eg: Main<E>)
        "extends": "SomeClass", // Defaults to nothing
        "implements": ["MyInterface", "AutoCloseable"], // Defaults to nothing
        "imports": ["java.lang.Math", {"static": true, "name": "java.io.BufferedWriter"}] // Defaults to nothing
    },
    "fields": {
        "myVariable": {
            "annotations": {
                // annotations go here, see class->annotations->SuppressWarnings
            },
            "modifiers": ["public", "static"], // Defaults to nothing
            "type": "String", // Defaults to Object
            "value": "\"Hello World!\"" // Defaults to non initialized variable
        }
    },
    "methods": {
        "myObjectAsString": {
            "annotations": {
                // annotations go here, see class->annotations->SuppressWarnings
            },
            "modifiers": [], // see fields->myVariable->modifiers
            "returnType": "String", // Defaults to void
            "args": {
                // args go here, see fields->myVariable, note that any illegal syntax will be shown in logs.
            },
            "throws": ["IOException"], // Defaults to nothing
            "contents": [ // Defaults to an empty method
                "this.myVariable = \"Goodbye!\";", // Semicolon is optional
                "return this.myVariable" // Defaults to null, if the method returns void, default to nothing
            ]
        }
    }
}
</pre>