package io.herrera.kevin.resource;

import static io.herrera.kevin.reflect.Reflect.findMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.herrera.kevin.reflect.Reflect;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the resource manager functions as intended.
 */
public class ResourceTest {

    /**
     * Verify that a file system resource can be retrieved as an input stream.
     */
    @Test
    public void getAsInputStreamFromFileSystemTest() throws Exception {
        assertEquals(
            "This is the root test resource.\n",
            streamToString(withFileSystem().getAsInputStream("root.txt"))
        );

        assertEquals(
            "This is the package test resource.\n",
            streamToString(withFileSystem().getAsInputStream(
                "io/herrera/kevin/resource/package.txt"
            ))
        );
    }

    /**
     * Verify that a JAR resource can be retrieved as an input stream.
     */
    @Test
    public void getAsInputStreamFromJarTest() throws Exception {
        assertEquals(
            "This is the root test resource in a JAR.\n",
            streamToString(withJar().getAsInputStream("root.txt"))
        );

        assertEquals(
            "This is the package test resource in a JAR.\n",
            streamToString(withJar().getAsInputStream("io/herrera/kevin/test/package.txt"))
        );
    }

    /**
     * Verify that a file system resource can be retrieved as a file path.
     */
    @Test
    public void getAsPathFromFileSystemTest() throws Exception {
        assertEquals(
            "This is the root test resource.\n",
            new String(Files.readAllBytes(withFileSystem().getAsPath("root.txt")))
        );

        assertEquals(
            "This is the package test resource.\n",
            new String(Files.readAllBytes(withFileSystem().getAsPath(
                "io/herrera/kevin/resource/package.txt"
            )))
        );
    }

    /**
     * Verify that a JAR resource can be retrieved as a file path.
     */
    @Test
    public void getAsPathFromJarTest() throws Exception {
        assertEquals(
            "This is the root test resource in a JAR.\n",
            new String(Files.readAllBytes(withJar().getAsPath("root.txt")))
        );

        assertEquals(
            "This is the package test resource in a JAR.\n",
            new String(Files.readAllBytes(withJar().getAsPath(
                "io/herrera/kevin/test/package.txt"
            )))
        );
    }

    /**
     * Verify that a file system resource can be retrieved as a string.
     */
    @Test
    public void getAsStringFromFileSystemTest() {
        assertEquals(
            "This is the root test resource.\n",
            withFileSystem().getAsString("root.txt", "utf-8")
        );

        assertEquals(
            "This is the package test resource.\n",
            withFileSystem().getAsString("io/herrera/kevin/resource/package.txt", "utf-8")
        );
    }

    /**
     * Verify that a JAR resource can be retrieved as a string.
     */
    @Test
    public void getAsStringFromJarTest() {
        assertEquals(
            "This is the root test resource in a JAR.\n",
            withJar().getAsString("root.txt", "utf-8")
        );

        assertEquals(
            "This is the package test resource in a JAR.\n",
            withJar().getAsString("io/herrera/kevin/test/package.txt", "utf-8")
        );
    }

    /**
     * Verify that an exception is thrown for an unsupported string character set.
     */
    @Test
    public void getAsStringUnsupportedCharsetTest() {
        assertThrows(
            ResourceException.class,
            () -> withFileSystem().getAsString("root.txt", "invalid")
        );
    }

    /**
     * Verify that an exception is thrown if a resource does not exist.
     */
    @Test
    public void getResourceNotFoundTest() {
        assertThrows(
            ResourceException.class,
            () -> Reflect.on(withFileSystem()).invoke("getResource", "does/not/exist")
        );
    }

    /**
     * Verify that an exception is thrown if a resource could not be read.
     */
    @Test
    public void getResourceReadErrorTest() throws Exception {
        ClassLoader classLoader = mock(ClassLoader.class);
        InputStream inputStream = mock(InputStream.class);
        String resourceName = "resource";

        when(classLoader.getResourceAsStream(resourceName)).thenReturn(inputStream);

        when(inputStream.read(any(), anyInt(), anyInt())).thenThrow(new IOException());

        Resource resource = new Resource(classLoader);

        assertThrows(
            ResourceException.class,
            () -> {
                try {
                    findMethod(resource, "getResource", String.class, Consumer.class).invoke(
                        resource,
                        resourceName,
                        null
                    );
                } catch (InvocationTargetException exception) {
                    throw exception.getCause();
                }
            }
        );
    }

    /**
     * Creates a string from an input stream.
     *
     * @param input The input stream.
     *
     * @return The string.
     *
     * @throws IOException If the stream could not be read.
     */
    private String streamToString(InputStream input) throws IOException {
        int read;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        while ((read = input.read()) != -1) {
            output.write(read);
        }

        return new String(output.toByteArray());
    }

    /**
     * Creates a resource manager for the file system resources.
     */
    private Resource withFileSystem() {
        return new Resource(ResourceTest.class.getClassLoader());
    }

    /**
     * Creates a resource manager for the JAR resources.
     */
    private Resource withJar() {
        URLClassLoader classLoader = URLClassLoader.newInstance(
            new URL[] {
                getClass().getClassLoader().getResource("test.jar")
            },
            null
        );

        return new Resource(classLoader);
    }
}
