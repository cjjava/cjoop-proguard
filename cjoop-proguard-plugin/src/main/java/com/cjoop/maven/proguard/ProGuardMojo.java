package com.cjoop.maven.proguard;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
*
* <p>
* The Obfuscate task provides a stand-alone obfuscation task
* </p>
*
* @goal proguard
* @phase package
* @description Create small jar files using ProGuard
* @requiresDependencyResolution compile
* @threadSafe
* @author chenjun
*/
public class ProGuardMojo extends AbstractMojo{
	private Log log;
	/**
	 * Set this to 'true' to bypass ProGuard processing entirely.
	 *
	 * @parameter property="proguard.skip"
	 */
	private boolean skip;
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		log = getLog();
		if (skip) {
			log.info("Bypass ProGuard processing because \"proguard.skip=true\"");
			return;
		}
		log.info("xxxxxxxxxxxxxxx");
	}

}
