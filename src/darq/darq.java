/*
 * (c) Copyright 2004, 2005, 2006 Hewlett-Packard Development Company, LP
 * [See end of file]
 */


package darq;

import darq.query;
import arq.cmd.CmdException;
import arq.cmd.CmdUtils;

import arq.cmd.TerminationException;

import java.util.* ;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

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
        try { main2(argv) ; }
        catch (CmdException ex)
        {
            if ( ex.getCause() != null )
                System.err.println(ex.getCause().getMessage()) ;
            else
                System.err.println(ex.getMessage()) ;
        }
   //     catch (TerminationException ex) { System.exit(ex.getCode()) ; }

    }
        
    public static void main2(String [] argv)
    {
        List a = new ArrayList() ;
        for ( int i = 0 ; i < argv.length ; i++ ) a.add(argv[i]) ;
        a.add(0, "--syntax=arq") ;
        argv = (String[])a.toArray(argv) ;
        query.main2(argv) ;
    }
 }

/*
 *  (c) Copyright 2004, 2005, 2006 Hewlett-Packard Development Company, LP
 *  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
