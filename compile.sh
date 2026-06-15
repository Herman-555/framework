rm -rf bin framework.jar
mkdir bin
javac -source 17 -target 17 -cp "lib/jakarta.servlet-api-6.0.0.jar" -d bin src/framework/*.java
jar cvf framework.jar -C bin .
