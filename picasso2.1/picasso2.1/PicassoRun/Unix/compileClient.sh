#!/bin/bash
cd ../../
d=`pwd`
cd - 

find $d/PicassoClient/ -iname '*.class' | xargs rm -f
find $d/PicassoCommon/ -iname '*.class' | xargs rm -f

export CLASSPATH=.:$d/PicassoClient:$d/PicassoCommon:$d/Libraries/visad.jar:$d/Libraries/j3dcore.jar:$d/Libraries/j3dutils.jar:$d/Libraries/jgraph.jar:$d/Libraries/jgraphlayout.jar:$d/Libraries/vecmath.jar:$d/Libraries/vecmath.jar:$d/Libraries/swing-layout-1.0.jar;

javac $d/PicassoClient/iisc/dsl/picasso/client/Picasso_Frame.java
javac $d/PicassoClient/iisc/dsl/picasso/client/PicassoCmd.java
mkdir Logs 2> file
rm file
