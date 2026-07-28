echo Checking for Drivers


echo Checking for DB2 Drivers db2jcc4.jar and db2jcc_license_cu.jar
if test -e ../../Libraries/db2jcc4.jar 
then
  if test -e ../../Libraries/db2jcc_license_cu.jar
  then
  	echo DB2 Driver Found. Copying r_DB2Database.j DB2Database.java
  	cp -f ../../PicassoServer/iisc/dsl/picasso/server/db/db2/r_DB2Database.j ../../PicassoServer/iisc/dsl/picasso/server/db/db2/DB2Database.java
  	echo DB2 Activated.
  else 
  	echo DB2 License Not Found
  	cp -f ../../PicassoServer/iisc/dsl/picasso/server/db/db2/w_DB2Database.j ../../PicassoServer/iisc/dsl/picasso/server/db/db2/DB2Database.java
  fi
else 
  echo DB2 Driver not found.
  cp -f ../../PicassoServer/iisc/dsl/picasso/server/db/db2/w_DB2Database.j ../../PicassoServer/iisc/dsl/picasso/server/db/db2/DB2Database.java
fi

echo Checking for Oracle Driver ojdbc14.jar
if test -e ../../Libraries/ojdbc14.jar
then
	echo Oracle Driver Found. Copying r_OracleDatabase.j OracleDatabase.java
	cp -f ../../PicassoServer/iisc/dsl/picasso/server/db/oracle/r_OracleDatabase.j ../../PicassoServer/iisc/dsl/picasso/server/db/oracle/OracleDatabase.java
	echo Oracle Activated
 else 
	echo Oracle Driver Not Found
	cp -f ../../PicassoServer/iisc/dsl/picasso/server/db/oracle/w_OracleDatabase.j ../../PicassoServer/iisc/dsl/picasso/server/db/oracle/OracleDatabase.java

fi

echo Checking for MSSQL 2005 Driver sqljdbc4.jar
if test -e ../../Libraries/sqljdbc4.jar 
then
	echo MSSQL 2005 Driver Found. Copying r_MSSQLDatabase.j MSSQLDatabase.java
	cp -f ../../PicassoServer/iisc/dsl/picasso/server/db/mssql/r_MSSQLDatabase.j ../../PicassoServer/iisc/dsl/picasso/server/db/mssql/MSSQLDatabase.java
	echo MSSQL Activated.
 else 
	echo MSSQL2005 Driver Not Found
	cp -f ../../PicassoServer/iisc/dsl/picasso/server/db/mssql/w_MSSQLDatabase.j ../../PicassoServer/iisc/dsl/picasso/server/db/mssql/MSSQLDatabase.java
fi


echo Checking for Postgres Driver postgresql-8.0-311.jdbc3.jar
if test -e ../../Libraries/postgresql-8.0-311.jdbc3.jar 
then
	echo Postgres Driver Found. Copying r_PostgresDatabase.j PostgresDatabase.java
	cp -f ../../PicassoServer/iisc/dsl/picasso/server/db/postgres/r_PostgresDatabase.j ../../PicassoServer/iisc/dsl/picasso/server/db/postgres/PostgresDatabase.java
	echo Postgres Activated
 else 
	echo Postgres Driver Not Found
	cp -f ../../PicassoServer/iisc/dsl/picasso/server/db/postgres/w_PostgresDatabase.j ../../PicassoServer/iisc/dsl/picasso/server/db/postgres/PostgresDatabase.java
fi



echo Checking for Sybase Driver jconn3.jar
if test -e ../../Libraries/jconn3.jar 
then
	echo Sybase Driver Found. Copying r_SybaseDatabase.j SybaseDatabase.java
	cp -f ../../PicassoServer/iisc/dsl/picasso/server/db/sybase/r_SybaseDatabase.j ../../PicassoServer/iisc/dsl/picasso/server/db/sybase/SybaseDatabase.java
	echo Sybase Activated
 else 
	echo Sybase Driver Not Found
	cp -f ../../PicassoServer/iisc/dsl/picasso/server/db/sybase/w__SybaseDatabase.j ../../PicassoServer/iisc/dsl/picasso/server/db/sybase/SybaseDatabase.java
fi



