[![Build Status](https://travis-ci.org/kherge/java.resource.svg?branch=master)](https://travis-ci.org/kherge/java.resource)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=kherge_java.resource&metric=alert_status)](https://sonarcloud.io/dashboard?id=kherge_java.resource)

Resource
========

A library to simplify resource access.

Resource access behaves differently depending on context. For example, resources can be on the file
system (during testing) or contained in a JAR (after a build). This library provides a more
consistent method of accessing resource by eliminating this distinction.

```java
Resource resource = new Resource(MyClass.class.getClassLoader());

System.out.println(resource.getAsString("hello-world.txt", "utf-8"));
System.out.println(resource.list("path/to/folder"));
```

Requirements
------------

- Java 8

Installation
------------

### Gradle

```groovy
compile 'io.herrera.kevin:resource:1.1.0'
```

### Maven

```xml
<dependency>
  <groupId>io.herrera.kevin</groupId>
  <artifactId>resource</artifactId>
  <version>1.1.0</version>
</dependency>
```

Usage
-----

```java
import io.herrera.io.resource.Resource;
import java.io.InputStream;
import java.nio.file.Path;

class Example {
    public static void main() {
        try {

            // The manager is created using the desired class loader.
            Resource resource = new Resource(Example.class.getClassLoader());

            // A resource can be retrieved as an InputStream.
            InputStream stream = resource.getAsInputStream("my-resource.txt");

            // A resource can be retrieved as a file path.
            Path path = resource.getAsPath("my-resource.txt");

            // A resource can be retrieved as a string.
            String string = resource.getAsString("my-resource.txt");

            // List resources in a folder.
            List<String> resources = resource.list("path/to/folder");

            // List matching resources in a folder.
            // (This is more performant than using list() and then filtering.)
            List<String> resources = resource.listMatch("path/to/folder", ".+pattern.+");

            // Stream resources in a folder.
            resource.stream("path/to/folder").forEach(name -> System.out.println(
                resource.getAsString(name)
            ));

        } catch (ResourceException exception) {

            // If a resource could not be retrieved or listed, this exception is thrown.
            throw exception;

        }
    }
}
```

License
-------

This library is made available under the MIT and Apache 2.0 licenses.
