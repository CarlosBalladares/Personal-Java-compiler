# Personal Java compiler

This is a java compiler I created with a classmate Daniel May https://github.com/dcmay22
and guidance of professor Ronald Garcia for CPSC411 Compiler construction.

This project is a compiler that accepts simple Java programs. The features
supported are a subset of all the feature offered in the Java language.



## Console instructions

The repository is configured with maven to retrieve all necessary dependencies, as well as to run automatically the parser generator on build.  If you do not know how to use maven, please look at [this tutorial](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html).  Once you have installed maven on your machine, follow the following instructions (if you have setup an SSH key with github.  Otherwise, replace the `git clone` line with the correct URL):

    git clone git@github.com:ubc-cpsc411-2016w2/functions-starter.git
	cd functions-starter/
	mvn clean dependency:copy-dependencies
	mvn test

And voila! You have run your tests.  Maven should recompile your code every time you run `mvn test` if there are any changes.
