clean:
	mvn clean \
		compile \
		exec:java -Dexec.mainClass=com.salescode.dim.jooq.JooqCodeGenerator \
				  -Dexec.cleanupDaemonThreads=false

		install
