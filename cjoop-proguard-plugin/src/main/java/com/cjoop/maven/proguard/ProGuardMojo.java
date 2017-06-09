package com.cjoop.maven.proguard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.jar.JarArchiver;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import proguard.Configuration;
import proguard.ConfigurationParser;
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
	private static final String WEB_INF = "WEB-INF";
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
	 * @parameter property="project.build.finalName"
	 */
	protected String finalName;
	
	/**
	 * Specifies the input jar name (or wars, ears, zips) of the application to be
	 * processed.
	 *
	 * You may specify a classes directory e.g. 'classes'. This way plugin will processed
	 * the classes instead of jar. You would need to bind the execution to phase 'compile'
	 * or 'process-classes' in this case.
	 * @parameter
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
	/**
	 * List of dependency exclusions
	 *
	 * @parameter
	 */
	private List<String> exclusions;
	/**
	 * 是否混淆jar文件名字,如果为true,所有的jar文件名字将会用uuid代替.
	 * @parameter default-value="false"
	 */
	protected boolean proGuardFileName;
	
	/**
	 * 记录哪些jar文件被修改了名字
	 * @parameter default-value="proguard_jars.txt"
	 */
	protected String jarFileName;
	
	File inFile,outFile,jarFile,proguardJarFile;
	/**
	 * 标记war是否被修改过
	 */
	protected boolean modifyWar = false;
	/**
	 * 混淆命令集合
	 */
	List<String> args = new ArrayList<String>();
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		log = getLog();
		if (skip) {
			log.info("Bypass ProGuard processing because \"proguard.skip=true\"");
			return;
		}
		jarFile = new File(outputDirectory,
				mavenProject.getArtifactId() + "-" + mavenProject.getVersion() + ".jar");
		proguardJarFile = new File(outputDirectory,
				FilenameUtils.getBaseName(jarFile.getName()) + "_proguard.jar");
		
		checkInOut();
		
		buildInJars();
		
		buildOutJars();
		
		modifyWarStructure();
		
		buildLibraryJars();
		
		buildOptions();
		
		executeProGuard();
		
		restoreWarStructure();
		
		proGuardFileName();
		
		clear();
	}
	
	private boolean isExclusion(String fileName) {
		if (exclusions == null) {
			return false;
		}
		for (String exclusion : exclusions) {
			if(fileName.startsWith(exclusion)){
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * 混淆文件名字
	 * @throws MojoExecutionException 
	 */
	protected void proGuardFileName() throws MojoExecutionException{
		if(proGuardFileName){
			String fileName = outFile.getName();
			try {
				if(fileName.endsWith(".war") || fileName.endsWith(".zip")){
					ZipParameters parameters = new ZipParameters();
					parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
					parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_FASTEST);
					ZipFile outZipFile = new ZipFile(outFile);
					File destPath = new File(outputDirectory,FilenameUtils.getBaseName(fileName)+"-proguard");
					outZipFile.extractAll(destPath.getAbsolutePath());
					List<String> logInfo = new ArrayList<String>();
					proGuardLib(destPath,logInfo);
					outFile.delete();
					outZipFile = new ZipFile(outFile);
					for (File file : destPath.listFiles()) {
						if(file.isDirectory()){
							outZipFile.addFolder(file, parameters);
						}else{
							outZipFile.addFile(file, parameters);
						}
					}
					try {
						FileUtils.writeLines(new File(outputDirectory,jarFileName), logInfo);
					} catch (IOException e) {
						log.warn("Error write proguard_jars.txt",e);
					}
				}
			} catch (ZipException e) {
				throw new MojoExecutionException("Error proGuardFileName JAR", e);
			}
		}
	}
	
	/**
	 * 执行清除工作
	 * @throws MojoFailureException 
	 */
	public void clear() throws MojoFailureException{
		if(sameArtifact){
			deleteFileOrDirectory(inFile);
		}
	}
	
	/**
	 * 执行混淆
	 */
	protected void executeProGuard(){
		log.info("-------------ProGuard Config----------------");
		String cmd = StringUtils.join(args,System.getProperty("line.separator"));
		System.out.println(cmd);
		
		if (args.size() == 0) {
			System.out.println(ProGuard.VERSION);
			System.out.println("Usage: java proguard.ProGuard [options ...]");
			System.exit(1);
		}
		// Create the default options.
		Configuration configuration = new Configuration();
		try {
			// Parse the options specified in the command line arguments.
			ConfigurationParser parser = new ConfigurationParser(args.toArray(new String[args.size()]),
					System.getProperties());
			try {
				parser.parse(configuration);
			} finally {
				parser.close();
			}

			// Execute ProGuard with these options.
			new ProGuard(configuration).execute();
		} catch (Exception ex) {
			if (configuration.verbose) {
				// Print a verbose stack trace.
				ex.printStackTrace();
			} else {
				// Print just the stack trace message.
				System.err.println("Error: " + ex.getMessage());
			}
			System.exit(1);
		}
		
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
		sb.append(fileToString(inFile));
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
		sb.append(fileToString(outFile));
		if(StringUtils.isNotBlank(outFilter)){
			sb.append("(").append(outFilter).append(")");
		}
		args.add(sb.toString());
	}
	
	/**
	 * 是否是war文件
	 * @return true|false
	 */
	protected boolean mainIsWar(){
		return mavenProject.getPackaging().equals("war");
	}
	
	/**
	 * 如果输入的文件是一个war包,对其进行改造
	 * @throws MojoExecutionException 
	 */
	protected void modifyWarStructure() throws MojoExecutionException{
		if (mainIsWar()) {
			File warDir = new File(outputDirectory,finalName);
			File webInfDir = new File(warDir,WEB_INF);
			File classesDir = new File(webInfDir,"classes");
			if(classesDir.list().length>0){
				modifyWar = true;
				File jarFile = new File(outputDirectory,
						mavenProject.getArtifactId() + "-" + mavenProject.getVersion() + ".jar");
				if (!jarFile.exists()) {
					try {
						JarArchiver jarArchiver = new JarArchiver();
						jarArchiver.addDirectory(classesDir);
						jarArchiver.setDestFile(jarFile);
						jarArchiver.createArchive();
					} catch (Exception e) {
						throw new MojoExecutionException("Error assembling JAR", e);
					}
				}
				try {
					ZipFile zipFile = new ZipFile(inFile);
					List<FileHeader> removeList = new ArrayList<FileHeader>();
					for (Object item : zipFile.getFileHeaders()) {
						FileHeader fileHeader = (FileHeader) item;
						if(fileHeader.getFileName().startsWith("WEB-INF/classes")){
							removeList.add(fileHeader);
						}
					}
					for (FileHeader fileHeader : removeList) {
						zipFile.removeFile(fileHeader);
					}
				} catch (ZipException e) {
					throw new MojoExecutionException("Error remove WAR classes", e);
				}
				args.add("-injars " + fileToString(jarFile));
				args.add("-outjars " + fileToString(proguardJarFile));
			}
		}
	}
	
	/**
	 * 如果输入的文件是一个war包,对原始war其进行恢复，混淆的war进行完善
	 * @throws MojoExecutionException 
	 */
	protected void restoreWarStructure() throws MojoExecutionException{
		if (mainIsWar()) {
			try {
				ZipParameters parameters = new ZipParameters();
				parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
				parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_FASTEST);
				if(!sameArtifact){
					ZipFile zipFile = new ZipFile(inFile);
					File warDirectory = new File(outputDirectory,finalName);
					File webInfDirectory = new File(warDirectory,WEB_INF);
					zipFile.addFolder(webInfDirectory, parameters);
				}else{
					inFile.delete();
				}
				if(modifyWar){
					ZipFile zipFile = new ZipFile(proguardJarFile);
					File proguardClassesDir = new File(outputDirectory,"proguard-classes");
					File webInfDir=new File(proguardClassesDir,WEB_INF);
					File destPath = new File(webInfDir,"classes");
					destPath.mkdirs();
					for (Object item : zipFile.getFileHeaders()) {
						FileHeader fileHeader = (FileHeader)item;
						if(!fileHeader.getFileName().startsWith("META-INF")){
							zipFile.extractFile(fileHeader, destPath.getAbsolutePath());
						}
					}
					proguardJarFile.delete();
					ZipFile outZipFile = new ZipFile(outFile);
					outZipFile.addFolder(webInfDir, parameters);
				}
			} catch (ZipException e) {
				throw new MojoExecutionException("Error restore WAR classes", e);
			}
		}
	}
	
	/**
	 * 修改指定文件夹里面所有的jar,使用uuid命名
	 * @param file 文件目录
	 * @param logInfo 日志信息
	 */
	protected void proGuardLib(File file,List<String> logInfo){
		if(file.isDirectory()){
			for (File childFile : file.listFiles()) {
				proGuardLib(childFile,logInfo);
			}
		}else{
			String fileName = file.getName();
			if(fileName.endsWith(".jar") && !isExclusion(fileName)){
				String newName = uuid()+".jar";
				File dest = new File(file.getParent(),newName);
				file.renameTo(dest);
				logInfo.add(fileName + " -> " + dest);
			}
		}
	}
	
	protected String uuid(){
		return UUID.randomUUID().toString().replaceAll("-", "");
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
	 * 检查输入输出环境是否满足条件
	 * @throws MojoFailureException
	 */
	protected void checkInOut() throws MojoFailureException{
		log.info("---------inFile:" + injar);
		if(StringUtils.isBlank(injar)){
			injar = finalName + "." + mavenProject.getPackaging();
		}
		inFile = new File(outputDirectory, injar);
		if (!inFile.exists()) {
			throw new MojoFailureException("Can't find file " + inFile);
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
			outFile = (new File(outputDirectory, outjar)).getAbsoluteFile();
			if (outFile.exists()) {
				if (!deleteFileOrDirectory(outFile)) {
					throw new MojoFailureException("Can't delete " + outFile);
				}
			}
		} else {
			sameArtifact = true;
			outFile = inFile.getAbsoluteFile();
			File baseFile;
			if (inFile.isDirectory()) {
				baseFile = new File(outputDirectory, nameNoType(injar) + "_proguard_base");
			} else {
				baseFile = new File(outputDirectory, nameNoType(injar) + "_proguard_base." + FilenameUtils.getExtension(injar));
			}
			if (baseFile.exists()) {
				if (!deleteFileOrDirectory(baseFile)) {
					throw new MojoFailureException("Can't delete " + baseFile);
				}
			}
			if (inFile.exists()) {
				if (!inFile.renameTo(baseFile)) {
					throw new MojoFailureException("Can't rename " + inFile);
				}
			}
			inFile = baseFile;
		}
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
