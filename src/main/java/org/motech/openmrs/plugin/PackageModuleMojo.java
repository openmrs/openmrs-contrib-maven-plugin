package org.motech.openmrs.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Goal to package an OpenMRS module.
 * 
 * @goal package-module
 * @phase package
 * @requiresDependencyResolution runtime
 */
public class PackageModuleMojo
    extends AbstractMojo
{

    public static String PACKAGING = "omod";

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The configured archiver to use when constructing the omod file.
     * 
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * Location of the file.
     * 
     * @parameter default-value="${project.build.directory}"
     * @required
     * @readonly
     */
    private File outputDirectory;

    /**
     * Location of built classes and filtered resources.
     * 
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File classesDirectory;

    /**
     * The name of the generated module.
     * 
     * @parameter default-value="${project.build.finalName}"
     * @required
     * @readonly
     */
    private String omodName;

    /**
     * The directory where the omod is built.
     * 
     * @parameter default-value="${project.build.directory}/${project.build.finalName}"
     * @required
     */
    private File omodDirectory;

    /**
     * Classifier to add to the generated omod file. If given, the artifact will be an attachment rather than the main
     * artifact.
     * 
     * @parameter
     */
    private String classifier;

    /**
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="omod"
     */
    private JarArchiver omodArchiver;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * Whether this is the main artifact of the project.
     * 
     * @parameter default-value="true"
     */
    private boolean primaryArtifact;

    /**
     * Whether to fail the build if the contents of the module don't appear to comprise a valid OpenMRS module.
     * 
     * @parameter default-value="true"
     */
    private boolean validateFormat;

    /**
     * Whether to bundle the dependencies into the omod archive. This is useful if your module needs libraries that
     * aren't included, but don't conflict with the OpenMRS installation your module is developed for.
     * 
     * @parameter default-value="true";
     */
    private boolean bundleDependencies;

    /**
     * The sub-directory within the built omod archive where the bundled dependencies are to be placed. It is local to
     * the root of the archive.
     * 
     * @parameter default-value="lib"
     */
    private String libDir;

    /**
     * The path, relative to the root of an OpenMRS module, to the configuration file. This is used to validate the
     * built archive if the validateFormat option is set.
     * 
     * @parameter default-value="config.xml"
     */
    private String configFilePath;

    public void execute()
        throws MojoExecutionException
    {

        File omodFile = getTargetOmodFile();

        try
        {
            performPackaging( omodFile );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error assembling module: " + e.getMessage(), e );
        }
    }

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public File getClassesDirectory()
    {
        return classesDirectory;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public File getOmodDirectory()
    {
        return omodDirectory;
    }

    public String getOmodName()
    {
        return omodName;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public String getLibDir()
    {
        return libDir;
    }

    public boolean isValidateFormat()
    {
        return validateFormat;
    }

    public String getConfigFilePath()
    {
        return configFilePath;
    }

    public MavenArchiveConfiguration getArchive()
    {
        return archive;
    }

    /**
     * Get the name of the target file.
     * 
     * @return
     */
    protected File getTargetOmodFile()
    {

        String classifier = getClassifier();

        if ( classifier == null )
        {
            classifier = "";
        }
        else if ( classifier.trim().length() > 0 && !classifier.startsWith( "-" ) )
        {
            classifier = "-" + classifier;
        }

        return new File( getOutputDirectory(), getOmodName() + classifier + "." + PACKAGING );
    }

    /**
     * Package the project contents into the specified file.
     * 
     * @param omodFile
     * @throws ArchiverException
     * @throws ManifestException
     * @throws IOException
     * @throws DependencyResolutionRequiredException
     * @throws InvalidOmodException
     */
    protected void performPackaging( File omodFile )
        throws ArchiverException, ManifestException, IOException, DependencyResolutionRequiredException,
        InvalidOmodException
    {

        getLog().info( "Packaging OpenMRS module" );

        buildOmod( getProject(), getOmodDirectory() );

        if ( isValidateFormat() )
        {
            validateOmod( getOmodDirectory() );
        }

        omodArchiver.addDirectory( getOmodDirectory() );

        MavenArchiver archiver = new MavenArchiver();
        archiver.setArchiver( omodArchiver );
        archiver.setOutputFile( omodFile );

        getLog().debug( "creating archive" );
        archiver.createArchive( getProject(), getArchive() );

        getLog().debug( "attaching artifact(s)" );
        if ( getClassifier() != null )
        {
            getLog().debug( "attaching as secondary artifact: " + getClassifier() );
            projectHelper.attachArtifact( getProject(), PACKAGING, getClassifier(), omodFile );
        }
        else
        {
            Artifact artifact = getProject().getArtifact();
            if ( primaryArtifact || artifact.getFile() == null || artifact.getFile().isDirectory() )
            {
                getLog().debug( "attaching as primary artifact" );
                artifact.setFile( omodFile );
            }
        }
    }

    /**
     * Package the project contents into an 'exploded' directory.
     * 
     * @param project
     * @param omodDirectory
     * @throws IOException
     */
    protected void buildOmod( MavenProject project, File omodDirectory )
        throws IOException
    {
        getLog().debug( "building omod directory structure at: " + omodDirectory.getAbsolutePath() );

        getLog().debug( "copying classes to omod dir" );
        FileUtils.copyDirectoryStructure( getClassesDirectory(), getOmodDirectory() );

        if ( bundleDependencies )
        {
            File libDir = new File( getOmodDirectory(), getLibDir() );
            getLog().debug( "copying dependencies at: " + getLibDir() );
            ScopeArtifactFilter filter = new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME );
            Iterator artifacts = project.getArtifacts().iterator();
            while ( artifacts.hasNext() )
            {
                Artifact candidate = (Artifact) artifacts.next();
                if ( !candidate.isOptional() && filter.include( candidate ) )
                {
                    getLog().debug( "bundling artifact: " + candidate.getArtifactId() );
                    FileUtils.copyFile( candidate.getFile(), new File( libDir, candidate.getFile().getName() ) );
                }
            }
        }
    }

    /**
     * Validates the contents of the specified omod directory.
     * 
     * @param omodDirectory
     * @throws InvalidOmodException when the module doesn't look right
     */
    protected void validateOmod( File omodDirectory )
        throws InvalidOmodException
    {
        File omodConfig = new File( omodDirectory, getConfigFilePath() );
        if ( !omodConfig.exists() )
        {
            throw new InvalidOmodException( "config file does not exist: " + getConfigFilePath() );
        }
    }
}
