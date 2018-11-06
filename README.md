A proof-of-concept project to adaptively adjust the size of the thread pool used to process requests based on performance metrics.

Instructions for use
1. Clone the repository

2. Set the path of the log file to be written, as the value of `log4j.appender.FILE.File` in `<baseDir>/src/main/resources/log4j.properties`

3. This application requires a modified version of dropwizard metrics. Execute mvn install as follows to add the included jar file as a dependency.

mvn install:install-file \\
-Dfile=src/main/resources/metrics-core-3.1.0.jar \\
-DgroupId=io.dropwizard.metrics \\
-DartifactId=metrics-core-custom \\
-Dversion=3.1.0 \\
-Dpackaging=jar

3. Execute `mvn clean compile assembly:single`

4. Execute the produced jar file by providing the test name as the 1st argument. Possible test names that are accepted are Factorial, Prime, Sqrt, DbWrite and DbRead
