package com.hp.hpl.jena.query.darq.util;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Node_Literal;
import com.hp.hpl.jena.query.core.ARQInternalErrorException;
import com.hp.hpl.jena.query.core.Var;
import com.hp.hpl.jena.query.serializer.SerializationContext;
import com.hp.hpl.jena.query.util.FmtUtils;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.XSD;

public class DarqFmtUtils  {
	
	 static boolean applyUnicodeEscapes = false ;
	
	  public static String stringForNode(Node n, SerializationContext context)
	    {
	        if ( n == null )
	            return "<<null>>" ;
	        
	        if ( n.isBlank() )
	        {
	            if ( context == null )
	                return "[bNode]" ; 
	            if ( context.getBNodeMap() == null )
	                return "[bNode]" ;
	            // The BNodeMap controlls whether this becomes a told bnode or not
	            return context.getBNodeMap().asString(n) ;
	        }
	        
	        if ( n.isLiteral() )
	            return stringForLiteral((Node_Literal)n, context) ;

	        if ( n.isURI() )
	        {
	            String uri = n.getURI() ;
	            return stringForURI(uri, context) ;
	        }
	        if ( n.isVariable() )
	        {
	            if ( ! Var.isBlankNodeVar(n))
	                return "?"+n.getName() ;
	            if ( context == null )
	                return "_:"+n.getName() ;   // Wild guess
	            else
	                return context.getBNodeMap().asString(n) ;
	            
	        }

	        return n.toString() ;
	    }
	
	private static boolean checkValidLocalname(String localname)
    {
		//System.out.println("Localname: "+localname);
        if ( localname.length() == 0 )
            return false ;
        
        for ( int idx = 0 ; idx < localname.length() ; idx++ )
        {
            char ch = localname.charAt(idx) ;
            if ( ! validPNameChar(ch) )
                return false ;
        }
        
        // Test start and end - at least one character in the name.
        
        if ( localname.endsWith(".") )
            return false ;
        if ( localname.startsWith(".") )
            return false ;
        if ( localname.indexOf("-")>=0) return false;
        if ( localname.indexOf("-")>=0) return false;
        if (localname.indexOf(':') >= 0) return false;
        if (localname.indexOf('@') >= 0) return false;
        if (localname.indexOf('%') >= 0) return false;
        
        
        return true ;
    }
    
    private static boolean validPNameChar(char ch)
    {
        if ( Character.isLetterOrDigit(ch) ) return true ;
        
        
 /*       if ( ch == '.' )    return true ;
        if ( ch == '-' )    return true ;
        if ( ch == '_' )    return true ; */
        return false ;
    }
    
    private static boolean checkValidPrefixName(String prefixedName, String uri)
    {
        if ( ! isShort(prefixedName, uri) )
            return false ;
        
        // Split it to get the parts.
        int i = prefixedName.indexOf(':') ;
        if ( i < 0 )
            throw new ARQInternalErrorException("Broken short form -- "+prefixedName) ;
        String p = prefixedName.substring(0,i) ;
        String x = prefixedName.substring(i+1) ; 
        // Check legality
        if ( checkValidPrefix(p) && checkValidLocalname(x) )
            return true ;
        return false ;
    }
    
    private static boolean isShort(String prefixedName, String uri)
    { return prefixedName != null && ! prefixedName.equals(uri) ; }
    
    private static boolean checkValidPrefix(String prefixStr)
    {
        if ( prefixStr.startsWith("_"))
            return false ;
        return checkValidLocalname(prefixStr) ;
    }
    
    static public String stringForURI(String uri, SerializationContext context)
    {
        if ( context == null )
            return stringForURI(uri, (PrefixMapping)null) ;
        return stringForURI(uri, context.getPrefixMapping()) ;
    }
    
    static public String stringForURI(String uri, PrefixMapping mapping)
    {
        if ( mapping != null )
        {
            // qnameFor is more aggressive about spliting only at a non NCName char
            // Problems with "ns." , namespace of ".../ab" etc.
            // TODO Iterate to find "best" fit.
            
            // Try one way
            String tmp = mapping.shortForm(uri) ;
            if ( checkValidPrefixName(tmp, uri) )
                return tmp ;
            // No - try a different way
            tmp = mapping.qnameFor(uri) ;
            if ( checkValidPrefixName(tmp, uri) )
                return tmp ;
            // No match - fall through
        }
        
        return "<"+stringEsc(uri)+">" ; 
    }
    
    public static String stringForLiteral(Node_Literal literal, SerializationContext context)
    {
        String datatype = literal.getLiteralDatatypeURI() ;
        String lang = literal.getLiteralLanguage() ;
        String s = literal.getLiteralLexicalForm() ;
        
        if ( datatype != null )
        {
            // Special form we know how to handle?
            // Assume valid text
            if ( datatype.equals(XSD.integer.getURI()) )
            {
                try {
                    String s1 = s ;
                    // BigInteger does not allow leading +
                    // so chop it off before the format test
                    // BigDecimal does allow a leading +
                    if ( s.startsWith("+") )
                        s1 = s.substring(1) ;
                    new java.math.BigInteger(s1) ;
                    return s ;
                } catch (NumberFormatException nfe) {}
                // No luck.  Continue.
                // Continuing is always safe.
            }
            
            if ( datatype.equals(XSD.decimal.getURI()) )
            {
                if ( s.indexOf('.') > 0 )
                {
                    try {
                        // BigDecimal does allow a leading +
                        new java.math.BigDecimal(s) ;
                        return s ;
                    } catch (NumberFormatException nfe) {}
                    // No luck.  Continue.
                }
            }
            
            if ( datatype.equals(XSD.xdouble.getURI()) )
            {
                // Assumes SPARQL has decimals and doubles.
                // Must have 'e' or 'E' to be a double short form.

                if ( s.indexOf('e') >= 0 || s.indexOf('E') >= 0 )
                {
                    try {
                        Double.parseDouble(s) ;
                        return s ;  // returm the original lexical form. 
                    } catch (NumberFormatException nfe) {}
                    // No luck.  Continue.
                }
            }

            if ( datatype.equals(XSD.xboolean.getURI()) )
            {
                if ( s.equalsIgnoreCase("true") ) return s ;
                if ( s.equalsIgnoreCase("false") ) return s ;
            }
            // Not a recognized form.
        }
        
        StringBuffer sbuff = new StringBuffer() ;
        sbuff.append("\"") ;
        stringEsc(sbuff, s, true) ;
        sbuff.append("\"") ;
        
        // Format the language tag 
        if ( lang != null && lang.length()>0)
        {
            sbuff.append("@") ;
            sbuff.append(lang) ;
        }

        if ( datatype != null )
        {
            sbuff.append("^^") ;
            sbuff.append(stringForURI(datatype, context)) ;
        }
        
        return sbuff.toString() ;
    }
    
    public static String stringEsc(String s)
    { return stringEsc( s, true ) ; }
    
    public static String stringEsc(String s, boolean singleLineString)
    {
        StringBuffer sb = new StringBuffer() ;
        stringEsc(sb, s, singleLineString) ;
        return sb.toString() ;
    }
    
    public static void stringEsc(StringBuffer sbuff, String s)
    { stringEsc( sbuff,  s, true ) ; }

    public static void stringEsc(StringBuffer sbuff, String s, boolean singleLineString)
    {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            // Escape escapes and quotes
            if (c == '\\' || c == '"' )
            {
                sbuff.append('\\') ;
                sbuff.append(c) ;
                continue ;
            }
            
            // Characters to literally output.
            // This would generate 7-bit safe files 
//            if (c >= 32 && c < 127)
//            {
//                sbuff.append(c) ;
//                continue;
//            }    

            // Whitespace
            if ( singleLineString && ( c == '\n' || c == '\r' || c == '\f' ) )
            {
                if (c == '\n') sbuff.append("\\n");
                if (c == '\t') sbuff.append("\\t");
                if (c == '\r') sbuff.append("\\r");
                if (c == '\f') sbuff.append("\\f");
                continue ;
            }
            
            // Output as is (subject to UTF-8 encoding on output that is)
            
            if ( ! applyUnicodeEscapes )
                sbuff.append(c) ;
            else
            {
                // Unicode escapes
                // c < 32, c >= 127, not whitespace or other specials
                if ( c >= 32 && c < 127 )
                {
                    sbuff.append(c) ;
                }
                else
                {
                    String hexstr = Integer.toHexString(c).toUpperCase();
                    int pad = 4 - hexstr.length();
                    sbuff.append("\\u");
                    for (; pad > 0; pad--)
                        sbuff.append("0");
                    sbuff.append(hexstr);
                }
            }
        }
    }
}
