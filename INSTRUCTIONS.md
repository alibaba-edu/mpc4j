# 使用`mpc4j`时的常用指令

## Maven

可以使用[Maven版本管理插件](http://www.mojohaus.org/versions-maven-plugin/)在多模块Maven项目中更新模块的版本号，参见[《如何轻松地在多模块Maven项目中更新模块的版本号》](https://segmentfault.com/a/1190000021421931)和[《maven的版本升级、切换》](https://my.oschina.net/dylw/blog/896906)。

如果想将版本设置为2.50.1-SNAPSHOT，可执行下述指令。此指令将调整多模块项目中的所有pom版本、父版本和依赖版本。

```shell
mvn versions:set -DnewVersion=2.50.1-SNAPSHOT
```

如果输入的版本号有误，可以执行下述指令恢复到上一个版本。

```shell
mvn versions:revert
```

如果对结果感到满意，可以执行下述指令提交版本号。

```shell
mvn versions:commit
```

## SSH

### 安装SSH

Ubuntu使用下述指令安装SSH服务。

```shell
sudo apt-get install openssh-server
```

CentOS使用下述指令安装SSH服务。

```shell
sudo yum -y install openssh-server
```

### 查看SSH是否启动

使用下述指令查看SSH是否启动。如果启动，命令结果下方应该显示sshd。

```shell
sudo ps -e | grep ssh
```

### 启动SSH

如果上述查看SSH的命令没有显示sshd，则需要启动SSH服务。

```shell
sudo /etc/init.d/ssh start
```

### 远程连接

假定远程服务器的用户名为username，IP地址为`xxx.xxx.xxx.xxx`，则使用下述指令完成远程连接。

```shell
ssh username@xxx.xxx.xxx.xxx
```

如果提示输入口令，则正确输入口令后即可启动远程连接。

### 向远程服务器上传文件

假定远程服务器的用户名为username，IP地址为`xxx.xxx.xxx.xxx`，想要上传的文件为当前目录下的`test.jar`，想要上传到远程服务器的目录`/home/username`下，则执行下述指令。

```shell
scp test.jar username@xxx.xxx.xxx.xxx:/home/username/
```

如果想要上传文件夹`doc/`，则执行下述指令。

```shell
scp -r doc/ username@xxx.xxx.xxx.xxx:/home/username/
```

### 从远程服务器下载文件

假定远程服务器的用户名为username，IP地址为`xxx.xxx.xxx.xxx`，想要下载远程服务器的文件`/home/username/test.jar`，想要下载到当前目录下，则使用下述指令。

>  最后的`.`表示为下载到当前目录下。

```shell
scp username@xxx.xxx.xxx.xxx:/home/username/test.jar .
```

## 后台运行程序命令`nohup`

虽然可以通过SSH控制服务器远程运行程序，但运行过程中SSH不能断开，否则运行的程序也会中止。这个问题的关键在于要让程序在服务器的后台运行。为了达到这一目的，需要使用`nohup`命令。`nohup`的英文全称是"no hang up"（不挂起），用于在系统后台不挂断地运行命令，退出终端不会影响程序的运行。`nohup`命令在默认情况下（非重定向时），会输出一个名叫`nohup.out`的文件到当前目录下，如果当前目录的`nohup.out`文件不可写，输出重定向到`$HOME/nohup.out`文件中。`nohup`的介绍参见[《Linux nohup 命令》](https://www.runoob.com/linux/linux-comm-nohup.html)。

假定我们要在后台挂起一个`test.jar`、参数为`10`的任务，则在SSH下执行下述命令：

```shell
nohup java -jar test.jar 10 &
```

如果想让输出结果重定向到另一个文件（如`log.out`），则在SSH下执行下述命令：

```shell
nohup java -jar test.jar 10 > log.out 2>&1 &
```

如果想查看此任务是否已经执行，在`SSH`执行下述命令：

```shell
ps -aux|grep java
```

输出格式：
```text
USER PID %CPU %MEM VSZ RSS TTY STAT START TIME COMMAND
```

## 网络限流命令`TC`

在进行性能测试时，往往需要人工对网络限流。经过测试，网上搜索到的`tcconfig`、`wondershaper`等工具都无法成功对局域网限流。因此，我们仍然需要使用`tc`实现限流。`tc`是一个非常复杂的工具，我们参考文章[《How to Use the Linux Traffic Control》](https://netbeez.net/blog/how-to-use-the-linux-traffic-control/)，只对局域网进行简单的限流。

### 安装`iproute`

`tc`命令包含在`iproute`中，执行下述指令安装`iproute`。

```shell
sudo apt-get install iproute
```

### 显示与删除策略

假定网卡名称为`lo`，执行下述指令，可以查看当前网卡`lo`的限流策略。

```shell
sudo tc qdisc show dev lo
```

如果想修改限流策略，首先需要删除当前的策略。假定网卡名称为`lo`，执行下述指令，可以删除当前网卡`lo`的限流策略。

```shell
sudo tc qdisc del dev lo root
```

### 增加网络延迟

假定网卡名称为`lo`，执行下述指令，可以将当前网卡`lo`网络带宽限制到1Mbit，RTT时间为80ms。

```shell
sudo tc qdisc add dev lo root netem rate 1mbit latency 80ms
```