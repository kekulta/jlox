all: $(wildcard src/*.java)
	javac -cp src -d build src/tech/kekulta/lox/Lox.java
	
run:
	cd build; java tech/kekulta/lox/Lox
