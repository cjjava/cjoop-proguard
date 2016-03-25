### cjoop-proguard - maven项目混淆插件
# Installation

## Java Requirements
* JDK1.6+.

## Maven Requirements
* maven3.0.0+.

该项目是对https://github.com/wvengen/proguard-maven-plugin 项目的简化,同时提供对war项目2种环境的混淆支持.

----
	<plugin>
	<groupId>com.cjoop</groupId>
	<artifactId>cjoop-proguard-plugin</artifactId>
	<version>0.0.1</version>
	<executions>
		<execution>
			<id>run-proguard</id>
			<phase>package</phase>
			<goals>
				<goal>proguard</goal>
			</goals>
		</execution>
	</executions>
	<configuration>
		<options>
			<option>-target 1.7</option>
			<option>-dontoptimize</option>
			<option>-dontshrink</option>
			<option>-keepdirectories</option>
			<option>-renamesourcefileattribute SourceFile</option>
			<option>-useuniqueclassmembernames</option>
			<option>....</option>
		</options>
		<libs>
			<lib>${java.home}/lib/rt.jar</lib>
		</libs>
	</configuration>
</plugin>
----

如果在本机进行打包操作,先删除maven-gpg-plugin插件的引用(没有配置gpg的情况).

## Found a bug?
如果有请在[这里](https://github.com/cjjava/cjoop-proguard/issues/new)提交,我会及时修复.

Change log
----------
**ver 0.0.1:**

- 支持jar,war混淆.

