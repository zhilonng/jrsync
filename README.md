jrsync
=====
A Java implementation of the rsync algorithm (
Usage:
1. Start daemon:启动服务器监听端口
java -cp JrsyncDaemon.jar com.scut.jrsync.JsyncDaemon port

2. Synchronize directory 同步目录

2.1 Directory from local to remote:同步整个目录
Java -cp JsyncClient.jar com.scut.jrsync.JsyncClient ClientDirectory serverIP:port ServerDirectory

2.2 Directory contents from local to remote:同步目录下所有内容
java -cp build/libs/jsync.jar com.scut.jrsync.JsyncClient ClientDirectory/ serverIP:port ServerDirectory
ps:ServerDirectory will be created if non-existant.

6.6 月增加 
Filelist
RabinKarpRollingHash(指纹计算实现)

