jrsync
=====
A Java implementation of the rsync algorithm (
Usage:
1. Start daemon:���������������˿�
java -cp JrsyncDaemon.jar com.scut.jrsync.JsyncDaemon port

2. Synchronize directory ͬ��Ŀ¼

2.1 Directory from local to remote:ͬ������Ŀ¼
Java -cp JsyncClient.jar com.scut.jrsync.JsyncClient ClientDirectory serverIP:port ServerDirectory

2.2 Directory contents from local to remote:ͬ��Ŀ¼����������
java -cp build/libs/jsync.jar com.scut.jrsync.JsyncClient ClientDirectory/ serverIP:port ServerDirectory
ps:ServerDirectory will be created if non-existant.

6.6 ������ 
Filelist
RabinKarpRollingHash(ָ�Ƽ���ʵ��)

