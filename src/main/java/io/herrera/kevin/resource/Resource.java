package io.herrera.kevin.resource;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

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
     * Returns a list of resources available in a folder.
     *
     * @param folder The folder resource name.
     *
     * @return The resource names.
     */
    public List<String> list(String folder) {
        Objects.requireNonNull(folder, "The folder resource name is required.");

        try (Stream<String> resources = stream(folder)) {
            return resources.collect(Collectors.toList());
        }
    }

    /**
     * Returns a list of resources available in a folder whose name matches a given pattern.
     *
     * @param folder  The folder resource name.
     * @param pattern The pattern to match.
     *
     * @return The resource names.
     */
    public List<String> listMatching(String folder, Pattern pattern) {
        Objects.requireNonNull(folder, "The folder resource name is required.");
        Objects.requireNonNull(pattern, "The pattern to match is required.");

        try (Stream<String> resources = stream(folder)) {
            return resources
                .filter(resource -> pattern.matcher(resource).matches())
                .collect(Collectors.toList());
        }
    }

    /**
     * Streams the names of available resources in a folder.
     *
     * <p>You will need to close this stream to ensure some resources are released.</p>
     *
     * @param folder The folder resource name.
     *
     * @return The resource name stream.
     */
    public Stream<String> stream(String folder) {
        Objects.requireNonNull(folder, "The folder resource name is required.");

        try {
            Enumeration<URL> folderUrls = classLoader.getResources(folder);
            Stream<String> stream = null;

            while (folderUrls.hasMoreElements()) {
                URL folderUrl = folderUrls.nextElement();
                Stream<String> folderStream;

                if (folderUrl.getProtocol().equals("jar")) {
                    folderStream = streamFromJar(folder, folderUrl);
                } else {
                    folderStream = streamFromFileSystem(folder, folderUrl);
                }

                if (stream == null) {
                    stream = folderStream;
                } else {
                    stream = Stream.concat(stream, folderStream);
                }
            }

            if (stream == null) {
                return Stream.<String>builder().build();
            }

            return stream
                .filter(resource -> !resource.endsWith(".class"))
                .distinct();
        } catch (IOException cause) {
            throw new ResourceException(
                String.format("The resource folder, %s, could not be read.", folder),
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
    private void getResource(String name, IntConsumer reader) {
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

    /**
     * Streams the names of available resources from the file system.
     *
     * @param folder    The folder resource name.
     * @param folderUrl The folder location.
     *
     * @return The resource name stream.
     */
    private Stream<String> streamFromFileSystem(String folder, URL folderUrl) {
        int base = folderUrl.getPath().length();

        try {
            return Files
                .walk(Paths.get(folderUrl.toURI()))
                .filter(path -> path.toFile().isFile())
                .map(path -> folder + path.toString().substring(base));
        } catch (IOException | URISyntaxException cause) {
            throw new ResourceException(
                String.format("The resource folder, %s, could not be read.", folder),
                cause
            );
        }
    }

    /**
     * Streams the names of available resources from the JAR.
     *
     * <p>This method operates under the assumption that only one JAR path is ever found by the
     * <code>stream()</code> method when <code>getResources()</code> is invoked. There could be
     * performance or other issues if this assumption turns out to not be true. Should be safe,
     * tests did not reveal otherwise.</p>
     *
     * @param folder    The folder resource name.
     * @param folderUrl The folder location.
     *
     * @return The resource name stream.
     */
    private Stream<String> streamFromJar(String folder, URL folderUrl) {
        final String path = folderUrl.getPath().substring(5, folderUrl.getPath().indexOf("!/"));

        try {
            JarFile file = new JarFile(path);

            return file
                .stream()
                .map(ZipEntry::getName)
                .filter(name -> name.startsWith(folder))
                .filter(name -> !name.endsWith("/"))
                .onClose(() -> {
                    try {
                        file.close();
                    } catch (IOException cause) {
                        throw new ResourceException(
                            String.format("The JAR file, %s, could not be closed.", path),
                            cause
                        );
                    }
                });
        } catch (IOException cause) {
            throw new ResourceException(
                String.format("The resource folder in the JAR, %s, could not be read.", path),
                cause
            );
        }
    }
}
