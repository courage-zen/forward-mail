# forward-mail
基于javamail的简单邮件转发工具，支持pop3协议与smtp协议，配合crontab定时使用

＃ 部署方式
安装最新的maven
编译并生成jar文件：mvn package
在某个服务器上自己的用户下建立一个目录，例如： /home/username/forward-mail
建立一个子目录用于存放日志文件，例如：/home/username/forward-mail/log
利用ftp等方式将编译好的jar文件放入服务器上你的目录中
编写一个启动shell文件（例如forward-mail.sh)：
sh /home/username/.bash_profile
java -jar /home/username/forward-mail/forward-mail-0.1.jar 132.228.56.206 132.228.56.136 username password newmail@qq.com /home/username/for
ward-mail/log/
给shell文件可运行：chmod +x forward-mail.sh
编写一个crontab任务（例如每30分钟运行一次）： crontab -e进入编辑状态
*/30 * * * * /home/username/forward-mail/forward-mail.sh >> /home/zhengyong/forward-mail/log/run.log


