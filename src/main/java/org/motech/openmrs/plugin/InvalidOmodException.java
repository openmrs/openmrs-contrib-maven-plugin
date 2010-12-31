package org.motech.openmrs.plugin;

/**
 * Represents a condition in which an OpenMRS module is invalid.
 * 
 * @author batkinson
 */
public class InvalidOmodException
    extends Exception
{

    private static final long serialVersionUID = 1L;

    public InvalidOmodException()
    {
        super();
    }

    public InvalidOmodException( String message )
    {
        super( message );
    }

    public InvalidOmodException( Throwable cause )
    {
        super( cause );
    }

    public InvalidOmodException( String message, Throwable cause )
    {
        super( message, cause );
    }

}
