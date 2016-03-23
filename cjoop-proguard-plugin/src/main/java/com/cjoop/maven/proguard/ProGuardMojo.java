package com.cjoop.maven.proguard;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import proguard.ProGuard;

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
	
	/**
	 * The Maven project reference where the plugin is currently being executed. The default value is populated from
	 * maven.
	 *
	 * @parameter property="project"
	 * @readonly
	 * @required
	 */
	protected MavenProject mavenProject;
	
	/**
	 * Directory containing the input and generated JAR.
	 *
	 * @parameter property="project.build.directory"
	 * @required
	 */
	protected File outputDirectory;
	
	/**
	 * Specifies the input jar name (or wars, ears, zips) of the application to be
	 * processed.
	 *
	 * You may specify a classes directory e.g. 'classes'. This way plugin will processed
	 * the classes instead of jar. You would need to bind the execution to phase 'compile'
	 * or 'process-classes' in this case.
	 *
	 * @parameter expression="${project.build.finalName}.jar"
	 * @required
	 */
	protected String injar;
	
	/**
	 * Specifies the names of the output jars. If attach=true the value ignored and name constructed base on classifier
	 * If empty input jar would be overdriven.
	 *
	 * @parameter
	 */
	protected String outjar;
	
	/**
	 * Specifies whether or not to attach the created artifact to the project
	 *
	 * @parameter default-value="false"
	 */
	private boolean attach;
	
	protected boolean sameArtifact;
	
	/**
	 * Specifies attach artifact type
	 *
	 * @parameter default-value="jar"
	 */
	private String attachArtifactType;

	/**
	 * Specifies attach artifact Classifier, Ignored if attach=false
	 *
	 * @parameter default-value="small"
	 */
	private String attachArtifactClassifier;
	
	/**
	 * Set to false to exclude the attachArtifactClassifier from the Artifact final name. Default value is true.
	 *
	 * @parameter default-value="true"
	 */
	private boolean appendClassifier;
	
	/**
	 * Set to true to include META-INF/maven/** maven descriptord
	 *
	 * @parameter default-value="false"
	 */
	private boolean addMavenDescriptor;
	
	/**
	 * Specifies not to obfuscate the input class files.
	 *
	 * @parameter default-value="true"
	 */
	private boolean obfuscate;
	/**
	 * Recursively reads configuration options from the given file filename
	 *
	 * @parameter default-value="${basedir}/proguard.conf"
	 */
	private File proguardInclude;
	/**
	 * Apply ProGuard classpathentry Filters to input jar. e.g. <code>!**.gif,!**&#47;tests&#47;**'</code>
	 *
	 * @parameter
	 */
	protected String inFilter;
	
	/**
	 * Apply ProGuard classpathentry Filters to output jar. e.g. <code>!**.gif,!**&#47;tests&#47;**'</code>
	 *
	 * @parameter
	 */
	protected String outFilter;
	/**
	 * Additional -libraryjars e.g. ${java.home}/lib/rt.jar Project compile dependency are added automatically. See
	 * exclusions
	 *
	 * @parameter
	 */
	private List<String> libs;
	/**
	 * Sets the name of the ProGuard mapping file.
	 *
	 * @parameter default-value="proguard_map.txt"
	 */
	protected String mappingFileName = "proguard_map.txt";

	/**
	 * Sets the name of the ProGuard seed file.
	 *
	 * @parameter default-value="proguard_seed.txt"
	 */
	protected String seedFileName = "proguard_seeds.txt";
	/**
	 * ProGuard configuration options
	 *
	 * @parameter
	 */
	private String[] options;
	
	File inJarFile,outJarFile;
	
	List<String> args = new ArrayList<String>();
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		log = getLog();
		if (skip) {
			log.info("Bypass ProGuard processing because \"proguard.skip=true\"");
			return;
		}
		
		checkInOut();
		
		buildInJars();
		
		buildOutJars();
		
		buildLibraryJars();
		
		buildOptions();
		
		String cmd = StringUtils.join(args,System.lineSeparator());
		if(log.isDebugEnabled()){
			log.info("-------------ProGuard Config----------------");
			System.out.println(cmd);
		}
		ProGuard.main(args.toArray(new String[args.size()]));
	}
	
	/**
	 * 构造其他参数信息
	 */
	protected void buildOptions(){
		if (!obfuscate) {
			args.add("-dontobfuscate");
		}
		if (proguardInclude != null) {
			if (proguardInclude.exists()) {
				args.add("-include");
				args.add(fileToString(proguardInclude));
				log.debug("proguardInclude " + proguardInclude);
			} else {
				log.debug("proguardInclude config does not exists " + proguardInclude);
			}
		}
		
		args.add("-printmapping");
		args.add(fileToString((new File(outputDirectory, mappingFileName))));

		args.add("-printseeds");
		args.add(fileToString((new File(outputDirectory,seedFileName))));

		if (log.isDebugEnabled()) {
			args.add("-verbose");
		}
		
		if (options != null) {
			Collections.addAll(args, options);
		}
		
	}
	
	/**
	 * 构建输入文件的参数信息
	 */
	protected void buildInJars(){
		StringBuilder sb = new StringBuilder("-injars ");
		sb.append(fileToString(inJarFile));
		List<String> inFilters = new ArrayList<String>();
		if(StringUtils.isNotBlank(inFilter)){
			for (String filterItem : inFilter.split(",")) {
				inFilters.add(filterItem);
			}
		}
		if(!addMavenDescriptor){
			inFilters.add("!META-INF/maven/**");
		}
		if(inFilters.size()>0){
			sb.append("(");
			sb.append(StringUtils.join(inFilters, ","));
			sb.append(")");
		}
		args.add(sb.toString());
	}
	
	/**
	 * 构建输出文件的参数信息
	 */
	protected void buildOutJars(){
		StringBuilder sb = new StringBuilder("-outjars ");
		sb.append(fileToString(outJarFile));
		if(StringUtils.isNotBlank(outFilter)){
			sb.append("(").append(outFilter).append(")");
		}
		args.add(sb.toString());
	}
	
	/**
	 * 构建依赖库参数信息
	 * @throws MojoExecutionException 
	 */
	protected void buildLibraryJars() throws MojoExecutionException {
		Set<Artifact> artifacts = mavenProject.getArtifacts();
		for (Artifact artifact : artifacts) {
			log.debug("--- artifact " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":"
					+ artifact.getType() + ":" + artifact.getClassifier() + " Scope:" + artifact.getScope());
			File file = getClasspathElement(artifact, mavenProject);
			args.add("-libraryjars " + fileToString(file));
		}
		if (libs != null) {
			for (String lib : libs) {
				args.add("-libraryjars " + fileNameToString(lib));
			}
		}
	}

	/**
	 * 检查输入输入环境是否满足条件
	 * @throws MojoFailureException
	 */
	protected void checkInOut() throws MojoFailureException{
		inJarFile = new File(outputDirectory, injar);
		if (!inJarFile.exists()) {
			throw new MojoFailureException("Can't find file " + inJarFile);
		}
		if (!outputDirectory.exists()) {
			if (!outputDirectory.mkdirs()) {
				throw new MojoFailureException("Can't create " + outputDirectory);
			}
		}
		if (attach) {
			outjar = nameNoType(injar);
			if (useArtifactClassifier()) {
				outjar += "-" + attachArtifactClassifier;
			}
			outjar += "." + attachArtifactType;
		}
		
		if ((outjar != null) && (!outjar.equals(injar))) {
			sameArtifact = false;
			outJarFile = (new File(outputDirectory, outjar)).getAbsoluteFile();
			if (outJarFile.exists()) {
				if (!deleteFileOrDirectory(outJarFile)) {
					throw new MojoFailureException("Can't delete " + outJarFile);
				}
			}
		} else {
			sameArtifact = true;
			outJarFile = inJarFile.getAbsoluteFile();
			File baseFile;
			if (inJarFile.isDirectory()) {
				baseFile = new File(outputDirectory, nameNoType(injar) + "_proguard_base");
			} else {
				baseFile = new File(outputDirectory, nameNoType(injar) + "_proguard_base.jar");
			}
			if (baseFile.exists()) {
				if (!deleteFileOrDirectory(baseFile)) {
					throw new MojoFailureException("Can't delete " + baseFile);
				}
			}
			if (inJarFile.exists()) {
				if (!inJarFile.renameTo(baseFile)) {
					throw new MojoFailureException("Can't rename " + inJarFile);
				}
			}
			inJarFile = baseFile;
		}
		log.info("--- injar:" + inJarFile.getAbsolutePath()+"---");
		log.info("--- outjar:" + outJarFile.getAbsolutePath()+"---");
	}
	
	private boolean useArtifactClassifier() {
		return appendClassifier && ((attachArtifactClassifier != null) && (attachArtifactClassifier.length() > 0));
	}
	
	private static boolean deleteFileOrDirectory(File path) throws MojoFailureException {
		if (path.isDirectory()) {
			File[] files = path.listFiles();
			if (null != files) {
				for (File file : files) {
					if (file.isDirectory()) {
						if (!deleteFileOrDirectory(file)) {
							throw new MojoFailureException("Can't delete dir " + file);
						}
					} else {
						if (!file.delete()) {
							throw new MojoFailureException("Can't delete file " + file);
						}
					}
				}
			}
			return path.delete();
		} else {
			return path.delete();
		}
	}
	
	private static String nameNoType(String fileName) {
		int extStart = fileName.lastIndexOf('.');
		if (extStart == -1) {
			return fileName;
		}
		return fileName.substring(0, extStart);
	}

	/**
	 * ProGuard docs: Names with special characters like spaces and parentheses must be quoted with single or double
	 * quotes.
	 */
	private static String fileNameToString(String fileName) {
		return "'" + fileName + "'";
	}

	private static String fileToString(File file) {
		return fileNameToString(file.toString());
	}
	
	private static File getClasspathElement(Artifact artifact, MavenProject mavenProject) throws MojoExecutionException {
		if (artifact.getClassifier() != null) {
			return artifact.getFile();
		}
		String refId = artifact.getGroupId() + ":" + artifact.getArtifactId();
		MavenProject project = (MavenProject) mavenProject.getProjectReferences().get(refId);
		if (project != null) {
			return new File(project.getBuild().getOutputDirectory());
		} else {
			File file = artifact.getFile();
			if ((file == null) || (!file.exists())) {
				throw new MojoExecutionException("Dependency Resolution Required " + artifact);
			}
			return file;
		}
	}
}
