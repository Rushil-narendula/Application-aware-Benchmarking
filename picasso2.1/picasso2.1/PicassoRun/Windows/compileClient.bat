@ECHO OFF

del /F /Q ..\..\PicassoClient\iisc\dsl\picasso\client\*.class 2>NUL
del /F /Q ..\..\PicassoClient\iisc\dsl\picasso\client\frame\*.class 2>NUL
del /F /Q ..\..\PicassoClient\iisc\dsl\picasso\client\network\*.class 2>NUL
del /F /Q ..\..\PicassoClient\iisc\dsl\picasso\client\panel\*.class 2>NUL
del /F /Q ..\..\PicassoClient\iisc\dsl\picasso\client\print\*.class 2>NUL
del /F /Q ..\..\PicassoClient\iisc\dsl\picasso\client\util\*.class 2>NUL
del /F /Q ..\..\PicassoCommon\iisc\dsl\picasso\common\*.class 2>NUL
del /F /Q ..\..\PicassoCommon\iisc\dsl\picasso\common\ds\*.class 2>NUL
del /F /Q ..\..\PicassoCommon\iisc\dsl\picasso\common\db\*.class 2>NUL

javac -classpath .;../../PicassoClient;../../PicassoCommon;../../Libraries/visad.jar;../../Libraries/j3dcore.jar;../../Libraries/j3dutils.jar;../../Libraries/jgraph.jar;../../Libraries/jgraphlayout.jar;../../Libraries/vecmath.jar;../../Libraries/swing-layout-1.0.jar; ../../PicassoClient/iisc/dsl/picasso/client/Picasso_Frame.java
javac -classpath .;../../PicassoClient;../../PicassoCommon;../../Libraries/visad.jar;../../Libraries/j3dcore.jar;../../Libraries/j3dutils.jar;../../Libraries/jgraph.jar;../../Libraries/jgraphlayout.jar;../../Libraries/vecmath.jar;../../Libraries/swing-layout-1.0.jar; ../../PicassoClient/iisc/dsl/picasso/client/PicassoCmd.java

mkdir ..\Logs 2>NUL

pause
