package y86debuggerpackage;

public class StackObject {
	String name;
	int value;

public StackObject(String name, int value) {
	this.name = name;
	this.value = value;
}
public StackObject() {
	this.name = "";
	this.value = -1;
}
public String toString() {
	return this.name + " " + this.value;
}
}