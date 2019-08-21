# canteen-maven-plugin

The `canteen-maven-plugin` is used to make executable jar files self-executing. That is, executable from the command 
line as if they were native programs instead of requiring a `java -jar` command.

## Usage

```xml
<build>
    <plugins>
        <!-- Make an executable jar self-executing -->
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