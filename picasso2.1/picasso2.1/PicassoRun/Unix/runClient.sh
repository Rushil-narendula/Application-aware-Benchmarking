#!/bin/bash
cd ../../
d=`pwd`


ServerName=localhost
ServerPort=4444
export CLASSPATH=.:$d/PicassoClient:$d/PicassoCommon:$d/Libraries/visad.jar:$d/Libraries/j3dcore.jar:$d/Libraries/j3dutils.jar:$d/Libraries/jgraph.jar:$d/Libraries/jgraphlayout.jar:$d/Libraries/vecmath.jar:$d/Libraries/swing-layout-1.0.jar;
#If you included the SwisSQLAPI.jar uncomment the line below
#export CLASSPATH=$CLASSPATH:$d/Libraries/SwisSQLAPI.jar
java -Xmx256m iisc.dsl.picasso.client.Picasso_Frame $ServerName $ServerPort $d

cd PicassoRun/Unix

