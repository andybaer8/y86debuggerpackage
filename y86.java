package y86debuggerpackage;

import java.io.BufferedReader;
import java.io.DataInputStream;

import java.io.FileInputStream;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import javax.swing.JOptionPane;

public class y86 {
	static boolean zf;//Zero flag
	static boolean of;//Overflow flag
	static boolean sf;//sign flag
	
	static boolean b = false;
	static boolean e = false;
	static int pc;//program counter
	/* A map that stores our declared labels. The key is the name of the label, and the value is the
	 * index in data_arr that the label points to.
	 */
	public static TreeMap<String, Integer> label_map = new TreeMap<String,Integer>();
	
	/*Our LL of commands to exec*/
	static CommandNode commands = null;
	
	//Our memory model for this project.
	public static Integer[] data_arr = new Integer[4096];
	
	public static ArrayList<String> ref = new ArrayList<String>();
	static int[] registers = new int[8];
	public static ArrayStack stack = new ArrayStack();
	
	public static HashMap<String, CommandNode> jumps = new HashMap<String, CommandNode>();
	static Scanner sc = new Scanner(System.in);
	@SuppressWarnings("static-access")
	public static void main(String[] args) throws Exception {
		
		System.out.println("Thanks for using Andy's Y86 Debugger. Assembly I/0 will be here on the terminal," +
				"everything else will be on JOptionPane.");
		
		ref.add("%eax");
		ref.add("%ebx");
		ref.add("%ecx");
		ref.add("%edx");
		ref.add("%esi");
		ref.add("%edi");
		ref.add("%esp");
		ref.add("%ebp");
		
		
		
		//JOptionPane.showMessageDialog(null,y86.class.getClassLoader().getResource("y86.java"));
		try{
			  // Open the file that is the first 
			  // command line parameter
			
			FileInputStream fstream = new FileInputStream("prog.txt");
			  // Get the object of DataInputStream
			  DataInputStream in = new DataInputStream(fstream);
			  BufferedReader br = new BufferedReader(new InputStreamReader(in)); 
		        
			 
			 
			  String strLine;
			  //Here, we're parsing memory declarations.
			 while ((!(strLine = br.readLine()).matches("#INSTRUCTIONS\\s+")) &&
					  strLine != null)   {
				  
				 strLine = clean(strLine);
				// System.out.println(strLine);
				  if (strLine.startsWith(".pos")) {
					  //We're declaring a location for the label to be stored
					  String[] split = strLine.split(" "); 
					  int pos_index;
					  if (split[1].contains("x")) { //This is a hex number)
						  pos_index = Integer.parseInt(split[1],16);
					  }
					  else {
						  pos_index = Integer.parseInt(split[1]);
					  }
					  strLine = br.readLine();
					  
					  //Now, we're at the line with the label and .long
					  split = strLine.split(":| ");
					  //System.out.println(split[0]);//split[] now has the label name, .long, and the init val
					  label_map.put(split[0], pos_index);
					  int long_num;
					  if (split[2].contains("x")) { //This is a hex number)
						  long_num = Integer.parseInt(split[2],16);
					  }
					  else {
						  long_num = Integer.parseInt(split[2]);
					  }
					  data_arr[pos_index] = long_num;
				  }
				  
				  
			  }
			  
			  //Now, we're parsing instructions
			  while ((strLine = br.readLine())!=null) {
			  strLine = strLine.trim();
			  CommandNode new_node = processCMD(strLine);
			  commands = insert(new_node);
			  }
			  
			  //Close the input stream
		
			  br.close();
			    }catch (Exception e){//Catch exception if any
			  JOptionPane.showMessageDialog(null,"Syntax Error, make sure you follow the readme");
			  }
	
		/*Iterate through a second time, checking for labels*/
		CommandNode curr = commands;
		while(curr != null) {
			if (curr.getLabel() != null) {//current node has a label
				jumps.put(curr.getLabel(), curr); 
			}
			curr = curr.next;
		}
		/*Iterates a third time, setting the jnext values of the jump statements.*/
		curr = commands;
		
		while(curr != null) {
			
			if (curr.getName()!=null && (curr.getName().startsWith("j") || 
					curr.getName().equals("call"))) {
				curr.setJnext(jumps.get(curr.getParamOne())); 
			}
			curr = curr.next;
		}
		
		/*Now we can start executing*/
		curr = commands;
		int counter = 0;
		while (true) {
			
			boolean jump = false;
			if (curr == null || curr.getName()==null) {
				return;
			}
			if(curr.type == 'A'){
				if(curr.subtype == 0)
				{
					//irmovl
					if(curr.paramOne.startsWith("$"))
					{
						int num = 0;
						if (curr.paramOne.contains("x")) {//is a hex number
							 num = Integer.parseInt(curr.paramOne.substring(3), 16);
						}
						else {
						 num = new Integer(curr.paramOne.substring(1));
						
						}
						registers[ref.indexOf(curr.paramTwo)] = num;
					}
					else
					{ //Means we're loading a label into a register
						//load label (mem at 0) into register
						//registers[ref.indexOf(curr.paramTwo)] = data_arr[label_map.get(curr.getParamOne())];
						if (!label_map.containsKey(curr.paramOne)) {
							System.out.println("Debugger error, you're trying to load an undeclared label");
							System.exit(1);
						}
						else {
							//irmovl array,%eax
							//eax = the address of 'array' (aka it's index in the data_arr
							registers[ref.indexOf(curr.paramTwo)] = label_map.get(curr.paramOne);
						}
					}
				}
				else if(curr.subtype == 1)
				{
					//rrmovl
					registers[ref.indexOf(curr.paramTwo)] = registers[ref.indexOf(curr.paramOne)];
					stack.esp = registers[6];
				}
				else if(curr.subtype == 2)//mrmovl
				{
					int index;
					if(curr.getParamOne().matches("%esp|%ebp")) {//means we have a (register) in param1
						
						registers[ref.indexOf(curr.paramTwo)] = 
								 stack.stack[registers[ref.indexOf(curr.paramOne)]+curr.getMemOffset()].value;
					}
					else if(curr.getParamOne().matches("%eax|%ebx|%ecx|%edx|%edi|%esi")) {
						
						registers[ref.indexOf(curr.paramTwo)] =
								data_arr[registers[ref.indexOf(curr.paramOne)]+curr.getMemOffset()];
					}					//ref.indexof(eax) is 0. We need to add the pos.
					else {
					/*mrmovl, param1 is the label, param2 is the reg. */
					 index = label_map.get(curr.getParamOne());
					 registers[ref.indexOf(curr.paramTwo)] = data_arr[index];
					}
					
				}
				else if(curr.subtype == 3)//rmmovl
				{
					
					if(curr.getParamTwo().matches("%esp|%ebp")) { 
						if (stack.stack[registers[ref.indexOf(curr.paramTwo)]+curr.getMemOffset()] == null) {
							stack.stack[registers[ref.indexOf(curr.paramTwo)]+curr.getMemOffset()] = 
									new StackObject(curr.paramOne, -1);
						}
						 stack.stack[registers[ref.indexOf(curr.paramTwo)]+curr.getMemOffset()].value =
						 registers[ref.indexOf(curr.paramOne)]; 
					}
					else if(curr.getParamTwo().matches("%eax|%ebx|%ecx|%edx|%edi|%esi")) {
						data_arr[registers[ref.indexOf(curr.paramTwo)]+curr.getMemOffset()] =
								 registers[ref.indexOf(curr.paramOne)]; 	
					}
					else {
					//rmmovl, param1 is the reg, param2 is the label
					int arr_index = label_map.get(curr.getParamTwo());
					data_arr[arr_index] = registers[ref.indexOf(curr.paramOne)];
					
				}
			}
			}
			else if(curr.type == 'B'){
				if(curr.subtype == 0)
				{
					//rdch
					Object in = sc.next();
					in = parse(in);
					registers[ref.indexOf(curr.paramOne)]  = (Character)in;
					
				}
				else if(curr.subtype == 1)
				{
					//rdint
					Object in = sc.next();
					in = parse(in);
					Integer in2 =Integer.parseInt(in.toString());
					if (in2 == 13) {
						/*This part is needed because in Java, hitting the enter key
						 * is a carriage return instead of a newline.
						 */
						in = 10;
					}
					
					registers[ref.indexOf(curr.paramOne)] = in2;

				}
				else if(curr.subtype == 2)
				{
					//wrch
					System.out.print((char)registers[ref.indexOf(curr.paramOne)]);
				}
				else if(curr.subtype == 3)
				{
					//wrint
					
					System.out.print((int)registers[ref.indexOf(curr.paramOne)]);
				}
			}
			else if(curr.type == 'C'){
				boolean negative; //Is the register negative? Used to determine the sign flag
				if (registers[ref.indexOf(curr.paramTwo)] < 0) {
					negative = true;
				}
				else { negative = false; }
				if(curr.subtype == 0)
				{
					//add
					long test = 0;
					test = registers[ref.indexOf(curr.paramOne)] + registers[ref.indexOf(curr.paramTwo)];
					if(test > Integer.MAX_VALUE || test < Integer.MIN_VALUE)
						of = true;
					else
						of = false;
					registers[ref.indexOf(curr.paramTwo)] = registers[ref.indexOf(curr.paramOne)] + registers[ref.indexOf(curr.paramTwo)];	
				}
				else if(curr.subtype == 1)
				{//subl
					long test = 0;
					test = registers[ref.indexOf(curr.paramTwo)] - registers[ref.indexOf(curr.paramOne)];
					if(test > Integer.MAX_VALUE || test < Integer.MIN_VALUE)
						of = true;
					else
						of = false;
					registers[ref.indexOf(curr.paramTwo)] = registers[ref.indexOf(curr.paramTwo)] - registers[ref.indexOf(curr.paramOne)];
				}
				else if(curr.subtype == 2)
				{
					//andl
					registers[ref.indexOf(curr.paramTwo)] = registers[ref.indexOf(curr.paramOne)] & registers[ref.indexOf(curr.paramTwo)];
				}
				else if(curr.subtype == 3)
				{
					//xorl
					registers[ref.indexOf(curr.paramTwo)] = registers[ref.indexOf(curr.paramOne)] ^ registers[ref.indexOf(curr.paramTwo)];
				}
				else if(curr.subtype == 4)
				{
					//multl
					long test = 0;
					test = registers[ref.indexOf(curr.paramOne)] * registers[ref.indexOf(curr.paramTwo)];
					if(test > Integer.MAX_VALUE || test < Integer.MIN_VALUE)
						of = true;
					else
						of = false;
					registers[ref.indexOf(curr.paramTwo)] = registers[ref.indexOf(curr.paramOne)] * registers[ref.indexOf(curr.paramTwo)];
				}
				else if(curr.subtype == 5)
				{
					//divl
					registers[ref.indexOf(curr.paramTwo)] = registers[ref.indexOf(curr.paramOne)] / registers[ref.indexOf(curr.paramTwo)];
				}
				else if(curr.subtype == 6)
				{
					//modl
					registers[ref.indexOf(curr.paramTwo)] = registers[ref.indexOf(curr.paramTwo)] % registers[ref.indexOf(curr.paramOne)];
				}
				if(registers[ref.indexOf(curr.paramTwo)]==0)
					zf = true;
				else
					zf = false;
				if (registers[ref.indexOf(curr.paramTwo)] < 0) {
					if(negative == false)
						sf = true;
					else
						sf = false;
				}
				else if (registers[ref.indexOf(curr.paramTwo)] > 0) {
					if(negative==true)
						sf = true;
					else
						sf = false;
				}
				
				
			}
		    else if(curr.type == 'D'){
		    	
		        if(curr.subtype == 0)
		        {
		           //jmp
		        	jump = true;
		        }
		        else if(curr.subtype == 1)
		        {
		            //jle
		        	jump = ((sf^of)|zf);
		        }
		        else if(curr.subtype == 2)
		        {
		            //jl
		        	jump = (sf^of);
		        }
		        else if(curr.subtype == 3)
		        {
		            //je
		        	jump = zf;
		        }
		        else if(curr.subtype == 4)
		        {
		            //jne
		        	jump = !zf;
		        }
		        else if(curr.subtype == 5)
		        {
		            //jge
		        	jump = (!(sf^of));
		        }
		        else if(curr.subtype == 6)
		        {
		            //jg
		        	jump = (!(sf^of)&zf);
		        }
		        else if(curr.subtype == 7)
		        {
		            //call
		        	jump = true;
		        	StackObject so = new StackObject("RetAddr", curr.next.pc);
		        	stack.push(so);
		        	
		        	
		        }
		        
		        
			}
		    else if(curr.type == 'E'){
				if(curr.subtype == 0)
				{
					StackObject so = new StackObject(curr.paramOne, registers[ref.indexOf(curr.paramOne)]);
					stack.push(so);
					
				}
				else if(curr.subtype == 1)
				{
			
					registers[ref.indexOf(curr.paramOne)] =  stack.pop().value;
					
				}
			}
			
		    else if(curr.getName().equals("ret")) {
		    	if (!stack.top().name.equals("RetAddr")) {
					System.out.println("Just letting you know that what you're popping" +
							"off the stack isn't a return address. If you're getting infinite loops," +
							"this could be why.");
					
					
				}
		    	curr.next = find( stack.pop().value);
		    	
		    }
			
		    else if(curr.getName().equals("halt")) {
		    	
		    	System.out.println("\nStopped in " + counter + " steps at PC = " + curr.pc + ". Exeption 'HLT'");
		    	System.out.print("Press any key to exit.\n");
		    	
		    	String halt_str = sc.nextLine();
		    	if (halt_str.equals("any key")) {
		    		JOptionPane.showMessageDialog(null, "You're very good at following instructions :3");
		    	}
		    	System.exit(1);
		    	
		    }
		    
			/*This is all in the main while loop.
			 * This is what we want to do here:
			 * 1) The program prints the line that JUST GOT EXECUTED
			 * 2) Prompts the user for a command
			 * 3) If the command is "b", it skips steps 1-3 unless a breakpoint is found.
			 */
			
			if (curr.isBreak == true) {
				b = false;
				String uc;
				
				while (b == false && e == false) {
					uc = JOptionPane.showInputDialog(curr.getText()+"\nInput user command:");
					
					process_uc(uc);
					
				}
			}
		   
			if(jump == true) {
		    	curr = curr.jnext;
		    }
			else {
			curr = curr.next;
			}
			
			/*Checks if 100,000 instructions have been reached. If it has,
			 * it ends the program.
			 */
			if (counter >= 100000) {
				System.out.println("\nAOK");
				process_uc("r");
				process_uc("l");
				process_uc("d");
			
				process_uc("s");
				System.exit(1);
			}
		counter++;
		}
			
	}//End of main method

	

	private static Object parse(Object in) {
		Object o;
		String str = in.toString();
		if (str.matches("^[0-9]+")) {
			o = new Integer(str);
		}
		else {
			o = new Character(str.charAt(0));
		}
		return o;
	}



	//Removes comments and trailing whitespaces
	private static String clean(String strLine) {
		String cleaned = strLine;
		int pound = strLine.indexOf('#');
		if (pound != -1) {
		 cleaned = strLine.substring(0,pound);
		}
		cleaned.trim();
		return cleaned;
		
	}


//Processing the user command for the debugger
	private static void process_uc(String uc) {
		 if (uc.equals("h")) {
			 
			 JOptionPane.showMessageDialog(null, "h - show help" + 
					 	"\nr - show registers\nf - show flags\ns - show stack\nd - show data array" + 
					 "\nl - show memory labels\nb - skip to next bp\nx - exit\n e - end of program\nc - credits");
			
		}
		 else if (uc.equals("c")) {
			 JOptionPane.showMessageDialog(null,"Andy Baer, 2012, andybaer8@gmail.com");
		 }
		 else if(uc.equals("x")) {
			System.exit(1);
		}
		 else if(uc.equals("e")) {
			 e = true;
		 }
		else if(uc.equals("d")) {
			String d_str = "";
			for (int x = 0; x < data_arr.length; x++) {
				if (data_arr[x] != null) {
					d_str += ("[" + x + "]" + " " + data_arr[x] + "\n");
				}
			}
			JOptionPane.showMessageDialog(null, d_str);
		}
		else if(uc.equals("l")) {
			Iterator<Map.Entry<String,Integer>> iter = label_map.entrySet().iterator();
			Map.Entry<String,Integer> curr_entry;
			String l_str = "";
			while (iter.hasNext()) {
				curr_entry = iter.next();
				l_str += (curr_entry.getKey() + "=" + data_arr[curr_entry.getValue()] + "\n");
			}
			JOptionPane.showMessageDialog(null, l_str);
		}
		
		else if(uc.equals("r")) {
			JOptionPane.showMessageDialog(null,"%eax " + registers[0] +
			"\n%ebx " + registers[1] +
			"\n%ecx " + registers[2] +
			"\n%edx " + registers[3] +
		    "\n%esi " + registers[4] +
			"\n%edi " + registers[5] +
			"\n%esp " + registers[6] +
			"\n%ebp " + registers[7]);
			
		}
		
		else if(uc.equals("f")) {
			JOptionPane.showMessageDialog(null, "Zero flag " + zf + "\nSign flag " + sf + "\nOverflow flag " + of);
		}
		
		else if(uc.equals("s")) {
			String stack_str = "Top of stack:\n";
			
			for (int x = 23; stack.stack[x] != null; x--) {
				stack_str += (stack.stack[x] + "\n");
			}
			JOptionPane.showMessageDialog(null, stack_str);
		}
		else if(uc.equals("b")) {
			b = true;
		}
		else if(uc.equals("n")) {
			JOptionPane.showMessageDialog(null, "nextline isn't working at the moment. Sorry!");
		}
		
		else {
			JOptionPane.showMessageDialog(null,"Improper command");
		}
	}

/*Parses the command, generates a CommandNode.*/
public static CommandNode processCMD(String command_string) {
	CommandNode com = new CommandNode();
        
       

        
		if (command_string.contains("/bp")) {
			com.isBreak = true;
		}
		else {
			com.isBreak = false;
		}
        com.setText(command_string);
        command_string =clean(command_string);
        //System.out.println(command_string);
        int colon_index = command_string.indexOf(":"); /*Optimization here: We have a value that needs to be used twice
		( indexof(":") ). Instead of calculating it twice, we calculate it once 
		and then use it twice.*/
         if (colon_index != -1) {//This means we have a label
        	
        	
            com.setLabel(command_string.substring(0,colon_index).trim());
            command_string = command_string.substring(colon_index+1).trim();
        	
        }
        else { com.setLabel(null); }
        //The label assignment (or lack thereof) is already taken care of, so all we need to do here is parse the actual instruction.
         //Instructions can have 0, 1, or 2 args.
        String[] parts = command_string.split("\\s+",2);
        String instruction_name = parts[0].trim();
       
        com.setName(instruction_name);
        String[] args;
        
        /*All the move instructions have 2 arguments.*/
        if (instruction_name.equals("irmovl")) {
            args = parts[1].split("\\s*,\\s*");
            com.setType('A');
            com.setSubtype(0);
            com.setParamOne(args[0].trim());
            com.setParamTwo(args[1].trim());	
        }
        else if(instruction_name.equals("rrmovl")) {
        	args = parts[1].split("\\s*,\\s*");
            com.setType('A');
            com.setSubtype(1);
            com.setParamOne(args[0].trim());
            com.setParamTwo(args[1].trim());
        }
        
       
        else if(instruction_name.equals("mrmovl")) {
        	com.setType('A');
            com.setSubtype(2);
            com.setMemOffset(0);
            args = parts[1].split("\\s*,\\s*");
            String mos = null;
            if (args[0].contains("(")) {//We have parenthesis, so we have a mem offset
            	mos = args[0].substring(0,args[0].indexOf('('));
            	com.setMemOffset(Integer.parseInt(mos)/4);
            }
            
            
            
            int paren_index = args[0].indexOf('%');
            if (paren_index != -1) {
        	com.setParamOne(args[0].substring(paren_index, paren_index+4));
            } else {
            	com.setParamOne(args[0].trim());
            }
            com.setParamTwo(args[1].trim());
        }
        else if(instruction_name.equals("rmmovl")) {
            args = parts[1].split("\\s*,\\s*");
            com.setType('A');
            com.setSubtype(3);
            
            String mos = null;
            //args[0] = %edx in rmmovl %edx,0(%eax)
            if (args[1].contains("(")) {//We have parenthesis, so we have a mem offset
            	mos = args[1].substring(0,args[1].indexOf('('));
            	com.setMemOffset(Integer.parseInt(mos)/4);
            }
            
            int paren_index = args[1].indexOf('%');
            if (paren_index != -1) {
        	com.setParamTwo(args[1].substring(paren_index, paren_index+4));
            } else {
            	com.setParamTwo(args[1].trim());
            }
            com.setParamOne(args[0].trim());
        }
        /*The I/O instructions have 1 argument that is always a register.*/
        else if(instruction_name.equals("rdch")) {
            
            com.setType('B');
            com.setSubtype(0);
            com.setParamOne(parts[1].trim());
            com.setParamTwo(null);
        }
        else if(instruction_name.equals("rdint")) {
            com.setType('B');
            com.setSubtype(1);
            com.setParamOne(parts[1].trim());
            com.setParamTwo(null);
        }
        else if(instruction_name.equals("wrch")) {
            com.setType('B');
            com.setSubtype(2);
            com.setParamOne(parts[1].trim());
            com.setParamTwo(null);
        }
        else if(instruction_name.equals("wrint")) {
            com.setType('B');
            com.setSubtype(3);
            com.setParamOne(parts[1].trim());
            com.setParamTwo(null);
        }
        /*The arithmatic operations have 2 arguments, both registers.*/
        else if(instruction_name.equals("addl")) {
            args = parts[1].split("\\s*,\\s*");
            com.setType('C');
            com.setSubtype(0);
            com.setParamOne(args[0].trim());
            com.setParamTwo(args[1].trim());
        }
        else if(instruction_name.equals("subl")) {
            args = parts[1].split("\\s*,\\s*");
            com.setType('C');
            com.setSubtype(1);
            com.setParamOne(args[0].trim());
            com.setParamTwo(args[1].trim());
        }
        else if(instruction_name.equals("andl")) {
            args = parts[1].split("\\s*,\\s*");
            com.setType('C');
            com.setSubtype(2);
            com.setParamOne(args[0].trim());
            com.setParamTwo(args[1].trim());
        }
        else if(instruction_name.equals("xorl")) {
            args = parts[1].split("\\s*,\\s*");
            com.setType('C');
            com.setSubtype(3);
            com.setParamOne(args[0].trim());
            com.setParamTwo(args[1].trim());
        }
        else if(instruction_name.equals("multl")) {
            args = parts[1].split("\\s*,\\s*");
            com.setType('C');
            com.setSubtype(4);
            com.setParamOne(args[0].trim());
            com.setParamTwo(args[1].trim());
        }
        else if(instruction_name.equals("divl")) {
            args = parts[1].split("\\s*,\\s*");
            com.setType('C');
            com.setSubtype(5);
            com.setParamOne(args[0].trim());
            com.setParamTwo(args[1].trim());
        }
        else if(instruction_name.equals("modl")) {
            args = parts[1].split("\\s*,\\s*");
            com.setType('C');
            com.setSubtype(6);
            com.setParamOne(args[0].trim());
            com.setParamTwo(args[1].trim());
        }
        
        /*The jumps have 1 argument which is a label.*/
        else if(instruction_name.equals("jmp")) {
            com.setType('D');
            com.setSubtype(0);
            com.setParamOne(parts[1].trim());
            com.setParamTwo(null);
        }
        else if(instruction_name.equals("jle")) {
            com.setType('D');
            com.setSubtype(1);
            com.setParamOne(parts[1].trim());
            com.setParamTwo(null);
        }
        else if(instruction_name.equals("jl")) {
            com.setType('D');
            com.setSubtype(2);
            com.setParamOne(parts[1].trim());
            com.setParamTwo(null);
        }
        else if(instruction_name.equals("je")) {
            com.setType('D');
            com.setSubtype(3);
            com.setParamOne(parts[1].trim());
            com.setParamTwo(null);
        }
        else if(instruction_name.equals("jne")) {
            com.setType('D');
            com.setSubtype(4);
            com.setParamOne(parts[1].trim());
            com.setParamTwo(null);
        }
        else if(instruction_name.equals("jge")) {
            com.setType('D');
            com.setSubtype(5);
            com.setParamOne(parts[1].trim());
            com.setParamTwo(null);
        }
        else if(instruction_name.equals("jg")) {
            com.setType('D');
            com.setSubtype(6);
            com.setParamOne(parts[1].trim());
            com.setParamTwo(null);
        }
        else if(instruction_name.equals("call")) {
            com.setType('D');
            com.setSubtype(7);
            com.setParamOne(parts[1].trim());
            com.setParamTwo(null);
        }
        
        /*Push and pop have 1 arg, a register.*/
        else if(instruction_name.equals("pushl")) {
            com.setType('E');
            com.setSubtype(0);
            com.setParamOne(parts[1].trim());
            com.setParamTwo(null);
        }
        else if(instruction_name.equals("popl")) {
            com.setType('E');
            com.setSubtype(1);
            com.setParamOne(parts[1].trim());
            com.setParamTwo(null);
        }
        else if(instruction_name.equals("ret")) {
            com.setType('F');
            com.setSubtype(0);
            com.setParamOne(null);
            com.setParamTwo(null);
        }
        else if(instruction_name.equals("halt")) {
            com.setType('F');
            com.setSubtype(1);
            com.setParamOne(null);
            com.setParamTwo(null);
        }
        
        else if(instruction_name.equals(".pos")) {
        	com.setType('G');
            com.setSubtype(0);
            com.setParamOne(parts[1].trim());
            com.setParamTwo(null);
        }
        else if(instruction_name.equals(".long")) {
        	com.setType('G');
            com.setSubtype(1);
            com.setParamOne(parts[1].trim());
            com.setParamTwo(null);
        }
       // System.out.println(com.toString());
        com.pc = pc;
        pc++;
       // System.out.println(com.toString());
        
        return com;
}




/*Adds a CN to the end of the list, returning the head pointer to the new list*/
public static CommandNode insert(CommandNode new_node) {
	if(new_node == null) {
		return null;
	}
	CommandNode ret = commands;
	CommandNode curr = commands;
	if(commands == null) {
		return new CommandNode(new_node);
	}
	while(curr.next != null) {
		curr = curr.next;
	}
	curr.next = new CommandNode(new_node);
	return ret;
}

/*Finds and returns the CommandNode with the given program counter*/
public static CommandNode find(int pc) {
	CommandNode curr = commands;
	while(curr != null) {
		if (curr.pc == pc) {
			return curr;
		}
		 curr = curr.next;
	}
	return null;
}


}
