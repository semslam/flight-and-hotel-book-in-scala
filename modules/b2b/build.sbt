Common.serviceSettings("b2b", messagesFilesFrom = Seq("common", "b2b"))

// Add here the specific settings for this module


libraryDependencies ++= Common.commonDependencies ++: Seq(
	"org.webjars" % "bootswatch-superhero" % "3.3.5+4"
	// Add here the specific dependencies for this module:
	// jdbc,
	// anorm
)

scalariformSettings