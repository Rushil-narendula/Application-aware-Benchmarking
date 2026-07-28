#!/bin/bash
cd ../../
d=`pwd`
export CLASSPATH=.:$d/PicassoClient:$d/PicassoCommon:$d/Libraries/visad.jar:$d/Libraries/j3dcore.jar:$d/Libraries/j3dutils.jar:$d/Libraries/jgraph.jar:$d/Libraries/jgraphlayout.jar:$d/Libraries/vecmath.jar
java -Xmx256m iisc.dsl.picasso.client.PicassoCmd "$@"
cd PicassoRun/Unix
