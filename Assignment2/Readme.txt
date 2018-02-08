
Steps to Execute the Program:
Used two extra jars apart from the stanford lemmatizer jars. Added the two extra jars in "InformationRetrieval_assignemnet2\src" folder.
1) Copy all the files from folder "InformationRetrieval_assignemnet2\src" to your directory.
2)Place all the attached jar files,.java files and the attached stopwords file in one folder because I am compiling the attached jar files and stopwords file using ./jar_name (eg: ./commons-cli-1.4.jar)
3) Open Command Prompt/Putty and navigate to the folder where the files are located
4)use the below command to import all the standford lemmatizer jar files of csgrads path to my directory
   
   export corenlp=/usr/local/corenlp350 
	
5) Compile using the command

	javac -cp .:$corenlp/joda-time-2.3.jar:$corenlp/jollyday.jar:$corenlp/ejml-0.23.jar:$corenlp/xom-1.2.5.jar:$corenlp/javax.json.jar:$corenlp/stanford-corenlp-3.5.0.jar:$corenlp/stanford-corenlp-3.5.0-models.jar:./commons-cli-1.4.jar:./commons-lang3-3.0.jar MainClass.java

6) We need to supply input to the program, and we give it as->java <File> -path PATH_TO_CRANFIELD_DOCUMENTS -stop PATH_TO_STOPWORDS
   
   Run using the command
 java -cp .:$corenlp/joda-time-2.3.jar:$corenlp/jollyday.jar:$corenlp/ejml-0.23.jar:$corenlp/xom-1.2.5.jar:$corenlp/javax.json.jar:$corenlp/stanford-corenlp-3.5.0.jar:$corenlp/stanford-corenlp-3.5.0-models.jar:./commons-cli-1.4.jar:./commons-lang3-3.0.jar MainClass -path /people/cs/s/sanda/cs6322/Cranfield/ -stop Stopwords




