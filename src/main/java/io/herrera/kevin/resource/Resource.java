package io.herrera.kevin.resource;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Simplifies access to JAR resources.
 */
public class Resource {

    /**
     * The class loader for the resources.
     */
    private ClassLoader classLoader;

    /**
     * Initializes the manager with the given class loader.
     *
     * @param classLoader The class loader for the resources.
     */
    public Resource(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "The class loader is required.");

        this.classLoader = classLoader;
    }

    /**
     * Returns the resources as an input stream.
     *
     * @param name The name of the resource.
     *
     * @return The resource as an input stream.
     */
    public InputStream getAsInputStream(String name) {
        Objects.requireNonNull(name, "The resource name is required.");

        return getResource(name);
    }

    /**
     * Returns the path to the extracted resource.
     *
     * @param name The name of the resource.
     *
     * @return The path to the resource file.
     *
     * @throws ResourceException If the resource path could not be determined.
     */
    public Path getAsPath(String name) {
        Objects.requireNonNull(name, "The resource name is required.");

        try {
            Path path = Files.createTempFile("", "");

            Files.copy(getResource(name), path, StandardCopyOption.REPLACE_EXISTING);

            return path;
        } catch (IOException cause) {
            throw new ResourceException(
                String.format("A new temporary file could not be created for: %s", name),
                cause
            );
        }
    }

    /**
     * Returns the resource as a string.
     *
     * @param name    The name of the resource.
     * @param charset The string character set.
     *
     * @return The resource as a string.
     *
     * @throws ResourceException If the resource could not be read as a string.
     */
    public String getAsString(String name, String charset) {
        Objects.requireNonNull(name, "The resource name is required.");
        Objects.requireNonNull(charset, "The string character set is required.");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        getResource(name, bytes::write);

        try {
            return new String(bytes.toByteArray(), charset);
        } catch (UnsupportedEncodingException cause) {
            throw new ResourceException(
                String.format(
                    "The resource, %s, could not be read as a string using the character set, %s.",
                    name,
                    charset
                ),
                cause
            );
        }
    }

    /**
     * Retrieves the resources as a buffered input stream.
     *
     * @param name The name of the resource.
     *
     * @return The buffered input stream.
     *
     * @throws ResourceException If the resource does not exist.
     */
    private BufferedInputStream getResource(String name) {
        InputStream stream = classLoader.getResourceAsStream(name);

        if (stream == null) {
            throw new ResourceException(String.format(
                "The resource, %s, could not be found.",
                name
            ));
        }

        return new BufferedInputStream(stream);
    }

    /**
     * Reads a resource input stream and invokes a closure for each byte read.
     *
     * @param name   The name of the resource.
     * @param reader The byte reader.
     *
     * @throws ResourceException If the input stream could not be read.
     */
    private void getResource(String name, Consumer<Integer> reader) {
        try (InputStream stream = getResource(name)) {
            int read;

            while ((read = stream.read()) != -1) {
                reader.accept(read);
            }
        } catch (IOException cause) {
            throw new ResourceException(String.format(
                "The resource, %s, could not be read.",
                name
            ));
        }
    }
}
