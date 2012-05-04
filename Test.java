/*
Copyright (c) 2012, Yahoo! Inc.  All rights reserved.

Redistribution and use of this software in source and binary forms, 
with or without modification, are permitted provided that the following 
conditions are met:

* Redistributions of source code must retain the above
  copyright notice, this list of conditions and the
  following disclaimer.

* Redistributions in binary form must reproduce the above
  copyright notice, this list of conditions and the
  following disclaimer in the documentation and/or other
  materials provided with the distribution.

* Neither the name of Yahoo! Inc. nor the names of its
  contributors may be used to endorse or promote products
  derived from this software without specific prior
  written permission of Yahoo! Inc.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS 
IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import java.io.*;
import java.util.*;

class Test
{
	/////////////////////////////////////////////////////////////////////
	public static void main(String[] args)
		throws IOException, Throwable
	{
		if (args.length < 1)
		{
			//testATP();		// AnyTokenPattern
			//testTP();			// TokenPattern
			//testNTP();		// NotTokenPattern
			//testAP();			// AlternatePattern
			//testGRP();		// GreedyRepeatPattern
			//testNGRP();		// NonGreedyRepeatPattern
			//testLP();			// ListPattern
			//testCP();			// CapturePattern
			//testRP();			// ReferencePattern
			//testBMP();			// BraceMatchPattern
			
			testParser();
		}
		
		for (int i = 0; i < args.length; i++)
			HandleFile(args[i]);
	}
	
	/////////////////////////////////////////////////////////////////////
	public static void HandleFile(String name)
		throws IOException, Throwable
	{
		File f = new File(name);
		if (f.isDirectory())
		{
			String[] files = f.list();
			if (files != null)
			{
				for (int i = 0; i < files.length; i++)
					HandleFile(name + "/" + files[i]);
			}
		}
		else if (name.endsWith(".js"))
		{
			ProcessFile(name);
		}
	}
	
	/////////////////////////////////////////////////////////////////////
	public static void ProcessFile(String file)
		throws Throwable
	{
		boolean nameShown = false;
		
		char[]	filedata;

		int len = (int)(new File(file)).length();
		filedata = new char[len];
		FileReader fr = new FileReader(file);
		len = fr.read(filedata, 0, len);
		fr.close();

		// The file length (from file.length()) may not match the number
		// of characters read - the file length is in bytes, but the file
		// can use UTF-8 encoding, meaning there can be fewer characters
		// than bytes in the file.  This line re-allocates the filedata
		// array so it will only include file data, with no left over space.
		filedata = (new String(filedata, 0, len)).toCharArray();
		
		Tokenizer tz = new Tokenizer(filedata);
		Token t;
		int lastLine = 0;
		boolean isIncorrect = false;
		
		ArrayList tokens = new ArrayList();
		while ((t = tz.getToken()) != null)
			tokens.add(t);
		
		Token[] tokenArray = new Token[tokens.size()];
		for (int i = 0; i < tokens.size(); i++)
		    tokenArray[i] = (Token)tokens.get(i);

		JsMatcher m = new JsMatcher(tokenArray);

		AnyTokenPattern atp = new AnyTokenPattern(m);

System.out.println("about to match");
		while (m.match(atp))
		{
		    Token first = m.getFirstToken();
		    Token last =  m.getLastToken();
			if (last != null)
			    System.out.println("Found match starting on line " + first.lineNum + ", tokens " + m.firstToken + " to " + m.lastToken + " source: " + new String(filedata, first.offset-1, last.end - first.offset));
			else
				System.out.println("Found empty match on line " + first.lineNum + ", at token " + m.firstToken);
		    //lp.reset();
		}
	}
	
	////////////////////////////////////////////////////////
	public static void testATP()
	  throws Exception
	{
		char[] src = "function a".toCharArray();
		Token[] tokens = getTokens(src);

		JsMatcher m = new JsMatcher(tokens);

		AnyTokenPattern atp = new AnyTokenPattern(m);

		while (m.match(atp))
		{
		    Token first = m.getFirstToken();
		    Token last =  m.getLastToken();
			if (last != null)
			    System.out.println("Found match starting on line " + first.lineNum + ", tokens " + m.firstToken + " to " + m.lastToken + " source: " + new String(src, first.offset-1, last.end - first.offset));
			else
				System.out.println("Found empty match on line " + first.lineNum + ", at token " + m.firstToken);
		    //lp.reset();
		}
	}
	
	////////////////////////////////////////////////////////
	public static void testTP()
	  throws Exception
	{
		Token first, last;

		char[] src = "function a".toCharArray();
		Token[] tokens = getTokens(src);

		JsMatcher m = new JsMatcher(tokens);

		TokenPattern tp = new TokenPattern(m, Token.FUNCTION);

		while (m.match(tp))
		{
		    first = m.getFirstToken();
		    last =  m.getLastToken();
			if (last != null)
			    System.out.println("Found match starting on line " + first.lineNum + ", tokens " + m.firstToken + " to " + m.lastToken + " source: " + new String(src, first.offset-1, last.end - first.offset));
			else
				System.out.println("Found empty match on line " + first.lineNum + ", at token " + m.firstToken);
		    //lp.reset();
		}

		m.reset();
		tp = new TokenPattern(m, Token.NAME, "a");
		while (m.match(tp))
		{
		    first = m.getFirstToken();
		    last =  m.getLastToken();
			if (last != null)
			    System.out.println("Found match starting on line " + first.lineNum + ", tokens " + m.firstToken + " to " + m.lastToken + " source: " + new String(src, first.offset-1, last.end - first.offset));
			else
				System.out.println("Found empty match on line " + first.lineNum + ", at token " + m.firstToken);
		    //lp.reset();
		}

		System.out.println("match complete");
	}
	
	////////////////////////////////////////////////////////
	public static void testNTP()
	  throws Exception
	{
		Token first, last;

		char[] src = "function a b".toCharArray();
		Token[] tokens = getTokens(src);

		JsMatcher m = new JsMatcher(tokens);

		NotTokenPattern ntp = new NotTokenPattern(m);
		ntp.add(new TokenPattern(m, Token.FUNCTION));

		while (m.match(ntp))
		{
		    first = m.getFirstToken();
		    last =  m.getLastToken();
			if (last != null)
			    System.out.println("Found match starting on line " + first.lineNum + ", tokens " + m.firstToken + " to " + m.lastToken + " source: " + new String(src, first.offset-1, last.end - first.offset));
			else
				System.out.println("Found empty match on line " + first.lineNum + ", at token " + m.firstToken);
		    //lp.reset();
		}

		m.reset();
		ntp = new NotTokenPattern(m);
		ntp.add(new TokenPattern(m, Token.NAME, "a"));
		ntp.add(new TokenPattern(m, Token.FUNCTION));
		
		while (m.match(ntp))
		{
		    first = m.getFirstToken();
		    last =  m.getLastToken();
			if (last != null)
			    System.out.println("Found match starting on line " + first.lineNum + ", tokens " + m.firstToken + " to " + m.lastToken + " source: " + new String(src, first.offset-1, last.end - first.offset));
			else
				System.out.println("Found empty match on line " + first.lineNum + ", at token " + m.firstToken);
		    //lp.reset();
		}

		System.out.println("match complete");
	}
	
	////////////////////////////////////////////////////////
	public static void testAP()
	  throws Exception
	{
		Token first, last;
		NotTokenPattern ntp;
		
		char[] src = "function a b".toCharArray();
		Token[] tokens = getTokens(src);

		JsMatcher m = new JsMatcher(tokens);

		AlternatePattern ap = new AlternatePattern(m);
		ap.add(new TokenPattern(m, Token.FUNCTION));
		//ap.add(new TokenPattern(m, Token.NAME, "a"));
		ntp = new NotTokenPattern(m);
		ntp.add(new TokenPattern(m, Token.NAME, "a"));
		ap.add(ntp);

		while (m.match(ap))
		{
		    first = m.getFirstToken();
		    last =  m.getLastToken();
			if (last != null)
			    System.out.println("Found match starting on line " + first.lineNum + ", tokens " + m.firstToken + " to " + m.lastToken + " source: " + new String(src, first.offset-1, last.end - first.offset));
			else
				System.out.println("Found empty match on line " + first.lineNum + ", at token " + m.firstToken);
		    //lp.reset();
		}

		m.reset();

		ap = new AlternatePattern(m);
		ap.add(new TokenPattern(m, Token.FUNCTION));

		ntp = new NotTokenPattern(m);
		ntp.add(new TokenPattern(m, Token.FUNCTION));

		ap.add(ntp);

		while (m.match(ap))
		{
		    first = m.getFirstToken();
		    last =  m.getLastToken();
		    if (last != null)
		        System.out.println("Found match starting on line " + first.lineNum + ", tokens " + m.firstToken + " to " + m.lastToken + " source: " + new String(src, first.offset-1, last.end - first.offset));
		    else
		        System.out.println("Found empty match on line " + first.lineNum + ", at token " + m.firstToken);
		    //lp.reset();
		}

		System.out.println("match complete");
	}
	
	////////////////////////////////////////////////////////
	public static void testGRP()
	  throws Exception
	{
		Token first, last;
		NotTokenPattern ntp;
		
		char[] src = "function a function function".toCharArray();
		Token[] tokens = getTokens(src);

		JsMatcher m = new JsMatcher(tokens);

		TokenPattern tp = new TokenPattern(m, Token.FUNCTION);
		GreedyRepeatPattern grp = new GreedyRepeatPattern(m, tp, 0, 3);
		
		while (m.match(grp))
		{
		    first = m.getFirstToken();
		    last =  m.getLastToken();
			if (last != null)
			    System.out.println("Found match starting on line " + first.lineNum + ", tokens " + m.firstToken + " to " + m.lastToken + " source: " + new String(src, first.offset-1, last.end - first.offset));
			else
				System.out.println("Found empty match on line " + first.lineNum + ", at token " + m.firstToken);
		    //lp.reset();
		}
	}

	////////////////////////////////////////////////////////
	public static void testNGRP()
	  throws Exception
	{
		Token first, last;
		NotTokenPattern ntp;
		
		char[] src = "function a function function".toCharArray();
		Token[] tokens = getTokens(src);

		JsMatcher m = new JsMatcher(tokens);

		TokenPattern tp = new TokenPattern(m, Token.FUNCTION);
		NonGreedyRepeatPattern ngrp = new NonGreedyRepeatPattern(m, tp, 0, 3);
		
		while (m.match(ngrp))
		{
		    first = m.getFirstToken();
		    last =  m.getLastToken();
			if (last != null)
			    System.out.println("Found match starting on line " + first.lineNum + ", tokens " + m.firstToken + " to " + m.lastToken + " source: " + new String(src, first.offset-1, last.end - first.offset));
			else
				System.out.println("Found empty match on line " + first.lineNum + ", at token " + m.firstToken);
		    //lp.reset();
		}
	}

	////////////////////////////////////////////////////////
	public static void testLP()
	  throws Exception
	{
		Token first, last;
		NotTokenPattern ntp;
		
		char[] src = "function a function function".toCharArray();
		Token[] tokens = getTokens(src);

		JsMatcher m = new JsMatcher(tokens);

		TokenPattern tp = new TokenPattern(m, Token.FUNCTION);
		ListPattern lp = new ListPattern(m);
		lp.add(tp);
		lp.add(new AnyTokenPattern(m));
		
		while (m.match(lp))
		{
		    first = m.getFirstToken();
		    last =  m.getLastToken();
			if (last != null)
			    System.out.println("Found match starting on line " + first.lineNum + ", tokens " + m.firstToken + " to " + m.lastToken + " source: " + new String(src, first.offset-1, last.end - first.offset));
			else
				System.out.println("Found empty match on line " + first.lineNum + ", at token " + m.firstToken);
		    //lp.reset();
		}
	}

	////////////////////////////////////////////////////////
	public static void testCP()
	  throws Exception
	{
		Token first, last;
		
		char[] src = "function a function function".toCharArray();
		Token[] tokens = getTokens(src);

		JsMatcher m = new JsMatcher(tokens);

		TokenPattern tp = new TokenPattern(m, Token.FUNCTION);
		ListPattern lp = new ListPattern(m);
		lp.add(tp);
		lp.add(new AnyTokenPattern(m));

		CapturePattern cp = new CapturePattern(m, lp);
				
		while (m.match(cp))
		{
		    first = m.getFirstToken();
		    last =  m.getLastToken();
			if (last != null)
			    System.out.println("Found match starting on line " + first.lineNum + ", tokens " + m.firstToken + " to " + m.lastToken + " source: " + new String(src, first.offset-1, last.end - first.offset));
			else
				System.out.println("Found empty match on line " + first.lineNum + ", at token " + m.firstToken);
		    //lp.reset();
		}
	}

	////////////////////////////////////////////////////////
	public static void testRP()
	  throws Exception
	{
		Token first, last;
		
		char[] src = "function b function function b".toCharArray();
		Token[] tokens = getTokens(src);

		JsMatcher m = new JsMatcher(tokens);

		TokenPattern tp = new TokenPattern(m, Token.FUNCTION);
		ListPattern lp = new ListPattern(m);
		lp.add(tp);
		lp.add(new AnyTokenPattern(m));

		CapturePattern cp = new CapturePattern(m, lp);
		
		ReferencePattern rp = new ReferencePattern(m, cp);
	
		lp = new ListPattern(m);
		lp.add(cp);
		lp.add(new AnyTokenPattern(m));
		lp.add(rp);
				
		while (m.match(lp))
		{
		    first = m.getFirstToken();
		    last =  m.getLastToken();
			if (last != null)
			    System.out.println("Found match starting on line " + first.lineNum + ", tokens " + m.firstToken + " to " + m.lastToken + " source: " + new String(src, first.offset-1, last.end - first.offset));
			else
				System.out.println("Found empty match on line " + first.lineNum + ", at token " + m.firstToken);
		    //lp.reset();
		}
	}

	////////////////////////////////////////////////////////
	public static void testBMP()
	  throws Exception
	{
		Token first, last;
		
		char[] src = "{ member1: a(), member2: function() { }, member3: 0 }".toCharArray();
		Token[] tokens = getTokens(src);

		JsMatcher m = new JsMatcher(tokens);

		TokenPattern tp = new TokenPattern(m, Token.LC);
		CapturePattern cp = new CapturePattern(m, tp);
		
		AnyTokenPattern ap = new AnyTokenPattern(m);
		NonGreedyRepeatPattern ngrp = new NonGreedyRepeatPattern(m, ap, 0, 1000);
		
		ClosePattern bmp = new ClosePattern(m, cp, true);
		
		ListPattern lp = new ListPattern(m);
		lp.add(cp);
		lp.add(ngrp);
		lp.add(bmp);
				
		while (m.match(lp))
		{
		    first = m.getFirstToken();
		    last =  m.getLastToken();
			if (last != null)
			    System.out.println("Found match starting on line " + first.lineNum + ", tokens " + m.firstToken + " to " + m.lastToken + " source: " + new String(src, first.offset-1, last.end - first.offset));
			else
				System.out.println("Found empty match on line " + first.lineNum + ", at token " + m.firstToken);
		    //lp.reset();
		}
	}

	////////////////////////////////////////////////////////
	public static void testParser()
	  throws Exception
	{
		Token first, last;
		String regex = "";

		char[] src = "{ member1: a(), member2: function() { }, member3: 0 }".toCharArray();
		Token[] tokens = getTokens(src);

		JsMatcher m = new JsMatcher(tokens);

		JsPattern p = Maker.makePattern(m, "member1 | member2");
	
		p.dump(0);
			
		while (m.match(p))
		{
//			p.dump(0);
//			System.out.println();
			first = m.getFirstToken();
			last =  m.getLastToken();
			if (last != null)
			    System.out.println("Found match starting on line " + first.lineNum + ", tokens " + m.firstToken + " to " + m.lastToken + " source: " + new String(src, first.offset-1, last.end - first.offset));
			else
			    System.out.println("Found empty match on line " + first.lineNum + ", at token " + m.firstToken);
			//lp.reset();
		}
	}
	
	////////////////////////////////////////////////////////
	public static Token[] getTokens(char[] src)
	{
		Tokenizer tz = new Tokenizer(src);

		Token t;
		
		ArrayList tokens = new ArrayList();
		while ((t = tz.getToken()) != null)
			tokens.add(t);
		
		Token[] tokenArray = new Token[tokens.size()];
		for (int i = 0; i < tokens.size(); i++)
		    tokenArray[i] = (Token)tokens.get(i);

		return tokenArray;
	}
}
