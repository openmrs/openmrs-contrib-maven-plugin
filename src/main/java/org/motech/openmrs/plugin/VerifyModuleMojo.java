package org.motech.openmrs.plugin;

import java.io.File;
import java.util.StringTokenizer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

/**
 * Goal to verify that the OpenMRS module looks sane.
 * 
 * @goal verify-module
 * @phase verify
 * @requiresDependencyResolution runtime
 */
public class VerifyModuleMojo
    extends AbstractMojo
{

    /**
     * @parameter default-value="true"
     */
    private boolean verifyOmod;

    /**
     * The path in the final built module to the OpenMRS config file.
     * 
     * @parameter default-value="config.xml"
     * @required
     * @readonly
     */
    String configFilePath;

    /**
     * The path in the final built module to the OpenMRS module web files.
     * 
     * @parameter default-value="web/module"
     * @required
     * @readonly
     */
    private String webappTarget;

    /**
     * The directory where the omod is built.
     * 
     * @parameter default-value="${project.build.directory}/${project.build.finalName}"
     * @required
     */
    private File omodDirectory;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( verifyOmod )
        {
            getLog().debug( "verifying module" );

            getLog().debug( "verifying omod directory exists" );
            if ( !omodDirectory.exists() )
                throw new MojoFailureException( "omod directory " + omodDirectory + " not found" );

            getLog().debug( "verifying config file exists" );
            File configFile = new File( omodDirectory, configFilePath );
            if ( !configFile.exists() )
                throw new MojoFailureException( "omod config doesn't exist" );

            getLog().debug( "checking that files in config exist" );

            getLog().debug( "creating dom from config file" );
            XmlStreamReader configReader;
            Xpp3Dom configDom;
            try
            {
                configReader = new XmlStreamReader( configFile );
                configDom = Xpp3DomBuilder.build( configReader, true );
            }
            catch ( Exception ioe )
            {
                throw new MojoExecutionException( "failed to parse config", ioe );
            }

            getLog().debug( "retrieving required OpenMRS version" );
            Xpp3Dom versionDom = configDom.getChild( "require_version" );
            String requiredVersion = null;
            if ( versionDom != null )
                requiredVersion = versionDom.getValue();

            getLog().debug( "checking activator exists" );
            Xpp3Dom activatorDom = configDom.getChild( "activator" );
            if ( activatorDom != null )
            {
                String activatorFileName = classToFilename( activatorDom.getValue() );
                if ( !existsInOmod( activatorFileName ) )
                    throw new MojoFailureException( "activator " + activatorFileName + " not in omod" );
            }

            getLog().debug( "checking extensions exist" );
            Xpp3Dom[] extDoms = configDom.getChildren( "extension" );
            for ( int e = 0; e < extDoms.length; e++ )
            {
                String extensionFileName = classToFilename( extDoms[e].getChild( "class" ).getValue() );
                if ( !existsInOmod( extensionFileName ) )
                    throw new MojoFailureException( "extension " + extensionFileName + " not in omod" );
            }

            getLog().debug( "checking advice exist" );
            Xpp3Dom[] adviceDoms = configDom.getChildren( "advice" );
            for ( int a = 0; a < adviceDoms.length; a++ )
            {
                String adviceFileName = classToFilename( adviceDoms[a].getChild( "class" ).getValue() );
                if ( !existsInOmod( adviceFileName ) )
                    throw new MojoFailureException( "advice " + adviceFileName + " not in omod" );
            }

            getLog().debug( "checking servlets exist" );
            Xpp3Dom[] servletDoms = configDom.getChildren( "servlet" );
            for ( int s = 0; s < servletDoms.length; s++ )
            {
                String servletFileName = classToFilename( servletDoms[s].getChild( "class" ).getValue() );
                if ( !existsInOmod( servletFileName ) )
                    throw new MojoFailureException( "servlet " + servletFileName + " not in omod" );
            }

            getLog().debug( "checking messages exist" );
            Xpp3Dom[] msgDoms = configDom.getChildren( "messages" );
            for ( int m = 0; m < msgDoms.length; m++ )
            {
                String messageFileName = msgDoms[m].getChild( "file" ).getValue();
                if ( !existsInOmod( messageFileName ) )
                    throw new MojoFailureException( "message file " + messageFileName + " not in omod" );
            }

            getLog().debug( "checking mappingFiles exist" );
            Xpp3Dom[] mapDoms = configDom.getChildren( "mappingFiles" );
            for ( int m = 0; m < mapDoms.length; m++ )
            {
                String mappingFileNames = mapDoms[m].getValue();
                StringTokenizer mapTok = new StringTokenizer( mappingFileNames, " \n" );
                while ( mapTok.hasMoreTokens() )
                {
                    String mappingFile = mapTok.nextToken();
                    if ( !existsInOmod( mappingFile ) )
                        throw new MojoFailureException( "mapping file " + mappingFile + " not in omod" );
                }
            }

            File webDir = new File( omodDirectory, webappTarget );
            if ( webDir.exists() && ( requiredVersion == null || "1.5.0".compareTo( requiredVersion ) > 0 ) )
            {
                getLog().debug( "verifying all module web dirs exist (required for versions < 1.5.0)" );

                getLog().debug( "verifying portlets dir exists" );
                File portletDir = new File( webDir, "portlets" );
                if ( !portletDir.exists() )
                    throw new MojoFailureException( "omod portlets dir doesn't exist" );

                getLog().debug( "verifying resource dir exists" );
                File webResourceDir = new File( webDir, "resources" );
                if ( !webResourceDir.exists() )
                    throw new MojoFailureException( "omod web resources dir doesn't exist" );
            }
        }
    }

    private String classToFilename( String className )
    {
        return className.replace( '.', '/' ) + ".class";
    }

    private boolean existsInOmod( String fileName )
    {
        getLog().debug( "checking for " + fileName + " in " + omodDirectory );
        return new File( omodDirectory, fileName ).exists();
    }
}
