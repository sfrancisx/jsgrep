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

class Config
{
	public String		root;
	public ArrayList	directories;
	public ArrayList	filePatterns;
	public ArrayList	excludes;
};

class jsgrep
{
// -a # Show after context
	public static int     showAfter  = 0;

// -b # Show before context
	public static int     showBefore = 0;

// -c Show count of matches
// -c- 
// -c+ show matches even if 0
	public static boolean showCount	= false;	// Show number of matches per file
	public static boolean showZeroCount = false;
	
// -d Format output for DevStudio
// -d-
	public static boolean devStudio = false;

// -f* response file

// -fp:	additional file patterns that get added to default patterns
	public static ArrayList extraFilePatterns = new ArrayList();

// -fp-:  file pattern.  Replaces anything in config file
	public static ArrayList filePatterns = new ArrayList();

// -g show args
// -g-
	public static boolean showArgs   = false;

// -h Supress the file name for each match
// -h+: Show the file name for each match
// -ha: absolute file name
// -hc: file name relative to current directory
// -hr: file name relative to search root
	public static boolean showFile  = false;	// Show filename for every individual match
	public static int showFilePath = 0;			// 0->no path, 1->relative to current, 2->relative to search root, 3->absolute

// -i Ignore case
// -i-
	public static boolean insensitive = false;

// -l List files containing a match
// -l-
// -la
// -lc
// -lr
	public static boolean listFiles = true;		// List files containing a match
	public static int listFilePath = 1;			// 0->no path, 1->relative to current, 2->relative to search root, 3->absolute

// -m showMatch (defaults to false, showLine defaults to true)
// -m- don't show the match
// -m+ show the entire matching line
	public static boolean showMatch  = false;	
	public static boolean hideMatch = false;

// -n showLineNum
// -n-
	public static boolean showLineNum = true;	// Show line number for every match

// -o show options being used
// -o-
	public static boolean showOptions = false;

// -p Show full path
// -p-
// -pa
// -pc:  show path relative to current directory
// -pr:  show path relative to search root
	public static int filePath = 1;				// default value for -l & -h
	public static String searchRoot;

// -q quiet - no extra output
	public static boolean quiet = false;

// -r Recursive
	public static boolean recursive = true;

// -s showSearch
	public static boolean showSearch = false;	// Show the structure of the pattern we're trying to match

// -t match both token type and value
	public static boolean matchTokenType = false;
// -v Invert match
	public static boolean invertMatch = false;

// -x* Extended match (show all possible matches?)
// -x:	exit after showing args, matches, options
	public static boolean exit = false;

	public static Hashtable extensions  = new Hashtable();
	public static ArrayList ordered_extensions = new ArrayList();
	public static Hashtable config		= new Hashtable();
	public static String defaultSearch;

	public static int totalCount = 0;

	/////////////////////////////////////////////////////////////////////
	public static void addFilePattern(ArrayList to, String pattern)
	{
		// convert pattern from a file pattern to a regex,
		// i.e.  * -> .*   ? -> .   . -> \.
		pattern = pattern.replace(".", "\\.");
		pattern = pattern.replace("*", ".*");
		pattern = pattern.replace("?", ".");

		to.add(java.util.regex.Pattern.compile(pattern));
	}

	/////////////////////////////////////////////////////////////////////
	public static int readOptions(String[] args, int firstArg, int lastArg)
	{
		int argNum = firstArg;
		
		while (argNum < lastArg)
		{
			args[argNum] = args[argNum].intern();
			
			if (args[argNum] == "-a")
			{
				argNum++;
				showAfter = Integer.parseInt(args[argNum]);
			}
			else if (args[argNum] == "-b")
			{
				argNum++;
				showBefore = Integer.parseInt(args[argNum]);
			}
			else if (args[argNum] == "-c")
			{
				listFiles = true;
				showCount = true;
				showZeroCount = false;
				hideMatch = true;
			}
			else if (args[argNum] == "-c+")
			{
				listFiles = true;
				showCount = true;
				showZeroCount = true;
				hideMatch = true;
			}
			else if (args[argNum] == "-c-")
				showCount = false;
			else if (args[argNum] == "-d")
			{
				devStudio = true;
				showFilePath = 3;
				hideMatch = false;
			}
			else if (args[argNum] == "-d-")
				devStudio = false;
			else if (args[argNum] == "-fp-")
			{
				argNum++;
				addFilePattern(filePatterns, args[argNum]);
			}
			else if (args[argNum] == "-fp")
			{
				argNum++;
				addFilePattern(extraFilePatterns, args[argNum]);
			}
			else if (args[argNum] == "-g")
				showArgs = true;
			else if (args[argNum] == "-g-")
				showArgs = false;
			else if (args[argNum] == "-h")
			{
				showFile = false;
			}
			else if (args[argNum] == "-ha")
			{
				showFile = true;
				showFilePath = 3;
			}
			else if (args[argNum] == "-hc")
			{
				showFile = true;
				showFilePath = 1;
			}
			else if (args[argNum] == "-hf")
			{
				showFile = true;
				showFilePath = 0;
			}
			else if (args[argNum] == "-hr")
			{
				showFile = true;
				showFilePath = 2;
			}
			else if (args[argNum] == "-h+")
			{
				showFile = true;
				showFilePath = filePath;
			}
			else if (args[argNum] == "-i")
				insensitive = true;
			else if (args[argNum] == "-i-")
				insensitive = false;
			else if (args[argNum] == "-l")
			{
				listFiles = true;
				listFilePath = filePath;
				hideMatch = true;
			}
			else if (args[argNum] == "-la")
			{
				listFiles = true;
				listFilePath = 3;
				hideMatch = true;
			}
			else if (args[argNum] == "-lc")
			{
				listFiles = true;
				listFilePath = 1;
				hideMatch = true;
			}
			else if (args[argNum] == "-lf")
			{
				listFiles = true;
				listFilePath = 0;
				hideMatch = true;
			}
			else if (args[argNum] == "-lr")
			{
				listFiles = true;
				listFilePath = 2;
				hideMatch = true;
			}
			else if (args[argNum] == "-l-")
				listFiles = false;
			else if (args[argNum] == "-m")
			{
				hideMatch = false;
				showMatch = true;
			}
			else if (args[argNum] == "-m+")
			{
				hideMatch = false;
				showMatch = false;
			}
			else if (args[argNum] == "-m-")
				hideMatch = true;
			else if (args[argNum] == "-n")
				showLineNum = true;
			else if (args[argNum] == "-n-")
				showLineNum = false;
			else if (args[argNum] == "-o")
				showOptions = true;
			else if (args[argNum] == "-o-")
				showOptions = false;
			else if (args[argNum] == "-p" || args[argNum] == "-pr")
			{			
				filePath = 2;
				listFilePath = 2;
				showFilePath = 2;
			}
			else if (args[argNum] == "-p-" || args[argNum] == "-pf")
			{
				filePath = 0;
				listFilePath = 0;
				showFilePath = 0;
			}
			else if (args[argNum] == "-pa")
			{
				filePath = 3;
				listFilePath = 3;
				showFilePath = 3;
			}
			else if (args[argNum] == "-pc")
			{
				filePath = 1;
				listFilePath = 1;
				showFilePath = 1;
			}
			else if (args[argNum] == "-q")
			{
				quiet = true;

				showSearch = false;
				showArgs = false;
				showOptions = false;
				listFiles = false;
			}
			else if (args[argNum] == "-q-")
			{
				quiet = false;
			}
			else if (args[argNum] == "-r")
				recursive = true;
			else if (args[argNum] == "-r-")
				recursive = false;
			else if (args[argNum] == "-s")
				showSearch = true;
			else if (args[argNum] == "-s-")
				showSearch = false;
			else if (args[argNum] == "-t")
				matchTokenType = true;
			else if (args[argNum] == "-t-")
				matchTokenType = false;
			else if (args[argNum] == "-v")
				invertMatch = true;
			else if (args[argNum] == "-v-")
				invertMatch = false;
			else if (args[argNum] == "-x")
				exit = true;
			else if (args[argNum] == "-x-")
				exit = false;
			else
				break;
			
			argNum++;
		}
		
		return argNum - firstArg;
	}

	/////////////////////////////////////////////////////////////////////
	public static void showOptions(ArrayList files)
	{
		System.out.println("Files/directories to be searched:");
		String comma = "";
		for (int i = 0; i < files.size(); i++)
		{
			String s = (String)files.get(i);
			System.out.print(comma + s);
			comma = ", ";
		}
		System.out.println();
		
		
		if (listFiles)
		{
			System.out.print("File header includes ");
			if (showCount)
				System.out.print("count of matches and ");
			switch (listFilePath)
			{
				case 0: System.out.println("file name"); break;
				case 1: System.out.println("file name relative to current directory"); break;
				case 2: System.out.println("file name relative to " + searchRoot); break;
				case 3: System.out.println("absolute path to file"); break;
			}
		}

		System.out.print("Each match will ");
		if (hideMatch)
			System.out.print("be hidden ");
		else
		{
			if (invertMatch)
				System.out.print("be inverted and ");
			if (showMatch)
				System.out.print("show the matching tokens ");
			else
				System.out.print("show complete lines which contain a match ");

			if (showBefore > 0 || showAfter > 0)
				System.out.print("with " + showBefore + " items of context before the match and " + showAfter + " items after it");
			System.out.println();

			System.out.print("Matches will be prefixed with");
			if (showFile || showLineNum)
			{
				if (showFile)
				{
					System.out.print(" the file name");
					if (showLineNum)
						System.out.print(" and");
				}
				if (showLineNum)
				{
					System.out.print(" the line number");
					if (devStudio)
						System.out.print(" formatted for DevStudio");
				}
			}
			else
				System.out.print(" nothing");
		}

		System.out.println();
		System.out.println();
	}
	
	/////////////////////////////////////////////////////////////////////
	public static int readFiles(String[] args, ArrayList files, int firstArg, int lastArg)
	{
		int argNum = firstArg;
		
		while (argNum < lastArg)
		{
			File f = new File(args[argNum]);
			if (!f.exists())
				break;

			files.add(args[argNum]);

			argNum++;
		}
		
		return argNum - firstArg;
	}
	
	/////////////////////////////////////////////////////////////////////
	public static String[] reverseArgs(String[] args)
	{
		String[] reversed = new String[args.length];
		
		for (int i = 0; i < args.length; i++)
		{
			int p = args.length - i - 1;
			
			args[i] = args[i].intern();
			
			if (args[i] == "-a" || args[i] == "-b" || args[i] == "-fp")
			{
				reversed[p-1] = args[i];
				reversed[p] = args[i+1];
				i++;
				args[i] = args[i].intern();
			}
			else
				reversed[p] = args[i];
		}

		return reversed;
	}
	
	/////////////////////////////////////////////////////////////////////
	public static void main(String[] args)
		throws IOException, Throwable
	{	
		ArrayList	files = new ArrayList();
		String		regex = "";

		readConfigFile();		

		if (args.length == 0)
		{
			showHelp("--usage");
			return;
		}

		if (args.length == 1 && args[0].intern() == "-h")
		{
			showHelp("--help");
			return;
		}
		
 		if (args.length == 2 && args[0].intern() == "-h")
		{
			showHelp(args[1].intern());
			return;
		}

		if (args.length == 1 && args[0].startsWith("--"))
		{
			showHelp(args[0].intern());
			return;
		}

		String[] reversed = reverseArgs(args);

		int firstArg = 0, lastArg = args.length;
		Config cfg = null;
		
		while (firstArg < lastArg)
		{
			int cfirst = firstArg;
			int clast = lastArg;

			if (cfg == null)
			{
				cfg = (Config)config.get(args[firstArg]);
				if (cfg != null)
					firstArg++;
			}
			if (cfg == null)
			{
				cfg = (Config)config.get(args[lastArg-1]);
				if (cfg != null)
					lastArg--;
			}
			
			firstArg += readOptions(args, firstArg, lastArg);
			lastArg  -= readOptions(reversed, args.length - lastArg, args.length - firstArg);
			
			firstArg += readFiles(args, files, firstArg, lastArg);
			lastArg  -= readFiles(reversed, files, args.length - lastArg, args.length - firstArg);
			
			if (cfirst == firstArg && clast == lastArg)
				break;
		}

		if (cfg == null && defaultSearch != null)
			cfg = (Config)config.get(defaultSearch);

		if (showArgs)
		{
			for (int i = 0; i < args.length; i++)
				System.out.println("arg " + i + ": " + args[i]);
			System.out.println();
		}

		searchRoot = ".";

		if (files.size() == 0 && cfg != null)
		{
			if (cfg.root != null)
				searchRoot = cfg.root;
			
			if (cfg.directories == null)
				files.add(searchRoot);
			else
			{
				for (int i = 0; i < cfg.directories.size(); i++)
				{
					String dir = (String)cfg.directories.get(i);
					files.add(searchRoot + File.separator + dir);
				}
			}
		}
		
		if (filePatterns.size() == 0)
		{
			if (cfg != null && cfg.filePatterns != null)
			{
				for (int i = 0; i < cfg.filePatterns.size(); i++)
				{
					String pattern = (String)cfg.filePatterns.get(i);
					addFilePattern(filePatterns, pattern);
				}
			}
			else
				addFilePattern(filePatterns, "*.js");
		}

		for (int i = 0; i < extraFilePatterns.size(); i++)
		{
			java.util.regex.Pattern p = (java.util.regex.Pattern)extraFilePatterns.get(i);
			filePatterns.add(p);
		}

		File sr = new File(searchRoot);
		searchRoot = sr.getCanonicalPath();
		
		if (files.size() == 0)
			files.add(".");

		if (showOptions)
			showOptions(files);
		
		regex = "";
		
		if (lastArg > firstArg && args[firstArg].length() != 0)
		{
			while (lastArg > firstArg)
			{
				regex += " " + args[firstArg++];
			}
		}
		
		if (regex.length() == 0)
		{
			showHelp("--usage");
			System.out.println("ERROR: No regex specified\n");
			return;
		}
		
		if (showSearch)
		{
			System.out.println(regex);
			JsPattern p = (new Maker()).makePattern(new JsMatcher(null, null), regex, extensions, 0, matchTokenType, insensitive);
			p.dump(0);
			System.out.println();
		}
		
		if (!exit)
		{
			for (int i = 0; i < files.size(); i++)
			{
				String s = (String)files.get(i);

				if (s.startsWith("~"))
					s = System.getProperty("user.home") + s.substring(1);
	
				HandleFile(s, cfg, regex, true);
			}
		}
				
        if (!quiet)
        {
			if (showCount)
				System.out.println(totalCount + " matches found");
			System.out.println("jsgrep done");
		}
	}
	
	/////////////////////////////////////////////////////////////////////
	public static void readPatterns(Token[] tokens, int tn, int max)
	  throws Throwable
	{
		String name, pattern = null, regex = null, comment;
		
		// NAME COLON STRING COMMA or
		// NAME COLON LC NAME COLON STRING COMMA NAME COLON STRING RC COMMA
		
		while (tn < max)
		{
			Token t = tokens[tn++];
			name = t.value_;
			tn++;		// skip the colon
			
			t = tokens[tn++];

			if (t.type == Token.STRING)
			{
				regex = t.value_;
				pattern = null;
				comment = null;
			}
			else
			{
				int close = match(tokens, tn-1);
				pattern = null;
				regex = "";
				comment = null;

				while (tn < close)
				{
					t = tokens[tn++];
					String what = t.value_;
					tn++;				// skip the colon
					t = tokens[tn++];
					
					if (what == "pattern")
						pattern = t.value_;
					else if (what == "match")
						regex = t.value_;
					else if (what == "comment")
						comment = t.value_;

					tn++;	// skip the comma or right brace
				}
			}
			
			tn++;					// Skip the comma

			Extension e = new Extension(name, regex, pattern, comment);
			extensions.put(name, e);
			ordered_extensions.add(e);
		}
	}
	
	/////////////////////////////////////////////////////////////////////
	public static ArrayList readArray(Token[] tokens, int tn, int end)
	{
		ArrayList a = new ArrayList();
		
		while (tn < end)
		{
			a.add(tokens[tn++].value_);
			tn++;
		}
		
		return a;
	}

	/////////////////////////////////////////////////////////////////////
	public static ArrayList readFilePatternArray(Token[] tokens, int tn, int end)
	{
		ArrayList a = readArray(tokens, tn, end);
		ArrayList files = new ArrayList();
		
		for (int i = 0; i < a.size(); i++)
		{
			String p = (String)a.get(i);
			addFilePattern(files, p);
		}
		
		return files;
	}
	
	/////////////////////////////////////////////////////////////////////
	public static void readSearches(Token[] tokens, int tn, int max)
		throws Throwable
	{
		int			end;
		String		name;
		
		while (tn < max)
		{
			Token t = tokens[tn++];
			name = t.value_;

			Config cfg = new Config();
			config.put(name, cfg);

			tn++;		// skip the colon
			
			int close = match(tokens, tn);
			tn++;
				
			while (tn < close)
			{
				t = tokens[tn++];
				tn++;

				if (t.value_ == "root")
				{
					cfg.root = tokens[tn++].value_;
				}
				else if (t.value_ == "exclude")
				{
					end = tn+1;
					if (tokens[tn].type == Token.LB)
					{
						end = match(tokens, tn);
						tn++;
					}
						
					cfg.excludes = readFilePatternArray(tokens, tn, end);
					
					tn = end;
				}
				else if (t.value_ == "directories")
				{
					end = tn+1;
					if (tokens[tn].type == Token.LB)
					{
						end = match(tokens, tn);
						tn++;
					}
						
					cfg.directories = readArray(tokens, tn, end);

					tn = end;
				}
				else if (t.value_ == "filepatterns")
				{
					end = tn+1;
					if (tokens[tn].type == Token.LB)
					{
						end = match(tokens, tn);
						tn++;
					}
						
					cfg.filePatterns = readArray(tokens, tn, end);

					tn = end;
				}

				tn++;		// skip the comma
			}

			tn = close + 1;
		}
	}

	/////////////////////////////////////////////////////////////////////
	public static void getDefaultOptions(String o)
	{
		String[] options = o.split("\\s+");
		readOptions(options, 0, options.length);
	}

	/////////////////////////////////////////////////////////////////////
	public static void readConfigFile()
		throws Exception, Throwable
	{
		if (readConfigFile("jsgrep.cfg"))
			return;

		String cfgFile = System.getProperty("user.home") + "/.jsgrep.cfg";
		if (readConfigFile(cfgFile))
			return;

		readConfigFile("/home/y/conf/jsgrep/jsgrep.cfg");
	}
	
	/////////////////////////////////////////////////////////////////////
	public static boolean readConfigFile(String cfgFile)
		throws Exception, Throwable
	{
		int len = (int)(new File(cfgFile)).length();
		if (len == 0)
			return false;

		char[] filedata = new char[len];
		FileReader fr = new FileReader(cfgFile);
		len = fr.read(filedata, 0, len);
		fr.close();

		Token[] tokens = getTokens(filedata);

		int tn = 0;
		int max = tokens.length;
		
		if (tokens[tn].type == Token.LC)
		{
			tn++;
			max--;
		}

		while (tn < max)
		{
			Token t = tokens[tn++];
			
//			if (t.type != Token.NAME)
//				throw new Throwable("Invalid JSON in config file.  Expected member name on line " + t.lineNum);

			String name = t.value_.intern();
			
			t = tokens[tn++];
			if (t.type != Token.COLON)
				throw new Throwable("Invalid JSON in config file.  Expected : on line " + t.lineNum);
			
			t = tokens[tn];
//			if (t.type != Token.LC)
//				throw new Throwable("Invalid JSON in config file.  Expected { on line " + t.lineNum);

			int last = match(tokens, tn);
			if (name == "patterns")
				readPatterns(tokens, tn+1, last-1);
			else if (name == "searches")
				readSearches(tokens, tn+1, last-1);
			else if (name == "default")
			{
				defaultSearch = tokens[tn].value_;
				last = tn + 1;
			}
			else if (name == "options")
			{
				getDefaultOptions(tokens[tn].value_);
				last = tn + 1;
			}
			else
				System.out.println("Skipping unknown config section " + name + ".");
			
			tn = last+1;		// skip the comma
		}
	
		return true;	
	}
	
	/////////////////////////////////////////////////////////////////////
	public static int match(Token[] tokens, int tn)
		throws Throwable
	{
		int nesting   = 1;
		int searchFor = 0;
		Token opener  = tokens[tn++];

		if (opener.type == Token.LP)
			searchFor = Token.RP;
		else if (opener.type == Token.LC)
			searchFor = Token.RC;
		else if (opener.type == Token.LB)
			searchFor = Token.RB;
		else
			return tn;
		
		if (searchFor == 0)
			return tn;
		
		while (tn < tokens.length)
		{
			Token t = tokens[tn++];
			if (t.type == searchFor)
				nesting--;
			if (t.type == opener.type)
				nesting++;

			if (nesting == 0)
				return tn;
		}
		
		throw new Throwable("Unmatched " + opener.value_ + " on line " + opener.lineNum + " " + opener.charOnLine);
	}

	/////////////////////////////////////////////////////////////////////
	public static void HandleFile(String name, Config cfg, String regex, Boolean ignoreFilter)
		throws IOException, Throwable
	{
		File f = new File(name);
		
		if (!ignoreFilter)
		{
			if (cfg != null && cfg.excludes != null)
			{
				for (int i = 0; i < cfg.excludes.size(); i++)
				{
					java.util.regex.Pattern p = (java.util.regex.Pattern)cfg.excludes.get(i);

					java.util.regex.Matcher m = p.matcher(f.getName());

					if (m.matches())
						return;
				}
			}
		}

		if (f.isDirectory())
		{
			String[] files = f.list();
			if (files != null)
			{
				for (int i = 0; i < files.length; i++)
				{
					String oneName = name + File.separator + files[i];
					
					if (!recursive)
					{
						File f2 = new File(oneName);
						if (f2.isDirectory())
							continue;
					}
					
					HandleFile(oneName, cfg, regex, false);
				}
			}
		}
		else
		{
			boolean doFile = ignoreFilter;
			for (int i = 0; !doFile && i < filePatterns.size(); i++)
			{
				java.util.regex.Pattern p = (java.util.regex.Pattern)filePatterns.get(i);

				java.util.regex.Matcher m = p.matcher(f.getName());
				if (m.matches())
					doFile = true;
			}
			
			if (doFile)
			{
				try
				{
					ProcessFile(f, cfg, regex);
				}
				catch (Throwable e)
				{
					if (quiet)
						throw e;

					System.out.println(e);
					e.printStackTrace();
				}
			}
		}
	}
	
	/////////////////////////////////////////////////////////////////////
	public static String getLinePrefix(String file, int lineNum)
	{
		String output = "";

		if (showFile)
			output += file;

		if (showLineNum)
		{
			if (devStudio && showFile)
				output += "(" + lineNum + ")";
			else
			{
				if (showFile)
					output += ", ";
				output += "line " + lineNum;
			}
		}

		if (showFile || showLineNum)
			output += ": ";
			
		return output;
	}
	
	// line vs. match
	// invert vs not
	// context
	// line: show whatever lines, once each  // distinguish line w/ match vs. line w/o match?
	// match: show the matches.  Context may cause some overlap
	// line -v: show whatever lines, once each
	// match -v: show tokens, line number for each line
	
	/////////////////////////////////////////////////////////////////////
	public static void invert(int[] a)
	{
		for (int i = 0; i < a.length; i++)
		{
			if (a[i] == 0)
				a[i] = 2;
			else
				a[i] = 0;
		}
	}
	
	/////////////////////////////////////////////////////////////////////
	public static void addContext(int[] a)
	{
		for (int i = 0; i < a.length; i++)
		{
			if (a[i] == 2)
			{
				for (int j = i - showBefore; j <= i + showAfter; j++)
				{
					if (j > 0 && j < a.length && a[j] != 2)
						a[j] = 1;
				}
			}
			
			while (i < a.length && a[i] == 2)
				i++;
		}
	}

	/////////////////////////////////////////////////////////////////////
	public static String showTokens(String file, char[] filedata, Token[] tokens, int[] show)
	{
		String output = "";

		int lastStart = 0;
		
		for (int i = 0; i < show.length; i++)
		{
			if (show[i] != 0)
			{
				Token t = tokens[i];
				output += getLinePrefix(file, t.lineNum);
				
				int start = t.offset-1;
				if (i > 0 && show[i-1] != 0)
					start = t.lineStart;

				while (show[i] != 0 && tokens[i].lineNum == t.lineNum)
				{
					t = tokens[i];
					i++;
				}

				int end = t.end;
				if ((i+1) < show.length && show[i+1] != 0)
					end = getLineEnd(filedata, t);
				
				output += new String(filedata, start, end - start - 1) + "\n";
			}
		}

		return output;
	}
	
	/////////////////////////////////////////////////////////////////////
	public static String showTokens2(String file, char[] filedata, Token[] tokens, int[] show, boolean outer)
	{
		String output = "";
		int start, end, lineNum;
		
		for (int i = 0; i < show.length; i++)
		{
			if (show[i] == 0)
				continue;

			// 1. Get start & end for a contiguous block of displayed tokens
			start = i;
			while (i < show.length && show[i] != 0)
				i++;

			i--;
			end = i;

			// 2. Convert it to a character start & character end
			if (outer)	// outer == true -> include text starting at the end of the previous token
			{
				// start = 0 (first token) means we want to start at character 0
				if (start == 0)
				{
					lineNum = 1;
				}
				else
				{
					Token t = tokens[start-1];
					lineNum = t.lineNum;
					start = t.end2;
				}

				if (end+1 < show.length)
					end = tokens[end+1].start;
				else
					end = filedata.length;
			}
			else
			{
				Token t = tokens[start];
				start = t.start;
				lineNum = t.lineNum;
				
				end = tokens[end].end2;
			}

			if (filedata[start] == '\n')
				start++;
			if (filedata[end-1] == '\n')
				end--;

			// 3. Write it out, prefixing each line with the line prefix
			int current = start;
			if (!outer)
				output += getLinePrefix(file, lineNum);
				
			while (current <= end)
			{
				if (current == end || filedata[current] == '\n')
				{
					if (outer)
						output += getLinePrefix(file, lineNum);
					output += new String(filedata, start, current-start) + "\n";
					start = current+1;
					lineNum++;
				}

				current++;
			}
		}

		return output;
	}

	/////////////////////////////////////////////////////////////////////
	public static String showLines(String file, char[] filedata, Token[] tokens, int[] show)
	{
		String output = "";

		int lineNum = 1;
		int lineStart = 0;

		for (int i = 0; i < filedata.length; i++)
		{
			if (filedata[i] != '\n')
				continue;

			if (show[lineNum] != 0)
			{
				output += getLinePrefix(file, lineNum);
				output += new String(filedata, lineStart, i - lineStart) + "\n";
			}

			lineNum++;
			lineStart = i+1;
		}

		if ((show[lineNum] != 0) && lineStart < filedata.length)
		{
			output += getLinePrefix(file, lineNum);
			output += new String(filedata, lineStart, filedata.length - lineStart);
		}

		return output;
	}
	
	/////////////////////////////////////////////////////////////////////
	public static int getTokenOnLine(Token[] tokens, int lineNumber, int startAt)
	{
		while (startAt < tokens.length)
		{
			if (tokens[startAt].lineNum == lineNumber)
				break;

			startAt++;
		}
		
		return startAt;
	}

	/////////////////////////////////////////////////////////////////////
	public static int getLineEnd(char[] filedata, Token t)
	{
		int i = t.end;
		while (i < filedata.length && filedata[i] != '\n')
			i++;
		
		return i;
	}

	/////////////////////////////////////////////////////////////////////
	public static String getFileName(File f, int type)
		throws Throwable
	{
		if (type == 0)
			return f.getName();

		String name = f.getCanonicalPath();

		if (type == 3)
			return name;
		
		String relativeTo = searchRoot;
		if (type == 1)
			relativeTo = System.getProperty("user.dir");
		
		String[] n1 = name.split(File.separator);
		String[] r1 = relativeTo.split(File.separator);

		int i = 0;
		while (i < n1.length && i < r1.length && (n1[i].compareTo(r1[i]) == 0))
			i++;

		name = "";
		String sep = "";
		int j = i;
		while (j < n1.length)
		{
			name = name + sep + n1[j++];
			sep = File.separator;
		}
		
		j = i;
		while (j++ < r1.length)
			name = ".." + File.separator + name;
		
		return name;
	}
	
	/////////////////////////////////////////////////////////////////////
	public static void ProcessFile(File f, Config cfg, String regex)
		throws Throwable
	{
		String file;
		String output = "";
		char[]	filedata;

		file = getFileName(f, showFilePath);

		int len = (int)f.length();
		filedata = new char[len];
		FileReader fr = new FileReader(f);
		len = fr.read(filedata, 0, len);
		fr.close();

		int count = 0;
		int lastLineOutput = -1;

		// The file length (from file.length()) may not match the number
		// of characters read - the file length is in bytes, but the file
		// can use UTF-8 encoding, meaning there can be fewer characters
		// than bytes in the file.  This line re-allocates the filedata
		// array so it will only include file data, with no left over space.
		filedata = (new String(filedata, 0, len)).toCharArray();
		
		Token[] tokenArray = getTokens(filedata);
		
		JsMatcher m = new JsMatcher(tokenArray, filedata);
		JsPattern p = (new Maker()).makePattern(m, regex, extensions, 0, matchTokenType, insensitive);
		if (!p.printing)
			p.printing = p.printit = true;

		int[] tokensToShow = new int[tokenArray.length];
		int[] linesToShow  = new int[countLines(filedata)+1];
		
		while (m.match(p, true))
		{
			ArrayList matches = new ArrayList();
			p.getMatches(matches);

			count += matches.size();

			if (hideMatch)
				continue;
				
			for (int i = 0; i < matches.size(); i++)
			{
				Match match = (Match)matches.get(i);

				for (int j = match.first; j <= match.last; j++)
					tokensToShow[j] = 2;

				for (int j = tokenArray[match.first].lineNum; j <= tokenArray[match.last].lineNum; j++)
					linesToShow[j] = 2;
				
				if (showMatch && !invertMatch)
				{
					addContext(tokensToShow);
					
					output += showTokens2(file, filedata, tokenArray, tokensToShow, false);
					tokensToShow = new int[tokenArray.length];
				}
			}

			p.reset();
		}

		if (invertMatch)
		{
			invert(tokensToShow);
			invert(linesToShow);
		}
		
		addContext(tokensToShow);
		addContext(linesToShow);

		if (!hideMatch)
		{
			if (showMatch)
			{
				if (invertMatch)
					output += showTokens2(file, filedata, tokenArray, tokensToShow, true);
			}
			else
				output += showLines(file, filedata, tokenArray, linesToShow);
		}
			
		if (listFiles && (count > 0 || showZeroCount))
		{
			file = getFileName(f, listFilePath);
			
			System.out.print(file);
			if (showCount)
				System.out.print(":" + count);
			if (output.length() > 0)
				System.out.print(":");
			System.out.println();
		}

		totalCount += count;
		
		System.out.print(output);
		//if (listFiles && count > 0 && output.length() > 0)
		if (output.length() > 0)
			System.out.println();
	}

	////////////////////////////////////////////////////////
	public static int countLines(char[] filedata)
	{
		int numLines = 1;
		int offset = 0;
		while (offset < filedata.length)
			if (filedata[offset++] == '\n')
				numLines++;

		return numLines;
	}
	
	////////////////////////////////////////////////////////
	public static String getLines(char[] filedata, int start, int count)
	{
		int ln = 0;
		int offset = 0;
		int firstChar = -1;
		
		while (count > 0 && offset < filedata.length)
		{
			if (ln == start && firstChar == -1)
				firstChar = offset;
			
			if (filedata[offset] == '\n')
			{
				if (firstChar == -1)
					ln++;
				else
				{
					count--;
					if (count == 0)
						break;
				}
			}
			
			offset++;
		}
		
		return new String(filedata, firstChar, offset - firstChar);
	}
	
	////////////////////////////////////////////////////////
	public static Token[] getTokens(char[] src)
	{
		Tokenizer tz = new Tokenizer(src);

		Token t;
		
		ArrayList tokens = new ArrayList();
		while ((t = tz.getToken()) != null)
		{
//			if ((t.type != Token.COMMENT) && (t.type != 502))
				tokens.add(t);
		}

		Token[] tokenArray = new Token[tokens.size()];
		for (int i = 0; i < tokens.size(); i++)
		    tokenArray[i] = (Token)tokens.get(i);

		return tokenArray;
	}

	////////////////////////////////////////////////////////
	public static void showHelp(String type)
	{
		// Escape sequences from http://www.mit.edu/~vona/VonaUtils/vona/terminal/VT100_Escape_Codes.html
		byte[] b = { 27 };
		String underline = new String(b) + "[4m";
		String bold      = new String(b) + "[1m";
		String normal    = new String(b) + "[0m";

		String usage = bold + "Usage:" + normal + "\n" +
						" jsgrep [OPTION | FILE | CONFIG]... PATTERN [OPTION | FILE | CONFIG]...\n\n" +
						"Search for tokenized PATTERN in each tokenized FILE.\n\n" +
						bold + "Description:" + normal + "\n" +
						"jsgrep is similar to regular grep, except it works on a Javascript token stream.\n" +
						"This means it's insensitive to whitespace and it won't find stuff in comments or\n" +
						"strings, and it won't find parts of a name when you want to find the whole name.\n\n" +
						"jsgrep tries to be convenient to use.  Options and filenames can go at the\n" +
						"beginning of the command line, or at the end of the command line, or both.\n" +
						"It's recursive by default, and it will only search .js files.  It allows you to\n" +
						"define macros for commonly used PATTERNs and search sets for commonly used\n" +
						"FILEs.  I have defined macros to find function declarations, to find which\n" +
						"modules use a specified module, to find which module defines a function, to\n" +
						"find references to a variable inside a function, and others.\n";

		String help =	"jsgrep --macros    Show all macros defined in ~/.jsgrep.cfg\n" +
						"jsgrep --cmdline        all command line options\n" +
						"jsgrep --config         description of ~/.jsgrep.cfg\n" +
						"jsgrep --regex          help on regular expressions\n" +
						"jsgrep --bugs           bugs and limitations\n" +
						"jsgrep --help           all help\n";

		String cmdline = bold + "Options:" + normal + "\n" +
 						"Regexp selection and interpretation:\n" +
 						"  -i            ignore case distinctions in names and strings\n" +
 						"  -i-           don't ignore case distinctions in names and strings\n" +
 						"  -t            ignore token type when matching\n" +
 						"  -t-           don't ignore token type when matching\n" +
 						"  -v            select non-matching lines or tokens\n" +
 						"  -v-           select matching lines or tokens\n\n" +
 						"Miscellaneous:\n" +
 						"  -g            show command line arguments\n" +
 						"  -g-           don't show command line arguments\n" +
 						"  -o            show options being used\n" +
 						"  -o-           don't show options being used\n" +
 						"  -s            show the parsed search pattern\n" +
 						"  -s-           don't show the search pattern\n" +
 						"  -x            exit after showing arguments, options and search pattern\n" +
 						"  -x-           don't exit after showing arguments, options and search pattern\n\n" +
						"Input control:" + "\n" +  
						"  -fp FILEPAT   also search filenames matching FILEPAT" + "\n" +  
						"  -fp- FILEPAT  like -fp, but overrides patterns in the config file" + "\n" +  
						"  -r            recurse into subdirectories" + "\n" +  
						"  -r-           don't recurse into subdirectories\n" + "\n" +  
						"Output control:" + "\n" +  
						"  -c         only print a count of matches per FILE" + "\n" +  
						"  -c+        print the count of matches even if it's 0" + "\n" +  
						"  -c-        don't print a count of matches per FILE" + "\n" +  
						"  -d         format output for DevStudio" + "\n" +  
						"  -h         suppress the prefixing filename on output" + "\n" +  
						"  -h+        print the filename for each match" + "\n" +  
						"  -ha        print the absolute filename" + "\n" +  
						"  -hc        print the filename relative to the current directory" + "\n" +  
						"  -hr        print the filename relative to the search root" + "\n" +  
						"  -l         only print filenames containing matches" + "\n" +  
						"  -l-        don't show filenames for matching files" + "\n" +  
						"  -la        show the absolute filename" + "\n" +  
						"  -lc        show the filename relative to the current directory" + "\n" +  
						"  -lr        show the filename relative to the search root" + "\n" +  
						"  -m         only show the matching tokens" + "\n" +  
						"  -m-        don't show matches" + "\n" +  
						"  -m+        show the entire line for each match" + "\n" +  
						"  -n         show line numbers with output lines" + "\n" +  
						"  -n-        don't show line numbers" + "\n" +  
						"  -p         show filenames relative to the current directory" + "\n" +  
						"  -p-        only show file names - don't show the path" + "\n" +  
						"  -pa        show absolute filenames" + "\n" +  
						"  -pc        show filenames relative to the current directory" + "\n" +  
						"  -pr        show filenames relative to the search root" + "\n" +  
						"  -q         suppress extraneous output" + "\n" +  
						"  -q-        don't suppress extraneous output" + "\n" +  
						"  " + "\n" +  
						"Context control:" + "\n" +  
						"  -b NUM     print NUM lines of leading context" + "\n" +  
						"  -a NUM     print NUM lines of trailing context" + "\n";

		String regex = bold + "Expressions:" + normal + "\n" +
						".          any single token\n" +
						"X | Y      token X or Y\n" +
						"X?         0 or 1 copies of X, greedy\n" +
						"X+         1 or more copies of X, greedy\n" +
						"X*         0 or more copies of X, greedy\n" +
						"X{n,m}     n to m copies of X, greeedy\n" +
						"X{n,}      n or more copies of X, greedy\n" +
						"X{n}       Exactly n copies of X\n" +
 						"X??, X+? X*?, X{n,m}?, X{n,}?\n" +
 						"           As above, but reluctant (not greedy)\n" +
 						"(! X Y)    not X and not Y\n" +
 						"(X Y)      capture X & Y\n" +
 						"\\n         reference to capture #n\n" +
 						"C:n        n should capture a single (, [ or {.  This will match the ), ] or }\n" +
 						"           which closes the captured symbol.\n" +
 						"(? X Y)    non-capturing list of X followed by Y\n" +
 						"NAME       any name token\n" +
 						"STRING     any string token\n" +
 						"REGEXP     any regex token\n" +
 						"NUMBER     any number token\n" +
 						"/REGEX/    a (regular) regular expression for NAME tokens.  /[sg]et/ will match\n" +
 						"           the token 'get' or the token 'set'\n" +
 						"/'REGEX'/  a regular expression for STRING tokens\n" +
 						"'X'        matches the string 'X'.  This matches the value of the string so you\n" +
 						"           you don't have to worry about the string delimiter - 'X' matches both\n" +
 						"           \"X\" and 'X', and '\"' matches both '\"' and \"\\\"\"\n" +
 						"\\X         the token X.  This is used to escape regex characters - \\( matches\n" +
 						"           the left paren, rather than introducing a capturing group.\n" +
 						"X%         marks X as the pattern of interest.  The rest of the expression\n" +
 						"           becomes an assertion and is not considered part of the match.\n" +
 						"anything else\n" +
 						"           the javascript token (e.g. 'return' matches the return token.)\n\n" +
 						bold + "Examples:" + normal + "\n" +
 						"Find the declaration of a function named 'set':\n" +
 						"(? function set ) | (? set = function ) | (? set : function )\n\n" +
 						"Find the declaration and argument list of a function named 'set':\n" +
 						"(? function set ) | (? set = function ) | (? set : function ) (\\() .*? C:1\n\n" +
 						"Show the names of all functions\n" +
 						"(? function NAME% ) | (? NAME% = function ) | (? NAME% : function )\n";

		String config = bold + "Configuration:" + normal + "\n" +  
						"  jsgrep uses ~/.jsgrep.cfg as a configuration file.  This is a JSON file which\n" +  
						"can contain:\n" +  
						"{\n" +  
						"    // default command line options\n" +  
						"    options: \"\",\n" +  
						"\n" +  
						"    // Macros\n" +  
						"    patterns:\n" +  
						"    {\n" +  
						"        // A simple macro.  'macro1' is replaced with 'pattern'\n" +  
						"        macro1:  \"pattern\",\n" +  
						"\n" +  
						"        // A macro with substitution.  'pattern' is a regular expression.\n" +  
						"        // Anything captured in pattern gets substituted into 'match'\n" +  
						"        macro2:\n" +  
						"        {\n" +  
						"            pattern: \"(REGEX)\",\n" +  
						"            match:   \"\\1\"\n" +  
						"        },\n" +  
						"\n" +  
						"        // A macro to find function declarations\n" +  
						"        F:\n" +  
						"        {\n" +  
						"            pattern: \"(.)\",\n" +  
						"            match:   \"(? function \1) | (? \1 : function) | (? \1 = function)\"\n" +  
						"        }\n" +  
						"    },\n" +  
						"\n" +  
						"    // Search directories\n" +  
						"    searches:\n" +  
						"    {\n" +  
						"        // The name of the search.  Type this on the command line to use\n" +  
						"        // this set of directories\n" +  
						"        search1:\n" +  
						"        {\n" +  
						"            // The root directory.  This isn't required, it just saves typing\n" +  
						"            // in the directory list\n" +  
						"            root:   \"root\",\n" +  
						"\n" +  
						"            // A string or array of strings of directories to search\n" +  
						"            directories:\n" +  
						"            [\n" +  
						"                \"dir1\",\n" +  
						"                \"dir2\"\n" +  
						"            ],\n" +  
						"\n" +  
						"            // A list of patterns for filenames.  These are 'file patterns',\n" +  
						"            // not regular expressions.  '*' matches any number of characters,\n" +  
						"            // '?' matches a single character, and '.' matches the literal '.'.\n" +  
						"            // This is a string or an array of strings.\n" +  
						"            filepatterns:    \"*.js\",\n" +  
						"\n" +  
						"            // A string or array of strings of files or directories to exclude\n" +  
						"            exclude:\n" +  
						"            [\n" +  
						"                \"*-debug.js\",\n" +  
						"                \"*-min.js\"\n" +  
						"            ]\n" +  
						"        }\n" +  
						"\n" +  
						"        // Search the current directory\n" +  
						"        current:\n" +  
						"        {\n" +  
						"            root:	\".\"\n" +  
						"        },\n" +  
						"    },\n" +  
						"\n" +  
						"    default: \"current\"\n" +  
						"}\n";

		String bugs = bold + "Bugs and Limitations:" + normal + "\n" +  
						"  I wrote jsgrep in ten minute chunks during builds and smoke tests.  I'm sure\n" +  
						"it has a lot of bugs because I never had time to think about anything when I was\n" +  
						"writing it.\n" +  
						"\n" +  
						"Some specific limitations include:\n" +  
						"* It's very unforgiving of errors.  If you use an invalid expression, it will\n" +  
						"  throw an exception.\n" +  
						"* The handling of command line arguments is less than ideal.  Javascript uses\n" +  
						"  some regular expression special characters heavily, and they all need to be\n" +  
						"  escaped.  Many of the characters are also used by the shell, which makes\n" +  
						"  things very confusing (to me, at least.)  -g and -s options mitigate the\n" +  
						"  confusion.\n" +  
						"* The shell will expand filenames in the command line.\n" +  
						"* Reluctant repeat is extremely slow.  If you can get away with it, it may\n" +  
						"  be faster to use a greedy repeat, even if it has to backtrack over a lot of\n" +  
						"  tokens.\n" +  
						"* Limited regex matching.  REGEXP will match any regular expression, but you\n" +  
						"  can't match against the contents of the expression.  I.e. you can't search\n" +
						"  for only Javascript regular expressions that contain 'script'\n" +  
						"* Limited number matching.  Numbers should be evaluated and the values should\n" +  
						"  be compared.  \n" +  
						"* Only '\" and \\ are un-escaped in strings.\n" +  
						"* Macros can't be overloaded.  I'd like to have \"R:(NAME)\" search for\n" +  
						"  references to NAME, and \"R:(NAME1):(NAME2)\" search for references to NAME1\n" +  
						"  inside a function named NAME2.\n" +  
						"\n" +  
						"Report bugs to <sfrancisx@yahoo.com>\n";

		String macros = bold + "Macros:" + normal + "\n";

		for (int i = 0; i < ordered_extensions.size(); i++)
		{
			Extension ext = (Extension)ordered_extensions.get(i);
			if (ext.comment != null)
				macros = macros + ext.comment + "\n";
		}
		
		if (type == "--macros")
			System.out.println(macros);
		else if (type == "--cmdline")
			System.out.println(cmdline);
		else if (type == "--config")
			System.out.println(config);
		else if (type == "--regex")
			System.out.println(regex);
		else if (type == "--bugs")
			System.out.println(bugs);
		else if (type == "--help")
		{
			System.out.println(usage);
			System.out.println(regex);
			System.out.println(cmdline);
			System.out.println(config);
			System.out.println(macros);
			System.out.println(bugs);
		}
		else
		{
			System.out.println(usage);
			System.out.println(help);
		}
	}
}
