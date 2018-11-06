A proof-of-concept project to adaptively adjust the size of the thread pool used to process requests based on performance metrics.

Instructions for use
1. Clone the repository

2. cd into the base directory of the repository 

3. This requires a modified version of dropwizard metrics. Execute mvn install as follows to add the included jar file as a dependency.
mvn install:install-file \
-Dfile=src/main/resources/metrics-core-3.1.0.jar \
-DgroupId=io.dropwizard.metrics \
-DartifactId=metrics-core-custom \
-Dversion=3.1.0 \
-Dpackaging=jar

3. Execute mvn clean install
