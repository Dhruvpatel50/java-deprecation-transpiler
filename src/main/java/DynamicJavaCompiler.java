import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

public class DynamicJavaCompiler {
    public static void compileAndRun(String className, String sourceCode) throws Exception {
        Path tempDir = Files.createTempDirectory("java-compiler-");
        Path sourceFile = tempDir.resolve(className + ".java");
        Files.write(sourceFile, sourceCode.getBytes());

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Cannot find the system Java compiler. " +
                "Make sure you are running this with a JDK, not a JRE.");
        }
        
        // Use an output stream to capture compiler errors
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        int compilationResult = compiler.run(null, null, errStream, sourceFile.toString());

        if (compilationResult != 0) {
            System.err.println("Compilation failed!");
            System.err.println(new String(errStream.toByteArray()));
            return;
        }

        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{tempDir.toUri().toURL()});
        Class<?> cls = Class.forName(className, true, classLoader);
        
        // Assumes the class has a run() method, like our OldCode example.
        Object instance = cls.getDeclaredConstructor().newInstance();
        Method method = cls.getMethod("run");
        method.invoke(instance);

        // Clean up temporary files
        Files.delete(sourceFile);
        Files.delete(tempDir.resolve(className + ".class"));
        Files.delete(tempDir);
    }
}