# Canteen

The `canteen-maven-plugin` is used to make executable jar files self-executing. That is, executable from the command 
line as if they were native programs instead of requiring a `java -jar` command.

```bash
# For example
java -jar copyjar-1.0.0.jar fromFile toFile

# Becomes
./copyjar-1.0.0-linux-x86_64.exe fromFile toFile
```

Canteen creates platform native self-executing jars for 64-bit Linux, MacOS, and Windows.

## Usage

Bundle your jar as an executable jar, and add the `canteen-maven-plugin` to your pom.xml.

```xml
<build>
    <plugins>
        <!-- Shade dependencies into an uber jar -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.2.1</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    
        <!-- Populate the jar's manifest main class to make it executable -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-jar-plugin</artifactId>
            <version>2.4</version>
            <configuration>
                <archive>
                    <manifest>
                        <addClasspath>true</addClasspath>
                        <mainClass>mypackage.Main</mainClass>
                    </manifest>
                </archive>
            </configuration>
        </plugin>

        <!-- Make the jar self-executing with Canteen -->
        <plugin>
            <groupId>com.salesforce.servicelibs</groupId>
            <artifactId>canteen-maven-plugin</artifactId>
            <version>${canteen.version}</version>
            <executions>
                <execution>
                    <goals>
                        <goal>bootstrap</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Canteen will add three additional artifacts to your Maven module with types and classifiers compatible with the 
[os-maven-plugin](https://github.com/trustin/os-maven-plugin).

* `artifactId:groupId:version:exe:linux-x86_64`
* `artifactId:groupId:version:exe:osx-x86_64`
* `artifactId:groupId:version:exe:windows-x86_64`

## How does Canteen work?

Canteen leverages a quirk of Java's jar format to make jars behave like native executables. 

Under the hood, jar files are just zip archives. A zip archive is made up of one or more independently compressed files,
followed by an index table. The zip index table is rooted at the end of the archive with offset pointers relative to the
final byte address of the file. The backwards-looking nature of the zip archive index means you can prepend a zip
archive with arbitrary data or executable code without violating the integrity of the archive. 

Unlike zip archives which are read backwards, executable programs are loaded and executed from byte zero. A common
technique for adding "data" to executables is to append bytes to the end of the executable, where it won't affect
execution.

Concatenating an executable and a zip archive results in a file that is both a valid executable (read from the front),
and a valid zip archive (read from the back). This technique was historically used to create self-extracting zip files.
The `canteen-maven-plugin` uses the same technique to prepend a simple platform specific bootstrap program
cross-compiled with Go to the front of a jar.

When you execute a Canteen packaged jar, the bootstrap program captures all the command line arguments and the name
of the current file. On Windows, the bootstrap spawns a child process as `java -jar $args`, proxying `stdin`, `stdout`,
and `stderr` between the shell and the child process. On Linux and MacOS, the bootstrap transfers process control to
Java by invoking `syscall.Exec()`;