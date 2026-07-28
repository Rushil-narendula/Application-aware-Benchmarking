@ECHO OFF

set arglist=

:label
shift
set arglist=%arglist% %0
if not "%1"=="" goto label

title Picasso
cd ..\..\

java -Xmx256m -classpath .;./PicassoClient;./PicassoCommon;./Libraries/visad.jar;./Libraries/j3dcore.jar;./Libraries/j3dutils.jar;./Libraries/jgraph.jar;./Libraries/jgraphlayout.jar;./Libraries/vecmath.jar iisc.dsl.picasso.client.PicassoCmd %arglist%

cd PicassoRun\Windows
title DOS
