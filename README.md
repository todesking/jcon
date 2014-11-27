# JCON: Generic JDBC console

## USAGE

### Common Options

* `--driver-dir <dir>`
  * default: `~/.jcon/drivers/`
* `--driver-classes <classes>`
  * ex: `--driver-classes com.example.specialdb,com.example.superdb.Driver`

### Connect to database

```
jcon [-p|--password <password>] [-u|--user <user>] <jdbc_url>
```

### List all available drivers

```
jcon --drivers
```

## Default bundled drivers

* Mysql
  * `jdbc:mysql://host[:port][/database]`
  * [Reference](http://dev.mysql.com/doc/connector-j/en/connector-j-reference-configuration-properties.html)
* SQLite
  * `jdbc:sqlite:/path/to/file`(file), `jdbc:sqlite::memory`(on-memory database)
  * [Reference](https://bitbucket.org/xerial/sqlite-jdbc/wiki/Home)
* H2 Database Engine
  * `jdbc:h2:/path/to/file` etc.
  * [Reference](http://www.h2database.com/html/features.html#database_url)
* Postgres SQL Database
  * `jdbc:postgressql:[//host[:port]/]database`
  * [Reference](http://jdbc.postgresql.org/documentation/80/connect.html)

## Not bundled drivers

To use external drivers, put driver jars into jcon's drivers directory(default: `~/.jcon/drivers/`)

* Oracle
  * [Download](http://www.oracle.com/technetwork/database/features/jdbc/index-091264.html)
* SQL Server
  * [Download](http://www.microsoft.com/ja-jp/download/details.aspx?id=11774)

