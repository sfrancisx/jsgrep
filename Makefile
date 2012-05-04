jsgrep: *.java
	rm -f *class
	javac jsgrep.java JsMatcher.java Tokenizer.java
	jar -cvmf manifest.inf jsgrep.jar *.class
