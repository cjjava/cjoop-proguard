package com.cjoop.maven.proguard.simple;

import org.apache.commons.lang.StringUtils;

public class Application {

	public static void main(String[] args) {
		StringUtils.join(args);
		System.out.println("cjoop-proguard-simple-jar");
	}
}
