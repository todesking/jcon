# JCON: Generic JDBC console

## USAGE

```
jcon [-p|--password <password>] [-u|--user <user>] <jdbc_url>
```

## License

* The software: [GPL V3](./LICENSE)
* Launcher script(`bin/jcon`, `bin/jcon.bat` in distribution package): Belongs to `sbt-native-packager` plugin. See the [License](https://github.com/sbt/sbt-native-packager/blob/master/LICENSE.md)
* Scallop: [MIT](https://github.com/scallop/scallop)
* scalaz: [BSD](https://github.com/scalaz/scalaz)
* jansi: [ASL 2.0](http://jansi.fusesource.org)
* jline: [BSD](https://github.com/jline/jline2)
* Scala runtime: [Scala license](http://www.scala-lang.org/license.html)

### Drivers

* Postgres JDBC Driver: [BSD License](http://jdbc.postgresql.org/about/license.html)
* MySql Connector/J: [GPL](http://dev.mysql.com/downloads/connector/j/)
* SQLite JDBC Driver: [Apache License version 2.0](https://bitbucket.org/xerial/sqlite-jdbc), and SQLite is [Public Domain](http://www.sqlite.org/copyright.html)
* H2 Database Engine: [MPL 2.0 or EPL 1.0](http://www.h2database.com/html/license.html)
