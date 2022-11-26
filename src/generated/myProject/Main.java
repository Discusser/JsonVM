package generated.myProject;

import java.io.IOException;

public class Main {
public static String myVariable = "Hello World!";
boolean FLAG = false;

String myObjectAsString(boolean doFoo) throws IOException, NullPointerException  {
myVariable = "Goodbye!";
if (myVariable.hashCode() >= 10) myVariable = "too big";
return FLAG & doFoo ? myVariable : "Hello once again!";
}
}