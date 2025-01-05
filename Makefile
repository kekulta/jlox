all: $(wildcard src/*.java)
	@javac -cp src -d build src/tech/kekulta/lox/Lox.java
	
repl:
	@cd build; java tech/kekulta/lox/Lox

run:
	@cd build; java tech/kekulta/lox/Lox ../test/Test.lox

ast:
	@cd src; javac tech/kekulta/util/GenerateAst.java 
	@cd src; java tech/kekulta/util/GenerateAst tech/kekulta/lox
	@cd src; rm tech/kekulta/util/GenerateAst.class

printAst:
	@cd src; javac tech/kekulta/util/AstPrinter.java 
	@cd src; java tech/kekulta/util/AstPrinter
