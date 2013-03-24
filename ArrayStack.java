package y86debuggerpackage;

public class ArrayStack{

	   public final static int DEFAULT_CAPACITY = 24;
	   public StackObject stack[];     // The array that holds the stack
	   public static int esp;            // Index of top item on the stack

	   // Creates a stack with the default capacity
	   public ArrayStack (){
	      this(DEFAULT_CAPACITY);
	      esp = DEFAULT_CAPACITY;
	      y86.registers[6] = esp;
	 }

	   // Creates a stack with a user-specified capacity
	 public ArrayStack (int capacity){
	    if (capacity < 1)
	       throw new IllegalArgumentException (
	                      "Capacity must be > 0");
	    stack = new StackObject[capacity];
	    esp = capacity;
	    y86.registers[6] = esp;
	 }
	    
	 public boolean isEmpty(){
	     return esp == DEFAULT_CAPACITY;
	 }
	    
	 public boolean isFull(){
	    return esp == 0;
	 }
	    
	 public StackObject top() throws Exception{
	    if (isEmpty())
	       throw new Exception ("Top attempted on empty stack");
	    return stack[esp];
	 }

	 public StackObject pop() throws Exception{
	    if (isEmpty())
	       throw new Exception (
	                "Pop attempted on empty stack");
	    StackObject topItem = stack[esp];
	    stack[esp] = null; //permit garbage collection
	    esp++;
	    y86.registers[6] = esp;
	    return topItem;
	 }

	 public void push(StackObject item) throws Exception{
	    if (item == null)
	       throw new IllegalArgumentException ("Item is null");
	                            
	    if (isFull())
	       throw new Exception (
	                    "Push attempted on full stack");
	   esp--;
	    stack[esp] = item;
	    y86.registers[6] = esp;
	 }    
	    
	 public int size(){
	    return esp + 1;
	 }
	}