Common.appSettings(messagesFilesFrom = Seq("common", "admin", "web", "b2b", "api"))

lazy val common = (project in file("modules/common")).enablePlugins(PlayScala, PlayEbean)

lazy val admin = (project in file("modules/admin")).enablePlugins(PlayScala).dependsOn(common)

lazy val b2b = (project in file("modules/b2b")).enablePlugins(PlayScala).dependsOn(common)

lazy val web = (project in file("modules/web")).enablePlugins(PlayScala).dependsOn(common)

lazy val api = (project in file("modules/api")).enablePlugins(PlayScala).dependsOn(common)

lazy val root = (project in file(".")).enablePlugins(PlayScala).aggregate(common, admin, b2b, web, api).dependsOn(common, admin, b2b, web, api)


libraryDependencies ++= Common.commonDependencies

scalariformSettings