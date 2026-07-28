echo off
echo Checking for Drivers


echo Checking for DB2 Drivers db2jcc4.jar and db2jcc_license_cu.jar
IF EXIST ..\..\Libraries\db2jcc4.jar  (
  IF EXIST ..\..\Libraries\db2jcc_license_cu.jar (
  	echo DB2 Driver Found. Copying r_DB2Database.j DB2Database.java
  	copy /Y ..\..\PicassoServer\iisc\dsl\picasso\server\db\db2\r_DB2Database.j ..\..\PicassoServer\iisc\dsl\picasso\server\db\db2\DB2Database.java
  	echo DB2 Activated.
  ) ELSE (
  	echo DB2 License Not Found
  	copy /Y ..\..\PicassoServer\iisc\dsl\picasso\server\db\db2\w_DB2Database.j ..\..\PicassoServer\iisc\dsl\picasso\server\db\db2\DB2Database.java 2>NUL >NUL
  )
  ) ELSE (
  	echo DB2 Driver not found.
  	copy /Y ..\..\PicassoServer\iisc\dsl\picasso\server\db\db2\w_DB2Database.j ..\..\PicassoServer\iisc\dsl\picasso\server\db\db2\DB2Database.java 2>NUL >NUL
  )

echo Checking for Oracle Driver ojdbc14.jar
IF EXIST ..\..\Libraries\ojdbc14.jar (
	echo Oracle Driver Found. Copying r_OracleDatabase.j OracleDatabase.java
	copy /Y ..\..\PicassoServer\iisc\dsl\picasso\server\db\oracle\r_OracleDatabase.j ..\..\PicassoServer\iisc\dsl\picasso\server\db\oracle\OracleDatabase.java
	echo Oracle Activated.
) ELSE (
	echo Oracle Driver Not Found
	copy /Y ..\..\PicassoServer\iisc\dsl\picasso\server\db\oracle\w_OracleDatabase.j ..\..\PicassoServer\iisc\dsl\picasso\server\db\oracle\OracleDatabase.java 2>NUL >NUL
)

echo Checking for MSSQL 2005 Driver sqljdbc4.jar
IF EXIST ..\..\Libraries\sqljdbc4.jar (
	echo MSSQL 2005 Driver Found. Copying r_MSSQLDatabase.j MSSQLDatabase.java
	copy /Y ..\..\PicassoServer\iisc\dsl\picasso\server\db\mssql\r_MSSQLDatabase.j ..\..\PicassoServer\iisc\dsl\picasso\server\db\mssql\MSSQLDatabase.java
	echo MSSQL Activated
) ELSE (
	echo MSSQL2005 Driver Not Found
	copy /Y ..\..\PicassoServer\iisc\dsl\picasso\server\db\mssql\w_MSSQLDatabase.j ..\..\PicassoServer\iisc\dsl\picasso\server\db\mssql\MSSQLDatabase.java 2>NUL >NUL
)

echo Checking for Postgres Driver postgresql-8.0-311.jdbc3.jar
IF EXIST ..\..\Libraries\postgresql-8.0-311.jdbc3.jar (
	echo Postgres Driver Found. Copying r_PostgresDatabase.j PostgresDatabase.java
	copy /Y ..\..\PicassoServer\iisc\dsl\picasso\server\db\postgres\r_PostgresDatabase.j ..\..\PicassoServer\iisc\dsl\picasso\server\db\postgres\PostgresDatabase.java
	echo Postgres Activated
) ELSE (
	echo Postgres Driver Not Found
	copy /Y ..\..\PicassoServer\iisc\dsl\picasso\server\db\postgres\w_PostgresDatabase.j ..\..\PicassoServer\iisc\dsl\picasso\server\db\postgres\PostgresDatabase.java 2>NUL >NUL
)


echo Checking for Sybase Driver jconn3.jar
IF EXIST ..\..\Libraries\jconn3.jar (
	echo Sybase Driver Found. Copying r_SybaseDatabase.j SybaseDatabase.java
	copy /Y ..\..\PicassoServer\iisc\dsl\picasso\server\db\sybase\r_SybaseDatabase.j ..\..\PicassoServer\iisc\dsl\picasso\server\db\sybase\SybaseDatabase.java
	echo Sybase Activated
) ELSE (
	echo Sybase Driver Not Found
	copy /Y ..\..\PicassoServer\iisc\dsl\picasso\server\db\sybase\w_SybaseDatabase.j ..\..\PicassoServer\iisc\dsl\picasso\server\db\sybase\SybaseDatabase.java 2>NUL >NUL
)

pause
echo on
