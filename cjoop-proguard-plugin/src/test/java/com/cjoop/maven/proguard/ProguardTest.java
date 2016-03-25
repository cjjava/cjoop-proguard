package com.cjoop.maven.proguard;

import java.io.File;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

public class ProguardTest {
	
	public static void main(String[] args) throws Exception {
		ZipParameters parameters = new ZipParameters();
		parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
		parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_FASTEST);
		File srcFile = new File("d:\\cjoop-proguard-simple-war-0.0.2-SNAPSHOT.war");
		ZipFile zipFile = new ZipFile(srcFile);
		File destPath = new File(srcFile.getParent(),FilenameUtils.getBaseName(srcFile.getName())+"-proguard");
		zipFile.extractAll(destPath.getAbsolutePath());
		proGuardLib(destPath);
		srcFile.delete();
		zipFile = new ZipFile(srcFile);
		for (File file : destPath.listFiles()) {
			if(file.isDirectory()){
				zipFile.addFolder(file, parameters);
			}else{
				zipFile.addFile(file, parameters);
			}
		}
	}
	
	public static void proGuardLib(File file){
		if(file.isDirectory()){
			for (File childFile : file.listFiles()) {
				proGuardLib(childFile);
			}
		}else{
			String fileName = file.getName();
			if(fileName.endsWith(".jar")){
				String newName = uuid()+".jar";
				File dest = new File(file.getParent(),newName);
				file.renameTo(dest);
				System.out.println(fileName + " --> " + dest);
			}
		}
	}
	
	
	public static String uuid(){
		return UUID.randomUUID().toString().replaceAll("-", "");
	}
	
}
