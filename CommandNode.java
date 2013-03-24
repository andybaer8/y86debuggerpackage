package y86debuggerpackage;



public class CommandNode {
	 String text;//The full text of the line, ie, the command being read.
	 String name;//The name of the command being run
	 String label;//Will be != null if a label is present on the line
	 String paramOne;//first arg
	 String paramTwo;//second arg
	 char type;
	 
	 int subtype;
	 int pos;//If the previous node is .pos, it will put that value here.
	 int pc;//The program counter for this instruction
	 int memOffset; //Will be 0 unless we're trying to access an offset
	 
	 boolean isBreak;//Will be 1 if the node is a breakpoint
	 
	 CommandNode next;//The next line to execute
	 CommandNode jnext;//The next node if a jump passes (or a function call)
	 
	 public CommandNode(){
	 }
	 public CommandNode(CommandNode new_node) {
		 this.isBreak = new_node.isBreak;
		 this.memOffset = new_node.memOffset;
		 this.pos = new_node.pos;
		 this.text = new_node.text;
		 this.jnext = new_node.jnext;
		 this.next = new_node.next;
		 this.name = new_node.name;
		 this.label = new_node.label;
		 this.type = new_node.type;
		 this.subtype = new_node.subtype;
		 this.pc = new_node.pc;
		 this.paramOne = new_node.paramOne;
		 this.paramTwo = new_node.paramTwo;
	 }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public char getType() {
		return type;
	}

	public void setType(char type) {
		this.type = type;
	}

	public int getSubtype() {
		return subtype;
	}

	public void setSubtype(int subtype) {
		this.subtype = subtype;
	}

	public String getParamOne() {
		return paramOne;
	}

	public void setParamOne(String paramOne) {
		this.paramOne = paramOne;
	}

	public String getParamTwo() {
		return paramTwo;
	}

	public void setParamTwo(String paramTwo) {
		this.paramTwo = paramTwo;
	}

	public int getPc() {
		return pc;
	}

	public void setPc(int pc) {
		this.pc = pc;
	}

	public int getMemOffset() {
		return memOffset;
	}

	public void setMemOffset(int memOffset) {
		this.memOffset = memOffset;
	}

	public CommandNode getNext() {
		return next;
	}

	public void setNext(CommandNode next) {
		this.next = next;
	}

	public CommandNode getJnext() {
		return jnext;
	}

	public void setJnext(CommandNode jnext) {
		this.jnext = jnext;
	}
	
	public String toString() {
		return "Name " + name + "\nLabel " + label + "\nparam1 " + paramOne + "\nparam2 " + paramTwo + "\npc " + pc;/* +
				"\nnext " + next.name + "\njnext" + jnext.name;*/
	}
}