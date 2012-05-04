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

class Match
{
	//public Token first;
	//public Token last;
	
	//public Match(Token f, Token l)
	//{
	//    first = f;
	//    last = l;
	//}
	
	public int first;
	public int last;
	
	public Match(int f, int l)
	{
		first = f;
		last = l;
	}
}

class Maker
{
	/*
	 * Regex symbols:
	 * . : AnyTokenPattern
	 * non-regex symbol: TokenPattern
	 * (! X Y ): NotTokenPattern.
	 * |: AlternatePattern
	 * ?, +, *: GreedyRepeat
	 * ?-, +-, *-: Non backtracking greedy repeat
	 * ??, +?, *?: NonGreedyRepeat
	 * X{n}, X{n,}, X{n,m}: Greedy w/ range
	 * X{n}?, X{n,}?, X{n,m}?: reluctant w/ range
	 * ():  CapturePattern
	 * (?):  List pattern
	 * \#:  ReferencePattern
	 * ^$: beginning of line/end of line
	 * !\#: brace match
	 * F:name : function definition
	 * V:name : variable definition
	 * M:name : Module containing function
	 * R:number : Reference
	 * C:number : Close
	 * I:name   : Invocation
	 * all caps: token (e.g. LPAREN = left parenthesis)
	 * 
 			//testATP();		// AnyTokenPattern
			//testTP();			// TokenPattern
			//testNTP();		// NotTokenPattern
			//testAP();			// AlternatePattern
			//testGRP();		// GreedyRepeatPattern
			//testNGRP();		// NonGreedyRepeatPattern
			//testLP();			// ListPattern
			//testCP();			// CapturePattern
			//testRP();			// ReferencePattern
			testBMP();			// BraceMatchPattern
	 * 
	 * TODO: variable references
	 * function calls
	 * modules used
	 * modules required
	 * 
	 * Things I don't like:
	 *     - () & {} are common JS symbols, and needing to escape them is a pain
	 *     - I can't decide on a good syntax (or name) for close paren/brace/bracket
	 *     - Output control is lacking.  
	 *     - arguments in config file
*/

	public JsMatcher matcher;
	public ArrayList captures = new ArrayList();
	public Hashtable extensions;
	
	public int mode;
	public int captureIndex = 1;
	public boolean insensitive;
	public boolean typeSensitive;

	public JsPattern makePattern(JsMatcher m, String regex, Hashtable extensions, int mode, boolean typeSensitive, boolean insensitive)
		throws Throwable
	{
		this.extensions = extensions;
		this.mode = mode;
		this.insensitive = insensitive;
		this.typeSensitive = typeSensitive;

		JsPattern p, first;
		ListPattern list;

		matcher = m;
		Tokenizer tz = new Tokenizer(regex, true, true);
		first = null;
		list = null;
		
		ArrayList saveCaptures = captures;
//System.out.println("Saving " + captures.size());
//if (captures.size() > 0)
//    System.out.println(captures.get(0));

		captures = new ArrayList();

//System.out.println("makePattern() on regex: " + regex);

		while ((p = readOne(tz, regex)) != null)
		{
			if (first == null)
				first = p;
			else
			{
				if (list == null)
				{
					list = new ListPattern(matcher);
					list.add(first);
				}
				
				list.add(p);

				list.printing |= p.printing;
			}
		}

		captures = saveCaptures;
//System.out.println("Restored " + captures.size());
//if (captures.size() > 0)
//    System.out.println(captures.get(0));

		if (list != null)
			first = list;
//first.dump(0);
		return first;
	}

	////////////////////////////////////////////////////////
	public JsPattern readOne(Tokenizer tz, String regex)
		throws Throwable
	{
		String name;
		Token t;
		
		JsPattern p, np;

		p = null;
		np = null;
		
		t = tz.nextToken();

		if (t == null)
			return null;

//System.out.println("readOne() on regex: " + regex + " at " + regex.substring(t.offset-1));

		switch (t.type)
		{
			case Token.DOT:
				p = new AnyTokenPattern(matcher);
				break;
					
			case Token.LP:
				t = tz.peekToken();

				switch (t.type)
				{
					case Token.HOOK:
						t = tz.nextToken();
						p = new ListPattern(matcher);
						while ((np = readOne(tz, regex)) != null)
							((ListPattern)p).add(np);
						break;

					case Token.NOT:
						t = tz.nextToken();
						p = new NotTokenPattern(matcher, insensitive);
						while ((np = readOne(tz, regex)) != null)
							((NotTokenPattern)p).add((TokenPattern)np);

						break;

					case Token.COLON:
						t = tz.nextToken();
						t = tz.nextToken();
						if (t.type == Token.STRING)
							p = new TokenPattern(matcher, Token.STRING, t.value_, true, insensitive);
						else
						{
							name = "";
							while (t.type != Token.COLON)
							{
								name = name + t.value_;
								t = tz.nextToken();
							}

							p = new TokenPattern(matcher, Token.NAME, name, true, insensitive);
						}
						
						t = tz.nextToken();
						break;
					
					default:
						int i = captures.size();
						captures.add(null);		// reserve space, in case the capture has a capture inside it

						JsPattern next;
						ListPattern list = new ListPattern(matcher);
						p = null;
						
						while (tz.peekToken().type != Token.RP)
						{
							next = readOne(tz, regex);
							list.add(next);
							if (p == null)
								p = next;
							else
								p = list;
						}
							
						p = new CapturePattern(matcher, p, matcher.captures.size());

						captures.set(i, p);

						t = tz.nextToken();
						break;
				}
				break;

			case Token.RP:
				return null;

			case Token.BS:				// backslash
				t = tz.nextToken();
				if (t.type == Token.NUMBER)
					p = new ReferencePattern(matcher, (CapturePattern)captures.get(t.intValue-1));
				else
					p = new TokenPattern(matcher, t.type, t.value_, typeSensitive, insensitive);
				break;

			case Token.NOT:				// brace match.
				t = tz.nextToken();			// TODO: must be Token.BS
				t = tz.nextToken();			// TODO: must be number < captures.size
				p = new ClosePattern(matcher, (CapturePattern)captures.get(t.intValue-1), false);
				break;
			
			case Token.REGEXP:
				p = new RegexpPattern(matcher, t.value_, insensitive);
				break;

			case Token.NAME:			// LPAREN, DOT, LBRACE, etc.
				if (t.value_.compareTo("C") == 0)
				{
					t = tz.peekToken();
					if (t.type == Token.COLON)
					{
						tz.nextToken();
						t = tz.nextToken();

						p = new ClosePattern(matcher, (CapturePattern)captures.get(t.intValue-1), false);
						break;
					}
				}

				Extension extension = null;
				if (extensions != null)
					extension = (Extension)extensions.get(t.value_);

				if (extension != null)
				{
					Token t2 = tz.peekToken();
					if (t2 != null && t2.type == Token.COLON)
						t = tz.nextToken();		// Skip the ':'

					String rx;
					if (t == null)
						rx = "";
					else
						rx = regex.substring(t.end-1);

					// tz is the tokenizer for the current regex
					// I want to match extension.pattern against it
					JsMatcher exMatcher = new JsMatcher(rx.toCharArray(), true, true);

					if (extension.pattern != null)
					{
						JsPattern ep = (new Maker()).makePattern(exMatcher, extension.pattern, extensions, 0, typeSensitive, insensitive);
						if (!exMatcher.match(ep, false))
						{
							System.out.println("Unable to match " + rx + " against " + extension.pattern);
							throw new Throwable("Macro error");
						}
					}

					for (int i = 0; i < exMatcher.offset; i++)
					{
						t = tz.nextToken();	

						if (t == null)
						{
							System.out.println("Unable to match " + rx + " against " + extension.pattern);
							throw new Throwable("Macro error");
						}
					}

					ArrayList captured = new ArrayList();

					for (int i = 0; i < exMatcher.captures.size(); i++)
					{
						JsPattern capture = (JsPattern)exMatcher.captures.get(i);

						captured.add(capture.getMatch(1));
					}

					rx = JsMatcher.replace(extension.match, captured);

					p = this.makePattern(matcher, rx, extensions, mode, typeSensitive, insensitive);

					break;	
				}
				
				if (t.value_.compareTo("NAME") == 0)
				{
					p = new TokenPattern(matcher, Token.NAME);
					break;
				}

				if (t.value_.compareTo("STRING") == 0)
				{
					p = new TokenPattern(matcher, Token.STRING);
					break;
				}

				if (t.value_.compareTo("REGEXP") == 0)
				{
					p = new TokenPattern(matcher, Token.REGEXP);
					break;
				}

				if (t.value_.compareTo("NUMBER") == 0)
				{
					p = new TokenPattern(matcher, Token.NUMBER);
					break;
				}

			default:
				p = new TokenPattern(matcher, t.type, t.value_, typeSensitive, insensitive);
				break;
		}

		p = checkRepeat(tz, p);
		
		t = tz.peekToken();

		if (t == null)
			return p;
		
		if (t.type == Token.MOD)
		{
			tz.nextToken();
			t = tz.peekToken();
			
			p.printit = true;
			p.printing = true;
		}
		
		if (t == null)
			return p;

		if (t.type == Token.BITOR)
		{
			AlternatePattern ap;

			tz.nextToken();
			JsPattern p2 = readOne(tz, regex);
			
			try
			{
				ap = (AlternatePattern)p2;
				ap.add(p);
			}
			catch (ClassCastException e)
			{
				ap = new AlternatePattern(matcher);
				ap.add(p);
				ap.add(p2);
			}
				
			return ap;
		}
		
		return p;
	}
	
	public JsPattern checkRepeat(Tokenizer tz, JsPattern p)
	{
		// Check for qualifiers
		Token t = tz.peekToken();

		if (t == null)
			return p;

		int min, max;

		switch (t.type)
		{
			case Token.HOOK:			// {0, 1}
				t = tz.nextToken();
				min = 0;
				max = 1;
				break;
				
			case Token.ADD:				// { 1, }
				t = tz.nextToken();
				min = 1;
				max = 1000000;
				break;

			case Token.MUL:				// { 0, }
				t = tz.nextToken();
				min = 0;
				max = 1000000;
				break;
			
			case Token.LC:
				t = tz.nextToken();			// eat Token.LC
				t = tz.nextToken();			// get min matches
				min = t.intValue;			// TODO: it's got to be a number

				t = tz.nextToken();			// Token.COMMA | Token.RC

				if (t.type == Token.RC)
					max = min;
				else // TODO: it's got to be a comma
				{
					t = tz.nextToken();		// Token.RC or max number
					if (t.type == Token.RC)
						max = 1000000;
					else
					{
						// TODO: it's got to be a number
						max = t.intValue;
						t = tz.nextToken();	// TODO: it's got to be RC
					}
				}
				break;
				
				default:	
					return p;
		}

		t = tz.peekToken();
		if (t == null || t.type != Token.HOOK)
		{
			if (t != null && t.type == Token.SUB)
			{
				tz.nextToken();
				return new GreedyRepeatPattern(matcher, p, min, max, false);
			}

			return new GreedyRepeatPattern(matcher, p, min, max, true);
		}

		t = tz.nextToken();
		return new NonGreedyRepeatPattern(matcher, p, min, max);
	}
}

// Maintains current position & captures
class JsMatcher
{
	public int			offset;
	public int			startAt;
	public Token[]		stream;
	public int			firstToken;
	public int			lastToken;
	public char[]		filedata;

	public ArrayList	captures;

	private void init(Token[] stream, char[] filedata)
	{
		this.stream = stream;
		this.filedata = filedata;
		offset = 0;
		startAt = 0;
		captures = new ArrayList();
	}

	public JsMatcher(Token[] stream, char[] filedata)
	{
		init(stream, filedata);
	}
	
	public JsMatcher(char[] filedata, boolean extended, boolean getRegex)
	{
		Tokenizer tz = new Tokenizer(filedata, extended, getRegex);

		Token t;
		
		ArrayList tokens = new ArrayList();
		while ((t = tz.getToken()) != null)
			tokens.add(t);
		
		Token[] tokenArray = new Token[tokens.size()];
		for (int i = 0; i < tokens.size(); i++)
		    tokenArray[i] = (Token)tokens.get(i);

		init(tokenArray, filedata);
	}
	
	public void reset()
	{
		offset = 0;
		startAt = 0;
	}
	
	public boolean match(JsPattern p, boolean advance)
	  throws Exception
	{
		lastToken = -1;

		do
		{
			offset = startAt;

			firstToken = offset;

			if (p.match())
			{
				lastToken = offset-1;
				
				// Check for zero-width match, and make sure to advance so we don't just
				// re-match it forever
				if (offset == startAt)
				{
					// Make sure a zero width match ends at the end of the token stream
					if (startAt == stream.length)
						return false;

					startAt++;
				}
				else
					startAt = offset;

				return true;
			}

//			if (offset != firstToken)
//			    throw new Exception("No match shouldn't change offset " + offset + " " + firstToken);

			if (!advance)
				return false;

//			p.reset();

			startAt++;

		} while (startAt < stream.length);
		
		return false;
	}
	
	public static String replace(String src, ArrayList replacements)
	{
		Token t;
		
		String result = "";
		int start = 0;
		int end = 0;
		
		Tokenizer tz = new Tokenizer(src, true, true);

		while ((t = tz.getToken()) != null)
		{
			if (t.type == Token.BS)
			{
				end = t.offset-1;
				
				t = tz.getToken();
				if (t.type == Token.NUMBER)
				{
					result = result + src.substring(start, end);
					result = result + (String)replacements.get(t.intValue-1);
					start = t.end-1;
				}
			}
		}

		result = result + src.substring(start);
		
		return result;
	}

	public void dumpTokens()
	{
		for (int i = 0; i < stream.length; i++)
		{
			Token t = stream[i];
			System.out.println(t.value_);
		}
	}
	
	public boolean nextMatch()		{ startAt++; return startAt < stream.length; }
	
	public boolean atEnd()			{ return offset >= stream.length; }
	public Token getFirstToken()	{ return stream[firstToken]; }
	public Token getLastToken()		{ if (lastToken < firstToken) return null; return stream[lastToken]; }
	
	public Token current()			{ if (offset >= stream.length) return null; return stream[offset]; }
	public void advance()			{ offset++; }
}

////////////////////////////////////////////////////////
abstract class JsPattern
{
	public JsMatcher	matcher;

	public int			start;
	public boolean		firstTime;
	
	public int			firstToken;
	public int			lastToken;
	
	public boolean		showDebug;
	
	public boolean		printing;
	public boolean		printit;

	public JsPattern(JsMatcher matcher)
	{
		this.matcher = matcher; 
		firstTime = true; 

		printing = false;
		printit = false;

		reset();
	}

	public void copy(JsPattern src)
	{
		firstTime = true;
		showDebug = src.showDebug;
		start = src.start; 
		firstTime = src.firstTime;
		
		printing = src.printing;
		printit = src.printit;
	}

	public void reset()
	{
		if (!firstTime)
		{
			firstTime = true;
			matcher.offset = start;
		}
	}

	public boolean setStart()
	{
		if (!firstTime)
		{
			reset();
			return false;
		}
		
		firstTime = false;
		start = matcher.offset;
		firstToken = start;
		
		return true;
	}

	// case sensitive for string tokens
	// match source string, not value
	// All on one line
	
	// side effect: matcher.offset gets updated if the match succeeds
	// matches() can be called multiple times.  It will return alternative
	// matches until all matches have been returned.  At that point, the
	// matcher will reset itself and return false.
	public boolean match()
	{
		if (!setStart() || !matches())
		{
			reset();
			return false;
		}
		
		lastToken = matcher.offset - 1;
		return true;
	}
	
	protected abstract boolean matches();

	public abstract void dump(int depth);
	
	public void indent(int depth)		{ if (printit) System.out.print("+"); while (depth-- > 0) System.out.print(" "); }
		
	public abstract JsPattern duplicate();

	public void debug(String msg)		{ System.out.println(msg); }

	public String getMatch(int type)
	{
		if (type == 0 && !printit)
			return "";

		Token first = matcher.stream[start];
		if (lastToken < start)
			lastToken = start;
		Token last  = matcher.stream[lastToken];

		return new String(matcher.filedata, first.offset-1, last.end - first.offset);
	}
	
	public void getMatches(ArrayList m)
	{
		if (printit)
		{
			//Token first = matcher.stream[start];
			//if (lastToken < start)
			//    lastToken = start;
			//Token last  = matcher.stream[lastToken];

			//m.add(new Match(first, last));

			if (!firstTime)
				m.add(new Match(start, lastToken));
		}
	}
}

////////////////////////////////////////////////////////
class AnyTokenPattern extends JsPattern
{
	public AnyTokenPattern(JsMatcher matcher)
	{
		super(matcher);
	}
	
	protected boolean matches()
	{
		if (matcher.atEnd())
			return false;

		matcher.offset++;
		return true;
	}

	public JsPattern duplicate()
	{
		AnyTokenPattern atp = new AnyTokenPattern(matcher);
		atp.copy(this);
		return atp;
	}
	
	public void dump(int depth)
	{
		indent(depth);
		if (firstTime)
			System.out.println("AnyToken");
		else
			System.out.println("AnyToken: token #" + start + " = " + matcher.stream[start].value_);
	}
}

////////////////////////////////////////////////////////
class TokenPattern extends JsPattern
{
	int		token;
	String	value;
	boolean	insensitive;
	boolean typeSensitive;

	public TokenPattern(JsMatcher matcher, int token)
	{
		super(matcher);
		
		this.token = token;
		value = null;
	}
	
	public TokenPattern(JsMatcher matcher, int token, String value, boolean typeSensitive, boolean insensitive)
	{
		super(matcher);

		this.token = token;
		this.value = value;
		this.insensitive = insensitive;
		this.typeSensitive = typeSensitive;
	}

	public boolean matches()
	{
		Token t = matcher.current();

		if (t == null)
			return false;

		if (!typeSensitive || token == 0 || token == t.type)
		{
			if (value == null 
				|| (insensitive && value.compareToIgnoreCase(t.value_) == 0) 
				|| value.compareTo(t.value_) == 0)
			{
				matcher.offset++;
				return true;
			}
		}

		return false;
	}

	public JsPattern duplicate()
	{
		TokenPattern tp = new TokenPattern(matcher, token, value, typeSensitive, insensitive);
		//tp.copy(this);
		tp.printing = printing;
		tp.printit = printit;
		
		return tp;
	}

	public void dump(int depth)
	{
		indent(depth);
		if (value != null)
		{
			if (!typeSensitive)
			{
				System.out.println("Token " + value);
			}
			else
			{
				if ((token == Token.STRING || token == Token.NAME) && insensitive)
					System.out.print("Case Insensitive ");
				if (token == Token.STRING)
					System.out.println("String token " + value);
				else if (token == Token.NAME)
					System.out.println("Name token " + value);
				else
					System.out.println("Token " + value);
			}
		}
		else
			System.out.println("Token type " + token);
	}
}

////////////////////////////////////////////////////////
class RegexpPattern extends JsPattern
{
	int		type;
	String	value;
	String	originalValue;
	boolean	insensitive;
	
	java.util.regex.Pattern pattern;
	
	public RegexpPattern(JsMatcher matcher, String value, boolean insensitive)
	{
		super(matcher);

		originalValue = value;
		this.insensitive = insensitive;
		
		if (value.charAt(0) == '"' || value.charAt(0) == '\'')
		{
			// TODO: do something about escaped quotes in the string
			this.type = Token.STRING;
			this.value = value.substring(1, value.length()-1);
		}
		else
		{
			this.type = Token.NAME;
			this.value = value;
		}

		int flags = 0;
		if (insensitive)
			flags = java.util.regex.Pattern.CASE_INSENSITIVE;

		pattern = java.util.regex.Pattern.compile(this.value, flags);
	}

	public boolean matches()
	{
		Token t = matcher.current();

		if (t == null || t.type != type)
			return false;

		java.util.regex.Matcher m = pattern.matcher(t.value_);
		if (m.matches())
		{
			matcher.offset++;
			return true;
		}

		return false;
	}

	public JsPattern duplicate()
	{
		return new RegexpPattern(matcher, originalValue, insensitive);
	}

	public void dump(int depth)
	{
		indent(depth);

		if (insensitive)
			System.out.print("Case Insensitive ");
			
		if (type == Token.NAME)
			System.out.println("Name Regex: " + value);
		else
			System.out.println("String Regex: " + value);
	}
}

////////////////////////////////////////////////////////
class NotTokenPattern extends JsPattern
{
	ArrayList patterns;
	boolean insensitive;
	
	public NotTokenPattern(JsMatcher matcher, boolean insensitive)
	{
		super(matcher);

		this.insensitive = insensitive;
		patterns = new ArrayList();
	}
	
	public void add(TokenPattern tp)
	{
		patterns.add(tp);
		
		if (tp.printing)
			printing = true;
	}
	
	public boolean matches()
	{
		Token t = matcher.current();
		if (t == null)
			return false;

		for (int i = 0; i < patterns.size(); i++)
		{
			TokenPattern tp = (TokenPattern)patterns.get(i);
			// TODO: shouldn't this just do:
			//   if (tp.matches()) return false;
			if (tp.token == 0 || tp.token == t.type)
			{
				if (tp.value == null 
					|| (insensitive && tp.value.compareToIgnoreCase(t.value_) == 0) 
					|| tp.value.compareTo(t.value_) == 0)
				{
					return false;
				}
			}
		}
		
		matcher.offset++;
		return true;
	}

	public JsPattern duplicate()
	{
		NotTokenPattern ntp = new NotTokenPattern(matcher, insensitive);
		ntp.copy(this);
	
		for (int i = 0; i < patterns.size(); i++)
		{
			TokenPattern tp = (TokenPattern)patterns.get(i);
			
			ntp.add((TokenPattern)tp.duplicate());
		}
			
		return ntp;
	}
	
	public void dump(int depth)
	{
		indent(depth);
		
		String line = "";
		for (int i = 0; i < patterns.size(); i++)
		{
			TokenPattern tp = (TokenPattern)patterns.get(i);
			line += tp.token + " ";
		}
		
		System.out.println("NotToken " + line);
	}
}

////////////////////////////////////////////////////////
class AlternatePattern extends JsPattern
{
	ArrayList	patterns;
	int			index;
		
	public AlternatePattern(JsMatcher matcher)
	{
		super(matcher);

		patterns = new ArrayList();
	}
	
	public void add(JsPattern tp)
	{
		patterns.add(tp);

		if (tp.printing)
			printing = true;
	}
	
	public void reset()
	{
		if (patterns != null)
		{
			for (int i = 0; i < patterns.size(); i++)
			{
				JsPattern p = (JsPattern)patterns.get(i);
				p.reset();
			}
		}
		
		index = 0;
		super.reset();
	}

	public boolean setStart()
	{
		if (firstTime)
		{
			firstTime = false;
			start = matcher.offset;
			firstToken = start;
		}
		else
			matcher.offset = start;
		
		return true;
	}
	
	protected boolean matches()
	{
		while (index < patterns.size())
		{
			JsPattern p = (JsPattern)patterns.get(index++);

			if (p.match())
				return true;
		}
		
		return false;
	}

	public JsPattern duplicate()
	{
		AlternatePattern ap = new AlternatePattern(matcher);
		ap.copy(this);
	
		for (int i = 0; i < patterns.size(); i++)
		{
			JsPattern p = (JsPattern)patterns.get(i);
			
			ap.add(p.duplicate());
		}
			
		return ap;
	}

	public void getMatches(ArrayList m)
	{
		if (printit)
			super.getMatches(m);
		else
		{
			for (int i = 0; i < patterns.size(); i++)
			{
				JsPattern p = (JsPattern)patterns.get(i);
				p.getMatches(m);
			}
		}
	}
	
	public void dump(int depth)
	{
		indent(depth);
		System.out.println("One of:");
		for (int i = 0; i < patterns.size(); i++)
		{
			JsPattern p = (JsPattern)patterns.get(i);
			if (p != null)
				p.dump(depth + 4);
		}
	}
}


////////////////////////////////////////////////////////
abstract class RepeatPattern extends JsPattern
{
	protected JsPattern		pattern;
	protected int			min, max;
	protected int			current;		// the current number of matches
	public ArrayList		states;
//	public boolean			emptyMatch;
	
	public RepeatPattern(JsMatcher matcher, JsPattern p, int min, int max)
	{
		super(matcher);
		
		this.pattern = p;
		this.min = min;
		this.max = max;

		if (p.printing)
			printing = true;

		reset();
	}
	
	protected boolean matches()
	{
		while (simplematch())
		{
			if (current < 2)
				return true;
			
			JsPattern p = (JsPattern)states.get(current-2);
			
			// Don't allow two empty matches in a row
			if (matcher.offset > p.start)
				return true;
			return false;
		}

		return false;
	}
	
	protected abstract boolean simplematch();

	public void reset()
	{
//		emptyMatch = false;
		states = new ArrayList();
		current = 0;
		super.reset();
	}

	public boolean setStart()
	{
		if (firstTime)
		{
			firstTime = false;
			start = matcher.offset;
			firstToken = start;
		}
		else
			matcher.offset = start;
		
		return true;
	}
	
	public void copy(JsPattern src)
	{
		super.copy(src);
		
	    RepeatPattern rp = (RepeatPattern)src;
	    
	    current = rp.current;
	    
	    for (int i = 0; i < rp.states.size(); i++)
	    {
			JsPattern p = (JsPattern)rp.states.get(i);
			if (p == null)
				states.add(i, null);
			else
				states.add(i, p.duplicate());
	    }
	}

	public void getMatches(ArrayList m)
	{
		if (printit)
			super.getMatches(m);
		else
		{
			for (int i = 0; i < current; i++)
			{
				JsPattern p = (JsPattern)states.get(i);
				p.getMatches(m);
			}
		}
	}
	
	//public String getMatch(int type)
	//{
	//    if (type == 1)
	//        return super.getMatch(type);
		
	//    String value = "";
	//    String comma = "";

	//    for (int i = 0; i < states.length; i++)
	//    {
	//        if (states[i] != null)
	//        {
	//            String v = states[i].getMatch(type);
	//            if (v.length() > 0)
	//            {
	//                value = value + comma + v;
	//                comma = ", ";
	//            }
	//        }
	//    }
		
	//    return value;
	//}
}

////////////////////////////////////////////////////////
class GreedyRepeatPattern extends RepeatPattern
{
	public boolean backtrack;
	
	public GreedyRepeatPattern(JsMatcher matcher, JsPattern p, int min, int max, boolean backtrack)
	{
		super(matcher, p, min, max);
		
		this.backtrack = backtrack;
	}
	
	protected boolean simplematch()
	{
		while (current > 0)
		{
			if (!backtrack)
				return false;

			if (((JsPattern)states.get(current-1)).match())
				break;
			current--;

			if (current >= min)
				return true;

			if (current == 0)
				return false;
		}

		while (current < max)
		{
			JsPattern p = pattern.duplicate();
			states.add(current, p);
			if (!p.match())
				break;
			current++;
		}

		//if (current == 0)
		//{
		//    if (emptyMatch)
		//        return false;

		//    emptyMatch = true;
		//}

		return (current >= min);
	}
	
	public boolean setStart()
	{
		if (!firstTime && current == 0)
			return false;

		return super.setStart();
	}
	
	public JsPattern duplicate()
	{
		GreedyRepeatPattern grp = new GreedyRepeatPattern(matcher, pattern, min, max, backtrack);
		grp.copy(this);
		return grp;
	}

	public void dump(int depth)
	{
		indent(depth);
		if (firstTime)
		{
			if (backtrack)
				System.out.println("Greedy repeat " + min + " to " + max + " of:");
			else
				System.out.println("Non-backtracking greedy repeat " + min + " to " + max + " of:");
				
			pattern.dump(depth + 4);
		}
		else
		{
			System.out.println("Greedy repeat matched " + current + " times:");
		    for (int i = 0; i < current; i++)
				((JsPattern)states.get(i)).dump(depth+4);
		}
	}
}

////////////////////////////////////////////////////////
class NonGreedyRepeatPattern extends RepeatPattern
{	
	int target;
	
	public NonGreedyRepeatPattern(JsMatcher matcher, JsPattern p, int min, int max)
	{
		super(matcher, p, min, max);
		target = min;
		
		for (int i = 0; i <= min; i++)
		{
			JsPattern pc = pattern.duplicate();
			states.add(pc);
		}
	}
	
	protected boolean simplematch()
	{
//		if (!firstTime && target == 0)
//			target++;
			
		while (current > 0)
		{
			if (((JsPattern)states.get(current-1)).match())
				break;
			current--;

			if (current == 0)
			{
				if (target == max)
					return false;
				target++;

				JsPattern p = pattern.duplicate();
				states.add(current, p);
			}

		}

		while (current < target)
		{
			JsPattern p = (JsPattern)states.get(current);
			if (!p.match())
				return false;
			current++;
		}

//System.out.println("rrp matched " + current);
//System.out.println(getMatch(1));

		return true;
	}
	
	public boolean setStart()
	{
		if (!firstTime && target == 0)
		{
			target = 1;
			JsPattern p = pattern.duplicate();
			states.add(current, p);
		}

		return super.setStart();
	}
	
	public void reset()
	{
		super.reset();

		target = min;
		if (pattern != null)
		{
			for (int i = 0; i <= target; i++)
			{
				JsPattern pc = pattern.duplicate();
				states.add(pc);
			}
		}
	}
	
	public JsPattern duplicate()
	{
		NonGreedyRepeatPattern ngrp = new NonGreedyRepeatPattern(matcher, pattern, min, max);
		ngrp.copy(this);
		return ngrp;
	}

	public void dump(int depth)
	{
		indent(depth);
		System.out.println("Reluctant repeat " + min + " to " + max + " of:");
		pattern.dump(depth + 4);
	}
}

////////////////////////////////////////////////////////
class ListPattern extends JsPattern
{
	ArrayList	patterns;
	int			current;
	
	public ListPattern(JsMatcher matcher)
	{
		super(matcher);
	}
	
	public void add(JsPattern p)
	{
		if (p.printing)
			printing = true;

		patterns.add(p);
	}

	protected boolean matches()
	{
		int count = 0;
		
		Token t = matcher.current();
//System.out.println("trying to match list on line " + t.lineNum);
		if (current == patterns.size())
			current--;
			
		while (current < patterns.size())
		{
			JsPattern p = (JsPattern)patterns.get(current);
			count++;
			if (p.match())
			{
				current++;
//System.out.println("list advancing to " + current);
				continue;
			}
			
			current--;
			if (current < 0)
			{
//System.out.println("list failed");
				//if (count > 1)
				//    System.out.println("match failed after " + count + " attempts");
				return false;
			}
//System.out.println("list retreating to " + current);
		}

		
//System.out.println("list succeeded");
//		System.out.print("list matched at " + t.value_ + " after " + count + " attempts");
		t = matcher.current();
//		System.out.println(" to " + t.value_);
		lastToken = matcher.offset - 1;
//		System.out.println(getMatch(1));
		
		return true;
	}
	
	public boolean setStart()
	{
		if (firstTime)
		{
			firstTime = false;
			start = matcher.offset;
			firstToken = start;
		}
		else
		{
			matcher.offset = start;
			if (patterns.size() == 0)
				return false;
		}
		
		return true;
	}
	
	public void reset()
	{
		current = 0;

		if (patterns == null)
			patterns = new ArrayList();

		for (int i = 0; i < patterns.size(); i++)
		{
			JsPattern p = (JsPattern)patterns.get(i);
			p.reset();
		}
			
		super.reset();
	}
		
	public JsPattern duplicate()
	{
		ListPattern lp = new ListPattern(matcher);
		for (int i = 0; i < patterns.size(); i++)
		{
			JsPattern p = (JsPattern)patterns.get(i);
			lp.add(p.duplicate());
		}
		lp.copy(this);
		return lp;
	}
	
	public void getMatches(ArrayList m)
	{
		if (printit)
			super.getMatches(m);
		else
		{
			for (int i = 0; i < patterns.size(); i++)
			{
				JsPattern p = (JsPattern)patterns.get(i);
				p.getMatches(m);
			}
		}
	}

	public void dump(int depth)
	{
		indent(depth);
		System.out.println("List of:");
		for (int i = 0; i < patterns.size(); i++)
		{
			JsPattern p = (JsPattern)patterns.get(i);
			p.dump(depth + 4);
		}
	}
	
	public String getMatch(int type)
	{
	    if (type == 1 || printit)
	        return super.getMatch(1);
		
	    if (!printing)
	        return "";
		
	    String newline = "";
	    String value = "";
		
	    for (int i = 0; i < patterns.size(); i++)
	    {
	        JsPattern p = (JsPattern)patterns.get(i);
	        String v = p.getMatch(type);
	        if (v.length() > 0)
	        {
	            value = value + newline + v;
	            newline = "\n";
	        }
	    }
		
	    return value;
	}
}

////////////////////////////////////////////////////////
class CapturePattern extends JsPattern
{
	JsPattern pattern;
	public int lastToken;
	public int index;
	
	public CapturePattern(JsMatcher matcher, JsPattern p, int index)
	{
		super(matcher);
		
		pattern = p;

		if (p.printing)
			printing = true;
//		if (p.printit)
//			printit = true;

		if (matcher.captures.size() <= index)
			matcher.captures.add(null);

		matcher.captures.set(index, this);
		
		this.index = index;
	}
	
	protected boolean matches()
	{
		boolean ret = pattern.matches();
		if (!ret)
			return false;
		
		lastToken = pattern.lastToken = matcher.offset-1;
		
		return true;
	}
		
	public boolean setStart()
	{
		if (firstTime)
		{
			firstTime = false;
			start = matcher.offset;
			firstToken = start;
		}

		return pattern.setStart();
	}
	
	public void reset()
	{
		if (pattern != null)
			pattern.reset();
		super.reset();
	}

	public CapturePattern duplicate()
	{
		CapturePattern cp = new CapturePattern(matcher, pattern.duplicate(), index);
		cp.copy(this);
		cp.lastToken = lastToken;

		matcher.captures.set(index, cp);

		return cp;
	}
	
	public void dump(int depth)
	{
		indent(depth);
		System.out.println("Capture " + (index+1) + ":");
		pattern.dump(depth + 4);
	}

	public String getMatch(int type)
	{
	    if (type == 0)
	        return pattern.getMatch(1);
	    else
	        return pattern.getMatch(type);
	}

	public void getMatches(ArrayList m)
	{
		if (printit)
			super.getMatches(m);
		else
			pattern.getMatches(m);
	}
}


////////////////////////////////////////////////////////
class ReferencePattern extends JsPattern
{
	int index;
	
	public ReferencePattern(JsMatcher matcher, CapturePattern cp)
	{
		super(matcher);
		index = cp.index;
	}

	public boolean matches()
	{
		CapturePattern pattern = (CapturePattern)matcher.captures.get(index);

		for (int i = pattern.start; i <= pattern.lastToken; i++)
		{
			Token t = matcher.current();
			matcher.offset++;
			
			if (t == null || t.value_.compareTo(matcher.stream[i].value_) != 0)
				return false;
		}

		return true;
	}

	public ReferencePattern duplicate()
	{
		CapturePattern pattern = (CapturePattern)matcher.captures.get(index);
		ReferencePattern rp = new ReferencePattern(matcher, pattern);
		return rp;
	}
		
	public void dump(int depth)
	{
		indent(depth);
		System.out.println("ReferencePattern");
	}
}

////////////////////////////////////////////////////////
class ClosePattern extends JsPattern
{
	int				index;
	boolean			requireOpener;

	int				lastMatch;
	int				lastSearch;

	////////////////////////////////////////////////////////
	public ClosePattern(JsMatcher matcher, CapturePattern cp, boolean requireOpener)
	{
		super(matcher);
		
		this.index = cp.index;
		this.requireOpener = requireOpener;
		
		lastMatch = -1;
		lastSearch = -1;
	}

	////////////////////////////////////////////////////////
	public boolean matches()
	{
		requireOpener = true;

		CapturePattern pattern = (CapturePattern)matcher.captures.get(index);

		if (lastSearch != pattern.firstToken)
		{
		    lastSearch = pattern.firstToken;
		    lastMatch = -1;
		}
		else if (lastMatch != -1)
		{
		    if (start != lastMatch)
		        return false;

//System.out.println("Found close for capture " + (index + 1) + " at " + start);
		    matcher.advance();
		    return true;
		}

		int captureSize = pattern.lastToken - pattern.firstToken + 1;

		Token t = matcher.current();

		if (t == null)
			return false;

		if (captureSize > 1)
			return false;
		
		Token opener = matcher.stream[pattern.firstToken];

		int searchFor = -1;
		
		if (opener.type == Token.LP)
			searchFor = Token.RP;
		if (opener.type == Token.LC)
			searchFor = Token.RC;
		if (opener.type == Token.LB)
			searchFor = Token.RB;

		if (searchFor == -1 && !requireOpener)
			return true;

		if (t.type != searchFor)
			return false;

		int count = 0;
		if (lastMatch == -1)
		{
//	System.out.println("searching for close");
			for (int i = pattern.start; i < matcher.stream.length; i++)
			{
				t = matcher.stream[i];

				if (t.type == opener.type)
					count++;
				if (t.type == searchFor)
					count--;
				
				if (count == 0)
				{
//	System.out.println("setting lastmatch to " + i);
	                lastMatch = i;
//					break;
	//System.out.println("setting lastmatch to " + lastMatch);
	                if (start == lastMatch)
	                {
	                    matcher.advance();
//	System.out.println("found close at " + start);
//System.out.println("Found close for capture " + (index + 1) + " at " + start);
	                    return true;
	                }

	                return false;
				}
					
				if (count <= 0)
					break;
			}
		}
		lastMatch = -2;
		return false;
		
//        count = 0;
			
//        for (int i = pattern.start; i < start; i++)
//        {
//            t = matcher.stream[i];

//            if (t.type == opener.type)
//                count++;
//            if (t.type == searchFor)
//                count--;
				
//            if (count <= 0)
//                return false;
//        }

//        if (count == 1)
//        {
//            lastMatch = start;
//            matcher.advance();
//System.out.println("close brace found at " + lastMatch);
////System.out.println(start + " " + lastMatch);
//            return true;
//        }
		
//        return false;
	}

	////////////////////////////////////////////////////////
	public ClosePattern duplicate()
	{
		CapturePattern pattern = (CapturePattern)matcher.captures.get(index);
		ClosePattern cp = new ClosePattern(matcher, pattern, requireOpener);
		cp.copy(this);
		
		return cp;
	}

	public void dump(int depth)
	{
		indent(depth);
		System.out.println("Brace close of capture " + (index+1));
	}
}

