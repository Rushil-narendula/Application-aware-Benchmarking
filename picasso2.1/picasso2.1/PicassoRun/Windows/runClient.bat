@ECHO OFF
IF !%1==! SET SERVER=localhost 
IF NOT !%1==! SET SERVER=%1
IF !%2==! SET PORT=4444
IF NOT !%2==! SET PORT=%2
IF !%3==! SET CWD=%cd%\..\.. 
IF NOT !%3==! SET CWD=%3

ECHO SERVER=%SERVER%
ECHO PORT=%PORT%
ECHO CWD=%CWD%

title Picasso Client
cd ..\..\
java -Xmx256m -classpath .;./PicassoClient;./PicassoCommon;./Libraries/visad.jar;./Libraries/j3dcore.jar;./Libraries/j3dutils.jar;./Libraries/jgraph.jar;./Libraries/jgraphlayout.jar;./Libraries/vecmath.jar;./Libraries/swing-layout-1.0.jar; iisc.dsl.picasso.client.Picasso_Frame %SERVER% %PORT% %CWD% 

rem To activate the AutoConvert SQL Dialect feature, include the SwisSQLAPI.jar file in Libraries folder, comment the line above and uncomment the line below
rem java -Xmx256m -classpath .;./PicassoClient;./PicassoCommon;./Libraries/visad.jar;./Libraries/j3dcore.jar;./Libraries/j3dutils.jar;./Libraries/jgraph.jar;./Libraries/jgraphlayout.jar;./Libraries/vecmath.jar;./Libraries/swing-layout-1.0.jar;./Libraries/SwisSQLAPI.jar; iisc.dsl.picasso.client.Picasso_Frame %SERVER% %PORT% %CWD% 
cd PicassoRun\Windows
title DOS
pause