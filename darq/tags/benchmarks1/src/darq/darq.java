/*
 * (c) Copyright 2004, 2005, 2006 Hewlett-Packard Development Company, LP
 * [See end of file]
 */


package darq;

import arq.cmd.CmdUtils;

/** A program to execute queries from the command line in ARQ mode.
  *
 * @author  Andy Seaborne
 * @version $Id: arq.java,v 1.8 2006/06/07 16:53:21 andy_seaborne Exp $
 */

public class darq
{
    static { CmdUtils.setLog4j() ; CmdUtils.setN3Params() ; }

    //static protected Log logger = LogFactory.getLog( arq.class );

    public static void main (String [] argv)
    {
        query.main(argv);
    }
        
}