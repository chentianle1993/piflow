1.maven error
apt-get install maven
mvn install:install-file -Dfile=/opt/project/piflow/piflow-bundle/lib/spark-xml_2.11-0.4.2.jar -DgroupId=com.databricks -DartifactId=spark-xml_2.11 -Dversion=0.4.2 -Dpackaging=jar
mvn install:install-file -Dfile=/Work/piflow/piflow-bundle/lib/java_memcached-release_2.6.6.jar -DgroupId=com.memcached -DartifactId=java_memcached-release -Dversion=2.6.6 -Dpackaging=jar
mvn install:install-file -Dfile=/Work/piflow/piflow-bundle/lib/ojdbc6.jar -DgroupId=jdbc_oracle -DartifactId=ojdbc -Dversion=6.0.0 -Dpackaging=jar
mvn install:install-file -Dfile=/Work/piflow/piflow-bundle/lib/ojdbc5.jar -DgroupId=jdbc_oracle -DartifactId=ojdbc -Dversion=5.0.0 -Dpackaging=jar

2.packaging

clean package -Dmaven.test.skip=true -U

3.set SPARK_HOME in Configurations
  Edit Configurations --> Application(HttpService) --> Configurations --> Environment Variable

4. yarn log aggregation
  Edit yarn-site.xml, add the following content
     <property>
      <name>yarn.log-aggregation-enable</name>
      <value>true</value>
     </property>

     <property>
      <name>yarn.nodemanager.log-aggregation.debug-enabled</name>
      <value>true</value>
     </property>

     <property>
      <name>yarn.nodemanager.log-aggregation.roll-monitoring-interval-seconds</name>
      <value>3600</value>
     </property>


