package com.oracle.coherence.common.util;

/**
 * <p>A {@link DefaultMockInterfaceImplementation} of the {@link com.oracle.coherence.common.util.MockInterface}.
 * Used for unit testing of {@link ObjectProxyFactory}. </p>
 *
 * @author Christer Fahlgren
 */
public class DefaultMockInterfaceImplementation implements com.oracle.coherence.common.util.MockInterface
{

    /**
     * A string containing a message.
     */
    private String message;


    /**
     * Default constructor.
     */
    public DefaultMockInterfaceImplementation()
    {
    }


    /**
     * Standard constructor.
     * 
     * @param message the message to set
     */
    public DefaultMockInterfaceImplementation(String message)
    {
        this.message = message;
    }


    /**
     * {@inheritDoc}
     */
    public String getMessage()
    {
        return message;
    }


    /**
     * {@inheritDoc}
     */
    public void setMessage(String message)
    {
        this.message = message;
    }
}
