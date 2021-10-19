lazy val extPagedMediaVersion = "2.50.1"

name         := "paged-media"
organization := "com.xmlcalabash"
homepage     := Some(url("https://xmlcalabash.com/"))
version      := extPagedMediaVersion
scalaVersion := "2.13.5"
//maintainer   := "ndw@nwalsh.com" // for packaging

resolvers += "Restlet" at "https://maven.restlet.com"
resolvers += "Geotoolkit" at "https://maven.geotoolkit.org" // javax.media: ...
resolvers += "JBoss" at "https://repository.jboss.org/nexus/content/repositories/thirdparty-releases/" // com.sun.media: ...

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.32"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.6"
libraryDependencies += "com.xmlcalabash" % "xml-calabash_2.13" % "2.99.5"

libraryDependencies += "org.apache.xmlgraphics" % "fop" % "2.4"
libraryDependencies += "org.apache.avalon.framework" % "avalon-framework-api" % "4.3.1"
libraryDependencies += "org.apache.avalon.framework" % "avalon-framework-impl" % "4.3.1"
libraryDependencies += "javax.media" % "jai_core" % "1.1.3"
libraryDependencies += "com.sun.media" % "jai-codec" % "1.1.3"

Compile / unmanagedClasspath += file(s"${baseDirectory.value}/lib")
Runtime / unmanagedClasspath += file(s"${baseDirectory.value}/lib")
