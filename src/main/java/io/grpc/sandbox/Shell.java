
package io.grpc.sandbox;

import java.lang.System;

/**
 * public keyword makes this class accessible globally.
 * By default, we'd only have access to this class within
 * the class & package... not within subclass/world.
 * 
 * src: https://docs.oracle.com/javase/tutorial/java/javaOO/accesscontrol.html
 */
public class Shell
{
    public static void main(String[] args)
    {
        System.out.println("Enter a command: ");
    }
}