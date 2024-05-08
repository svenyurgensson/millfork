all: 
	sbt -DskipTests compile && sbt -DskipTests assembly && cp target/scala-2.12/millfork.jar /usr/local/opt/millfork/
