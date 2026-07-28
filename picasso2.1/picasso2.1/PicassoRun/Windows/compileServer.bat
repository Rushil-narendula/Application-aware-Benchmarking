@ECHO OFF

del /F /Q ..\..\PicassoServer\iisc\dsl\picasso\server\*.class 2>NUL
del /F /Q ..\..\PicassoServer\iisc\dsl\picasso\server\network\*.class 2>NUL
del /F /Q ..\..\PicassoServer\iisc\dsl\picasso\server\plan\*.class 2>NUL
del /F /Q ..\..\PicassoServer\iisc\dsl\picasso\server\query\*.class 2>NUL
del /F /Q ..\..\PicassoServer\iisc\dsl\picasso\server\db\*.class 2>NUL
del /F /Q ..\..\PicassoServer\iisc\dsl\picasso\server\db\datatype\*.class 2>NUL
del /F /Q ..\..\PicassoServer\iisc\dsl\picasso\server\db\db2\*.class 2>NUL
del /F /Q ..\..\PicassoServer\iisc\dsl\picasso\server\db\mssql\*.class 2>NUL
del /F /Q ..\..\PicassoServer\iisc\dsl\picasso\server\db\oracle\*.class 2>NUL
del /F /Q ..\..\PicassoServer\iisc\dsl\picasso\server\db\postgres\*.class 2>NUL
del /F /Q ..\..\PicassoServer\iisc\dsl\picasso\server\db\sybase\*.class 2>NUL
del /F /Q ..\..\PicassoCommon\iisc\dsl\picasso\common\*.class 2>NUL
del /F /Q ..\..\PicassoCommon\iisc\dsl\picasso\common\ds\*.class 2>NUL
del /F /Q ..\..\PicassoCommon\iisc\dsl\picasso\common\db\*.class 2>NUL

javac -classpath .;../../PicassoServer;../../PicassoCommon;../../Libraries/pic_linearalgebra.jar;../../Libraries/db2jcc4.jar;../../Libraries/db2jcc_license_cu.jar;../../Libraries/ojdbc14.jar;../../Libraries/sqljdbc4.jar;../../Libraries/mysql-connector-java-5.1.8-bin.jar;../../Libraries/jconn3.jar;../../Libraries/postgresql-8.0-311.jdbc3.jar ../../PicassoServer/iisc/dsl/picasso/server/Picasso_Server.java

mkdir ..\Logs 2>NUL

pause
