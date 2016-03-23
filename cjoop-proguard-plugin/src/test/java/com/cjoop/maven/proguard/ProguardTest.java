package com.cjoop.maven.proguard;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import proguard.ProGuard;

public class ProguardTest {
	
	public static void main(String[] args) throws Exception {
		File target = new File("D:\\git\\cjoop-proguard\\cjoop-proguard-simple\\cjoop-proguard-simple-war\\target");
		 
		List<String> list = new ArrayList<String>();
		//list.add("-injars D:\\git\\cjoop-proguard\\cjoop-proguard-simple\\cjoop-proguard-simple-war\\target\\cjoop-proguard-simple-war-0.0.1-SNAPSHOT.war");
		//list.add("-injars D:\\git\\cjoop-proguard\\cjoop-proguard-simple\\cjoop-proguard-simple-war\\target\\test.jar");
		list.add("-outjars D:\\git\\cjoop-proguard\\cjoop-proguard-simple\\cjoop-proguard-simple-war\\target\\test-p.jar");
		list.add("-libraryjars D:\\java\\jdk1.7.0_45_x86\\jre\\lib\\rt.jar");
		list.add("-keep class org.**{*;}");
		list.add("-dontnote org.**");
		list.add("-dontshrink");
		list.add("-keeppackagenames com.cjoop");
		list.add("-keepclassmembers class com.cjoop.**.domain.* {void set*(***);*** get*();}");
		String cmd = StringUtils.join(list,System.lineSeparator());
		System.out.println(cmd);
		args = list.toArray(new String[list.size()]);
		
		//package jar
		

		ProGuard.main(args);
	}
	
}
