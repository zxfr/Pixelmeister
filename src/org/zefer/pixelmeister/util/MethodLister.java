package org.zefer.pixelmeister.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.zefer.pixelmeister.device.itdb02.LCD;
import org.zefer.pixelmeister.device.itdb02.tinyFAT;
import org.zefer.pixelmeister.device.itdb02.uText;
import org.zefer.pixelmeister.device.pixels.Pixels;

public class MethodLister {
	
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("not enough args");
			System.exit(1);
		}
		
		String result = "";
		
		if ( "-utft".equals(args[0]) ) {
			result = initMethodSignaturesMap(1);
		}

		if ( "-ufat".equals(args[0]) ) {
			result = initMethodSignaturesMap(2);
		}

		if ( "-utext".equals(args[0]) ) {
			result = initMethodSignaturesMap(3);
		}

		if ( "-pixels".equals(args[0]) ) {
			result = initMethodSignaturesMap(4);
		}

		try {
			FileOutputStream file = new FileOutputStream(args[1]);
			file.write(result.getBytes());
			file.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String initMethodSignaturesMap( int mode ) {

		StringBuffer result = new StringBuffer();
		
        Class declaringClass;
        
        switch ( mode ) {
        	case 1: declaringClass = LCD.class; break;
        	case 2: declaringClass = tinyFAT.class; break;
        	case 3: declaringClass = uText.class; break;
        	case 4: declaringClass = Pixels.class; break;
        	default: declaringClass = LCD.class; break;
        }
		
		ClassLoader declaringClassLoader = declaringClass.getClassLoader();
	    Type declaringType = Type.getType(declaringClass);
	    String url = declaringType.getInternalName() + ".class";

	    InputStream classFileInputStream = declaringClassLoader.getResourceAsStream(url);
	    if (classFileInputStream == null) {
	        throw new IllegalArgumentException("The constructor's class loader cannot find the bytecode that defined the constructor's class (URL: " + url + ")");
	    }

	    ClassNode classNode;
	    try {
	        classNode = new ClassNode();
	        ClassReader classReader = new ClassReader(classFileInputStream);
	        classReader.accept(classNode, 0);
	    } catch ( IOException e ) {
	    	return "";
	    } finally {
	        try {
				classFileInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
				return "";
			}
	    }

	    List methods = classNode.methods;
	    Iterator ii = methods.iterator();
	    while (ii.hasNext()) {
	    	MethodNode method = (MethodNode)ii.next();
	    	if ( method.name.equals("<init>") ||
	    			/* method.name.startsWith("render") || // a render() call is implicitly present in enclosing Java code */
	    			(method.access & Opcodes.ACC_PUBLIC) == 0 || 
	    			(method.access & Opcodes.ACC_DEPRECATED) == 1) {
	    		continue;
	    	}

	    	String full = method.name + "(";
	    	String brief = method.name + "(";
	    	
	    	Type[] argumentTypes = Type.getArgumentTypes(method.desc);

            List localVariables = method.localVariables;
            
            Collections.sort(localVariables, new Comparator() {
            	public int compare(Object s1, Object s2) {
            		return ((LocalVariableNode)s1).index - ((LocalVariableNode)s2).index;
            	}
            });
            
            for (int i = 0; i < argumentTypes.length; i++) {
            	
           		full += "java.lang.String".equals(argumentTypes[i].getClassName()) ? "String" : argumentTypes[i].getClassName();
           		
           		if( localVariables.size() > 0 ) {
                    // The first local variable actually represents the "this" object in non-static methods
               		int corr = 0;
               		if ( "this".equals(((LocalVariableNode)localVariables.get(0)).name) ) {
               			corr = 1;
               		}
               		full += " " + ((LocalVariableNode)localVariables.get(i + corr)).name;
               		brief += ((LocalVariableNode)localVariables.get(i + corr)).name;
           		}
           		
           		if ( i < argumentTypes.length - 1) {
           			full += ", ";
           			brief += ", ";
           		}
            }
            String returnType = Type.getMethodType(method.desc).getReturnType().getClassName();
            if ("java.lang.String".equals(returnType)) {
            	returnType = "String";
            }
            full += ") : " + returnType;
            brief += ")";
            
            result.append( full ).append( "\t" ).append( brief ).append( "\n" );
	    }
	    
	    return result.toString();
	}
}
